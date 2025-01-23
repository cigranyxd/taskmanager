package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class Telefonszam : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var aktualisPenzTextView: TextView
    private lateinit var AktualisPenzEditText: EditText
    private var aktualisPenz: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fooldal)

        auth = FirebaseAuth.getInstance()

        // Az ablak margóinak beállítása a rendszer sávokhoz
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

        // TextWatcher az AktualisPenzEditText mezőhöz és formázáshoz
        AktualisPenzEditText.addTextChangedListener(object : TextWatcher {
            private var szerkesztes = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (szerkesztes) return

                szerkesztes = true

                try {
                    val original = s.toString()

                    // Tisztítjuk a nem kívánt karaktereket (csak számjegyek, pont és vessző)
                    val cleanString = original.replace("[^0-9,.]".toRegex(), "")

                    // Ha üres a string, nem történik semmi
                    if (cleanString.isBlank()) {
                        szerkesztes = false
                        return
                    }

                    // A vesszőt ideiglenesen pontra cseréljük a számítás miatt
                    val numberForCalculation = cleanString.replace(".", "").replace(",", ".")
                    val number = numberForCalculation.toDoubleOrNull() ?: 0.0

                    // Formázás ezres elválasztóval és tizedesvesszővel
                    val formatted = DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
                        groupingSeparator = '.'  // Ezres elválasztó pont
                        decimalSeparator = ','   // Tizedes elválasztó vessző
                    }).format(number)

                    // Az új érték mentése
                    aktualisPenz = number
                    MennyisegMentes(aktualisPenz)

                    // A formázott szöveg visszaállítása
                    AktualisPenzEditText.setText(formatted)
                    AktualisPenzEditText.setSelection(formatted.length) // Kurzor a végére

                } catch (e: Exception) {
                    // Hibakezelés
                }

                szerkesztes = false
            }
        })


        // Hozzáadás gomb eseménykezelője
        val hozzaadasButton = findViewById<Button>(R.id.hozzaadas_gomb)
        hozzaadasButton.setOnClickListener {
            showAmountInputDialog("Hozzáadás", true)
        }

        // Levonás gomb eseménykezelője
        val levonasButton = findViewById<Button>(R.id.levonas_gomb)
        levonasButton.setOnClickListener {
            showAmountInputDialog("Levonás", false)
        }



        // Lenyíló menü inicializálása
        val spinner: Spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Rendszeres kifizetések", "Kijelentkezés")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

// Változó az esemény kezelésének ellenőrzésére
        var eloszorFut_eLe = false

// Lenyíló menü kiválasztásának figyelése
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (!eloszorFut_eLe) {
                    // Az inicializálás során ne hajtsuk végre a műveletet
                    eloszorFut_eLe = true
                    return
                }

                val selectedItem = parent.getItemAtPosition(position).toString()
                when (selectedItem) {
                    "Főoldal" -> {
                        val intent = Intent(this@Telefonszam, Telefonszam::class.java)
                        startActivity(intent)
                    }
                    "Elemzés" -> {
                        val intent = Intent(this@Telefonszam, ElemzesActivity::class.java)
                        startActivity(intent)
                    }
                    "Rendszeres kifizetések" -> {
                        val intent = Intent(this@Telefonszam, Rendszeres_kifizetesek::class.java)
                        startActivity(intent)
                    }
                    "Kijelentkezés" -> {
                        Kijelentkezes()
                    }
                }
            }


            override fun onNothingSelected(parent: AdapterView<*>) {
                // Semmi sem történt
            }
        }


    }

    // Kijelentkezési funkció
    private fun Kijelentkezes() {
        auth.signOut()
        // Töröljük a belépési állapotot
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()

        // Visszatérés a bejelentkezési képernyőre
        val intent = Intent(this, Bejelentkezes::class.java)
        startActivity(intent)
        finish()
    }

    // Pénzösszeg mentése SharedPreferences-be
    private fun MennyisegMentes(amount: Double) {
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("aktualisPenz", amount.toString())
        editor.apply()
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
            groupingSeparator = '.'  // Ezres elválasztó pont
            decimalSeparator = ','   // Tizedes elválasztó vessző
        })
        val formattedAmount = df.format(aktualisPenz)
        AktualisPenzEditText.setText(formattedAmount)
    }

    // Pénz formázása
    private fun formatAmountForDisplay(amount: String): String {
        val number = amount.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        return DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
            groupingSeparator = '.'  // Ezres elválasztó pont
            decimalSeparator = ','   // Tizedes elválasztó vessző
        }).format(number)
    }

    // Pénz bevitele vagy levonása
    private fun showAmountInputDialog(action: String, Hozzaad: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(action)

        // Létrehozunk egy EditText-et, ahol a felhasználó beírhatja az összeget
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)

        // TextWatcher hozzáadása, hogy automatikusan formázza a számot
        input.addTextChangedListener(object : TextWatcher {
            private var jelenlegi = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != jelenlegi) {
                    input.removeTextChangedListener(this)

                    try {
                        val LetisztitottString = s.toString().replace("[^0-9,.]".toRegex(), "")

                        // Számítás céljából a vesszőt pontra cseréljük
                        val numberForCalculation = LetisztitottString.replace(".", "").replace(",", ".")
                        val number = numberForCalculation.toDoubleOrNull() ?: 0.0

                        // Formázás a megfelelő elválasztókkal
                        val formatted = DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
                            groupingSeparator = '.'  // Ezres elválasztó pont
                            decimalSeparator = ','   // Tizedes elválasztó vessző
                        }).format(number)

                        input.setText(formatted)
                        input.setSelection(formatted.length)

                        jelenlegi = formatted
                    } catch (e: Exception) {
                        // Hibakezelés
                    }

                    input.addTextChangedListener(this)
                }
            }
        })

        // OK gomb hozzáadása a párbeszédablakhoz
        builder.setPositiveButton("OK") { dialog, which ->
            val inputText = input.text.toString()
            // A számítás előtt átalakítjuk a formázott szöveget számmá
            val mennyiseg = inputText.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0

            if (!Hozzaad && (mennyiseg <= 0 || mennyiseg > aktualisPenz)) {
                Toast.makeText(this, "Érvénytelen összeg! Nem vonható le több, mint a jelenlegi egyenleg.", Toast.LENGTH_SHORT).show()
            } else {
                aktualisPenz = if (Hozzaad) {
                    aktualisPenz + mennyiseg
                } else {
                    aktualisPenz - mennyiseg
                }
                MennyisegMentes(aktualisPenz)
                PenzosszegFrissites()
            }
        }

        builder.setNegativeButton("Mégse") { dialog, which -> dialog.cancel() }
        builder.show()
    }
}