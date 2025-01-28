package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Bejelentkezes : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.bejelentkezes)

        auth = FirebaseAuth.getInstance()

        // Ellenőrizzük, hogy a felhasználó már be van-e jelentkezve
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            val intent = Intent(this, Telefonszam::class.java)
            startActivity(intent)
            finish()
        }

        val email = findViewById<EditText>(R.id.Email)
        val jelszo = findViewById<EditText>(R.id.Jelszo)
        val loginButton = findViewById<Button>(R.id.Bejelentkezes_gomb)

        loginButton.setOnClickListener {
            val emailText = email.text.toString()
            val jelszoText = jelszo.text.toString()

            // E-mail formátum ellenőrzése
            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Kérjük, adjon meg egy érvényes e-mail címet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (emailText.isNotEmpty() && jelszoText.isNotEmpty()) {
                auth.signInWithEmailAndPassword(emailText, jelszoText)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Ellenőrizzük, hogy az e-mail cím hitelesítve van-e
                            val user = auth.currentUser
                            if (user != null && user.isEmailVerified) {
                                // Ha hitelesített, mentjük az állapotot
                                sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

                                val intent = Intent(this, Telefonszam::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Kérjük, erősítse meg az e-mail címét.", Toast.LENGTH_SHORT).show()
                                user?.sendEmailVerification()
                            }
                        } else {
                            Toast.makeText(this, "Azonosítás sikertelen.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Kérjük írja be E-mail címét és jelszavát.", Toast.LENGTH_SHORT).show()
            }
        }

        val regisztracio_gomb = findViewById<Button>(R.id.Regisztracio_gomb_atvezeto)

        regisztracio_gomb.setOnClickListener {
            val intent = Intent(this, Regisztracio::class.java)
            startActivity(intent)
        }
    }



}
