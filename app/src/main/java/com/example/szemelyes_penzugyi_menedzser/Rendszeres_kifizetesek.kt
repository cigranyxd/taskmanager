package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Rendszeres_kifizetesek : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var kifizetesekAdapter: KifizetesAdapter
    private lateinit var kifizetesekTextView: TextView  // A változó osztály szintjén

    private var osszeg: Double = 100000.0 // Kezdeti fő összeg

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rendszeres_kifizetesek)

        // Firebase Firestore inicializálása
        db = FirebaseFirestore.getInstance()

        // UI elemek
        val osszegTextView: TextView = findViewById(R.id.osszegTextView)
        val nevEditText: EditText = findViewById(R.id.nevEditText)
        val osszegEditText: EditText = findViewById(R.id.osszegEditText)
        val hozzaadButton: Button = findViewById(R.id.hozzaadButton)
        val megjelenitButton: Button = findViewById(R.id.megjelenitButton)
        kifizetesekTextView = findViewById(R.id.kifizetesekTextView) // Az osztály szintjén deklaráljuk

        // Kezdeti összeg megjelenítése
        osszegTextView.text = "Fő összeg: ${osszeg} Ft"

        // Rendszeres kifizetés hozzáadása
        hozzaadButton.setOnClickListener {
            val nev = nevEditText.text.toString()
            val osszegInput = osszegEditText.text.toString().toDoubleOrNull()

            if (nev.isNotEmpty() && osszegInput != null) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val uid = currentUser?.uid

                if (uid != null) {
                    val kifizetes = hashMapOf(
                        "nev" to nev,
                        "osszeg" to osszegInput
                    )

                    // Kifizetés mentése az aktuális felhasználó alá
                    db.collection("users")
                        .document(uid)
                        .collection("kifizetesek")
                        .add(kifizetes)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Sikeresen mentve: $it")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Hiba történt a mentés során", e)
                        }
                } else {
                    Log.w("Firestore", "Nincs bejelentkezett felhasználó")
                }
            }
        }

        // Kifizetések megjelenítése (kiegészítve törlési lehetőséggel)
        megjelenitButton.setOnClickListener {
            frissitKifizetesekMegjelenites()
        }

        // RecyclerView beállítása
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    // Kifizetések frissítése
    private fun frissitKifizetesekMegjelenites() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            db.collection("users")
                .document(uid)
                .collection("kifizetesek")
                .get()
                .addOnSuccessListener { result ->
                    val kifizetesek = mutableListOf<Kifizetes>()
                    for (document in result) {
                        val docId = document.id
                        val nev = document.getString("nev") ?: "N/A"
                        val osszeg = document.getDouble("osszeg") ?: 0.0
                        kifizetesek.add(Kifizetes(nev, osszeg, docId))
                    }

                    // Adapter inicializálása a kifizetések listájával és törlés eseménykezelővel
                    kifizetesekAdapter = KifizetesAdapter(kifizetesek) { docId ->
                        torlesKifizetes(docId)
                    }
                    recyclerView.adapter = kifizetesekAdapter
                }
                .addOnFailureListener { e ->
                    kifizetesekTextView.text = "Hiba: ${e.message}"
                }
        } else {
            kifizetesekTextView.text = "Felhasználói azonosító nem található!"
        }
    }

    // Kifizetés törléséhez szükséges funkció
    private fun torlesKifizetes(docId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            db.collection("users")
                .document(uid)
                .collection("kifizetesek")
                .document(docId)
                .delete()
                .addOnSuccessListener {
                    kifizetesekTextView.text = "Sikeres törlés!"
                    frissitKifizetesekMegjelenites()
                }
                .addOnFailureListener { e ->
                    kifizetesekTextView.text = "Törlési hiba: ${e.message}"
                }
        } else {
            kifizetesekTextView.text = "Felhasználói azonosító nem található!"
        }
    }
}
