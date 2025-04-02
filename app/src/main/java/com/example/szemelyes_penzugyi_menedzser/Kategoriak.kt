package com.example.szemelyes_penzugyi_menedzser

import KategoriaAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Data class az időszak elemekhez
data class PeriodusElem(val megjelenitoSzoveg: String, val kezdoDatum: LocalDate, val zaroDatum: LocalDate)

class Kategoriak :  BaseActivity() {

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
        "sport" to R.drawable.edzes_icon,
        "közlekedés" to R.drawable.kozlekedes_icon,
        "egyéb" to R.drawable.egyeb_icon,
        "fizetés" to R.drawable.szabadido_icon
    )

    // Az aktuális oldal neve (ez az Activity célja)
    private val aktualisOldal = "Kategóriák"

    private lateinit var navigaciosSpinner: Spinner
    private lateinit var idoszakValasztoSpinner: Spinner
    private lateinit var auth: FirebaseAuth

    // Változó, mely tárolja az aktuálisan kiválasztott fő időszakot ("Nap", "Het", "Honap", "Ev")
    private var aktualisIdoszak: String = "Nap"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Az activity_kategoriak.xml tartalmazza az időszakválasztó elemeket, a ListView–t,
        // a navigációs spinner–t és az egyedi időszak kiválasztó sor elemeit (spinner két nyíllal)
        setContentView(R.layout.activity_kategoriak)

        auth = FirebaseAuth.getInstance()
        // Betöltjük az egyedi kategóriákat SharedPreferences–ből
        EgyediKategoriak.betolt(this)

        val firestore = FirebaseFirestore.getInstance()
        val aktualisFelhasznalo = auth.currentUser
        val felhasznaloId = aktualisFelhasznalo?.uid ?: return

        // Időszak választó elemek (pl. NapFelirat, HetFelirat, stb.)
        val napFelirat = findViewById<TextView>(R.id.NapFelirat)
        val hetFelirat = findViewById<TextView>(R.id.HetFelirat)
        val honapFelirat = findViewById<TextView>(R.id.HonapFelirat)
        val evFelirat = findViewById<TextView>(R.id.EvFelirat)

        // OnClickListener–ek az időszak elemekhez:
        // Frissítjük az aktuális időszak változót és frissítjük az egyedi időszak kiválasztó menüt
        napFelirat.setOnClickListener {
            aktualisIdoszak = "Nap"
            frissitIdoszakValasztot(felhasznaloId)
        }
        hetFelirat.setOnClickListener {
            aktualisIdoszak = "Het"
            frissitIdoszakValasztot(felhasznaloId)
        }
        honapFelirat.setOnClickListener {
            aktualisIdoszak = "Honap"
            frissitIdoszakValasztot(felhasznaloId)
        }
        evFelirat.setOnClickListener {
            aktualisIdoszak = "Ev"
            frissitIdoszakValasztot(felhasznaloId)
        }

        // Gomb az új kategória hozzáadásához
        findViewById<Button>(R.id.ujKategoriaHozzaadasa).setOnClickListener {
            val intent = Intent(this, KategoriaHozzaadasActivity::class.java)
            startActivity(intent)
        }

        // Navigációs spinner inicializálása (Főoldal, Elemzés, stb.)
        navigaciosSpinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val spinnerAdapter = CustomFontSizeSpinnerAdapter(this, R.layout.spinner_item, lehetosegek)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
        navigaciosSpinner.adapter = spinnerAdapter

        // Az aktuális oldal beállítása
        navigaciosSpinner.setSelection(2)

        var elsoFutas = true
        navigaciosSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (elsoFutas) {
                    elsoFutas = false
                    return
                }
                val kiválasztottElem = parent.getItemAtPosition(position).toString()
                if (kiválasztottElem == aktualisOldal) return
                when (kiválasztottElem) {
                    "Főoldal" -> startActivity(Intent(this@Kategoriak, Telefonszam::class.java))
                    "Elemzés" -> startActivity(Intent(this@Kategoriak, ElemzesActivity::class.java))
                    "Rendszeres kifizetések" -> startActivity(Intent(this@Kategoriak, RendszeresKifizetesek::class.java))
                    "Beállítások" -> startActivity(Intent(this@Kategoriak, BeallitasokActivity::class.java))
                    "Kijelentkezés" -> kijelentkezes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        // Az egyedi időszak kiválasztó spinner inicializálása
        idoszakValasztoSpinner = findViewById(R.id.idoszakValasztoSpinner)
        // Alapértelmezetten a "Nap" időszak legyen kiválasztva, így frissítjük a kiválasztó menüt
        frissitIdoszakValasztot(felhasznaloId)

        // Inicializáljuk az arrow gombokat az időszak navigációhoz
        val arrowBack = findViewById<ImageButton>(R.id.arrowBack)
        val arrowForward = findViewById<ImageButton>(R.id.arrowForward)

        // Bal oldali nyíl: régebbi időszak (növeli az indexet a spinner listában)
        arrowBack.setOnClickListener {
            val currentIndex = idoszakValasztoSpinner.selectedItemPosition
            if (currentIndex < idoszakValasztoSpinner.adapter.count - 1) {
                idoszakValasztoSpinner.setSelection(currentIndex + 1)
            }
        }

        // Jobb oldali nyíl: újabb időszak (csökkenti az indexet a spinner listában)
        arrowForward.setOnClickListener {
            val currentIndex = idoszakValasztoSpinner.selectedItemPosition
            if (currentIndex > 0) {
                idoszakValasztoSpinner.setSelection(currentIndex - 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Visszatéréskor állítsuk be a navigációs spinner értékét az aktuális oldalnak megfelelően
        navigaciosSpinner.setSelection(2)
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        // Vissza gomb: mindig a Főoldalra navigálunk
        val intent = Intent(this, Telefonszam::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun kijelentkezes() {
        auth.signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()
        val intent = Intent(this, Bejelentkezes::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Lekéri az adott időszak tranzakcióit, majd összegzi és megjeleníti az adatokat.
     */
    private fun tranzakciokBetoltese(felhasznaloId: String, period: String) {
        tranzakciokLekerdezese(felhasznaloId, period) { tranzakciok ->
            val (bevetelMap, kiadasMap) = tranzakciokOsszegzese(tranzakciok)
            osszegzettAdatokMegjelenitese(period, bevetelMap, kiadasMap)
        }
    }

    /**
     * Az időszak kezdő és záró dátumát adja vissza (yyyy-mm-dd formátumban).
     */
    fun idoszakKezelo(period: String): Pair<String, String> {
        val ma = LocalDate.now()
        return when (period) {
            "Nap" -> Pair(ma.toString(), ma.toString())
            "Het" -> {
                val kezdo = ma.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val zaro = ma.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                Pair(kezdo.toString(), zaro.toString())
            }
            "Honap" -> {
                val kezdo = ma.withDayOfMonth(1)
                val zaro = ma.withDayOfMonth(ma.lengthOfMonth())
                Pair(kezdo.toString(), zaro.toString())
            }
            "Ev" -> {
                val kezdo = ma.withDayOfYear(1)
                val zaro = ma.withDayOfYear(ma.lengthOfYear())
                Pair(kezdo.toString(), zaro.toString())
            }
            else -> Pair("", "")
        }
    }

    fun tranzakciokLekerdezese(felhasznaloId: String, period: String, callback: (List<Tranzakcio>) -> Unit) {
        val (kezdoDatum, zaroDatum) = idoszakKezelo(period)
        val firestore = FirebaseFirestore.getInstance()
        if (period == "Nap") {
            firestore.collection("users")
                .document(felhasznaloId)
                .collection("nap")
                .document(kezdoDatum)
                .get()
                .addOnSuccessListener { document ->
                    val tranzakciok = mutableListOf<Tranzakcio>()
                    if (document.exists()) {
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
        } else {
            firestore.collection("users")
                .document(felhasznaloId)
                .collection("nap")
                .whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), kezdoDatum)
                .whereLessThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), zaroDatum)
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

    // A megjelenítő név visszaadása – módosítva, hogy "fizetési csekk" -> "Fizetés" és "edzés" -> "Sport"
    private fun megjelenitoNev(normalizaltNev: String): String {
        if(normalizaltNev == "fizetési csekk") return "Fizetés"
        if(normalizaltNev == "edzés") return "Sport"
        val custom = EgyediKategoriak.kategoriak.find { it.nev.trim().toLowerCase() == normalizaltNev }
        if (custom != null) return custom.nev
        for (orig in kategoriaIkonTerkep.keys) {
            if (orig.trim().toLowerCase() == normalizaltNev) return orig.capitalize()
        }
        return normalizaltNev.capitalize()
    }

    // Az ikon visszaadása a kategória nevéhez – módosítva, hogy "fizetési csekk" -> "fizetés" és "edzés" -> "sport"
    private fun ikonKulcsra(normalizaltNev: String): Int {
        if(normalizaltNev == "fizetési csekk") return kategoriaIkonTerkep["fizetés"] ?: R.drawable.placeholder_icon
        if(normalizaltNev == "edzés") return kategoriaIkonTerkep["sport"] ?: R.drawable.placeholder_icon
        val custom = EgyediKategoriak.kategoriak.find { it.nev.trim().toLowerCase() == normalizaltNev }
        if (custom != null) return custom.ikon
        for ((orig, ikon) in kategoriaIkonTerkep) {
            if (orig.trim().toLowerCase() == normalizaltNev) return ikon
        }
        return R.drawable.placeholder_icon
    }

    /**
     * Megjeleníti az összegzett adatokat.
     * @param period Az aktuális időszak ("Nap", "Het", "Kiadás", stb.)
     */
    fun osszegzettAdatokMegjelenitese(period: String, bevetelMap: Map<String, Float>, kiadasMap: Map<String, Float>) {
        val teljesBevetel = bevetelMap.values.sum()
        val teljesKiadas = kiadasMap.values.sum()

        // Csak azokat a kategóriákat jelenítjük meg, amelyekhez tartozik tranzakció
        val normKulcsok = (bevetelMap.keys + kiadasMap.keys).toMutableSet()

        val vegsoNormKulcsok = normKulcsok.toList()
        val vegsoKategoriaNevek = vegsoNormKulcsok.map { megjelenitoNev(it) }
        val vegsoKategoriaIkonok = vegsoNormKulcsok.map { ikonKulcsra(it) }

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
            totalIncomeTextView.text = "Összbevétel: ${formatNumber(teljesBevetel.toInt())} Ft"
            totalExpenseTextView.text = "Összkiadás: ${formatNumber(teljesKiadas.toInt())} Ft"

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

            listaNezet.setOnItemClickListener { parent, view, position, id ->
                val selectedCategory = vegsoKategoriaNevek[position]
                val intent = Intent(this, KategoriaTranzakciokActivity::class.java)
                intent.putExtra("categoryName", selectedCategory)
                intent.putExtra("period", aktualisIdoszak)
                startActivity(intent)
            }
        }
    }

    // --- SEGÉLFÜGGVÉNYEK AZ EGYEDI IDŐSZAK KIVÁLASZTÁSÁHOZ ---

    private fun getLetoltesDatum(): LocalDate {
        val alapDatum = LocalDate.of(2024, 10, 1)
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        prefs.edit().putString("letoltesDatum", alapDatum.toString()).apply()
        return alapDatum
    }

    private fun generalPeriodLista(period: String): List<PeriodusElem> {
        val lista = mutableListOf<PeriodusElem>()
        val letoltesDatum = getLetoltesDatum()
        val ma = LocalDate.now()
        when (period) {
            "Nap" -> {
                var datum = ma
                while (!datum.isBefore(letoltesDatum)) {
                    lista.add(PeriodusElem(datum.toString(), datum, datum))
                    datum = datum.minusDays(1)
                }
            }
            "Het" -> {
                var hetKezdo = ma.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                while (!hetKezdo.isBefore(letoltesDatum)) {
                    val hetVege = hetKezdo.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    lista.add(PeriodusElem("${hetKezdo.toString()} - ${hetVege.toString()}", hetKezdo, hetVege))
                    hetKezdo = hetKezdo.minusWeeks(1)
                }
            }
            "Honap" -> {
                var honapKezdo = ma.withDayOfMonth(1)
                val letoltesHonapKezdo = letoltesDatum.withDayOfMonth(1)
                val honapNevek = mapOf(
                    1 to "január",
                    2 to "február",
                    3 to "március",
                    4 to "április",
                    5 to "május",
                    6 to "június",
                    7 to "július",
                    8 to "augusztus",
                    9 to "szeptember",
                    10 to "október",
                    11 to "november",
                    12 to "december"
                )
                while (!honapKezdo.isBefore(letoltesHonapKezdo)) {
                    val honapVege = honapKezdo.withDayOfMonth(honapKezdo.lengthOfMonth())
                    val display = "${honapNevek[honapKezdo.monthValue]} ${honapKezdo.year}"
                    lista.add(PeriodusElem(display, honapKezdo, honapVege))
                    honapKezdo = honapKezdo.minusMonths(1)
                }
            }
            "Ev" -> {
                var ev = ma.year
                val letoltesEv = letoltesDatum.year
                while (ev >= letoltesEv) {
                    val kezdo = LocalDate.of(ev, 1, 1)
                    val zaro = LocalDate.of(ev, 12, 31)
                    lista.add(PeriodusElem(ev.toString(), kezdo, zaro))
                    ev--
                }
            }
        }
        return lista
    }

    private fun tranzakciokBetolteseEgyedi(felhasznaloId: String, kezdoDatum: String, zaroDatum: String) {
        val firestore = FirebaseFirestore.getInstance()
        if (kezdoDatum == zaroDatum) {
            firestore.collection("users")
                .document(felhasznaloId)
                .collection("nap")
                .document(kezdoDatum)
                .get()
                .addOnSuccessListener { document ->
                    val tranzakciok = mutableListOf<Tranzakcio>()
                    if (document.exists()) {
                        val trxLista = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        trxLista.forEach { trx ->
                            val mennyiseg = (trx["mennyiseg"] as? Number)?.toFloat() ?: 0f
                            val tipus = trx["tipus"] as? String ?: ""
                            val kategoria = (trx["kategoria"] as? String ?: "").trim().toLowerCase()
                            val leiras = trx["leiras"] as? String ?: ""
                            tranzakciok.add(Tranzakcio(kategoria, leiras, mennyiseg, tipus))
                        }
                    }
                    val (bevetelMap, kiadasMap) = tranzakciokOsszegzese(tranzakciok)
                    osszegzettAdatokMegjelenitese("Egyedi", bevetelMap, kiadasMap)
                }
                .addOnFailureListener {
                    osszegzettAdatokMegjelenitese("Egyedi", emptyMap(), emptyMap())
                }
        } else {
            firestore.collection("users")
                .document(felhasznaloId)
                .collection("nap")
                .whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), kezdoDatum)
                .whereLessThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), zaroDatum)
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
                    val (bevetelMap, kiadasMap) = tranzakciokOsszegzese(tranzakciok)
                    osszegzettAdatokMegjelenitese("Egyedi", bevetelMap, kiadasMap)
                }
                .addOnFailureListener {
                    osszegzettAdatokMegjelenitese("Egyedi", emptyMap(), emptyMap())
                }
        }
    }

    /**
     * Frissíti az egyedi időszak kiválasztó spinner–t a kiválasztott fő időszak alapján
     */
    private fun frissitIdoszakValasztot(felhasznaloId: String) {
        val idoszakLista = generalPeriodLista(aktualisIdoszak)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, idoszakLista.map { it.megjelenitoSzoveg })
        adapter.setDropDownViewResource(R.layout.spinner_item)
        idoszakValasztoSpinner.adapter = adapter

        idoszakValasztoSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val kiválasztottElem = idoszakLista[position]
                tranzakciokBetolteseEgyedi(felhasznaloId, kiválasztottElem.kezdoDatum.toString(), kiválasztottElem.zaroDatum.toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // Privát függvény a számok formázásához (3 számjegyenként szóköz)
    private fun formatNumber(value: Int): String {
        val df = DecimalFormat("#,###", DecimalFormatSymbols(Locale("hu", "HU")).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        })
        return df.format(value)
    }
}
