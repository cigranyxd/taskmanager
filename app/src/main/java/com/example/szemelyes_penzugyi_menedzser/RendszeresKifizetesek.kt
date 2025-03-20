package com.example.szemelyes_penzugyi_menedzser

import Kifizetesitem
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class RendszeresKifizetesek : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var osszeg: Double = 0.0 // Az oldalra betöltött összeg, amit csökkenteni fogunk
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KifizetesAdapter
    private val kifizetesekLista = mutableListOf<Kifizetesitem>()
    private lateinit var spinner: Spinner
    // Új: időszakválasztó spinner
    private lateinit var periodSpinner: Spinner

    // Segédfüggvény a fő összeg formázásához: 3 karakterenként szőköz
    private fun formatOsszeg(osszeg: Double): String {
        val symbols = DecimalFormatSymbols(Locale("hu", "HU"))
        // Ha a helyi beállítás nem ad szőközt, kényszerítjük
        symbols.groupingSeparator = ' '
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(osszeg)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rendszeres_kifizetesek)

        // Firebase Firestore és Auth inicializálása
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user == null) {
            finish() // Ha nincs bejelentkezett felhasználó, lépjünk ki az activity-ből
            return
        }

        val userId = user.uid // Bejelentkezett felhasználó UID-ja

        // UI elemek
        val osszegTextView: TextView = findViewById(R.id.osszegTextView)
        val nevEditText: EditText = findViewById(R.id.nevEditText)
        val osszegEditText: EditText = findViewById(R.id.osszegEditText)
        val hozzaadButton: Button = findViewById(R.id.hozzaadButton)
        val torlesButton: Button = findViewById(R.id.torlesButton)

        // Spinner inicializálása a navigációhoz
        spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegekSpinner = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegekSpinner)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // Ebben az Activity-ben az aktuális oldal "Rendszeres kifizetések", tehát index 3
        spinner.setSelection(3)
        var elsoFutas = true
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (elsoFutas) {
                    elsoFutas = false
                    return
                }
                val kiválasztottElem = parent.getItemAtPosition(position).toString()
                // Ha a kiválasztott elem az aktuális oldal ("Rendszeres kifizetések"), ne navigáljunk
                if (kiválasztottElem == "Rendszeres kifizetések") return

                when (kiválasztottElem) {
                    "Főoldal" -> startActivity(Intent(this@RendszeresKifizetesek, Telefonszam::class.java))
                    "Elemzés" -> startActivity(Intent(this@RendszeresKifizetesek, ElemzesActivity::class.java))
                    "Kategóriák" -> startActivity(Intent(this@RendszeresKifizetesek, Kategoriak::class.java))
                    "Beállítások" -> startActivity(Intent(this@RendszeresKifizetesek, BeallitasokActivity::class.java))
                    "Kijelentkezés" -> Kijelentkezes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        // Időszakválasztó spinner inicializálása
        periodSpinner = findViewById(R.id.periodSpinner)
        val periodOpciók = listOf("Naponta", "Hetente", "Havonta", "Évente")
        val periodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periodOpciók)
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = periodAdapter
        // Alapértelmezett érték legyen "Havonta"
        periodSpinner.setSelection(2)

        // RecyclerView beállítása
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = KifizetesAdapter(kifizetesekLista)
        recyclerView.adapter = adapter

        // Az aktuális egyenleg betöltése Firestore-ból
        loadBalanceFromFirestore(userId, osszegTextView)

        // Kifizetés hozzáadása
        hozzaadButton.setOnClickListener {
            val nev = nevEditText.text.toString()
            val osszegInput = osszegEditText.text.toString().toDoubleOrNull()
            val period = periodSpinner.selectedItem.toString() // Az időszak kiválasztása

            if (nev.isNotEmpty() && osszegInput != null) {
                // Az új kifizetéshez eltároljuk a period értéket is
                val kifizetes = hashMapOf(
                    "nev" to nev,
                    "osszeg" to osszegInput,
                    "period" to period
                )

                db.collection("users").document(userId)
                    .collection("kifizetesek")
                    .add(kifizetes)
                    .addOnSuccessListener {
                        // Az oldalra betöltött összeg csökkenése
                        osszeg -= osszegInput
                        osszegTextView.text = "Fő összeg: ${formatOsszeg(osszeg)} Ft"
                        nevEditText.text.clear()
                        osszegEditText.text.clear()
                        frissitKifizetesekMegjelenites(userId)
                    }
            }
        }

        // Törlés gomb működése
        torlesButton.setOnClickListener {
            val kijeloltElemek = adapter.getKijeloltElemek()
            for (kifizetes in kijeloltElemek) {
                torlesKifizetes(userId, kifizetes.docId)
            }
        }

        // Kifizetések betöltése
        frissitKifizetesekMegjelenites(userId)
    }

    /**
     * Lekéri az aktuális egyenleget Firestore-ból, és frissíti a fő összeg kijelzőt
     */
    private fun loadBalanceFromFirestore(userId: String, osszegTextView: TextView) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    osszeg = document.getDouble("aktualisPenz") ?: 0.0
                    osszegTextView.text = "Fő összeg: ${formatOsszeg(osszeg)} Ft"
                } else {
                    osszeg = 0.0
                    osszegTextView.text = "Fő összeg: ${formatOsszeg(osszeg)} Ft"
                }
            }
            .addOnFailureListener {
                osszeg = 0.0
                osszegTextView.text = "Hiba történt az összeg betöltésekor!"
            }
    }

    /**
     * Frissíti a kifizetések listáját
     */
    private fun frissitKifizetesekMegjelenites(userId: String) {
        db.collection("users").document(userId)
            .collection("kifizetesek")
            .get()
            .addOnSuccessListener { result ->
                kifizetesekLista.clear()
                for (document in result) {
                    val docId = document.id
                    val nev = document.getString("nev") ?: "N/A"
                    val osszeg = document.getDouble("osszeg") ?: 0.0
                    val period = document.getString("period") ?: "N/A"
                    kifizetesekLista.add(Kifizetesitem(docId, nev, osszeg, period))
                }
                adapter.notifyDataSetChanged()
            }
    }

    /**
     * Töröl egy kifizetést a Firestore-ból és visszaállítja az egyenleget
     */
    private fun torlesKifizetes(userId: String, docId: String) {
        db.collection("users").document(userId)
            .collection("kifizetesek").document(docId)
            .get()
            .addOnSuccessListener { document ->
                val osszegVisszaallitando = document.getDouble("osszeg") ?: 0.0
                osszeg += osszegVisszaallitando
                val osszegTextView: TextView = findViewById(R.id.osszegTextView)
                osszegTextView.text = "Fő összeg: ${formatOsszeg(osszeg)} Ft"
            }

        db.collection("users").document(userId)
            .collection("kifizetesek").document(docId)
            .delete()
            .addOnSuccessListener {
                frissitKifizetesekMegjelenites(userId)
            }
    }

    private fun Kijelentkezes() {
        auth.signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()
        val intent = Intent(this, Bejelentkezes::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Ha a felhasználó megnyomja a vissza gombot, navigáljunk a Telefonszám Activity-be.
     */
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, Telefonszam::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
