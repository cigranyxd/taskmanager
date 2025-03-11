data class Kifizetesitem(
    val docId: String,
    val nev: String,
    val osszeg: Double,
    val period: String = "", // új mező a period értéknek
    var isChecked: Boolean = false
)
