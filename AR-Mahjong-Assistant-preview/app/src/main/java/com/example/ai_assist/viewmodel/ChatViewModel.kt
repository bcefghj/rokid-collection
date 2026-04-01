package com.example.ai_assist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai_assist.model.AnalyzeResponse
import com.example.ai_assist.repository.ChatRepository
import com.example.ai_assist.service.RayNeoDeviceManager
import com.example.ai_assist.utils.MahjongMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatViewModel(
    private val repository: ChatRepository,
    private val deviceManager: RayNeoDeviceManager
) : ViewModel() {

    // Expose mapped result
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

    init {
        deviceManager.listener = object : RayNeoDeviceManager.InteractionListener {
            override fun onVoiceResult(text: String) {
                // Not used
            }

            override fun onImageCaptured(path: String) {
                // Not used
            }

            override fun onTempleTouch() {}
            override fun onTempleLongPress() {}
        }
    }

    fun takePhoto() {
        deviceManager.takePhoto()
    }

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
                repository.analyzeImage(file, sessionId)
            } catch (e: Exception) {
                e.printStackTrace()
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
    private val deviceManager: RayNeoDeviceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, deviceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
