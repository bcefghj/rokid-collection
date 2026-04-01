package com.example.ai_assist.model

import com.google.gson.annotations.SerializedName

data class AnalyzeResponse(
    @SerializedName("user_hand") val userHand: List<String>,
    @SerializedName("melded_tiles") val meldedTiles: List<String> = emptyList(),
    @SerializedName("suggested_play") val suggestedPlay: String
)
