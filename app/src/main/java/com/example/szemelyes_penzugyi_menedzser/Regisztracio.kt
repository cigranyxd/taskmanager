package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class Regisztracio : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_regisztracio)

        // Firebase Authentication inicializálása
        mAuth = FirebaseAuth.getInstance()

        // Felhasználói adatokat tartalmazó EditText mezők
        val felhasznalonev = findViewById<EditText>(R.id.et_username)
        val email = findViewById<EditText>(R.id.et_email)
        val jelszo = findViewById<EditText>(R.id.et_password)
        val jelszoMegerositese = findViewById<EditText>(R.id.et_confirm_password)

        // Regisztráció gomb
        val regisztracioGomb = findViewById<Button>(R.id.btn_register)

        regisztracioGomb.setOnClickListener {
            val emailText = email.text.toString()
            val jelszoText = jelszo.text.toString()
            val jelszoMegerositeseText = jelszoMegerositese.text.toString()

            if (jelszoText != jelszoMegerositeseText) {
                Toast.makeText(this, "A jelszavak nem egyeznek!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Érvénytelen email cím!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(jelszoText)) {
                showAlertDialog("A jelszó nem megfelelő. Legalább 8 karakter, tartalmazzon kis- és nagybetűket, számot és speciális karaktert.")
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(emailText, jelszoText)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser
                        user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(this, "Sikeres regisztráció! Kérjük, erősítse meg az e-mail címét.", Toast.LENGTH_LONG).show()

                                // Automatikus email ellenőrzés (reload) 3 másodpercenként
                                val handler = Handler(Looper.getMainLooper())
                                val runnable = object : Runnable {
                                    override fun run() {
                                        user.reload().addOnCompleteListener { reloadTask ->
                                            if (reloadTask.isSuccessful && user.isEmailVerified) {
                                                val intent = Intent(this@Regisztracio, Bejelentkezes::class.java)
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                handler.postDelayed(this, 3000)
                                            }
                                        }
                                    }
                                }
                                handler.post(runnable)
                            } else {
                                Toast.makeText(this, "Hiba történt az ellenőrző e-mail küldésekor: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Ez az email cím már regisztrálva van.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "A regisztráció sikertelen: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        val bejelentkezesSzoveg = findViewById<TextView>(R.id.tv_login)
        bejelentkezesSzoveg.setOnClickListener {
            startActivity(Intent(this, Bejelentkezes::class.java))
        }

        applyFontSizeToCurrentActivity()
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
        if (view is TextView) view.textSize = fontSize
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextViewsFontSize(view.getChildAt(i), fontSize)
            }
        }
    }

    private fun showAlertDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Hiba")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

// Jelszó formátum ellenőrzése
private fun isValidPassword(password: String): Boolean {
    val regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
    return password.matches(regex.toRegex())
}
