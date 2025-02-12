package com.example.szemelyes_penzugyi_menedzser

data class KifizetesItem(
    val docId: String,
    val nev: String,
    val osszeg: Double,
    var isChecked: Boolean = false
)
