package com.sarahmaeapps.rabbitopiacompanion.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.sarahmaeapps.rabbitopiacompanion.data.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

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
            coroutineScope {
                salesSnapshot.documents.map { saleDoc ->
                    async {
                        val rabbitId = saleDoc.getString("rabbitId") ?: ""
                        val saleDate = saleDoc.getLong("date") ?: 0L
                        val rabbit = getFullRabbitData(rabbitId)
                        
                        // Fetch local overrides/notes
                        val localDoc = db.collection("local_data")
                            .document(emailLower)
                            .collection("rabbits")
                            .document(rabbitId)
                            .get()
                            .await()
                        
                        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
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
                }.awaitAll()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateRabbit(rabbit: Rabbit) {
        try {
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

    suspend fun getFullRabbitData(rabbitId: String): Rabbit {
        val doc = db.collection("rabbits").document(rabbitId).get().await()
        val baseRabbit = mapDocumentToRabbit(doc) ?: Rabbit(id = rabbitId)
        
        // Fetch evaluations
        val evaluations = fetchSopEvaluations(rabbitId)
        
        // Calculate average score if evaluations exist
        val averageScore = if (evaluations.isNotEmpty()) {
            val scores = evaluations.mapNotNull { it.score.replace("/100", "").toDoubleOrNull() }
            if (scores.isEmpty()) "0/100" else "${String.format(Locale.getDefault(), "%.1f", scores.average())}/100"
        } else {
            baseRabbit.sopScore.ifEmpty { "0/100" }
        }

        // Fetch weigh-ins
        val weighIns = try {
            db.collection("weigh_ins")
                .whereEqualTo("rabbitId", rabbitId)
                .get()
                .await()
                .documents.mapNotNull { d ->
                    val dateVal = d.get("date")
                    val dateStr = when (dateVal) {
                        is Number -> java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(java.util.Date(dateVal.toLong()))
                        is String -> dateVal
                        else -> ""
                    }
                    WeighIn(date = dateStr, weight = d.get("weight")?.toString() ?: "")
                }
        } catch (e: Exception) { emptyList() }

        // Fetch medical
        val medical = try {
            db.collection("medical")
                .whereEqualTo("rabbitId", rabbitId)
                .get()
                .await()
                .documents.mapNotNull { d ->
                    val dateVal = d.get("date")
                    val dateStr = when (dateVal) {
                        is Number -> java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(java.util.Date(dateVal.toLong()))
                        is String -> dateVal
                        else -> ""
                    }
                    MedicalRecord(
                        date = dateStr,
                        note = d.getString("treatment") ?: d.getString("notes") ?: ""
                    )
                }
        } catch (e: Exception) { emptyList() }

        return baseRabbit.copy(
            id = rabbitId,
            sopScore = averageScore,
            sopEvaluations = evaluations,
            weighIns = weighIns,
            medicalRecords = medical,
            pedigree = Pedigree(
                father = getRelative(doc.getString("fatherId") ?: doc.getString("sireId") ?: doc.getString("FatherId")),
                mother = getRelative(doc.getString("motherId") ?: doc.getString("damId") ?: doc.getString("MotherId"))
            )
        )
    }

    private suspend fun fetchSopEvaluations(rabbitId: String): List<SopEvaluation> {
        return try {
            val evalsSnapshot = db.collection("sop_evaluations")
                .whereEqualTo("rabbitId", rabbitId)
                .get()
                .await()
            
            evalsSnapshot.documents.mapNotNull { evalDoc ->
                val checklistRaw = evalDoc.get("checklist") as? Map<*, *>
                val checkedItemsRaw = evalDoc.get("checkedItems") as? List<*>
                
                val checklist = mutableMapOf<String, String>()
                checkedItemsRaw?.forEachIndexed { index, b -> checklist["Item $index"] = b.toString() }
                checklistRaw?.forEach { (k, v) -> checklist[k.toString()] = v.toString() }

                val dateVal = evalDoc.get("date")
                val dateStr = when (dateVal) {
                    is Number -> java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(java.util.Date(dateVal.toLong()))
                    is String -> dateVal
                    else -> evalDoc.id
                }

                SopEvaluation(
                    date = dateStr,
                    score = evalDoc.get("bodyScore")?.toString() ?: evalDoc.getString("score") ?: evalDoc.get("totalScore")?.toString() ?: "0",
                    checklist = checklist
                )
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            emptyList()
        }
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
            // Optimized parallel fetch
            val forsaleSnapshot = db.collection("forsale").get().await()
            coroutineScope {
                forsaleSnapshot.documents.map { doc ->
                    async {
                        val base = mapDocumentToRabbit(doc) ?: Rabbit(id = doc.id)
                        // Fetch latest SOP score for the list
                        val evaluations = fetchSopEvaluations(doc.id)
                        val latestScore = if (evaluations.isNotEmpty()) {
                            val score = evaluations.first().score
                            if (score.contains("/")) score else "$score/100"
                        } else "0/100"
                        base.copy(sopScore = latestScore)
                    }
                }.awaitAll()
            }
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
            
            val dobVal = doc.get("dateOfBirth") ?: doc.get("birthDate")
            val birthDateStr = when (dobVal) {
                is Number -> {
                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
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
                notes = doc.getString("notes") ?: ""
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
            
            coroutineScope {
                snapshot.documents.map { doc ->
                    async { getFullRabbitData(doc.id) }
                }.awaitAll()
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

    suspend fun getMarketplaceItems(): List<MarketplaceItem> {
        return try {
            val snapshot = db.collection("marketplace").get().await()
            snapshot.toObjects(MarketplaceItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
