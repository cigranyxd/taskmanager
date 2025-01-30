package com.example.szemelyes_penzugyi_menedzser

import KategoriaAdapter
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.szemelyes_penzugyi_menedzser.ui.theme.Szemelyes_penzugyi_menedzserTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class HozzaadasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hozaadas)

        val osszegInput = findViewById<EditText>(R.id.osszegInput)
        val kategoriakSpinner = findViewById<Spinner>(R.id.kategoriakSpinner)
        val mentesGomb = findViewById<Button>(R.id.mentesGomb)
        val leirasInput = findViewById<EditText>(R.id.leirasInput)
        val kategoriakGridView = findViewById<GridView>(R.id.kategoriakGridView)

        // Kategóriák és ikonok beállítása
        val kategoriakNevek = listOf(
            "Egészség", "Szabadidő", "Otthon", "Kávézó", "Oktatás", "Ajándékok",
            "Élelmiszerek", "Család", "Edzés", "Közlekedés", "Egyéb"
        )

        val kategoriakIkonok = listOf(
            R.drawable.egeszseg_icon, R.drawable.szabadido_icon, R.drawable.otthon_icon,
            R.drawable.kavezo_icon, R.drawable.oktatas_icon, R.drawable.ajandekok_icon,
            R.drawable.elelmiszerek_icon, R.drawable.csalad_icon, R.drawable.edzes_icon,
            R.drawable.kozlekedes_icon, R.drawable.egyeb_icon
        )

        val kategoriakAdapter =KategoriaAdapter(this, kategoriakNevek, kategoriakIkonok)
        kategoriakGridView.adapter = kategoriakAdapter

        // Kategória kiválasztása GridView-ban
        kategoriakGridView.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = kategoriakNevek[position]
            Toast.makeText(this, "Kiválasztott kategória: $selectedCategory", Toast.LENGTH_SHORT).show()
        }

        // Mentés gomb eseménykezelő
        mentesGomb.setOnClickListener {
            val osszeg = osszegInput.text.toString().toDoubleOrNull()
            val kategoria = kategoriakSpinner.selectedItem.toString()
            val leiras = leirasInput.text.toString()

            val firestore = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = currentUser.uid
            val dokNev = LocalDate.now().toString()

            if (osszeg != null && leiras.isNotBlank()) {
                val transaction = hashMapOf<String, Any>(
                    "mennyiseg" to osszeg,
                    "kategoria" to kategoria,
                    "datum" to Timestamp.now(),
                    "leiras" to leiras
                )

                firestore.collection("users")
                    .document(uid)
                    .collection("nap")
                    .document(dokNev)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val existingTransactions = document.get("tranzakciok") as? List<HashMap<String, Any>> ?: mutableListOf()
                            val mutableTransactions = existingTransactions.toMutableList()
                            mutableTransactions.add(transaction)

                            firestore.collection("users")
                                .document(uid)
                                .collection("nap")
                                .document(dokNev)
                                .update("tranzakciok", mutableTransactions)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Sikeresen mentve: $dokNev", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            val transactions = mutableListOf(transaction)

                            firestore.collection("users")
                                .document(uid)
                                .collection("nap")
                                .document(dokNev)
                                .set(mapOf("tranzakciok" to transactions))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Sikeresen mentve: $dokNev", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Érvényes összeget és leírást adj meg!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

