package com.zorindisplays.display.util

object CardAssetMapper {

    fun faceFile(card: String): String {
        // examples: "10♣", "K♠", "A♦"
        val suitChar = card.lastOrNull() ?: return "card_back.svg"
        val rankPart = card.dropLast(1)

        val rank = when (rankPart) {
            "J", "Q", "K", "A" -> rankPart.lowercase()
            else -> rankPart // 2..10
        }

        val suit = when (suitChar) {
            '♣' -> "c"
            '♦' -> "d"
            '♥' -> "h"
            '♠' -> "s"
            else -> "c"
        }

        return "card_${rank}${suit}.svg"
    }

    fun backFile(): String = "card_back.svg"
}
