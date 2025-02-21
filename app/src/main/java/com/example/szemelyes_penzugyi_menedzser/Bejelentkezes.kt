package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Bejelentkezes : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bejelentkezes)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.Email)
        val jelszo = findViewById<EditText>(R.id.Jelszo)
        val loginButton = findViewById<Button>(R.id.Bejelentkezes_gomb)
        val regisztracioGomb = findViewById<TextView>(R.id.SignUpText)
        val passwordToggle = findViewById<ImageButton>(R.id.PasswordToggle)

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                jelszo.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                jelszo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(android.R.drawable.ic_menu_view)
            }
            jelszo.setSelection(jelszo.text.length)
        }

        loginButton.setOnClickListener {
            val emailText = email.text.toString()
            val jelszoText = jelszo.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Kérjük, adjon meg egy érvényes e-mail címet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (emailText.isNotEmpty() && jelszoText.isNotEmpty()) {
                auth.signInWithEmailAndPassword(emailText, jelszoText)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null && user.isEmailVerified) {
                                startActivity(Intent(this, Telefonszam::class.java))
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

        regisztracioGomb.setOnClickListener {
            startActivity(Intent(this, Regisztracio::class.java))
        }
    }
}
