package com.example.szemelyes_penzugyi_menedzser

import KategoriaAdapter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class HozzaadasActivity : AppCompatActivity() {

    private var kivalasztottKategoria: String? = null   // Kiválasztott kategória
    private var tranzakcioTipus: String? = null          // "Bevétel" vagy "Kiadás"
    private var selectedView: View? = null                // A kijelölt elem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hozaadas)

        // Nézetek inicializálása
        val osszegInput = findViewById<EditText>(R.id.osszegInput)
        val tranzakcioTipusSpinner = findViewById<Spinner>(R.id.tranzakcioTipusSpinner)
        val mentesGomb = findViewById<Button>(R.id.mentesGomb)
        val leirasInput = findViewById<EditText>(R.id.leirasInput)
        val kategoriakGridView = findViewById<GridView>(R.id.kategoriakGridView)

        // Spinner beállítása
        val tranzakcioTipusNevek = listOf("Bevétel", "Kiadás")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tranzakcioTipusNevek)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tranzakcioTipusSpinner.adapter = spinnerAdapter

        // Alapértelmezett érték legyen "Bevétel"
        tranzakcioTipusSpinner.setSelection(0)
        tranzakcioTipus = "Bevétel"

        // Definiáljuk az alapértelmezett kategóriákat külön listákban:
        val defaultBevetelek = listOf("Fizetési csekk", "Ajándékok", "Egyéb")
        val defaultBevetelekIkonok = listOf(R.drawable.szabadido_icon, R.drawable.ajandekok_icon, R.drawable.egyeb_icon)
        val defaultKiadasok = listOf(
            "Egészség", "Szabadidő", "Otthon", "Kávézó", "Oktatás", "Ajándékok",
            "Élelmiszerek", "Család", "Edzés", "Közlekedés", "Egyéb"
        )
        val defaultKiadasokIkonok = listOf(
            R.drawable.egeszseg_icon, R.drawable.szabadido_icon, R.drawable.otthon_icon,
            R.drawable.kavezo_icon, R.drawable.oktatas_icon, R.drawable.ajandekok_icon,
            R.drawable.elelmiszerek_icon, R.drawable.csalad_icon, R.drawable.edzes_icon,
            R.drawable.kozlekedes_icon, R.drawable.egyeb_icon
        )

        // Inicializáljuk az adaptert a spinner alapértelmezett értékének megfelelően ("Bevétel").
        // Mivel a Firestore-ból történő betöltés aszinkron, a custom kategóriák listája lehet, hogy üres.
        val initialNames = defaultBevetelek + EgyediKategoriak.kategoriak.filter {
            it.tipus.equals("Bevétel", ignoreCase = true)
        }.map { it.nev }
        val initialIcons = defaultBevetelekIkonok + EgyediKategoriak.kategoriak.filter {
            it.tipus.equals("Bevétel", ignoreCase = true)
        }.map { it.ikon }
        val kategoriakAdapter = KategoriaAdapter(this, initialNames, initialIcons)
        kategoriakGridView.adapter = kategoriakAdapter

        // Spinner eseménykezelő: csak akkor frissítjük az adaptert, ha a felhasználó vált.
        var firstSelection = true
        tranzakcioTipusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (firstSelection) {
                    firstSelection = false
                    return
                }
                tranzakcioTipus = tranzakcioTipusNevek[position]
                updateAdapterFor(tranzakcioTipusNevek[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                tranzakcioTipus = null
            }
        }

        // Ha a felhasználó be van jelentkezve, Firestore-ból betöltjük a custom kategóriákat, majd frissítjük az adaptert
        if (FirebaseAuth.getInstance().currentUser != null) {
            EgyediKategoriak.betoltFirestore(this, FirebaseAuth.getInstance().currentUser!!.uid) {
                // Miután a Firestore-ból betöltődtek a custom kategóriák, frissítjük az adaptert a spinner aktuális értéke alapján.
                val currentType = tranzakcioTipusSpinner.selectedItem?.toString() ?: "Bevétel"
                updateAdapterFor(currentType)
            }
        } else {
            EgyediKategoriak.betolt(this)
        }

        kategoriakGridView.setOnItemClickListener { parent, view, position, _ ->
            // Visszaállítjuk a korábbi kiválasztást
            kategoriakAdapter.notifyDataSetChanged() // Ez szükséges lehet az új adapter frissítéséhez
            // A kiválasztott elem háttérszínének kezelését az adapter kezeli, ha implementálva van (lásd KategoriaAdapter)
            kivalasztottKategoria = parent.getItemAtPosition(position) as? String
            if (kivalasztottKategoria != null) {
                Toast.makeText(this, "Kiválasztott kategória: $kivalasztottKategoria", Toast.LENGTH_SHORT).show()
            }
        }

        mentesGomb.setOnClickListener {
            val osszeg = osszegInput.text.toString().toDoubleOrNull()
            val leiras = leirasInput.text.toString()

            val firestore = FirebaseFirestore.getInstance()
            if (FirebaseAuth.getInstance().currentUser == null) {
                Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
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
                                if (tranzakcioTipus.equals("Bevétel", ignoreCase = true)) {
                                    updateMainBalance(osszeg.toFloat())
                                } else if (tranzakcioTipus.equals("Kiadás", ignoreCase = true)) {
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
                                if (tranzakcioTipus.equals("Bevétel", ignoreCase = true)) {
                                    updateMainBalance(osszeg.toFloat())
                                } else if (tranzakcioTipus.equals("Kiadás", ignoreCase = true)) {
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

    // Frissíti az adapter tartalmát a megadott tranzakció típus alapján
    private fun updateAdapterFor(tipus: String) {
        val kategoriakGridView = findViewById<GridView>(R.id.kategoriakGridView)
        val defaultBevetelek = listOf("Fizetési csekk", "Ajándékok", "Egyéb")
        val defaultBevetelekIkonok = listOf(R.drawable.szabadido_icon, R.drawable.ajandekok_icon, R.drawable.egyeb_icon)
        val defaultKiadasok = listOf(
            "Egészség", "Szabadidő", "Otthon", "Kávézó", "Oktatás", "Ajándékok",
            "Élelmiszerek", "Család", "Edzés", "Közlekedés", "Egyéb"
        )
        val defaultKiadasokIkonok = listOf(
            R.drawable.egeszseg_icon, R.drawable.szabadido_icon, R.drawable.otthon_icon,
            R.drawable.kavezo_icon, R.drawable.oktatas_icon, R.drawable.ajandekok_icon,
            R.drawable.elelmiszerek_icon, R.drawable.csalad_icon, R.drawable.edzes_icon,
            R.drawable.kozlekedes_icon, R.drawable.egyeb_icon
        )
        if (tipus.equals("Bevétel", ignoreCase = true)) {
            val customBevetel = EgyediKategoriak.kategoriak.filter { it.tipus.trim().toLowerCase() == "bevétel" }
            val finalNames = defaultBevetelek + customBevetel.map { it.nev }
            val finalIcons = defaultBevetelekIkonok + customBevetel.map { it.ikon }
            val newAdapter = KategoriaAdapter(this, finalNames, finalIcons)
            kategoriakGridView.adapter = newAdapter
        } else {
            val customKiadas = EgyediKategoriak.kategoriak.filter { it.tipus.trim().toLowerCase() == "kiadás" }
            val finalNames = defaultKiadasok + customKiadas.map { it.nev }
            val finalIcons = defaultKiadasokIkonok + customKiadas.map { it.ikon }
            val newAdapter = KategoriaAdapter(this, finalNames, finalIcons)
            kategoriakGridView.adapter = newAdapter
        }
    }

    /**
     * Frissíti a felhasználó fő egyenlegét az "aktualisPenz" mezőben úgy, hogy hozzáadja (pozitív delta)
     * vagy kivonja (negatív delta) a megadott értéket.
     */
    fun updateMainBalance(delta: Float) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
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
