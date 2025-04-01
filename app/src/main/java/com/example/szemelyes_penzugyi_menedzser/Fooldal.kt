package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class Telefonszam : AppCompatActivity() {

    companion object {
        var aktualisOldalIndex = 0
    }
    private val aktualisOldal = "Főoldal"
    private lateinit var aktualisPenzTextView: TextView
    private lateinit var AktualisPenzEditText: EditText
    private lateinit var spinner: Spinner
    private var aktualisPenz: Double = 0.0

    // Feltételezzük, hogy az activity_fooldal.xml-ben van egy LinearLayout id-vel "recentTransactionsLayout"
    private lateinit var recentTransactionsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fooldal)

        // UI elemek inicializálása
        aktualisPenzTextView = findViewById(R.id.JelenlegiText)
        AktualisPenzEditText = findViewById(R.id.Aktualis_penz)
        spinner = findViewById(R.id.lenyilo_menu)
        recentTransactionsLayout = findViewById(R.id.recentTransactionsLayout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Indítsuk el a valós idejű figyelést az aktuális egyenlegre!
        listenToBalance()

        // Spinner beállítása (a spinner_item.xml dizájn szerint)
        setupSpinner()

        // Szövegmező figyelése: ha módosul az érték, frissítjük az adatbázist
        AktualisPenzEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val amount = s.toString().replace(" ", "").replace(",", ".").toDoubleOrNull()
                if (amount != null && amount != aktualisPenz) {
                    aktualisPenz = amount
                    saveBalanceToFirestore(aktualisPenz)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        // Formázás a beírt összegnél: 3 számjegyenként szóközök
        AktualisPenzEditText.addTextChangedListener(object : TextWatcher {
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
                        decimalSeparator = '.'
                    }
                    val formatter = DecimalFormat("#,###.##", symbols)
                    val formatted = formatter.format(parsed)
                    currentText = formatted
                    AktualisPenzEditText.removeTextChangedListener(this)
                    AktualisPenzEditText.setText(formatted)
                    AktualisPenzEditText.setSelection(formatted.length)
                    AktualisPenzEditText.addTextChangedListener(this)
                } catch (e: Exception) { }
            }
        })

        // Utolsó 5 tranzakció betöltése
        val uid = FirebaseManager.getCurrentUserUID()
        if (uid != null) {
            loadRecentTransactions(uid)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            val selection = it.getIntExtra("spinnerSelection", 0)
            spinner.setSelection(selection)
        }
    }

    /**
     * A snapshot listener, amely a Firestore-ban lévő felhasználói dokumentumot figyeli,
     * és az "aktualisPenz" mező változásakor frissíti az aktuális egyenleget.
     */
    private fun listenToBalance() {
        val uid = FirebaseManager.getCurrentUserUID() ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Telefonszam", "Hiba az egyenleg frissítésének figyelésekor: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val newBalance = snapshot.getDouble("aktualisPenz") ?: 0.0
                aktualisPenz = newBalance
                PenzosszegFrissites()
            }
        }
    }

    /**
     * A meglévő mentett egyenleg megjelenítése formázva.
     */
    private fun PenzosszegFrissites() {
        val df = DecimalFormat("#,###", DecimalFormatSymbols().apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        })
        val formattedAmount = df.format(aktualisPenz)
        AktualisPenzEditText.setText(formattedAmount)
    }

    /**
     * Az aktuális egyenleg Firestore-ba mentése.
     */
    private fun saveBalanceToFirestore(amount: Double) {
        val uid = FirebaseManager.getCurrentUserUID()
        if (uid != null) {
            FirebaseManager.saveBalance(
                uid,
                amount,
                onComplete = { /* Ha szükséges, frissíthetjük a globális értéket is */ },
                onError = { exception ->
                    Log.e("Firestore", "Hiba a mentés során: ", exception)
                }
            )
        }
    }

    /**
     * Spinner beállítása és navigáció, a spinner_item.xml dizájn szerint.
     */
    private fun setupSpinner() {
        val lehetosegek = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        // Használjuk a saját spinner_item layoutot a megjelenítéshez
        val adapter = ArrayAdapter(this, R.layout.spinner_item, lehetosegek)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter

        // Az intent extra alapján állítsuk be a spinner értékét; alapértelmezett: 0 ("Főoldal")
        val selection = intent.getIntExtra("spinnerSelection", 0)
        spinner.setSelection(selection)

        var elsoFutas = true
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (elsoFutas) {
                    elsoFutas = false
                    return
                }
                val kiválasztottElem = parent.getItemAtPosition(position).toString()
                if (kiválasztottElem == aktualisOldal) return
                when (kiválasztottElem) {
                    "Elemzés" -> startActivity(
                        Intent(this@Telefonszam, ElemzesActivity::class.java)
                            .putExtra("spinnerSelection", 0)
                    )
                    "Kategóriák" -> startActivity(Intent(this@Telefonszam, Kategoriak::class.java))
                    "Rendszeres kifizetések" -> startActivity(Intent(this@Telefonszam, RendszeresKifizetesek::class.java))
                    "Beállítások" -> startActivity(Intent(this@Telefonszam, BeallitasokActivity::class.java))
                    "Kijelentkezés" -> Kijelentkezes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    /**
     * Kijelentkezés és a mentett beállítások törlése.
     */
    private fun Kijelentkezes() {
        FirebaseManager.signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()
        startActivity(Intent(this, Bejelentkezes::class.java))
        finish()
    }

    /**
     * Segédfüggvény az összegek formázásához: 3 számjegyenként szóközzel.
     */
    private fun formatAmount(amount: Double): String {
        val df = DecimalFormat("#,###", DecimalFormatSymbols(Locale("hu", "HU")).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        })
        return df.format(amount)
    }

    /**
     * Segédfüggvény a tranzakciós elem megjelenítéséhez.
     */
    private fun createTransactionView(
        category: String,
        amount: Float,
        type: String,
        transaction: Map<String, Any>,
        docId: String
    ): View {
        val ctx = this
        val containerLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(8, 8, 8, 8)
        }
        val categoryTextView = TextView(ctx).apply {
            text = category
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }
        containerLayout.addView(categoryTextView)
        val amountTextView = TextView(ctx).apply {
            textSize = 16f
            setTextColor(
                when {
                    type.equals("bevétel", true) -> Color.GREEN
                    type.equals("kiadás", true) -> Color.RED
                    else -> Color.BLACK
                }
            )
            text = if (type.equals("bevétel", true))
                "+ ${formatAmount(amount.toDouble())}"
            else
                "- ${formatAmount(amount.toDouble())}"
            gravity = Gravity.CENTER
        }
        containerLayout.addView(amountTextView)
        return containerLayout
    }

    /**
     * Az utolsó 5 tranzakció betöltése a Firestore-ból.
     */
    private fun loadRecentTransactions(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("nap")
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val allTransactions = mutableListOf<Pair<Map<String, Any>, String>>()
                for (doc in result.documents) {
                    val transactions = doc.get("tranzakciok") as? List<Map<String, Any>>
                    if (transactions != null) {
                        for (transaction in transactions) {
                            allTransactions.add(Pair(transaction, doc.id))
                        }
                    }
                }
                // Rendezés a "datum" (timestamp) mező alapján csökkenő sorrendbe
                val sortedTransactions = allTransactions.sortedByDescending { pair ->
                    val ts = pair.first["datum"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
                    ts.toDate().time
                }
                val top5 = sortedTransactions.take(5)
                recentTransactionsLayout.removeAllViews()
                for ((transactionMap, docId) in top5) {
                    val category = transactionMap["kategoria"] as? String ?: "N/A"
                    val amount = (transactionMap["mennyiseg"] as? Number)?.toFloat() ?: 0f
                    val type = transactionMap["tipus"] as? String ?: "kiadás"
                    val view = createTransactionView(category, amount, type, transactionMap, docId)
                    recentTransactionsLayout.addView(view)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hiba a tranzakciók betöltésekor: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Telefonszam", "Error loading transactions", e)
            }
    }
}
