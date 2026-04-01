package com.example.ai_assist.repository

import com.example.ai_assist.model.AnalyzeResponse
import com.example.ai_assist.model.EndSessionRequest
import com.example.ai_assist.model.StartSessionRequest
import com.example.ai_assist.service.GameApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ChatRepository(private val apiService: GameApiService) {

    private val _analyzeResult = MutableStateFlow<AnalyzeResponse?>(null)
    val analyzeResult: StateFlow<AnalyzeResponse?> = _analyzeResult

    suspend fun startSession(sessionId: String): Boolean {
        return try {
            val response = apiService.startSession(StartSessionRequest(sessionId))
            response.status == "success"
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun analyzeImage(imageFile: File, sessionId: String) {
        try {
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            val sessionBody = sessionId.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.analyzeHand(body, sessionBody)
            _analyzeResult.value = response
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Rethrow so ViewModel can handle/show error
        }
    }

    suspend fun uploadAudio(audioFile: File, sessionId: String) {
        try {
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("audio", audioFile.name, requestFile)
            val sessionBody = sessionId.toRequestBody("text/plain".toMediaTypeOrNull())

            apiService.processAudio(body, sessionBody)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun endSession(sessionId: String) {
        try {
            apiService.endSession(EndSessionRequest(sessionId))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
