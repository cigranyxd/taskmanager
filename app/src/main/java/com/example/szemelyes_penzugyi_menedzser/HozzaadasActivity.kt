package com.example.szemelyes_penzugyi_menedzser

import KategoriaAdapter
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

class HozzaadasActivity : AppCompatActivity() {

    private var kivalasztottKategoria: String? = null   // Kiválasztott kategória
    private var tranzakcioTipus: String? = null          // "Bevétel" vagy "Kiadás"
    private var selectedView: View? = null                // A kijelölt elem

    // Mostantól class–szintű property a leírás input számára:
    private lateinit var leirasInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hozaadas)

        // Nézetek inicializálása
        val osszegInput = findViewById<EditText>(R.id.osszegInput)
        val tranzakcioTipusSpinner = findViewById<Spinner>(R.id.tranzakcioTipusSpinner)
        val mentesGomb = findViewById<Button>(R.id.mentesGomb)
        val hozzaadasGorgetesGridView = findViewById<GridView>(R.id.hozzaadasGorgetesGridView)
        leirasInput = findViewById(R.id.leirasInput)
        // DatePicker hozzáadása – a layoutban a "datePicker" id-vel szerepel
        val datePicker = findViewById<DatePicker>(R.id.datePicker)

        // Csak numerikus (decimal) billentyűzet az összeg beviteléhez
        osszegInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // TextWatcher az összeg formázásához (3 számjegyenként szóközökkel)
        osszegInput.addTextChangedListener(object : TextWatcher {
            var currentText = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                if (str == currentText) return

                val cleanString = str.replace(" ", "")
                if (cleanString.isEmpty()) {
                    currentText = ""
                    return
                }
                try {
                    val parsed = cleanString.toDouble()
                    val symbols = DecimalFormatSymbols(Locale("hu", "HU")).apply {
                        groupingSeparator = ' '
                    }
                    val formatter = DecimalFormat("#,###.##", symbols)
                    val formatted = formatter.format(parsed)
                    currentText = formatted
                    osszegInput.removeTextChangedListener(this)
                    osszegInput.setText(formatted)
                    osszegInput.setSelection(formatted.length)
                    osszegInput.addTextChangedListener(this)
                } catch (e: Exception) { }
            }
        })

        // Spinner beállítása
        val tranzakcioTipusNevek = listOf("Bevétel", "Kiadás")
        val spinnerAdapter = ArrayAdapter(this, R.layout.spinner_item, tranzakcioTipusNevek)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
        tranzakcioTipusSpinner.adapter = spinnerAdapter
        tranzakcioTipusSpinner.setPopupBackgroundResource(R.drawable.spinner_lenyitott_bg)



        // Alapértelmezett érték: "Bevétel"
        tranzakcioTipusSpinner.setSelection(0)
        tranzakcioTipus = "Bevétel"

        // Alapértelmezett kategóriák
        // Bevétel esetén: "Fizetés", "Ajándékok", "Egyéb"
        val defaultBevetelek = listOf("Fizetés", "Ajándékok", "Egyéb")
        val defaultBevetelekIkonok = listOf(R.drawable.szabadido_icon, R.drawable.ajandekok_icon, R.drawable.egyeb_icon)
        // Kiadás esetén: "Egészség", "Szabadidő", "Otthon", "Kávézó", "Oktatás", "Ajándékok", "Élelmiszerek", "Család", "Sport", "Közlekedés", "Egyéb"
        val defaultKiadasok = listOf(
            "Egészség", "Szabadidő", "Otthon", "Kávézó", "Oktatás", "Ajándékok",
            "Élelmiszerek", "Család", "Sport", "Közlekedés", "Egyéb"
        )
        val defaultKiadasokIkonok = listOf(
            R.drawable.egeszseg_icon, R.drawable.szabadido_icon, R.drawable.otthon_icon,
            R.drawable.kavezo_icon, R.drawable.oktatas_icon, R.drawable.ajandekok_icon,
            R.drawable.elelmiszerek_icon, R.drawable.csalad_icon, R.drawable.edzes_icon,
            R.drawable.kozlekedes_icon, R.drawable.egyeb_icon
        )

        // Adapter inicializálása a "Bevétel" típushoz
        val initialNames = defaultBevetelek + EgyediKategoriak.kategoriak.filter {
            it.tipus.equals("Bevétel", ignoreCase = true)
        }.map { it.nev }
        val initialIcons = defaultBevetelekIkonok + EgyediKategoriak.kategoriak.filter {
            it.tipus.equals("Bevétel", ignoreCase = true)
        }.map { it.ikon }
        val kategoriakAdapter = KategoriaAdapter(this, initialNames, initialIcons)
        hozzaadasGorgetesGridView.adapter = kategoriakAdapter

        // Spinner eseménykezelő: váltáskor frissítjük az adaptert
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

        // Custom kategóriák betöltése Firestore-ból, ha be van jelentkezve
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            EgyediKategoriak.betoltFirestore(this, uid) {
                val currentType = tranzakcioTipusSpinner.selectedItem?.toString() ?: "Bevétel"
                updateAdapterFor(currentType)
            }
        } else {
            EgyediKategoriak.betolt(this)
        }

        // Kategória kiválasztásának kezelése: ugyanarra kattintva visszavonás
        hozzaadasGorgetesGridView.setOnItemClickListener { parent, view, position, _ ->
            (hozzaadasGorgetesGridView.adapter as? KategoriaAdapter)?.apply {
                if (selectedPosition == position) {
                    selectedPosition = -1
                    kivalasztottKategoria = null
                } else {
                    selectedPosition = position
                    kivalasztottKategoria = parent.getItemAtPosition(position) as? String
                    kivalasztottKategoria?.let {
                        Toast.makeText(this@HozzaadasActivity, "Kiválasztott kategória: $it", Toast.LENGTH_SHORT).show()
                    }
                }
                notifyDataSetChanged()
            }
        }

        // Hosszú nyomás esemény hozzáadása az egyedi kategóriák törléséhez
        hozzaadasGorgetesGridView.setOnItemLongClickListener { parent, view, position, _ ->
            val category = parent.getItemAtPosition(position) as? String ?: return@setOnItemLongClickListener false

            if (tranzakcioTipus.equals("Bevétel", ignoreCase = true) && defaultBevetelek.contains(category)) {
                Toast.makeText(this, "Alapértelmezett kategóriát nem lehet törölni", Toast.LENGTH_SHORT).show()
                return@setOnItemLongClickListener false
            }
            if (tranzakcioTipus.equals("Kiadás", ignoreCase = true) && defaultKiadasok.contains(category)) {
                Toast.makeText(this, "Alapértelmezett kategóriát nem lehet törölni", Toast.LENGTH_SHORT).show()
                return@setOnItemLongClickListener false
            }

            AlertDialog.Builder(this)
                .setTitle("Kategória törlése")
                .setMessage("Biztosan törlöd a kategóriát: $category?")
                .setPositiveButton("Igen") { dialog, which ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        EgyediKategoriak.torolKategoriat(this, uid, category) { success ->
                            if (success) {
                                Toast.makeText(this, "Kategória törölve", Toast.LENGTH_SHORT).show()
                                updateAdapterFor(tranzakcioTipus ?: "Bevétel")
                            } else {
                                Toast.makeText(this, "Hiba a kategória törlése során", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Mégse", null)
                .show()

            true
        }

        mentesGomb.setOnClickListener {
            val osszeg = osszegInput.text.toString().replace(" ", "").toDoubleOrNull()
            val leiras = leirasInput.text.toString()
            val finalLeiras = if (leiras.isBlank()) "nincs leírás" else leiras

            val firestore = FirebaseFirestore.getInstance()
            if (FirebaseAuth.getInstance().currentUser == null) {
                Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            // Használjuk a DatePicker által kiválasztott dátumot
            val selectedDate = LocalDate.of(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
            val dokNev = selectedDate.toString()

            if (osszeg == null || tranzakcioTipus == null || kivalasztottKategoria == null) {
                Toast.makeText(this, "Érvényes összeget, tranzakció típust és kategóriát adj meg!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = hashMapOf<String, Any>(
                "mennyiseg" to osszeg,
                "kategoria" to (kivalasztottKategoria ?: "Nincs kategória"),
                "datum" to Timestamp.now(),
                "leiras" to finalLeiras,
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
        val hozzaadasGorgetesGridView = findViewById<GridView>(R.id.hozzaadasGorgetesGridView)
        val defaultBevetelek = listOf("Fizetés", "Ajándékok", "Egyéb")
        val defaultKiadasok = listOf(
            "Egészség", "Szabadidő", "Otthon", "Kávézó", "Oktatás", "Ajándékok",
            "Élelmiszerek", "Család", "Sport", "Közlekedés", "Egyéb"
        )
        val defaultBevetelekIkonok = listOf(R.drawable.szabadido_icon, R.drawable.ajandekok_icon, R.drawable.egyeb_icon)
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
            val index = finalNames.indexOf(kivalasztottKategoria)
            if (index != -1) {
                newAdapter.selectedPosition = index
            }
            hozzaadasGorgetesGridView.adapter = newAdapter
        } else {
            val customKiadas = EgyediKategoriak.kategoriak.filter { it.tipus.trim().toLowerCase() == "kiadás" }
            val finalNames = defaultKiadasok + customKiadas.map { it.nev }
            val finalIcons = defaultKiadasokIkonok + customKiadas.map { it.ikon }
            val newAdapter = KategoriaAdapter(this, finalNames, finalIcons)
            val index = finalNames.indexOf(kivalasztottKategoria)
            if (index != -1) {
                newAdapter.selectedPosition = index
            }
            hozzaadasGorgetesGridView.adapter = newAdapter
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
