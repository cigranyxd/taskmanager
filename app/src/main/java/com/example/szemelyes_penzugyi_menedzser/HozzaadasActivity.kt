package com.example.szemelyes_penzugyi_menedzser

import KategoriaAdapter
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class HozzaadasActivity : AppCompatActivity() {

    private var kivalasztottKategoria: String? = null  // A kiválasztott kategória neve
    private var tranzakcioTipus: String? = null         // "Bevétel" vagy "Kiadás"
    private var kijeloltElem: View? = null               // A GridView-ban kijelölt elem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hozaadas)

        // Betöltjük az egyedi kategóriákat a SharedPreferences-ből
        EgyediKategoriak.load(this)

        val osszegInput = findViewById<EditText>(R.id.osszegInput)
        val tranzakcioTipusSpinner = findViewById<Spinner>(R.id.tranzakcioTipusSpinner)
        val mentesGomb = findViewById<Button>(R.id.mentesGomb)
        val leirasInput = findViewById<EditText>(R.id.leirasInput)
        val kategoriakGridView = findViewById<GridView>(R.id.kategoriakGridView)

        // Alapértelmezett kategóriák
        val alapKategoriakNevek = listOf(
            "Egészség", "Szabadidő", "Otthon", "Kávézó", "Oktatás", "Ajándékok",
            "Élelmiszerek", "Család", "Edzés", "Közlekedés", "Egyéb"
        )
        val alapKategoriakIkonok = listOf(
            R.drawable.egeszseg_icon, R.drawable.szabadido_icon, R.drawable.otthon_icon,
            R.drawable.kavezo_icon, R.drawable.oktatas_icon, R.drawable.ajandekok_icon,
            R.drawable.elelmiszerek_icon, R.drawable.csalad_icon, R.drawable.edzes_icon,
            R.drawable.kozlekedes_icon, R.drawable.egyeb_icon
        )

        // Kezdeti adapter: alapértelmezett kategóriák
        var aktualisAdapter = KategoriaAdapter(this, alapKategoriakNevek, alapKategoriakIkonok)
        kategoriakGridView.adapter = aktualisAdapter

        kategoriakGridView.setOnItemClickListener { _, view, position, _ ->
            kijeloltElem?.setBackgroundColor(Color.TRANSPARENT)
            kijeloltElem = view
            view.setBackgroundColor(Color.LTGRAY)
            val adapter = kategoriakGridView.adapter as KategoriaAdapter
            kivalasztottKategoria = adapter.getItem(position).toString()
            Toast.makeText(this, "Kiválasztott kategória: $kivalasztottKategoria", Toast.LENGTH_SHORT).show()
        }

        // Tranzakció típus Spinner
        val tranzakcioTipusNevek = listOf("Bevétel", "Kiadás")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tranzakcioTipusNevek)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tranzakcioTipusSpinner.adapter = spinnerAdapter

        tranzakcioTipusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val valasztottTipus = tranzakcioTipusNevek[position]
                tranzakcioTipus = valasztottTipus
                if (valasztottTipus == "Bevétel") {
                    val defaultBevetelNevek = listOf("Fizetési csekk", "Ajándékok", "Egyéb")
                    val defaultBevetelIkonok = listOf(
                        R.drawable.szabadido_icon, R.drawable.ajandekok_icon, R.drawable.egyeb_icon
                    )
                    loadCustomKategoriak("Bevétel") { customKategoriak ->
                        val vegsoNevek = defaultBevetelNevek.toMutableList()
                        val vegsoIkonok = defaultBevetelIkonok.toMutableList()
                        for ((nev, ikon) in customKategoriak) {
                            vegsoNevek.add(nev)
                            vegsoIkonok.add(ikon)
                        }
                        aktualisAdapter = KategoriaAdapter(this@HozzaadasActivity, vegsoNevek, vegsoIkonok)
                        kategoriakGridView.adapter = aktualisAdapter
                    }
                } else {
                    loadCustomKategoriak("Kiadás") { customKategoriak ->
                        val vegsoNevek = alapKategoriakNevek.toMutableList()
                        val vegsoIkonok = alapKategoriakIkonok.toMutableList()
                        for ((nev, ikon) in customKategoriak) {
                            vegsoNevek.add(nev)
                            vegsoIkonok.add(ikon)
                        }
                        aktualisAdapter = KategoriaAdapter(this@HozzaadasActivity, vegsoNevek, vegsoIkonok)
                        kategoriakGridView.adapter = aktualisAdapter
                    }
                }
            }
            override fun onNothingSelected(parentView: AdapterView<*>?) {
                tranzakcioTipus = null
            }
        }

        mentesGomb.setOnClickListener {
            val osszeg = osszegInput.text.toString().toDoubleOrNull()
            val leiras = leirasInput.text.toString()

            val firestore = FirebaseFirestore.getInstance()
            val aktualisFelhasznalo = FirebaseAuth.getInstance().currentUser

            if (aktualisFelhasznalo == null) {
                Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val felhasznaloId = aktualisFelhasznalo.uid
            val dokNev = LocalDate.now().toString()

            if (osszeg == null || leiras.isBlank() || tranzakcioTipus.isNullOrEmpty() || kivalasztottKategoria.isNullOrEmpty()) {
                Toast.makeText(this, "Adj meg érvényes összeget, leírást, tranzakció típust és kategóriát!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tranzakcio = hashMapOf<String, Any>(
                "mennyiseg" to osszeg,
                "kategoria" to kivalasztottKategoria!!,
                "datum" to Timestamp.now(),
                "leiras" to leiras,
                "tipus" to tranzakcioTipus!!
            )

            firestore.collection("users")
                .document(felhasznaloId)
                .collection("nap")
                .document(dokNev)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val elozoTranzakciok = document.get("tranzakciok") as? List<HashMap<String, Any>> ?: mutableListOf()
                        val modositottTranzakciok = elozoTranzakciok.toMutableList()
                        modositottTranzakciok.add(tranzakcio)
                        firestore.collection("users")
                            .document(felhasznaloId)
                            .collection("nap")
                            .document(dokNev)
                            .update("tranzakciok", modositottTranzakciok)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Sikeresen mentve: $dokNev", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        val tranzakciok = mutableListOf(tranzakcio)
                        firestore.collection("users")
                            .document(felhasznaloId)
                            .collection("nap")
                            .document(dokNev)
                            .set(mapOf("tranzakciok" to tranzakciok))
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
        }
    }

    /**
     * Mivel a kategóriák nem adatbázisban tárolódnak, hanem a programban (EgyediKategoriak objektumban),
     * itt ezeket szűrjük a megadott tranzakció típussal ("Bevétel" vagy "Kiadás").
     * Visszatérünk egy listával, amely párokban tartalmazza a kategória nevét és ikon resource id-t.
     */
    private fun loadCustomKategoriak(tipus: String, callback: (List<Pair<String, Int>>) -> Unit) {
        val customKategoriak = EgyediKategoriak.kategoriak.filter { it.tipus == tipus }
            .map { Pair(it.nev, it.ikon) }
        callback(customKategoriak)
    }
}
