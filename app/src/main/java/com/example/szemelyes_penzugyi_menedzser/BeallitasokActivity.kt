package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

@Suppress("DEPRECATION")
class BeallitasokActivity : AppCompatActivity() {

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

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Új jelszó"
        builder.setView(input)

        builder.setPositiveButton("Mentés") { _, _ ->
            val newPassword = input.text.toString()
            if (newPassword.isNotBlank()) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // Jelszó újrahitelesítés
                    val email = user.email
                    val currentPassword = "jelenlegi_jelszo" // Ezt valós inputból kellene beszerezni
                    val credential = EmailAuthProvider.getCredential(email!!, currentPassword)

                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            user.updatePassword(newPassword).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Jelszó frissítve!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Hiba történt: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Újrahitelesítés sikertelen.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "A jelszó nem lehet üres!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Mégse") { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    private fun showProfilTorlesDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Profil törlése")
            .setMessage("Biztosan törölni szeretné a profilját?")
            .setPositiveButton("Igen") { _, _ ->
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // Újrahitelesítés a profil törlés előtt
                    val email = user.email
                    val currentPassword = "jelenlegi_jelszo" // Ezt valós inputból kellene beszerezni
                    val credential = EmailAuthProvider.getCredential(email!!, currentPassword)

                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            user.delete().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Profil törölve.", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, Bejelentkezes::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Hiba történt: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Újrahitelesítés sikertelen.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Mégse") { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }
}
