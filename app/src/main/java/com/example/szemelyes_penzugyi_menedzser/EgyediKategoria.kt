package com.example.szemelyes_penzugyi_menedzser

import androidx.annotation.Keep

@Keep
data class EgyediKategoria(
    var nev: String = "",
    var ikon: Int = 0,
    var tipus: String = "" // "Bevétel" vagy "Kiadás"
) {
    // Explicit üres konstruktor
    constructor() : this("", 0, "")
}
