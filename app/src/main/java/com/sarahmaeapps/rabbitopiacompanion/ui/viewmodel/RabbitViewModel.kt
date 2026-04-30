package com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarahmaeapps.rabbitopiacompanion.data.model.Rabbit
import com.sarahmaeapps.rabbitopiacompanion.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RabbitViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _rabbits = MutableStateFlow<List<Rabbit>>(emptyList())
    val rabbits: StateFlow<List<Rabbit>> = _rabbits

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _forSaleRabbits = MutableStateFlow<List<Rabbit>>(emptyList())
    val forSaleRabbits: StateFlow<List<Rabbit>> = _forSaleRabbits

    private val _wishListRabbits = MutableStateFlow<List<Rabbit>>(emptyList())
    val wishListRabbits: StateFlow<List<Rabbit>> = _wishListRabbits

    fun loadRabbits() {
        val email = repository.getCurrentUserEmail() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _rabbits.value = repository.getRabbits(email)
            _isLoading.value = false
        }
    }

    fun loadRabbitsForSale() {
        viewModelScope.launch {
            _isLoading.value = true
            val rabbits = repository.getRabbitsForSale()
            _forSaleRabbits.value = rabbits
            _isLoading.value = false
        }
    }

    fun loadWishList() {
        val email = repository.getCurrentUserEmail() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _wishListRabbits.value = repository.getWishList(email)
            _isLoading.value = false
        }
    }

    fun addToWishList(rabbit: Rabbit) {
        val email = repository.getCurrentUserEmail() ?: return
        viewModelScope.launch {
            repository.addToWishList(email, rabbit)
            loadWishList()
        }
    }

    fun updateRabbit(rabbit: Rabbit) {
        viewModelScope.launch {
            repository.updateRabbit(rabbit)
            loadRabbits() // Refresh list
        }
    }

    fun getRabbitById(id: String): Rabbit? {
        return _rabbits.value.find { it.id == id } ?: _forSaleRabbits.value.find { it.id == id } ?: _wishListRabbits.value.find { it.id == id }
    }
}
