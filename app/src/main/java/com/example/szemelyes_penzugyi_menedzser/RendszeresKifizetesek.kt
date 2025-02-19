package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RendszeresKifizetesek : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var osszeg: Double = 100000.0 // Kezdeti fő összeg

    // UI elemek
    private lateinit var spinner: Spinner

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rendszeres_kifizetesek)

        // Firebase Firestore és Auth inicializálása
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // UI elemek betöltése
        val osszegTextView: TextView = findViewById(R.id.osszegTextView)
        val nevEditText: EditText = findViewById(R.id.nevEditText)
        val osszegEditText: EditText = findViewById(R.id.osszegEditText)
        val hozzaadButton: Button = findViewById(R.id.hozzaadButton)
        val megjelenitButton: Button = findViewById(R.id.megjelenitButton)
        val kifizetesekTextView: TextView = findViewById(R.id.kifizetesekTextView)

        // Kezdeti összeg megjelenítése
        osszegTextView.text = "Fő összeg: ${osszeg} Ft"

        // Kifizetés hozzáadása
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

        // Kifizetések megjelenítése
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

        // Ablak insets kezelése
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Spinner navigáció beállítása
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
                // Ha a kiválasztott elem az aktuális oldal, ne navigáljunk
                if (kiválasztottElem == "Rendszeres kifizetések") return

                when (kiválasztottElem) {
                    "Főoldal" -> {
                        val intent = Intent(this@RendszeresKifizetesek, Telefonszam::class.java)
                        startActivity(intent)
                    }
                    "Elemzés" -> {
                        val intent = Intent(this@RendszeresKifizetesek, ElemzesActivity::class.java)
                        startActivity(intent)
                    }
                    "Kategóriák" -> {
                        val intent = Intent(this@RendszeresKifizetesek, Kategoriak::class.java)
                        startActivity(intent)
                    }
                    "Beállítások" -> {
                        val intent = Intent(this@RendszeresKifizetesek, BeallitasokActivity::class.java)
                        startActivity(intent)
                    }
                    "Kijelentkezés" -> {
                        kijelentkezes()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }

        }

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
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    override fun onResume() {
        super.onResume()
        // Visszatéréskor állítsuk be a spinner értékét az aktuális oldalnak megfelelően (index 3)
        spinner.setSelection(3)
    }
}
