package com.example.szemelyes_penzugyi_menedzser

import KategoriaAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class Kategoriak : AppCompatActivity() {

    // A beépített kategória ikon térkép – a kulcsokat normalizáljuk (kisbetűs, trim)
    val kategoriaIkonTerkep = mapOf(
        "egészség" to R.drawable.egeszseg_icon,
        "szabadidő" to R.drawable.szabadido_icon,
        "otthon" to R.drawable.otthon_icon,
        "kávézó" to R.drawable.kavezo_icon,
        "oktatás" to R.drawable.oktatas_icon,
        "ajándékok" to R.drawable.ajandekok_icon,
        "élelmiszerek" to R.drawable.elelmiszerek_icon,
        "család" to R.drawable.csalad_icon,
        "edzés" to R.drawable.edzes_icon,
        "közlekedés" to R.drawable.kozlekedes_icon,
        "egyéb" to R.drawable.egyeb_icon,
        "fizetési csekk" to R.drawable.szabadido_icon
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity_kategoriak.xml tartalmazza az időszakválasztó elemeket, a ListView-t stb.
        setContentView(R.layout.activity_kategoriak)

        // Betöltjük az egyedi kategóriákat (SharedPreferences-ből)
        EgyediKategoriak.load(this)

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val aktualisFelhasznalo = auth.currentUser
        val felhasznaloId = aktualisFelhasznalo?.uid ?: return

        // Időszak választó elemek
        findViewById<TextView>(R.id.NapFelirat).setOnClickListener {
            tranzakciokBetoltese(felhasznaloId, "Nap")
        }
        findViewById<TextView>(R.id.HetFelirat).setOnClickListener {
            tranzakciokBetoltese(felhasznaloId, "Het")
        }
        findViewById<TextView>(R.id.HonapFelirat).setOnClickListener {
            tranzakciokBetoltese(felhasznaloId, "Honap")
        }
        findViewById<TextView>(R.id.EvFelirat).setOnClickListener {
            tranzakciokBetoltese(felhasznaloId, "Ev")
        }

        // Alapértelmezetten a "Nap" időszak legyen kiválasztva
        tranzakciokBetoltese(felhasznaloId, "Nap")
    }

    /**
     * Lekéri az adott időszak tranzakcióit, majd összegzi és megjeleníti az adatokat.
     */
    private fun tranzakciokBetoltese(felhasznaloId: String, period: String) {
        tranzakciokLekerdezese(felhasznaloId, period) { tranzakciok ->
            val (bevetelMap, kiadasMap) = tranzakciokOsszegzese(tranzakciok)
            osszegzettAdatokMegjelenitese(bevetelMap, kiadasMap)
        }
    }

    /**
     * Az időszak kezdő és záró dátumát adja vissza (yyyy-mm-dd formátumban).
     */
    fun idoszakKezelo(period: String): Pair<String, String> {
        val today = LocalDate.now()
        return when (period) {
            "Nap" -> Pair(today.toString(), today.toString())
            "Het" -> {
                val start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val end = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                Pair(start.toString(), end.toString())
            }
            "Honap" -> {
                val start = today.withDayOfMonth(1)
                val end = today.withDayOfMonth(today.lengthOfMonth())
                Pair(start.toString(), end.toString())
            }
            "Ev" -> {
                val start = today.withDayOfYear(1)
                val end = today.withDayOfYear(today.lengthOfYear())
                Pair(start.toString(), end.toString())
            }
            else -> Pair("", "")
        }
    }

    fun tranzakciokLekerdezese(felhasznaloId: String, period: String, callback: (List<Tranzakcio>) -> Unit) {
        val (startDate, endDate) = idoszakKezelo(period)
        val firestore = FirebaseFirestore.getInstance()
        if (period == "Nap") {
            firestore.collection("users")
                .document(felhasznaloId)
                .collection("nap")
                .document(startDate)
                .get()
                .addOnSuccessListener { document ->
                    val tranzakciok = mutableListOf<Tranzakcio>()
                    if (document.exists()) {
                        val trxLista = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        trxLista.forEach { trx ->
                            val mennyiseg = (trx["mennyiseg"] as? Number)?.toFloat() ?: 0f
                            val tipus = trx["tipus"] as? String ?: ""
                            // Normalizáljuk a kategória nevet
                            val kategoria = (trx["kategoria"] as? String ?: "").trim().toLowerCase()
                            val leiras = trx["leiras"] as? String ?: ""
                            tranzakciok.add(Tranzakcio(kategoria, leiras, mennyiseg, tipus))
                        }
                    }
                    callback(tranzakciok)
                }
                .addOnFailureListener {
                    callback(emptyList())
                }
        } else {
            firestore.collection("users")
                .document(felhasznaloId)
                .collection("nap")
                .whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), startDate)
                .whereLessThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), endDate)
                .get()
                .addOnSuccessListener { documents ->
                    val tranzakciok = mutableListOf<Tranzakcio>()
                    documents.forEach { document ->
                        val trxLista = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        trxLista.forEach { trx ->
                            val mennyiseg = (trx["mennyiseg"] as? Number)?.toFloat() ?: 0f
                            val tipus = trx["tipus"] as? String ?: ""
                            val kategoria = (trx["kategoria"] as? String ?: "").trim().toLowerCase()
                            val leiras = trx["leiras"] as? String ?: ""
                            tranzakciok.add(Tranzakcio(kategoria, leiras, mennyiseg, tipus))
                        }
                    }
                    callback(tranzakciok)
                }
                .addOnFailureListener {
                    callback(emptyList())
                }
        }
    }

    /**
     * Összegzi a tranzakciókat kategóriák szerint.
     * Normalizáljuk a kategória neveket (kisbetűs, trimelt) a konzisztencia érdekében.
     * Visszaad egy párost: az első map a bevételek, a második a kiadások.
     */
    fun tranzakciokOsszegzese(tranzakciok: List<Tranzakcio>): Pair<Map<String, Float>, Map<String, Float>> {
        val bevetelMap = mutableMapOf<String, Float>()
        val kiadasMap = mutableMapOf<String, Float>()
        tranzakciok.forEach { trx ->
            val kulcs = trx.kategoria.trim().toLowerCase()
            when (trx.tipus) {
                "Bevétel" -> {
                    val aktualis = bevetelMap.getOrDefault(kulcs, 0f)
                    bevetelMap[kulcs] = aktualis + trx.mennyiseg
                }
                "Kiadás" -> {
                    val aktualis = kiadasMap.getOrDefault(kulcs, 0f)
                    kiadasMap[kulcs] = aktualis + trx.mennyiseg
                }
            }
        }
        return Pair(bevetelMap, kiadasMap)
    }

    private fun megjelenitoNev(normalizaltNev: String): String {
        // Ha custom kategória létezik ezzel a normalizált névvel, visszaadjuk az eredeti nevet
        val custom = EgyediKategoriak.kategoriak.find { it.nev.trim().toLowerCase() == normalizaltNev }
        if (custom != null) return custom.nev
        // Ellenőrizzük az alapértelmezett kulcsokat is
        for (orig in kategoriaIkonTerkep.keys) {
            if (orig.trim().toLowerCase() == normalizaltNev) return orig.capitalize()
        }
        return normalizaltNev.capitalize()
    }

    private fun ikonForKey(normalizaltNev: String): Int {
        val custom = EgyediKategoriak.kategoriak.find { it.nev.trim().toLowerCase() == normalizaltNev }
        if (custom != null) return custom.ikon
        for ((orig, ikon) in kategoriaIkonTerkep) {
            if (orig.trim().toLowerCase() == normalizaltNev) return ikon
        }
        return R.drawable.placeholder_icon
    }

    fun osszegzettAdatokMegjelenitese(bevetelMap: Map<String, Float>, kiadasMap: Map<String, Float>) {
        val teljesBevetel = bevetelMap.values.sum()
        val teljesKiadas = kiadasMap.values.sum()

        // Gyűjtsük össze a normalizált kulcsokat az adatbázisból érkező tranzakciókból
        val normKulcsok = (bevetelMap.keys + kiadasMap.keys).toMutableSet()
        // Adjuk hozzá a custom kategóriák normalizált neveit is
        EgyediKategoriak.kategoriak.forEach { customKategoria ->
            normKulcsok.add(customKategoria.nev.trim().toLowerCase())
        }
        val vegsoNormKulcsok = normKulcsok.toList()

        // Alakítsuk át a normalizált kulcsokat megjelenítendő névvé és ikonná
        val vegsoKategoriaNevek = vegsoNormKulcsok.map { megjelenitoNev(it) }
        val vegsoKategoriaIkonok = vegsoNormKulcsok.map { ikonForKey(it) }

        // Számoljuk ki a progress értékeket és a konkrét összegeket
        val vegsoExpenseProgress = vegsoNormKulcsok.map { key ->
            val kiadas = kiadasMap[key] ?: 0f
            if (teljesKiadas > 0) {
                val prog = ((kiadas / teljesKiadas) * 100).toInt()
                if (kiadas > 0 && prog == 0) 1 else prog
            } else 0
        }
        val vegsoIncomeProgress = vegsoNormKulcsok.map { key ->
            val bevetel = bevetelMap[key] ?: 0f
            if (teljesBevetel > 0) {
                val prog = ((bevetel / teljesBevetel) * 100).toInt()
                if (bevetel > 0 && prog == 0) 1 else prog
            } else 0
        }
        val vegsoExpenseAmount = vegsoNormKulcsok.map { key ->
            kiadasMap[key] ?: 0f
        }
        val vegsoIncomeAmount = vegsoNormKulcsok.map { key ->
            bevetelMap[key] ?: 0f
        }

        val totalIncomeTextView = findViewById<TextView>(R.id.totalIncomeTextView)
        val totalExpenseTextView = findViewById<TextView>(R.id.totalExpenseTextView)
        val listaNezet = findViewById<ListView>(R.id.kategoriakListView)
        val nincsAdatTextView = findViewById<TextView>(R.id.noDataTextView)

        if (vegsoKategoriaNevek.isEmpty()) {
            listaNezet.visibility = View.GONE
            totalIncomeTextView.visibility = View.GONE
            totalExpenseTextView.visibility = View.GONE
            nincsAdatTextView.visibility = View.VISIBLE
            nincsAdatTextView.text = "nincs adat a kiválasztott időszakra"
        } else {
            listaNezet.visibility = View.VISIBLE
            nincsAdatTextView.visibility = View.GONE

            totalIncomeTextView.visibility = View.VISIBLE
            totalExpenseTextView.visibility = View.VISIBLE
            totalIncomeTextView.text = "Összbevétel: ${teljesBevetel.toInt()} Ft"
            totalExpenseTextView.text = "Összkiadás: ${teljesKiadas.toInt()} Ft"

            val vegsoAdapter = KategoriakbaRendszerezesAdapter(
                context = this,
                kategoriakNevek = vegsoKategoriaNevek,
                kategoriakIkonok = vegsoKategoriaIkonok,
                kategoriakExpenseProgress = vegsoExpenseProgress,
                kategoriakIncomeProgress = vegsoIncomeProgress,
                kategoriakExpenseAmount = vegsoExpenseAmount,
                kategoriakIncomeAmount = vegsoIncomeAmount
            )
            listaNezet.adapter = vegsoAdapter
            vegsoAdapter.notifyDataSetChanged()
        }
    }
}
