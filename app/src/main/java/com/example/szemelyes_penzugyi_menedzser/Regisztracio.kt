package com.example.szemelyes_penzugyi_menedzser

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.app.AlertDialog
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class Regisztracio : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_regisztracio)

        // jelszó ellenőrző üzenet inicializálása
        fun showAlertDialog(message: String) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hiba")
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }

        // Firebase Authentication inicializálása
        mAuth = FirebaseAuth.getInstance()

        // Felhasználói adatokat tartalmazó EditText mezők
        val felhasznalonev = findViewById<EditText>(R.id.felhasznalonev)
        val email = findViewById<EditText>(R.id.Email_regisztacio)
        val jelszo = findViewById<EditText>(R.id.Jelszo_regisztracio)
        val jelszo_megerositese = findViewById<EditText>(R.id.Jelszo_megerositese)

        // Regisztráció gomb
        val regisztracio_vegrehajtas = findViewById<Button>(R.id.Regisztracio_vegrehajtas)

        // A gomb megnyomásakor végrehajtandó művelet
        regisztracio_vegrehajtas.setOnClickListener {
            val emailText = email.text.toString()
            val jelszoText = jelszo.text.toString()
            val jelszoMegerositeseText = jelszo_megerositese.text.toString()

            // Ellenőrizzük, hogy a jelszavak megegyeznek-e
            if (jelszoText != jelszoMegerositeseText) {
                Toast.makeText(this, "A jelszavak nem egyeznek!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ellenőrizzük, hogy az email formátum helyes-e
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Érvénytelen email cím!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ellenőrizzük, hogy a jelszó formátuma megfelelő-e
            if (!isValidPassword(jelszoText)) {
                showAlertDialog("A jelszó nem megfelelő. Legalább 8 karakter, tartalmazzon kis- és nagybetűket, számot és speciális karaktert.")
                return@setOnClickListener
            }

            // Regisztráljuk a felhasználót Firebase Authentication segítségével
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
                                                // Ha az email megerősítése sikeres volt, átirányítjuk a bejelentkezés oldalra
                                                val intent = Intent(this@Regisztracio, Bejelentkezes::class.java)
                                                startActivity(intent)
                                                finish()  // lezárja az aktuális aktivitást
                                            } else {
                                                // Ha még nincs megerősítve, próbálkozzunk újra
                                                handler.postDelayed(this, 3000)  // 3 másodpercenként
                                            }
                                        }
                                    }
                                }
                                handler.post(runnable)  // Elindítja a futtatást
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
    }

    // Jelszó formátum ellenőrzése
    private fun isValidPassword(password: String): Boolean {
        // Minimum 8 karakter, kis- és nagybetű, szám és speciális karakter
        val regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
        return password.matches(regex.toRegex())
    }
}
