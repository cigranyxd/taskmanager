package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RendszeresKifizetesek : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var osszeg: Double = 0.0 // Az oldalra betöltött összeg, amit csökkenteni fogunk
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KifizetesAdapter
    private val kifizetesekLista = mutableListOf<KifizetesItem>()

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

        // RecyclerView beállítása
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = KifizetesAdapter(kifizetesekLista)
        recyclerView.adapter = adapter

        // Betöltjük az aktuális egyenleget Firestore-ból
        loadBalanceFromFirestore(userId, osszegTextView)

        // Kifizetés hozzáadása
        hozzaadButton.setOnClickListener {
            val nev = nevEditText.text.toString()
            val osszegInput = osszegEditText.text.toString().toDoubleOrNull()

            if (nev.isNotEmpty() && osszegInput != null) {
                val kifizetes = hashMapOf(
                    "nev" to nev,
                    "osszeg" to osszegInput
                )

                db.collection("users").document(userId)
                    .collection("kifizetesek")
                    .add(kifizetes)
                    .addOnSuccessListener {
                        // Csak az oldalra betöltött összeg csökken
                        osszeg -= osszegInput
                        osszegTextView.text = "Fő összeg: ${osszeg} Ft"
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
     * Lekéri az aktuális egyenleget Firestore-ból
     */
    private fun loadBalanceFromFirestore(userId: String, osszegTextView: TextView) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Csak az adatbázisban tárolt egyenleget nem változtatjuk
                    osszeg = document.getDouble("aktualisPenz") ?: 0.0
                    osszegTextView.text = "Fő összeg: ${osszeg} Ft"
                } else {
                    osszeg = 0.0
                    osszegTextView.text = "Fő összeg: ${osszeg} Ft"
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
                    kifizetesekLista.add(KifizetesItem(docId, nev, osszeg))
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
                // Ha törölsz egy kifizetést, akkor visszaállítjuk az oldalon megjelenített egyenleget
                osszeg += osszegVisszaallitando
                // Csak az oldal összegét frissítjük
                val osszegTextView: TextView = findViewById(R.id.osszegTextView)
                osszegTextView.text = "Fő összeg: ${osszeg} Ft"
            }

        db.collection("users").document(userId)
            .collection("kifizetesek").document(docId)
            .delete()
            .addOnSuccessListener {
                frissitKifizetesekMegjelenites(userId)
            }
    }
}
