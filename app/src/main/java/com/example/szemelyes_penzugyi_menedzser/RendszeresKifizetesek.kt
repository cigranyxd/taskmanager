package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class RendszeresKifizetesek : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var osszeg: Double = 100000.0 // Kezdeti fő összeg

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rendszeres_kifizetesek)

        // Firebase Firestore inicializálása
        db = FirebaseFirestore.getInstance()

        // UI elemek
        val osszegTextView: TextView = findViewById(R.id.osszegTextView)
        val nevEditText: EditText = findViewById(R.id.nevEditText)
        val osszegEditText: EditText = findViewById(R.id.osszegEditText)
        val hozzaadButton: Button = findViewById(R.id.hozzaadButton)
        val megjelenitButton: Button = findViewById(R.id.megjelenitButton)
        val kifizetesekTextView: TextView = findViewById(R.id.kifizetesekTextView)

        // Kezdeti összeg megjelenítése
        osszegTextView.text = "Fő összeg: ${osszeg} Ft"

        // Rendszeres kifizetés hozzáadása
        hozzaadButton.setOnClickListener {
            val nev = nevEditText.text.toString()
            val osszegInput = osszegEditText.text.toString().toDoubleOrNull()

            if (nev.isNotEmpty() && osszegInput != null) {
                val kifizetes = hashMapOf(
                    "nev" to nev,
                    "osszeg" to osszegInput
                )

                db.collection("kifizetesek")
                    .add(kifizetes)
                    .addOnSuccessListener {
                        osszeg -= osszegInput
                        osszegTextView.text = "Fő összeg: ${osszeg} Ft"
                        nevEditText.text.clear()
                        osszegEditText.text.clear()
                    }
                    .addOnFailureListener { e ->
                        kifizetesekTextView.text = "Hiba: ${e.message}"
                    }
            }
        }
        // Kifizetések megjelenítése (kiegészítve törlési lehetőséggel)
        fun frissitKifizetesekMegjelenites() {
            db.collection("kifizetesek")
                .get()
                .addOnSuccessListener { result ->
                    val builder = StringBuilder()
                    for (document in result) {
                        val docId = document.id
                        val nev = document.getString("nev") ?: "N/A"
                        val osszeg = document.getDouble("osszeg") ?: 0.0
                        builder.append("Név: $nev, Összeg: $osszeg Ft\n")
                        builder.append("Törléshez kattints ide: $docId\n")
                    }
                    kifizetesekTextView.text = builder.toString()
                }
                .addOnFailureListener { e ->
                    kifizetesekTextView.text = "Hiba: ${e.message}"
                }
        }
        megjelenitButton.setOnClickListener {
            frissitKifizetesekMegjelenites()
        }
        // Új funkció: Kifizetés törlése
        fun torlesKifizetes(docId: String) {
            db.collection("kifizetesek").document(docId)
                .delete()
                .addOnSuccessListener {
                    kifizetesekTextView.text = "Sikeres törlés!"
                    frissitKifizetesekMegjelenites()
                }
                .addOnFailureListener { e ->
                    kifizetesekTextView.text = "Törlési hiba: ${e.message}"
                }
        }




        // Ablak insets kezelése
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
