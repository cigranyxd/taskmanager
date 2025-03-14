package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

        // Eredeti bejelentkezési elemek
        val email = findViewById<EditText>(R.id.Email)
        val jelszo = findViewById<EditText>(R.id.Jelszo)
        val loginButton = findViewById<Button>(R.id.Bejelentkezes_gomb)
        val regisztracioGomb = findViewById<Button>(R.id.Regisztracio_gomb_atvezeto)
        val togglePasswordButton = findViewById<ImageButton>(R.id.togglePasswordButton)

        // Alapértelmezett: a jelszó rejtett
        jelszo.transformationMethod = PasswordTransformationMethod.getInstance()
        var passwordVisible = false

        togglePasswordButton.setOnClickListener {
            if (passwordVisible) {
                jelszo.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePasswordButton.setImageResource(R.drawable.visibility_off)
                passwordVisible = false
            } else {
                jelszo.transformationMethod = null
                togglePasswordButton.setImageResource(R.drawable.visibility)
                passwordVisible = true
            }
            // Cursor pozíció megtartása
            jelszo.setSelection(jelszo.text.length)
        }

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

        regisztracioGomb.setOnClickListener {
            val intent = Intent(this, Regisztracio::class.java)
            startActivity(intent)
        }

        // Forgot Password elemek
        val forgotPasswordText = findViewById<TextView>(R.id.elfelejtettemJelszavam)
        val forgotPasswordLayout = findViewById<View>(R.id.forgotPasswordLayout)
        val resetPasswordPrompt = findViewById<TextView>(R.id.resetPasswordPrompt)
        val resetEmailInput = findViewById<EditText>(R.id.resetEmailInput)
        val resetPasswordButton = findViewById<Button>(R.id.resetPasswordButton)
        val confirmationMessage = findViewById<TextView>(R.id.confirmationMessage)
        val backToLoginText = findViewById<TextView>(R.id.backToLoginText)

        // A jelszó visszaállító felület alapértelmezett állapotban rejtve van
        forgotPasswordLayout.visibility = View.GONE

        // "Elfelejtettem a jelszavam" gombra kattintva az eredeti elemek eltűnnek,
        // és megjelenik a jelszó visszaállító felület
        forgotPasswordText.setOnClickListener {
            email.visibility = View.GONE
            jelszo.visibility = View.GONE
            forgotPasswordText.visibility = View.GONE
            findViewById<CheckBox>(R.id.Maradjak_bejelentkezve).visibility = View.GONE
            loginButton.visibility = View.GONE
            regisztracioGomb.visibility = View.GONE
            togglePasswordButton.visibility = View.GONE

            forgotPasswordLayout.visibility = View.VISIBLE
        }

        // Jelszó visszaállítása gomb
        resetPasswordButton.setOnClickListener {
            val resetEmail = resetEmailInput.text.toString()
            if (!Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                Toast.makeText(this, "Kérjük, adjon meg egy érvényes e-mail címet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(resetEmail).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    resetPasswordPrompt.visibility = View.GONE
                    resetEmailInput.visibility = View.GONE
                    resetPasswordButton.visibility = View.GONE
                    confirmationMessage.visibility = View.VISIBLE
                    backToLoginText.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Hiba történt az e-mail küldésekor.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Vissza a kezdőképernyőre felirat
        backToLoginText.setOnClickListener {
            confirmationMessage.visibility = View.GONE
            backToLoginText.visibility = View.GONE
            resetEmailInput.visibility = View.VISIBLE
            resetPasswordButton.visibility = View.VISIBLE
            resetPasswordPrompt.visibility = View.VISIBLE

            email.visibility = View.VISIBLE
            jelszo.visibility = View.VISIBLE
            forgotPasswordText.visibility = View.VISIBLE
            findViewById<CheckBox>(R.id.Maradjak_bejelentkezve).visibility = View.VISIBLE
            loginButton.visibility = View.VISIBLE
            regisztracioGomb.visibility = View.VISIBLE
            togglePasswordButton.visibility = View.VISIBLE

            resetEmailInput.text.clear()
            forgotPasswordLayout.visibility = View.GONE
        }

        applyFontSizeToCurrentActivity()
    }

    // Az XML-ből meghívható forgot password metódus
    fun onForgotPasswordClick(view: View) {
        findViewById<EditText>(R.id.Email).visibility = View.GONE
        findViewById<EditText>(R.id.Jelszo).visibility = View.GONE
        findViewById<TextView>(R.id.elfelejtettemJelszavam).visibility = View.GONE
        findViewById<CheckBox>(R.id.Maradjak_bejelentkezve).visibility = View.GONE
        findViewById<Button>(R.id.Bejelentkezes_gomb).visibility = View.GONE
        findViewById<Button>(R.id.Regisztracio_gomb_atvezeto).visibility = View.GONE
        findViewById<ImageButton>(R.id.togglePasswordButton).visibility = View.GONE

        findViewById<View>(R.id.forgotPasswordLayout).visibility = View.VISIBLE
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
}
