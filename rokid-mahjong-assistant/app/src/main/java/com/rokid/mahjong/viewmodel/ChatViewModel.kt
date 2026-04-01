package com.rokid.mahjong.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rokid.mahjong.model.AnalyzeResponse
import com.rokid.mahjong.repository.ChatRepository
import com.rokid.mahjong.service.RokidDeviceManager
import com.rokid.mahjong.utils.MahjongMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatViewModel(
    private val repository: ChatRepository,
    private val deviceManager: RokidDeviceManager
) : ViewModel() {

    val analyzeResult: StateFlow<AnalyzeResponse?> = repository.analyzeResult

    val mappedResult = analyzeResult.map { response ->
        response?.let {
            AnalyzeResponse(
                userHand = MahjongMapper.mapListToUnicode(it.userHand),
                meldedTiles = MahjongMapper.mapListToUnicode(it.meldedTiles),
                suggestedPlay = MahjongMapper.mapToUnicode(it.suggestedPlay)
            )
        }
    }

    private var sessionId = UUID.randomUUID().toString()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    suspend fun startNewSession(): Boolean {
        sessionId = UUID.randomUUID().toString()
        return repository.startSession(sessionId)
    }

    fun endCurrentSession() {
        val currentId = sessionId
        viewModelScope.launch {
            repository.endSession(currentId)
        }
    }

    fun uploadPhoto(file: File) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.analyzeImage(file, sessionId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadAudio(file: File) {
        viewModelScope.launch {
            repository.uploadAudio(file, sessionId)
        }
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val deviceManager: RokidDeviceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, deviceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
