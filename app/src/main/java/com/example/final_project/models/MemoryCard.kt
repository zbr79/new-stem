package com.example.final_project.models

// Memmory Card detect handle face up and down card
data class MemoryCard(
    val identifier: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)
