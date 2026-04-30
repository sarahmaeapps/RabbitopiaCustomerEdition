package com.sarahmaeapps.rabbitopiacompanion.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.sarahmaeapps.rabbitopiacompanion.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun getCurrentUserName(): String? {
        return auth.currentUser?.displayName
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email?.lowercase()?.trim()
    }

    suspend fun getRabbits(email: String): List<Rabbit> {
        val emailLower = email.lowercase().trim()
        
        return try {
            // Get all sales for this customer
            val salesSnapshot = db.collection("sales")
                .whereEqualTo("customerId", emailLower)
                .get()
                .await()
            
            if (salesSnapshot.isEmpty) return emptyList()

            // Map sales to rabbits and set purchase date from sale record
            salesSnapshot.documents.map { saleDoc ->
                val rabbitId = saleDoc.getString("rabbitId") ?: ""
                val saleDate = saleDoc.getLong("date") ?: 0L
                val rabbit = getRabbitWithPedigree(rabbitId)
                
                // Fetch local overrides/notes
                val localDoc = db.collection("local_data")
                    .document(emailLower)
                    .collection("rabbits")
                    .document(rabbitId)
                    .get()
                    .await()
                
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val formattedDate = if (saleDate > 0) sdf.format(java.util.Date(saleDate)) else "Unknown"
                
                val baseRabbit = rabbit.copy(purchaseDate = formattedDate, status = "Purchased")
                
                if (localDoc.exists()) {
                    val localRabbit = mapDocumentToRabbit(localDoc)
                    baseRabbit.copy(
                        grade = localRabbit?.grade?.ifEmpty { baseRabbit.grade } ?: baseRabbit.grade,
                        arbaScore = localRabbit?.arbaScore?.ifEmpty { baseRabbit.arbaScore } ?: baseRabbit.arbaScore,
                        notes = localRabbit?.notes?.ifEmpty { baseRabbit.notes } ?: baseRabbit.notes,
                        weighIns = localRabbit?.weighIns?.ifEmpty { baseRabbit.weighIns } ?: baseRabbit.weighIns,
                        medicalRecords = localRabbit?.medicalRecords?.ifEmpty { baseRabbit.medicalRecords } ?: baseRabbit.medicalRecords,
                        sopScore = localRabbit?.sopScore?.ifEmpty { baseRabbit.sopScore } ?: baseRabbit.sopScore
                    )
                } else {
                    baseRabbit
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateRabbit(rabbit: Rabbit) {
        try {
            // Determine if it's a "My Rabbit" or "Wishlist" update
            // For simplicity, we'll allow updating the main 'rabbits' collection 
            // but also a 'local_rabbits' collection for customer-specific notes
            val email = getCurrentUserEmail() ?: return
            
            db.collection("local_data")
                .document(email)
                .collection("rabbits")
                .document(rabbit.id)
                .set(rabbit)
                .await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    private suspend fun getRabbitWithPedigree(rabbitId: String): Rabbit {
        val doc = db.collection("rabbits").document(rabbitId).get().await()
        val baseRabbit = mapDocumentToRabbit(doc) ?: Rabbit(id = rabbitId)
        
        // Fetch evaluations from 'sop_evaluations' collection
        val evaluations = try {
            // Case 1: Search by field 'rabbitId'
            val evalsSnapshot = db.collection("sop_evaluations")
                .whereEqualTo("rabbitId", rabbitId)
                .get()
                .await()
            
            val fromField = evalsSnapshot.documents.mapNotNull { evalDoc ->
                SopEvaluation(
                    date = evalDoc.getString("date") ?: "",
                    score = evalDoc.getString("score") ?: evalDoc.getLong("totalScore")?.toString() ?: "0",
                    checklist = (evalDoc.get("checklist") as? Map<String, String>) ?: emptyMap()
                )
            }

            // Case 2: Check if there's a subcollection under a doc named rabbitId
            val subSnapshot = db.collection("sop_evaluations")
                .document(rabbitId)
                .collection("evaluations")
                .get()
                .await()
            
            val fromSub = subSnapshot.documents.mapNotNull { evalDoc ->
                SopEvaluation(
                    date = evalDoc.getString("date") ?: evalDoc.id,
                    score = evalDoc.getString("score") ?: evalDoc.get("totalScore")?.toString() ?: evalDoc.get("score")?.toString() ?: "0",
                    checklist = (evalDoc.get("checklist") as? Map<String, String>) ?: emptyMap()
                )
            }

            // Case 3: Check the document itself (if evaluations are stored directly by rabbitId)
            val directDoc = db.collection("sop_evaluations").document(rabbitId).get().await()
            val fromDirect = if (directDoc.exists()) {
                listOf(SopEvaluation(
                    date = directDoc.getString("date") ?: "Latest",
                    score = directDoc.getString("score") ?: directDoc.get("totalScore")?.toString() ?: directDoc.get("score")?.toString() ?: "0",
                    checklist = (directDoc.get("checklist") as? Map<String, String>) ?: emptyMap()
                ))
            } else emptyList()

            (fromField + fromSub + fromDirect).distinctBy { it.date }.sortedByDescending { it.date }
        } catch (e: Exception) {
            emptyList()
        }

        val latestScore = if (evaluations.isNotEmpty()) {
            val score = evaluations.first().score
            if (score.contains("/")) score else "$score/100"
        } else {
            baseRabbit.sopScore.ifEmpty { "0/100" }
        }

        // Fetch parents recursively to 3rd generation
        return baseRabbit.copy(
            id = rabbitId,
            sopScore = latestScore,
            sopEvaluations = evaluations,
            pedigree = Pedigree(
                father = getRelative(doc.getString("fatherId") ?: doc.getString("sireId") ?: doc.getString("FatherId")),
                mother = getRelative(doc.getString("motherId") ?: doc.getString("damId") ?: doc.getString("MotherId"))
            )
        )
    }

    private suspend fun getRelative(id: String?, depth: Int = 1): RabbitRelative? {
        if (id == null || id.isEmpty() || depth > 2) return null
        val doc = db.collection("rabbits").document(id).get().await()
        if (!doc.exists()) return null
        
        return RabbitRelative(
            name = doc.getString("name") ?: "Unknown",
            father = getRelative(doc.getString("fatherId") ?: doc.getString("sireId") ?: doc.getString("FatherId"), depth + 1),
            mother = getRelative(doc.getString("motherId") ?: doc.getString("damId") ?: doc.getString("MotherId"), depth + 1)
        )
    }

    suspend fun getPurchases(email: String): List<Purchase> {
        val emailLower = email.lowercase().trim()
        
        return try {
            val snapshot = db.collection("sales")
                .whereEqualTo("customerId", emailLower)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { doc ->
                    val p = doc.toObject(Purchase::class.java)
                    val amount = doc.getDouble("amount") ?: doc.getDouble("price") ?: doc.getLong("salePrice")?.toDouble() ?: 0.0
                    p?.copy(id = doc.id, amount = amount)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getMessages(email: String): Flow<List<Message>> = callbackFlow {
        val emailLower = email.lowercase().trim()
        
        val registration = db.collection("messages")
            .document(emailLower)
            .collection("chat")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val m = doc.toObject(Message::class.java) ?: Message()
                            val text = doc.getString("text") ?: doc.getString("message") ?: ""
                            m.copy(id = doc.id, text = text)
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.timestamp }
                    trySend(messages)
                }
            }
            
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(email: String, message: Message) {
        val emailLower = email.lowercase().trim()
        val breederEmail = "rabbitopiafarm@gmail.com"
        val messageMap = mutableMapOf(
            "senderId" to emailLower,
            "receiverId" to breederEmail,
            "senderName" to message.senderName,
            "text" to message.text,
            "message" to message.text,
            "imageUrl" to message.imageUrl,
            "read" to false,
            "timestamp" to System.currentTimeMillis()
        )
        try {
            db.collection("messages")
                .document(emailLower)
                .collection("chat")
                .add(messageMap)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun canUploadImage(email: String): Boolean {
        val emailLower = email.lowercase().trim()
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        
        return try {
            val lastImageMessage = db.collection("messages")
                .document(emailLower)
                .collection("chat")
                .whereGreaterThan("timestamp", oneHourAgo)
                .get()
                .await()
            
            // Check if any message in the last hour has an image
            lastImageMessage.documents.none { it.contains("imageUrl") && it.getString("imageUrl") != null }
        } catch (e: Exception) {
            true // Allow if check fails
        }
    }

    suspend fun uploadMessageImage(email: String, uri: android.net.Uri): String {
        val fileName = "chat_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child("chat_images/${email.lowercase().trim()}/$fileName")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun getRabbitsForSale(): List<Rabbit> {
        return try {
            // 1. Look specifically in the 'forsale' collection as per the updated protocol
            val forsaleSnapshot = db.collection("forsale").get().await()
            val fromForsale = forsaleSnapshot.documents.map { doc ->
                getRabbitWithPedigree(doc.id) // Get full enriched data including SOPs
            }

            // 2. Keep the main 'rabbits' collection check as a fallback
            val mainSnapshot = db.collection("rabbits")
                .whereEqualTo("forSale", true)
                .get()
                .await()
            val fromMain = mainSnapshot.documents.map { doc ->
                getRabbitWithPedigree(doc.id)
            }

            (fromForsale + fromMain).distinctBy { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapDocumentToRabbit(doc: com.google.firebase.firestore.DocumentSnapshot): Rabbit? {
        if (!doc.exists()) return null
        return try {
            val salePriceVal = doc.get("salePrice")
            val priceVal = doc.get("price")
            val finalPrice = when (val p = salePriceVal ?: priceVal) {
                is Number -> p.toDouble()
                is String -> p.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val genCountVal = doc.get("genCount")
            val generationVal = doc.get("generation")
            val finalGen = when (val g = genCountVal ?: generationVal) {
                is Number -> g.toInt()
                is String -> g.toIntOrNull() ?: 1
                else -> 1
            }

            val scoreVal = doc.get("arbaScore") ?: doc.get("score")
            val sopScoreVal = doc.getString("sopScore") ?: ""
            
            // Map SOP evaluations if they exist
            val sopList = try {
                (doc.get("sopEvaluations") as? List<Map<String, Any>>)?.map { eval ->
                    SopEvaluation(
                        date = eval["date"] as? String ?: "",
                        score = eval["score"] as? String ?: "",
                        checklist = (eval["checklist"] as? Map<String, String>) ?: emptyMap()
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val dobVal = doc.get("dateOfBirth") ?: doc.get("birthDate")
            val birthDateStr = when (dobVal) {
                is Number -> {
                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(dobVal.toLong()))
                }
                is String -> dobVal
                else -> null
            }

            Rabbit(
                id = doc.id,
                name = doc.getString("name") ?: "",
                earTattooId = doc.getString("earTattoo") ?: doc.getString("earTattooId") ?: "",
                birthDate = birthDateStr,
                pictureUrl = doc.getString("imagePath") ?: doc.getString("pictureUrl"),
                isForSale = doc.getBoolean("forSale") ?: doc.getBoolean("isForSale") ?: false,
                price = finalPrice,
                arbaScore = scoreVal?.toString() ?: "",
                grade = doc.getString("grade") ?: "",
                breed = doc.getString("breed") ?: "",
                color = doc.getString("color") ?: "",
                sex = doc.getString("sex") ?: "",
                hutchId = doc.getString("hutchId") ?: "",
                generation = finalGen,
                status = doc.getString("status") ?: "",
                source = doc.getString("source") ?: "",
                notes = doc.getString("notes") ?: "",
                sopScore = sopScoreVal,
                sopEvaluations = sopList
            )
        } catch (e: Exception) {
            // Create a basic rabbit if full mapping fails to avoid empty screen
            Rabbit(
                id = doc.id,
                name = doc.getString("name") ?: "Unknown"
            )
        }
    }

    suspend fun getWishList(email: String): List<Rabbit> {
        return try {
            val emailLower = email.lowercase().trim()
            val snapshot = db.collection("wishlist")
                .document(emailLower)
                .collection("rabbits")
                .get()
                .await()
            
            snapshot.documents.map { doc ->
                getRabbitWithPedigree(doc.id) // Get latest data from source
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToWishList(email: String, rabbit: Rabbit) {
        try {
            val emailLower = email.lowercase().trim()
            db.collection("wishlist")
                .document(emailLower)
                .collection("rabbits")
                .document(rabbit.id)
                .set(mapOf("addedAt" to System.currentTimeMillis()))
                .await()
        } catch (e: Exception) {
            // Log error
        }
    }
}
