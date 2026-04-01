package com.rokid.mahjong.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RokidDeviceManager(private val context: Context) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    interface InteractionListener {
        fun onVoiceResult(text: String)
        fun onImageCaptured(path: String)
        fun onTouchpadTap()
        fun onTouchpadLongPress()
    }

    var listener: InteractionListener? = null

    fun takePhoto() {
        listener?.onImageCaptured("")
    }

    fun handleKeyEvent(keyCode: Int): Boolean {
        return false
    }
}
