package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Suppress("DEPRECATION")
class BeallitasokActivity :  BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beallitasok)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        applyFontSizeToCurrentActivity()

        findViewById<Button>(R.id.betumeret_button).setOnClickListener { showBetumeretDialog() }

        findViewById<Button>(R.id.jelszo_button).setOnClickListener { showJelszoDialog() }
        findViewById<Button>(R.id.profil_torles_button).setOnClickListener { showProfilTorlesDialog() }
        findViewById<Button>(R.id.penzugyi_adatok_torles_button).setOnClickListener { showPenzugyiAdatokTorlesDialog() }

        spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val spinnerAdapter = CustomFontSizeSpinnerAdapter(this, R.layout.spinner_item, lehetosegek)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = spinnerAdapter
        spinner.setSelection(4)

        var elsoFutas = true
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (elsoFutas) {
                    elsoFutas = false
                    return
                }
                when (parent.getItemAtPosition(position).toString()) {
                    "Főoldal" -> startActivity(Intent(this@BeallitasokActivity, Telefonszam::class.java))
                    "Elemzés" -> startActivity(Intent(this@BeallitasokActivity, ElemzesActivity::class.java))
                    "Kategóriák" -> startActivity(Intent(this@BeallitasokActivity, Kategoriak::class.java))
                    "Rendszeres kifizetések" -> startActivity(Intent(this@BeallitasokActivity, RendszeresKifizetesek::class.java))
                    "Kijelentkezés" -> kijelentkezes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        spinner.setSelection(4)
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, Telefonszam::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun kijelentkezes() {
        FirebaseAuth.getInstance().signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit().putBoolean("isLoggedIn", false).apply()
        startActivity(Intent(this, Bejelentkezes::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun applyFontSizeToCurrentActivity() {
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val fontSize = prefs.getString("betumeret", "Közepes") ?: "Közepes"
        val size = when (fontSize) {
            "Kicsi" -> 12f
            "Nagy" -> 20f
            else -> 18f
        }
        updateTextViewsFontSize(findViewById(android.R.id.content), size)
    }

    private fun updateTextViewsFontSize(view: View, fontSize: Float) {
        if (view is TextView) view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            updateTextViewsFontSize(view.getChildAt(i), fontSize)
        }
    }

    private fun showBetumeretDialog() {
        val betumeretek = arrayOf("Kicsi", "Közepes", "Nagy")
        AlertDialog.Builder(this)
            .setTitle("Betűméret beállítása")
            .setItems(betumeretek) { _, which ->
                val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                prefs.edit().putString("betumeret", betumeretek[which]).apply()
                recreate()
            }
            .create().show()
    }

    private fun showNyelvDialog() {
        val nyelvek = arrayOf("Magyar", "Angol", "Német")
        val nyelvkodok = arrayOf("hu", "en", "de")
        AlertDialog.Builder(this)
            .setTitle("Nyelv kiválasztása")
            .setItems(nyelvek) { _, which ->
                setLocale(nyelvkodok[which])
                recreate()
                Toast.makeText(this, "Nyelv: ${nyelvek[which]}", Toast.LENGTH_SHORT).show()
            }
            .create().show()
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration().apply { setLocale(locale) }
        resources.updateConfiguration(config, resources.displayMetrics)
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit().putString("nyelv", languageCode).apply()
    }

    private fun showJelszoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Jelszó változtatás")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val currentPasswordInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Jelenlegi jelszó"
        }
        val newPasswordInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Új jelszó"
        }
        layout.addView(currentPasswordInput)
        layout.addView(newPasswordInput)
        builder.setView(layout)
        builder.setPositiveButton("Mentés") { _, _ ->
            val currentPassword = currentPasswordInput.text.toString()
            val newPassword = newPasswordInput.text.toString()
            val user = FirebaseAuth.getInstance().currentUser
            val email = user?.email
            if (!currentPassword.isBlank() && !newPassword.isBlank() && user != null && email != null) {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful)
                                Toast.makeText(this, "Jelszó frissítve!", Toast.LENGTH_SHORT).show()
                            else
                                Toast.makeText(this, "Hiba: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Hibás jelszó: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Töltsd ki mindkét mezőt!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Mégse") { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    private fun showProfilTorlesDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Profil törlése")
            .setMessage("Biztosan törölni szeretné a profilját?")
        val passwordInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Jelenlegi jelszó"
        }
        builder.setView(passwordInput)
        builder.setPositiveButton("Igen") { _, _ ->
            val currentPassword = passwordInput.text.toString()
            val user = FirebaseAuth.getInstance().currentUser
            val email = user?.email
            if (!currentPassword.isBlank() && user != null && email != null) {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                kijelentkezes()
                                Toast.makeText(this, "Profil törölve.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Hiba: ${deleteTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Hibás jelszó: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Töltsd ki a mezőt!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Mégse") { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    private fun showPenzugyiAdatokTorlesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Pénzügyi adatok törlése")
            .setMessage("Biztosan törölni szeretné az összes pénzügyi adatát?")
            .setPositiveButton("Igen") { _, _ -> torolPenzugyiAdatokat() }
            .setNegativeButton("Mégse") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun torolPenzugyiAdatokat() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).collection("nap").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Adatok törölve!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
