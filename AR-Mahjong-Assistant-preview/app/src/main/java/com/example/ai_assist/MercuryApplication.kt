package com.example.ai_assist

import android.app.Application
import com.ffalcon.mercury.android.sdk.MercurySDK

class MercuryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MercurySDK.init(this)
    }
}