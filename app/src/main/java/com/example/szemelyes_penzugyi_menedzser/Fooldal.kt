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

    // Save the balance to SharedPreferences
    private fun saveBalance(amount: Double) {
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("aktualisPenz", amount.toString()) // Save as string
        editor.apply()
    }

    // Load amount from SharedPreferences
    private fun MennyisegEltarol(): Double {
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val amountString = prefs.getString("aktualisPenz", "0.0")
        return amountString?.toDouble() ?: 0.0
    }

    // Update the balance display
    private fun PenzosszegFrissites() {
        val df = DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
            groupingSeparator = '.'  // Thousands separator as dot
            decimalSeparator = ','   // Decimal separator as comma
        })
        val formattedAmount = df.format(aktualisPenz)
        AktualisPenzEditText.setText(formattedAmount)
    }

    // Method for signing out
    private fun Kijelentkezes() {
        auth.signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()

        val intent = Intent(this, Bejelentkezes::class.java)
        startActivity(intent)
        finish()
    }

    // Dialog to show amount input (add or deduct)
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
                            groupingSeparator = '.'  // Thousands separator as dot
                            decimalSeparator = ','   // Decimal separator as comma
                        }).format(number)

                        input.setText(formatted)
                        input.setSelection(formatted.length)

                        jelenlegi = formatted
                    } catch (e: Exception) {}
                    input.addTextChangedListener(this)
                }
            }
        })

        builder.setPositiveButton("OK") { dialog, which ->
            val amount = input.text.toString().toDoubleOrNull() ?: 0.0
            if (Hozzaad) {
                aktualisPenz += amount
            } else {
                aktualisPenz -= amount
            }

            // Only update and save if balance has changed
            if (aktualisPenz != MennyisegEltarol()) {
                saveBalance(aktualisPenz)
                PenzosszegFrissites()
            }
        }
        builder.setNegativeButton("Mégse", null)

        builder.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fooldal)

        auth = FirebaseAuth.getInstance()

        // Set up the window padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI elements
        aktualisPenzTextView = findViewById(R.id.JelenlegiText)
        AktualisPenzEditText = findViewById(R.id.Aktualis_penz)

        // Load and display the balance only once when the activity starts
        aktualisPenz = MennyisegEltarol()
        PenzosszegFrissites()

        // TextWatcher for formatting the amount entered
        AktualisPenzEditText.addTextChangedListener(object : TextWatcher {
            private var szerkesztes = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (szerkesztes) return

                szerkesztes = true
                try {
                    val original = s.toString()

                    // Clean the string from unwanted characters (only digits, commas, and dots)
                    val cleanString = original.replace("[^0-9,.]".toRegex(), "")

                    if (cleanString.isBlank()) {
                        szerkesztes = false
                        return
                    }

                    val numberForCalculation = cleanString.replace(".", "").replace(",", ".")
                    val number = numberForCalculation.toDoubleOrNull() ?: 0.0

                    val formatted = DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
                        groupingSeparator = '.'  // Thousands separator as dot
                        decimalSeparator = ','   // Decimal separator as comma
                    }).format(number)

                    // Update current balance and save it only if it changes
                    if (aktualisPenz != number) {
                        aktualisPenz = number
                        saveBalance(aktualisPenz)
                    }

                    AktualisPenzEditText.setText(formatted)
                    AktualisPenzEditText.setSelection(formatted.length)
                } catch (e: Exception) {
                    // Handle error
                }

                szerkesztes = false
            }
        })

        // Add balance button
        val hozzaadasButton = findViewById<Button>(R.id.hozzaadas_gomb)
        hozzaadasButton.setOnClickListener {
            showAmountInputDialog("Hozzáadás", true)
        }

        // Deduct balance button
        val levonasButton = findViewById<Button>(R.id.levonas_gomb)
        levonasButton.setOnClickListener {
            showAmountInputDialog("Levonás", false)
        }

        // Initialize the dropdown menu
        val spinner: Spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                when (selectedItem) {
                    "Főoldal" -> {
                        // Only launch the main page if not already on it
                        if (this@Telefonszam::class.java != Telefonszam::class.java) {
                            val intent = Intent(this@Telefonszam, Telefonszam::class.java)
                            startActivity(intent)
                        }
                    }
                    "Elemzés" -> {
                        val intent = Intent(this@Telefonszam, ElemzesActivity::class.java)
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
                        Kijelentkezes()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}
