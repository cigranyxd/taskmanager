package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class Telefonszam : AppCompatActivity() {

    companion object {
        var aktualisOldalIndex = 0
    }

    // Ez az Activity aktuális oldalcíme (Telefonszám esetén "Főoldal")
    private val aktualisOldal = "Főoldal"

    private lateinit var auth: FirebaseAuth
    private lateinit var aktualisPenzTextView: TextView
    private lateinit var AktualisPenzEditText: EditText
    private var aktualisPenz: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fooldal)

        auth = FirebaseAuth.getInstance()

        // Beállítjuk a rendszer sávjaihoz a margókat
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Pénzösszeg megjelenítése
        aktualisPenzTextView = findViewById(R.id.JelenlegiText)
        AktualisPenzEditText = findViewById(R.id.Aktualis_penz)
        aktualisPenz = MennyisegEltarol()
        PenzosszegFrissites()

        // TextWatcher a pénzösszeg formázásához
        AktualisPenzEditText.addTextChangedListener(object : TextWatcher {
            private var szerkesztes = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (szerkesztes) return
                szerkesztes = true
                try {
                    val original = s.toString()
                    val cleanString = original.replace("[^0-9,.]".toRegex(), "")
                    if (cleanString.isBlank()) {
                        szerkesztes = false
                        return
                    }
                    val numberForCalculation = cleanString.replace(".", "").replace(",", ".")
                    val number = numberForCalculation.toDoubleOrNull() ?: 0.0
                    val formatted = DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
                        groupingSeparator = '.'
                        decimalSeparator = ','
                    }).format(number)
                    aktualisPenz = number
                    MennyisegMentes(aktualisPenz)
                    AktualisPenzEditText.setText(formatted)
                    AktualisPenzEditText.setSelection(formatted.length)
                } catch (e: Exception) { }
                szerkesztes = false
            }
        })

        // Gombok eseménykezelése
        val hozzaadasButton = findViewById<Button>(R.id.hozzaadas_gomb)
        hozzaadasButton.setOnClickListener {
            showAmountInputDialog("Hozzáadás", true)
        }
        val levonasButton = findViewById<Button>(R.id.levonas_gomb)
        levonasButton.setOnClickListener {
            showAmountInputDialog("Levonás", false)
        }

        // Lenyíló menü inicializálása
        val spinner: Spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        var elsoFutas = true
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Ha az inicializálás során kiválasztás történt, ne indítsunk navigációt
                if (elsoFutas) {
                    elsoFutas = false
                    return
                }
                val kiválasztottElem = parent.getItemAtPosition(position).toString()
                // Ha a kiválasztott elem megegyezik az aktuális oldalcímmel, akkor semmit sem teszünk
                if (kiválasztottElem == aktualisOldal) {
                    return
                }
                when (kiválasztottElem) {
                    "Elemzés" -> {
                        val intent = Intent(this@Telefonszam, ElemzesActivity::class.java)
                        startActivity(intent)
                    }
                    "Kategóriák" -> {
                        val intent = Intent(this@Telefonszam, Kategoriak::class.java)
                        startActivity(intent)
                    }
                    "Rendszeres kifizetések" -> {
                        val intent = Intent(this@Telefonszam, RendszeresKifizetesek::class.java)
                        startActivity(intent)
                    }
                    "Beállítások" -> {
                        val intent = Intent(this@Telefonszam, BeallitasokActivity::class.java)
                        startActivity(intent)
                    }
                    "Kijelentkezés" -> {
                        kijelentkezes()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        applyFontSizeToCurrentActivity()
    }

    override fun onResume() {
        super.onResume()
        // Visszatéréskor állítsuk be a spinner kiválasztását az aktuális oldalnak megfelelően
        val spinner: Spinner = findViewById(R.id.lenyilo_menu)
        spinner.setSelection(0)  // Mivel ebben az Activity-ben "Főoldal" az aktuális oldal
    }

    private fun applyFontSizeToCurrentActivity() {
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val fontSize = prefs.getString("betumeret", "Közepes") ?: "Közepes"
        val size = when (fontSize) {
            "Kicsi" -> 12f
            "Nagy" -> 20f
            else -> 16f
        }
        updateTextViewsFontSize(findViewById(android.R.id.content), size)
    }

    private fun updateTextViewsFontSize(view: View, fontSize: Float) {
        if (view is TextView) {
            view.textSize = fontSize
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextViewsFontSize(view.getChildAt(i), fontSize)
            }
        }
    }

    // Kijelentkezési funkció
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

    // Pénzösszeg mentése SharedPreferences-be
    private fun MennyisegMentes(amount: Double) {
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        prefs.edit().putString("aktualisPenz", amount.toString()).apply()
    }

    // Pénzösszeg betöltése SharedPreferences-ből
    private fun MennyisegEltarol(): Double {
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val amountString = prefs.getString("aktualisPenz", "0.0")
        return amountString?.toDouble() ?: 0.0
    }

    // Pénzösszeg frissítése és formázása
    private fun PenzosszegFrissites() {
        val df = DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        })
        val formattedAmount = df.format(aktualisPenz)
        AktualisPenzEditText.setText(formattedAmount)
    }

    // Pénz formázása
    private fun formatAmountForDisplay(amount: String): String {
        val number = amount.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        return DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }).format(number)
    }

    // Pénz bevitele vagy levonása
    private fun showAmountInputDialog(action: String, Hozzaad: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(action)
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)
        input.addTextChangedListener(object : TextWatcher {
            private var jelenlegi = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != jelenlegi) {
                    input.removeTextChangedListener(this)
                    try {
                        val LetisztitottString = s.toString().replace("[^0-9,.]".toRegex(), "")
                        val numberForCalculation = LetisztitottString.replace(".", "").replace(",", ".")
                        val number = numberForCalculation.toDoubleOrNull() ?: 0.0
                        val formatted = DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
                            groupingSeparator = '.'
                            decimalSeparator = ','
                        }).format(number)
                        input.setText(formatted)
                        input.setSelection(formatted.length)
                        jelenlegi = formatted
                    } catch (e: Exception) { }
                    input.addTextChangedListener(this)
                }
            }
        })
        builder.setPositiveButton("OK") { dialog, which ->
            val inputText = input.text.toString()
            val mennyiseg = inputText.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
            if (!Hozzaad && (mennyiseg <= 0 || mennyiseg > aktualisPenz)) {
                Toast.makeText(this, "Érvénytelen összeg! Nem vonható le több, mint a jelenlegi egyenleg.", Toast.LENGTH_SHORT).show()
            } else {
                aktualisPenz = if (Hozzaad) aktualisPenz + mennyiseg else aktualisPenz - mennyiseg
                MennyisegMentes(aktualisPenz)
                PenzosszegFrissites()
            }
        }
        builder.setNegativeButton("Mégse") { dialog, which -> dialog.cancel() }
        builder.show()
    }
}

