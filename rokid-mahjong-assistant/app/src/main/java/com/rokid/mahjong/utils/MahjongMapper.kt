package com.rokid.mahjong.utils

object MahjongMapper {
    private val map = mapOf(
        "1m" to "🀇", "2m" to "🀈", "3m" to "🀉", "4m" to "🀊", "5m" to "🀋",
        "6m" to "🀌", "7m" to "🀍", "8m" to "🀎", "9m" to "🀏",
        "1p" to "🀙", "2p" to "🀚", "3p" to "🀛", "4p" to "🀜", "5p" to "🀝",
        "6p" to "🀞", "7p" to "🀟", "8p" to "🀠", "9p" to "🀡",
        "1s" to "🀐", "2s" to "🀑", "3s" to "🀒", "4s" to "🀓", "5s" to "🀔",
        "6s" to "🀕", "7s" to "🀖", "8s" to "🀗", "9s" to "🀘",
        "1z" to "🀀", "2z" to "🀁", "3z" to "🀂", "4z" to "🀃",
        "5z" to "🀆", "6z" to "🀅", "7z" to "🀄"
    )

    fun mapToUnicode(text: String): String {
        val expandedText = Regex("([0-9]+)([mpsz])").replace(text) { matchResult ->
            val digits = matchResult.groupValues[1]
            val suffix = matchResult.groupValues[2]
            digits.map { "$it$suffix" }.joinToString("")
        }

        var result = expandedText
        for ((code, unicode) in map) {
            result = result.replace(code, unicode)
        }
        return result
    }

    fun mapListToUnicode(codes: List<String>): List<String> {
        return codes.map { mapToUnicode(it) }
    }
}
