package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class Telefonszam : AppCompatActivity() {

    private lateinit var aktualisPenzTextView: TextView
    private lateinit var AktualisPenzEditText: EditText
    private lateinit var spinner: Spinner
    private var aktualisPenz: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fooldal)

        // UI elemek inicializálása
        aktualisPenzTextView = findViewById(R.id.JelenlegiText)
        AktualisPenzEditText = findViewById(R.id.Aktualis_penz)
        spinner = findViewById(R.id.lenyilo_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Betöltjük az aktuális egyenleget
        checkAndLoadBalance()

        // Spinner beállítása
        setupSpinner()

        // Szövegmező figyelése: ha módosul az érték, frissítjük az adatbázist
        AktualisPenzEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val amount = s.toString().replace(",", ".").toDoubleOrNull()
                if (amount != null && amount != aktualisPenz) {
                    aktualisPenz = amount
                    saveBalanceToFirestore(aktualisPenz)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Gombok beállítása
        val hozzaadasButton = findViewById<Button>(R.id.hozzaadas_gomb)
        hozzaadasButton.setOnClickListener {
            showAmountInputDialog("Hozzáadás", true)
        }

        val levonasButton = findViewById<Button>(R.id.levonas_gomb)
        levonasButton.setOnClickListener {
            showAmountInputDialog("Levonás", false)
        }
    }

    private fun checkAndLoadBalance() {
        val uid = FirebaseManager.getCurrentUserUID()
        if (uid != null) {
            FirebaseManager.loadBalance(
                uid,
                onComplete = { balance ->
                    // Frissítjük a globális és lokális egyenleget, majd a UI-t
                    GlobalData.setAktualisPenz(balance)
                    aktualisPenz = balance
                    PenzosszegFrissites()
                },
                onError = { exception ->
                    Log.e("Firestore", "Hiba az egyenleg betöltésekor: ", exception)
                }
            )
        }
    }

    private fun saveBalanceToFirestore(amount: Double) {
        val uid = FirebaseManager.getCurrentUserUID()
        if (uid != null) {
            FirebaseManager.saveBalance(
                uid,
                amount,
                onComplete = {
                    GlobalData.setAktualisPenz(amount)
                },
                onError = { exception ->
                    Log.e("Firestore", "Hiba a mentés során: ", exception)
                }
            )
        }
    }

    private fun PenzosszegFrissites() {
        val df = DecimalFormat("#,###", DecimalFormatSymbols().apply {
            groupingSeparator = ' '  // Csoportosító jel: szóköz
            decimalSeparator = '.'   // Decimális elválasztó
        })
        val formattedAmount = df.format(aktualisPenz)
        AktualisPenzEditText.setText(formattedAmount)
    }

    private fun showAmountInputDialog(action: String, hozzaad: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(action)

        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val amount = input.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            if (hozzaad) {
                aktualisPenz += amount
            } else {
                aktualisPenz -= amount
            }
            saveBalanceToFirestore(aktualisPenz)
            PenzosszegFrissites()
        }
        builder.setNegativeButton("Mégse", null)
        builder.show()
    }

    private fun setupSpinner() {
        val lehetosegek = listOf("Főoldal", "Elemzés", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                when (selectedItem) {
                    "Elemzés" -> startActivity(Intent(this@Telefonszam, ElemzesActivity::class.java))
                    "Rendszeres kifizetések" -> startActivity(Intent(this@Telefonszam, RendszeresKifizetesek::class.java))
                    "Beállítások" -> startActivity(Intent(this@Telefonszam, BeallitasokActivity::class.java))
                    "Kijelentkezés" -> Kijelentkezes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun Kijelentkezes() {
        FirebaseManager.signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()

        startActivity(Intent(this, Bejelentkezes::class.java))
        finish()
    }
}
