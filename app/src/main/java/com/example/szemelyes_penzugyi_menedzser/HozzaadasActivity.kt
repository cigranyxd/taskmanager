package com.example.szemelyes_penzugyi_menedzser

import KategoriaAdapter
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import android.graphics.Color
import android.util.Log

class HozzaadasActivity : AppCompatActivity() {

    private var kivalasztottKategoria: String? = null  // Kiválasztott kategória tárolása
    private var tranzakcioTipus: String? = null  // Bevétel vagy kiadás tárolása
    private var selectedView: View? = null  // A kijelölt item tárolása

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hozaadas)

        val osszegInput = findViewById<EditText>(R.id.osszegInput)
        val tranzakcioTipusSpinner = findViewById<Spinner>(R.id.tranzakcioTipusSpinner)
        val mentesGomb = findViewById<Button>(R.id.mentesGomb)
        val leirasInput = findViewById<EditText>(R.id.leirasInput)
        val kategoriakGridView = findViewById<GridView>(R.id.kategoriakGridView)

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

        val kategoriakAdapter = KategoriaAdapter(this, kategoriakNevek, kategoriakIkonok)
        kategoriakGridView.adapter = kategoriakAdapter

        // Kategória kiválasztása GridView-ban
        kategoriakGridView.setOnItemClickListener { parent, view, position, _ ->
            // Ha van előzőleg kijelölt elem, állítsuk vissza az alap háttérszínt
            selectedView?.setBackgroundColor(Color.TRANSPARENT)

            // Most válasszuk ki az új elemet
            selectedView = view
            view.setBackgroundColor(Color.LTGRAY)  // Kijelölés vizuális jele (szürke háttér)

            kivalasztottKategoria = if (tranzakcioTipus == "Bevétel") {
                // Ha Bevétel van kiválasztva, csak a megfelelő kategóriákat engedjük
                val ujKategoriak = listOf("Fizetési csekk", "Ajándékok", "Egyéb")
                if (position in 0..2) ujKategoriak[position] else null
            } else {
                // Kiadás esetén minden kategória választható
                kategoriakNevek[position]
            }

            if (kivalasztottKategoria != null) {
                Toast.makeText(this, "Kiválasztott kategória: $kivalasztottKategoria", Toast.LENGTH_SHORT).show()
            }
        }

        // Tranzakció típus kiválasztása Spinner-ből (Bevétel/Kiadás)
        val tranzakcioTipusNevek = listOf("Bevétel", "Kiadás")
        val tranzakcioTipusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tranzakcioTipusNevek)
        tranzakcioTipusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tranzakcioTipusSpinner.adapter = tranzakcioTipusAdapter

        tranzakcioTipusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {  // Ha Bevétel van kiválasztva
                    val ujKategoriak = listOf("Fizetési csekk", "Ajándékok", "Egyéb")
                    val ujKategoriakIkonok = listOf(
                        R.drawable.szabadido_icon, R.drawable.ajandekok_icon, R.drawable.egyeb_icon
                    )
                    val ujKategoriakAdapter = KategoriaAdapter(this@HozzaadasActivity, ujKategoriak, ujKategoriakIkonok)
                    kategoriakGridView.adapter = ujKategoriakAdapter
                } else {
                    val kategoriakAdapter = KategoriaAdapter(this@HozzaadasActivity, kategoriakNevek, kategoriakIkonok)
                    kategoriakGridView.adapter = kategoriakAdapter
                }
                tranzakcioTipus = tranzakcioTipusNevek[position]
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                tranzakcioTipus = null
            }
        }

        // Mentés gomb eseménykezelő
        mentesGomb.setOnClickListener {
            val osszeg = osszegInput.text.toString().toDoubleOrNull()
            val leiras = leirasInput.text.toString()

            val firestore = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = currentUser.uid
            val dokNev = LocalDate.now().toString()

            if (osszeg == null || leiras.isBlank() || tranzakcioTipus == null || kivalasztottKategoria == null) {
                Toast.makeText(this, "Érvényes összeget, leírást, tranzakció típust és kategóriát adj meg!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = hashMapOf<String, Any>(
                "mennyiseg" to osszeg,
                "kategoria" to (kivalasztottKategoria ?: "Nincs kategória"),
                "datum" to Timestamp.now(),
                "leiras" to leiras,
                "tipus" to (tranzakcioTipus ?: "Nincs megadva")
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
                                // Frissítsük a fő egyenleget
                                if (tranzakcioTipus == "Bevétel") {
                                    updateMainBalance(osszeg.toFloat())
                                } else if (tranzakcioTipus == "Kiadás") {
                                    updateMainBalance(-osszeg.toFloat())
                                }
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
                                // Frissítsük a fő egyenleget
                                if (tranzakcioTipus == "Bevétel") {
                                    updateMainBalance(osszeg.toFloat())
                                } else if (tranzakcioTipus == "Kiadás") {
                                    updateMainBalance(-osszeg.toFloat())
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Frissíti a felhasználó fő egyenlegét (az "aktualisPenz" mezőt) úgy, hogy hozzáadja (pozitív delta)
     * vagy kivonja (negatív delta) a megadott értéket.
     * Ez a verzió tranzakciót használ a megbízható frissítéshez.
     */
    fun updateMainBalance(delta: Float) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val currentBalance = snapshot.getDouble("aktualisPenz") ?: 0.0
            val newBalance = currentBalance + delta
            transaction.update(userDocRef, "aktualisPenz", newBalance)
            newBalance
        }.addOnSuccessListener { newBalance ->
            Log.d("HozzaadasActivity", "Main balance updated to $newBalance")
        }.addOnFailureListener { e ->
            Log.e("HozzaadasActivity", "Failed to update main balance: ${e.message}")
        }
    }
}
