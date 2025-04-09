// Telefonszam.kt
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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class Telefonszam :  BaseActivity() {

    companion object {
        var aktualisOldalIndex = 0
    }

    private val aktualisOldal = "Főoldal"
    private lateinit var aktualisPenzTextView: TextView
    private lateinit var AktualisPenzEditText: EditText
    private lateinit var spinner: Spinner
    private var aktualisPenz: BigDecimal = BigDecimal.ZERO
    private lateinit var recentTransactionsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fooldal)

        val autoLevonas = OneTimeWorkRequestBuilder<RendszeresLevonásWorker>().build()
        WorkManager.getInstance(this).enqueue(autoLevonas)

        aktualisPenzTextView = findViewById(R.id.JelenlegiText)
        AktualisPenzEditText = findViewById(R.id.Aktualis_penz)
        spinner = findViewById(R.id.lenyilo_menu)
        recentTransactionsLayout = findViewById(R.id.recentTransactionsLayout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listenToBalance()
        setupSpinner()

        AktualisPenzEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val amount = s.toString().replace(" ", "").replace(",", ".").toBigDecimalOrNull()
                if (amount != null && amount != aktualisPenz) {
                    aktualisPenz = amount
                    saveBalanceToFirestore(aktualisPenz)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Számformázás
        AktualisPenzEditText.addTextChangedListener(object : TextWatcher {
            var currentText = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                if (str == currentText) return
                val cleanString = str.replace(" ", "")
                if (cleanString.isEmpty()) {
                    currentText = ""
                    return
                }
                try {
                    val parsed = cleanString.toBigDecimal()
                    val symbols = DecimalFormatSymbols(Locale("hu", "HU")).apply {
                        groupingSeparator = ' '
                        decimalSeparator = '.'
                    }
                    val formatter = DecimalFormat("#,###", symbols)
                    val formatted = formatter.format(parsed)
                    currentText = formatted
                    AktualisPenzEditText.removeTextChangedListener(this)
                    AktualisPenzEditText.setText(formatted)
                    AktualisPenzEditText.setSelection(formatted.length)
                    AktualisPenzEditText.addTextChangedListener(this)
                } catch (e: Exception) {
                }
            }
        })

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

    private fun listenToBalance() {
        val uid = FirebaseManager.getCurrentUserUID() ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Telefonszam", "Hiba: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val balanceDouble = snapshot.getDouble("aktualisPenz") ?: 0.0
                aktualisPenz = BigDecimal.valueOf(balanceDouble)
                PenzosszegFrissites()
            }
        }
    }

    private fun PenzosszegFrissites() {
        AktualisPenzEditText.setText("${formatAmount(aktualisPenz)} ")
    }

    private fun saveBalanceToFirestore(amount: BigDecimal) {
        val uid = FirebaseManager.getCurrentUserUID()
        if (uid != null) {
            FirebaseManager.saveBalance(
                uid,
                amount.toDouble(),
                onComplete = {},
                onError = { exception -> Log.e("Firestore", "Hiba mentéskor: ", exception) }
            )
        }
    }

    private fun setupSpinner() {
        val lehetosegek = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val adapter = CustomFontSizeSpinnerAdapter(this, R.layout.spinner_item, lehetosegek)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter

        val selection = intent.getIntExtra("spinnerSelection", 0)
        spinner.setSelection(selection)

        var elsoFutas = true
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (elsoFutas) {
                    elsoFutas = false
                    return
                }
                val elem = parent.getItemAtPosition(position).toString()
                if (elem == aktualisOldal) return
                when (elem) {
                    "Elemzés" -> startActivity(Intent(this@Telefonszam, ElemzesActivity::class.java).putExtra("spinnerSelection", 0))
                    "Kategóriák" -> startActivity(Intent(this@Telefonszam, Kategoriak::class.java))
                    "Rendszeres kifizetések" -> startActivity(Intent(this@Telefonszam, RendszeresKifizetesek::class.java))
                    "Beállítások" -> startActivity(Intent(this@Telefonszam, BeallitasokActivity::class.java))
                    "Kijelentkezés" -> Kijelentkezes()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun Kijelentkezes() {
        FirebaseManager.signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit().putBoolean("isLoggedIn", false).apply()
        startActivity(Intent(this, Bejelentkezes::class.java))
        finish()
    }

    private fun formatAmount(amount: BigDecimal): String {
        val symbols = DecimalFormatSymbols(Locale("hu", "HU")).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(amount) + " Ft"
    }

    private fun createTransactionView(
        category: String,
        amount: BigDecimal,
        type: String,
        transaction: Map<String, Any>,
        docId: String
    ): View {
        val ctx = this
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(8, 8, 8, 8)
        }

        val categoryTV = TextView(ctx).apply {
            text = category
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        val amountTV = TextView(ctx).apply {
            textSize = 16f
            setTextColor(
                when {
                    type.equals("bevétel", true) -> Color.GREEN
                    type.equals("kiadás", true) -> Color.RED
                    else -> Color.BLACK
                }
            )
            text = if (type.equals("bevétel", true))
                "+ ${formatAmount(amount)}"
            else
                "- ${formatAmount(amount)}"
            gravity = Gravity.CENTER
        }

        layout.addView(categoryTV)
        layout.addView(amountTV)
        return layout
    }

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
                        for (t in transactions) {
                            allTransactions.add(Pair(t, doc.id))
                        }
                    }
                }
                val sorted = allTransactions.sortedByDescending { pair ->
                    val ts = pair.first["datum"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
                    ts.toDate().time
                }
                val top5 = sorted.take(5)
                recentTransactionsLayout.removeAllViews()
                for ((map, docId) in top5) {
                    val category = map["kategoria"] as? String ?: "N/A"
                    val amount = when (val a = map["mennyiseg"]) {
                        is Double -> BigDecimal.valueOf(a)
                        is Long -> BigDecimal.valueOf(a.toDouble())
                        is String -> a.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        else -> BigDecimal.ZERO
                    }
                    val type = map["tipus"] as? String ?: "kiadás"
                    val view = createTransactionView(category, amount, type, map, docId)
                    recentTransactionsLayout.addView(view)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Telefonszam", "Tranzakció hiba", e)
            }
    }
}
