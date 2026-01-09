package com.example.keyless_app.register

import androidx.lifecycle.ViewModel
import com.example.keyless_app.data.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _isRegistered = MutableStateFlow(mainRepository.isUserRegistered())
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    fun register(userName: String, secretHash: String) {
        mainRepository.saveCredentials(userName, secretHash)
        _isRegistered.value = true
    }

    fun logout() {
        mainRepository.clearCredentials()
        _isRegistered.value = false
    }
}