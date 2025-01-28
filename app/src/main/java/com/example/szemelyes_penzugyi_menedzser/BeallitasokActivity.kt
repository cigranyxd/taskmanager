package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

@Suppress("DEPRECATION")
class BeallitasokActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beallitasok)

        val betumeretButton = findViewById<Button>(R.id.betumeret_button)
        val nyelvButton = findViewById<Button>(R.id.nyelv_button)
        val jelszoButton = findViewById<Button>(R.id.jelszo_button)
        val profilTorlesButton = findViewById<Button>(R.id.profil_torles_button)



        betumeretButton.setOnClickListener { showBetumeretDialog() }
        nyelvButton.setOnClickListener { showNyelvDialog() }
        jelszoButton.setOnClickListener { showJelszoDialog() }
        profilTorlesButton.setOnClickListener { showProfilTorlesDialog() }

        // Betűméret frissítése
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
        if (view is TextView) {
            view.textSize = fontSize
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextViewsFontSize(view.getChildAt(i), fontSize)
            }
        }
    }

    private fun showBetumeretDialog() {
        val betumeretek = arrayOf("Kicsi", "Közepes", "Nagy")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Betűméret beállítása")
            .setItems(betumeretek) { _, which ->
                val selectedSize = betumeretek[which]
                val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                prefs.edit().putString("betumeret", selectedSize).apply()

                // Az aktivitás újraindítása a betűméret frissítése érdekében
                val intent = Intent(this, BeallitasokActivity::class.java)
                finish()
                startActivity(intent)
            }
        builder.create().show()
    }

    private fun showNyelvDialog() {
        val nyelvek = arrayOf("Magyar", "Angol", "Német")
        val nyelvkodok = arrayOf("hu", "en", "de") // Nyelvek kódjai
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nyelv kiválasztása")
            .setItems(nyelvek) { _, which ->
                val selectedLanguage = nyelvek[which]
                val selectedLangCode = nyelvkodok[which]
                val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                prefs.edit().putString("nyelv", selectedLangCode).apply()

                // Nyelv beállítása és Activity újraindítása
                setLocale(selectedLangCode)
                Toast.makeText(this, "Nyelv: $selectedLanguage", Toast.LENGTH_SHORT).show()

                // Az Activity újraindítása
                val intent = intent
                finish()
                startActivity(intent)
            }
        builder.create().show()
    }

    // Nyelv beállítása (Locale frissítése)
    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun showJelszoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Jelszó változtatás")

        // LinearLayout a két input mezőhöz
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Jelenlegi jelszó input mező
        val currentPasswordInput = EditText(this)
        currentPasswordInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        currentPasswordInput.hint = "Jelenlegi jelszó"
        layout.addView(currentPasswordInput)

        // Új jelszó input mező
        val newPasswordInput = EditText(this)
        newPasswordInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        newPasswordInput.hint = "Új jelszó"
        layout.addView(newPasswordInput)

        builder.setView(layout)

        builder.setPositiveButton("Mentés") { _, _ ->
            val currentPassword = currentPasswordInput.text.toString()
            val newPassword = newPasswordInput.text.toString()

            if (currentPassword.isNotBlank() && newPassword.isNotBlank()) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val email = user.email
                    if (email != null) {
                        val credential = EmailAuthProvider.getCredential(email, currentPassword)

                        // Újrahitelesítés
                        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                            if (reauthTask.isSuccessful) {
                                user.updatePassword(newPassword)
                                    .addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            Toast.makeText(
                                                this,
                                                "Jelszó frissítve!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                this,
                                                "Hiba történt: ${updateTask.exception?.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Újrahitelesítés sikertelen: ${reauthTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Hiba: Az e-mail cím nem érhető el.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "A mezők nem lehetnek üresek!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Mégse") { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }


    private fun Kijelentkezes() {
        // Kijelentkeztetjük a felhasználót
        FirebaseAuth.getInstance().signOut()

        // Töröljük a belépési állapotot a SharedPreferences-ben
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()

        // Toast üzenet a kijelentkezés sikerességéről
        Toast.makeText(this, "Kijelentkezve!", Toast.LENGTH_SHORT).show()
    }

    private fun showProfilTorlesDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Profil törlése")
            .setMessage("Biztosan törölni szeretné a profilját?")

        val passwordInput = EditText(this)
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordInput.hint = "Jelenlegi jelszó"
        builder.setView(passwordInput)

        builder.setPositiveButton("Igen") { _, _ ->
            val currentPassword = passwordInput.text.toString()
            if (currentPassword.isNotBlank()) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val email = user.email
                    if (email != null) {
                        val credential = EmailAuthProvider.getCredential(email, currentPassword)

                        // Újrahitelesítés
                        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                            if (reauthTask.isSuccessful) {
                                // Profil törlése
                                user.delete().addOnCompleteListener { deleteTask ->
                                    if (deleteTask.isSuccessful) {
                                        // Kijelentkezés
                                        Kijelentkezes()

                                        // Toast üzenet
                                        Toast.makeText(this, "Profil törölve. Kijelentkeztetve.", Toast.LENGTH_SHORT).show()

                                        // Újraindítjuk az alkalmazást a Bejelentkezési oldallal
                                        val intent = Intent(applicationContext, Bejelentkezes::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        startActivity(intent)
                                        // Az aktuális aktivitás befejezése
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Hiba történt: ${deleteTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Újrahitelesítés sikertelen: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Az e-mail cím nem érhető el.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "A jelszó mező nem lehet üres!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Mégse") { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }



}


