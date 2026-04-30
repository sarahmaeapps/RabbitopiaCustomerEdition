package com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sarahmaeapps.rabbitopiacompanion.data.model.Purchase
import com.sarahmaeapps.rabbitopiacompanion.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PurchaseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseRepository()
    private val sharedPrefs = application.getSharedPreferences("local_purchases", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var firebasePurchases: List<Purchase> = emptyList()

    fun loadPurchases() {
        val email = repository.getCurrentUserEmail() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            firebasePurchases = repository.getPurchases(email)
            combineAndEmit()
            _isLoading.value = false
        }
    }

    private fun getLocalPurchases(): List<Purchase> {
        val json = sharedPrefs.getString("purchases", null) ?: return emptyList()
        val type = object : TypeToken<List<Purchase>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun combineAndEmit() {
        val local = getLocalPurchases()
        _purchases.value = firebasePurchases + local
    }

    fun addLocalPurchase(purchase: Purchase) {
        val currentLocal = getLocalPurchases().toMutableList()
        currentLocal.add(purchase.copy(isLocal = true, id = "local_${System.currentTimeMillis()}"))
        sharedPrefs.edit().putString("purchases", gson.toJson(currentLocal)).apply()
        combineAndEmit()
    }
}
