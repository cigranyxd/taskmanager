package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.firestore.FieldPath
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import com.example.szemelyes_penzugyi_menedzser.FirebaseManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class ElemzesActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elemzes)

        val currentUser = FirebaseManager.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Bejelentkezes::class.java))
            finish()
            return
        }

        // Navigációs feliratok (fix elhelyezés az XML-ben, pl. "feliratokLayout")
        findViewById<TextView>(R.id.NapFelirat).setOnClickListener { SzovegreKattint(it) }
        findViewById<TextView>(R.id.HetFelirat).setOnClickListener { SzovegreKattint(it) }
        findViewById<TextView>(R.id.HonapFelirat).setOnClickListener { SzovegreKattint(it) }
        findViewById<TextView>(R.id.EvFelirat).setOnClickListener { SzovegreKattint(it) }
        findViewById<TextView>(R.id.IdoszakFelirat).setOnClickListener { SzovegreKattint(it) }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Spinner
        val spinner: Spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegek)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        findViewById<Button>(R.id.hozzaadasGomb).setOnClickListener {
            startActivity(Intent(this, HozzaadasActivity::class.java))
        }

        spinner.setSelection(1)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (parent?.getItemAtPosition(position).toString()) {
                    "Főoldal" -> startActivity(Intent(this@ElemzesActivity, Telefonszam::class.java))
                    "Rendszeres kifizetések" -> startActivity(Intent(this@ElemzesActivity, RendszeresKifizetesek::class.java))
                    "Beállítások" -> startActivity(Intent(this@ElemzesActivity, BeallitasokActivity::class.java))
                    "Kijelentkezés" -> Kijelentkezes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        applyFontSizeToCurrentActivity()

        // Fragmentek a fix konténerbe (pl. R.id.oszlopDiagram)
        supportFragmentManager.beginTransaction()
            .replace(R.id.oszlopDiagram, NapFragment())
            .commit()
    }

    // Overlay eltávolítása a BarChart szülőjéből, ha létezik
    fun removeNoDataOverlay(chart: BarChart) {
        val parent = chart.parent as? ViewGroup ?: return
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i)
            if (child.tag == "noDataOverlay") {
                parent.removeViewAt(i)
            }
        }
        chart.visibility = View.VISIBLE
    }

    // Kategória ikon visszaadása
    fun getIconResForCategory(category: String): Int {
        val norm = category.trim().toLowerCase(Locale.getDefault())
        return when (norm) {
            "ajándékok", "ajandekok" -> R.drawable.ajandekok_icon
            "családi kiadás", "csalad kiadás", "családi kiadas", "csalad kiadas" -> R.drawable.csalad_icon
            "edzés", "edzes" -> R.drawable.edzes_icon
            "egészség", "egeszseg" -> R.drawable.egeszseg_icon
            "élelmiszerek", "elelmiszerek" -> R.drawable.elelmiszerek_icon
            "kávézó", "kavezo" -> R.drawable.kavezo_icon
            "közlekedés", "kozelekedes" -> R.drawable.kozlekedes_icon
            "oktatás", "oktatas" -> R.drawable.oktatas_icon
            "otthon" -> R.drawable.otthon_icon
            "szabadidő", "szabadido", "fizetési csekk" -> R.drawable.szabadido_icon
            else -> R.drawable.egyeb_icon
        }
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
        if (view is TextView) view.textSize = fontSize
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextViewsFontSize(view.getChildAt(i), fontSize)
            }
        }

        applyFontSizeToCurrentActivity()
    }

    private fun Kijelentkezes() {
        FirebaseManager.signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()
        val intent = Intent(this, Bejelentkezes::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        // Vissza gomb: mindig a Főoldalra navigálunk
        val intent = Intent(this, Telefonszam::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    fun SzovegreKattint(view: View) {
        val fragment: Fragment = when (view.id) {
            R.id.NapFelirat -> NapFragment()
            R.id.HetFelirat -> HetFragment()
            R.id.HonapFelirat -> HonapFragment()
            R.id.EvFelirat -> EvFragment()
            R.id.IdoszakFelirat -> IdoszakFragment()
            else -> DefaultFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.oszlopDiagram, fragment)
            .commit()
    }

    // --- Fragmentek ---

    // 1. NapFragment – Egy adott nap adatai
    class NapFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.visibility = View.VISIBLE
            chart.visibility = View.VISIBLE
            categoriesLayout.removeAllViews()

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return
            }
            val uid = currentUser.uid
            val todayStr = LocalDate.now().toString()

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereEqualTo(FieldPath.documentId(), todayStr)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        showNoDataOverlay("Nincs megjeleníthető adat a választott időszakra.", chart, categoriesLayout)
                        chart.visibility = View.INVISIBLE
                        return@addOnSuccessListener
                    }
                    (activity as ElemzesActivity).removeNoDataOverlay(chart)
                    try {
                        val dailyDoc = querySnapshot.documents[0]
                        val transactions = dailyDoc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        val sortedTransactions = if (transactions.isNotEmpty() && transactions[0].containsKey("timestamp"))
                            transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                        else transactions.reversed()

                        var totalRevenue = 0f
                        var totalExpense = 0f

                        for (trans in sortedTransactions) {
                            val amount = when (val a = trans["mennyiseg"]) {
                                is Double -> a.toFloat()
                                is Long -> a.toFloat()
                                else -> 0f
                            }
                            val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                            val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""

                            // A törlő ikon hozzáadása is: a createTransactionView mostantól tartalmaz egy bin ikont,
                            // mely törli a tranzakciót és utána frissíti a fragmentet
                            val transactionView = createTransactionView(category, amount, type, trans, todayStr)
                            categoriesLayout.addView(transactionView)

                            if (type == "bevétel") totalRevenue += amount
                            else if (type == "kiadás") totalExpense += amount
                        }
                        val revenueEntry = BarEntry(0f, totalRevenue)
                        val expenseEntry = BarEntry(1f, totalExpense)
                        val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Napi összesítés")
                        dataSet.colors = listOf(Color.GREEN, Color.RED)
                        dataSet.valueTextColor = Color.BLACK
                        val barData = BarData(dataSet)
                        chart.data = barData
                        chart.description.isEnabled = false
                        chart.setFitBars(true)
                        chart.invalidate()
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "Hiba a tranzakciók feldolgozása során: ${e.message}")
                        Toast.makeText(context, "Hiba az adatok feldolgozása közben.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba a napi adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba a napi adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                }
        }

        private fun showNoDataOverlay(message: String, chart: BarChart, container: LinearLayout) {
            val parent = chart.parent as? ViewGroup
            if (parent != null && parent.findViewWithTag<View>("noDataOverlay") == null) {
                val overlay = createNoDataOverlay(message)
                parent.addView(overlay)
                overlay.bringToFront()
            }
            // A container (a tranzakciós lista) és a chart továbbra is megjelenik, csak a chart overlay-t mutat
        }

        private fun createNoDataOverlay(message: String): View {
            val context = requireContext()
            val overlay = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                tag = "noDataOverlay"
            }
            val topHalf = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val iv = ImageView(context).apply {
                setImageResource(R.drawable.no_data_icon)
                val size = (48 * resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
            }
            topHalf.addView(iv)
            val bottomHalf = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val tv = TextView(context).apply {
                text = message
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER }
            }
            bottomHalf.addView(tv)
            overlay.addView(topHalf)
            overlay.addView(bottomHalf)
            return overlay
        }

        // A createTransactionView függvény, mely mostantól hozzáad egy bin ikont is a törléshez,
        // és törlés után frissíti a fragmentet
        private fun createTransactionView(category: String, amount: Float, type: String, transaction: Map<String, Any>, docId: String): View {
            val context = requireContext()
            val containerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            // Bin ikon törléshez
            val binIcon = ImageView(context).apply {
                setImageResource(R.drawable.bin_icon)
                val size = (24 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setMargins(8, 0, 8, 0)
                }
                setOnClickListener {
                    (activity as ElemzesActivity).deleteTransaction(docId, transaction, {
                        Toast.makeText(context, "Tranzakció törölve", Toast.LENGTH_SHORT).show()
                        // Frissítjük a fragmentet
                        requireActivity().supportFragmentManager.beginTransaction().detach(this@NapFragment).attach(this@NapFragment).commit()
                    }, { e ->
                        Toast.makeText(context, "Törlési hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                    })
                }
            }
            containerLayout.addView(binIcon)

            val transactionDetails = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val catTextView = TextView(context).apply {
                text = category
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 4, 0, 2)
                }
            }
            transactionDetails.addView(catTextView)
            val rowLayout = RelativeLayout(context).apply {
                layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            }
            val amountTextView = TextView(context).apply {
                id = View.generateViewId()
                textSize = 16f
                gravity = Gravity.START
                setTextColor(if (type == "bevétel") Color.GREEN else if (type == "kiadás") Color.RED else Color.BLACK)
                text = when (type) {
                    "bevétel" -> "+ $amount"
                    "kiadás" -> "- $amount"
                    else -> "$amount"
                }
            }
            val amountParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            amountTextView.layoutParams = amountParams
            rowLayout.addView(amountTextView)
            val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
            val iconView = ImageView(context).apply {
                id = View.generateViewId()
                setImageResource(iconRes)
            }
            val iconParams = RelativeLayout.LayoutParams((24 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt()).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
                setMargins(8, 0, 8, 0)
            }
            iconView.layoutParams = iconParams
            rowLayout.addView(iconView)
            transactionDetails.addView(rowLayout)
            containerLayout.addView(transactionDetails)
            return containerLayout
        }
    }

    // 2. HetFragment – Az aktuális hét adatai (csökkenő sorrendben)
    class HetFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.visibility = View.VISIBLE
            chart.visibility = View.VISIBLE
            categoriesLayout.removeAllViews()

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return
            }
            val uid = currentUser.uid
            val today = LocalDate.now()
            val weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val weekEnd = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), weekStart.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), weekEnd.toString())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        showNoDataOverlay("Nincs megjeleníthető adat a választott időszakra.", chart, categoriesLayout)
                        return@addOnSuccessListener
                    }
                    (activity as ElemzesActivity).removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id, formatter) }
                    for (doc in sortedDocs) {
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 4) }
                        }
                        categoriesLayout.addView(dateHeader, 0)
                        try {
                            val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            val sortedTransactions = if (transactions.isNotEmpty() && transactions[0].containsKey("timestamp"))
                                transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                            else transactions.reversed()

                            for (trans in sortedTransactions) {
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                                val transactionView = createTransactionView(category, amount, type, trans, doc.id)
                                categoriesLayout.addView(transactionView, 0)
                                if (type == "bevétel") totalRevenue += amount
                                else if (type == "kiadás") totalExpense += amount
                            }
                        } catch (e: Exception) {
                            Log.e("FirestoreDebug", "Error processing doc $dateStr: ${e.message}")
                        }
                    }
                    val revenueEntry = BarEntry(0f, totalRevenue)
                    val expenseEntry = BarEntry(1f, totalExpense)
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Hét összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    val barData = BarData(dataSet)
                    chart.data = barData
                    chart.description.isEnabled = false
                    chart.setFitBars(true)
                    chart.invalidate()
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba a heti adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba a heti adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                }
        }

        private fun showNoDataOverlay(message: String, chart: BarChart, container: LinearLayout) {
            val parent = chart.parent as? ViewGroup
            if (parent != null && parent.findViewWithTag<View>("noDataOverlay") == null) {
                val overlay = createNoDataOverlay(message)
                parent.addView(overlay)
                overlay.bringToFront()
            }
            container.visibility = View.VISIBLE
            chart.visibility = View.INVISIBLE
        }

        private fun createNoDataOverlay(message: String): View {
            val context = requireContext()
            val overlay = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
                tag = "noDataOverlay"
            }
            val topHalf = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val iv = ImageView(context).apply {
                setImageResource(R.drawable.no_data_icon)
                val size = (48 * resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
            }
            topHalf.addView(iv)
            val bottomHalf = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val tv = TextView(context).apply {
                text = message
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER }
            }
            bottomHalf.addView(tv)
            overlay.addView(topHalf)
            overlay.addView(bottomHalf)
            return overlay
        }

        private fun createTransactionView(category: String, amount: Float, type: String, transaction: Map<String, Any>, docId: String): View {
            val context = requireContext()
            val containerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            // Bin ikon törléshez
            val binIcon = ImageView(context).apply {
                setImageResource(R.drawable.bin_icon)
                val size = (24 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setMargins(8, 0, 8, 0)
                }
                setOnClickListener {
                    (activity as ElemzesActivity).deleteTransaction(docId, transaction, {
                        Toast.makeText(context, "Tranzakció törölve", Toast.LENGTH_SHORT).show()
                        // Frissítjük a NapFragment-et
                        requireActivity().supportFragmentManager.beginTransaction().detach(NapFragment()).attach(NapFragment()).commit()
                    }, { e ->
                        Toast.makeText(context, "Törlési hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                    })
                }
            }
            containerLayout.addView(binIcon)

            val transactionDetails = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val catTextView = TextView(context).apply {
                text = category
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 4, 0, 2)
                }
            }
            transactionDetails.addView(catTextView)
            val rowLayout = RelativeLayout(context).apply {
                layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            }
            val amountTextView = TextView(context).apply {
                id = View.generateViewId()
                textSize = 16f
                gravity = Gravity.START
                setTextColor(if (type == "bevétel") Color.GREEN else if (type == "kiadás") Color.RED else Color.BLACK)
                text = when (type) {
                    "bevétel" -> "+ $amount"
                    "kiadás" -> "- $amount"
                    else -> "$amount"
                }
            }
            val amountParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            amountTextView.layoutParams = amountParams
            rowLayout.addView(amountTextView)
            val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
            val iconView = ImageView(context).apply {
                id = View.generateViewId()
                setImageResource(iconRes)
            }
            val iconParams = RelativeLayout.LayoutParams((24 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt()).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
                setMargins(8, 0, 8, 0)
            }
            iconView.layoutParams = iconParams
            rowLayout.addView(iconView)
            transactionDetails.addView(rowLayout)
            containerLayout.addView(transactionDetails)
            return containerLayout
        }
    }

    // 3. HonapFragment – Az aktuális hónap adatai (csökkenő sorrendben)
    class HonapFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.visibility = View.VISIBLE
            chart.visibility = View.VISIBLE
            categoriesLayout.removeAllViews()

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return
            }
            val uid = currentUser.uid
            val today = LocalDate.now()
            val monthStart = today.withDayOfMonth(1)
            val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), monthStart.format(formatter))
                .whereLessThanOrEqualTo(FieldPath.documentId(), monthEnd.format(formatter))
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        showNoDataOverlay("Nincs megjeleníthető adat a választott időszakra.", categoriesLayout)
                        chart.visibility = View.INVISIBLE
                        return@addOnSuccessListener
                    }
                    (activity as ElemzesActivity).removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id, formatter) }
                    for (doc in sortedDocs) {
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, 8, 0, 4)
                            }
                        }
                        categoriesLayout.addView(dateHeader)
                        try {
                            val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            val sortedTransactions = if (transactions.isNotEmpty() && transactions[0].containsKey("timestamp"))
                                transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                            else transactions.reversed()

                            for (trans in sortedTransactions) {
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                                val transactionView = createTransactionView(category, amount, type, trans, doc.id)
                                categoriesLayout.addView(transactionView)
                                if (type == "bevétel") totalRevenue += amount
                                else if (type == "kiadás") totalExpense += amount
                            }
                        } catch (e: Exception) {
                            Log.e("FirestoreDebug", "Error processing doc $dateStr: ${e.message}")
                        }
                    }
                    val revenueEntry = BarEntry(0f, totalRevenue)
                    val expenseEntry = BarEntry(1f, totalExpense)
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Havi összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    val barData = BarData(dataSet)
                    chart.data = barData
                    chart.description.isEnabled = false
                    chart.setFitBars(true)
                    chart.invalidate()
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba a havi adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba a havi adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                }
        }

        private fun showNoDataOverlay(message: String, container: LinearLayout) {
            for (i in 0 until container.childCount) {
                if (container.getChildAt(i).tag == "noDataOverlay") return
            }
            val overlay = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    container.width.takeIf { it > 0 } ?: LinearLayout.LayoutParams.MATCH_PARENT,
                    container.height.takeIf { it > 0 } ?: LinearLayout.LayoutParams.MATCH_PARENT
                )
                tag = "noDataOverlay"
            }
            val topHalf = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val iv = ImageView(requireContext()).apply {
                setImageResource(R.drawable.no_data_icon)
                val size = (48 * resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
            }
            topHalf.addView(iv)
            val bottomHalf = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val tv = TextView(requireContext()).apply {
                text = message
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER }
            }
            bottomHalf.addView(tv)
            overlay.addView(topHalf)
            overlay.addView(bottomHalf)
            container.addView(overlay)
            overlay.bringToFront()
        }

        private fun createTransactionView(category: String, amount: Float, type: String, transaction: Map<String, Any>, docId: String): View {
            val context = requireContext()
            val containerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            // Bin ikon törléshez
            val binIcon = ImageView(context).apply {
                setImageResource(R.drawable.bin_icon)
                val size = (24 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setMargins(8, 0, 8, 0)
                }
                setOnClickListener {
                    (activity as ElemzesActivity).deleteTransaction(docId, transaction, {
                        Toast.makeText(context, "Tranzakció törölve", Toast.LENGTH_SHORT).show()
                        // Frissítjük a HonapFragment-et
                        requireActivity().supportFragmentManager.beginTransaction().detach(this@HonapFragment).attach(this@HonapFragment).commit()
                    }, { e ->
                        Toast.makeText(context, "Törlési hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                    })
                }
            }
            containerLayout.addView(binIcon)
            val transactionDetails = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val catTextView = TextView(context).apply {
                text = category
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 4, 0, 2)
                }
            }
            transactionDetails.addView(catTextView)
            val rowLayout = RelativeLayout(context).apply {
                layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            }
            val amountTextView = TextView(context).apply {
                id = View.generateViewId()
                textSize = 16f
                gravity = Gravity.START
                setTextColor(if (type == "bevétel") Color.GREEN else if (type == "kiadás") Color.RED else Color.BLACK)
                text = when (type) {
                    "bevétel" -> "+ $amount"
                    "kiadás" -> "- $amount"
                    else -> "$amount"
                }
            }
            val amountParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            amountTextView.layoutParams = amountParams
            rowLayout.addView(amountTextView)
            val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
            val iconView = ImageView(context).apply {
                id = View.generateViewId()
                setImageResource(iconRes)
            }
            val iconParams = RelativeLayout.LayoutParams((24 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt()).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
                setMargins(8, 0, 8, 0)
            }
            iconView.layoutParams = iconParams
            rowLayout.addView(iconView)
            transactionDetails.addView(rowLayout)
            containerLayout.addView(transactionDetails)
            return containerLayout
        }
    }

    // 4. EvFragment – Az aktuális év adatai (csökkenő sorrendben)
    class EvFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.visibility = View.VISIBLE
            chart.visibility = View.VISIBLE
            categoriesLayout.removeAllViews()

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return
            }
            val uid = currentUser.uid

            val yearStart = LocalDate.of(LocalDate.now().year, 1, 1)
            val yearEnd = LocalDate.of(LocalDate.now().year, 12, 31)

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), yearStart.format(formatter))
                .whereLessThanOrEqualTo(FieldPath.documentId(), yearEnd.format(formatter))
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        showNoDataOverlay("Nincs megjeleníthető adat a választott időszakra.", categoriesLayout)
                        chart.visibility = View.INVISIBLE
                        return@addOnSuccessListener
                    }
                    (activity as ElemzesActivity).removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id, formatter) }
                    for (doc in sortedDocs) {
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, 8, 0, 4)
                            }
                        }
                        categoriesLayout.addView(dateHeader)
                        try {
                            val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            val sortedTransactions = if (transactions.isNotEmpty() && transactions[0].containsKey("timestamp"))
                                transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                            else transactions.reversed()

                            for (trans in sortedTransactions) {
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                                val transactionView = createTransactionView(category, amount, type, trans, doc.id)
                                categoriesLayout.addView(transactionView)
                                if (type == "bevétel") totalRevenue += amount
                                else if (type == "kiadás") totalExpense += amount
                            }
                        } catch (e: Exception) {
                            Log.e("FirestoreDebug", "Error processing doc $dateStr: ${e.message}")
                        }
                    }
                    val revenueEntry = BarEntry(0f, totalRevenue)
                    val expenseEntry = BarEntry(1f, totalExpense)
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Év összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    val barData = BarData(dataSet)
                    chart.data = barData
                    chart.description.isEnabled = false
                    chart.setFitBars(true)
                    chart.invalidate()
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba az éves adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba az éves adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                }
        }

        private fun showNoDataOverlay(message: String, container: LinearLayout) {
            for (i in 0 until container.childCount) {
                if (container.getChildAt(i).tag == "noDataOverlay") return
            }
            val overlay = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    container.width.takeIf { it > 0 } ?: LinearLayout.LayoutParams.MATCH_PARENT,
                    container.height.takeIf { it > 0 } ?: LinearLayout.LayoutParams.MATCH_PARENT
                )
                tag = "noDataOverlay"
            }
            val topHalf = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val iv = ImageView(requireContext()).apply {
                setImageResource(R.drawable.no_data_icon)
                val size = (48 * resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
            }
            topHalf.addView(iv)
            val bottomHalf = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val tv = TextView(requireContext()).apply {
                text = message
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER }
            }
            bottomHalf.addView(tv)
            overlay.addView(topHalf)
            overlay.addView(bottomHalf)
            container.addView(overlay)
            overlay.bringToFront()
        }

        private fun createTransactionView(category: String, amount: Float, type: String, transaction: Map<String, Any>, docId: String): View {
            val context = requireContext()
            val containerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            // Bin ikon törléshez
            val binIcon = ImageView(context).apply {
                setImageResource(R.drawable.bin_icon)
                val size = (24 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setMargins(8, 0, 8, 0)
                }
                setOnClickListener {
                    (activity as ElemzesActivity).deleteTransaction(docId, transaction, {
                        Toast.makeText(context, "Tranzakció törölve", Toast.LENGTH_SHORT).show()
                        // Frissítjük a HonapFragment-et
                        requireActivity().supportFragmentManager.beginTransaction().detach(HonapFragment()).attach(HonapFragment()).commit()
                    }, { e ->
                        Toast.makeText(context, "Törlési hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                    })
                }
            }
            containerLayout.addView(binIcon)
            val transactionDetails = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val catTextView = TextView(context).apply {
                text = category
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 4, 0, 2)
                }
            }
            transactionDetails.addView(catTextView)
            val rowLayout = RelativeLayout(context).apply {
                layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            }
            val amountTextView = TextView(context).apply {
                id = View.generateViewId()
                textSize = 16f
                gravity = Gravity.START
                setTextColor(if (type == "bevétel") Color.GREEN else if (type == "kiadás") Color.RED else Color.BLACK)
                text = when (type) {
                    "bevétel" -> "+ $amount"
                    "kiadás" -> "- $amount"
                    else -> "$amount"
                }
            }
            val amountParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            amountTextView.layoutParams = amountParams
            rowLayout.addView(amountTextView)
            val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
            val iconView = ImageView(context).apply {
                id = View.generateViewId()
                setImageResource(iconRes)
            }
            val iconParams = RelativeLayout.LayoutParams((24 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt()).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
                setMargins(8, 0, 8, 0)
            }
            iconView.layoutParams = iconParams
            rowLayout.addView(iconView)
            transactionDetails.addView(rowLayout)
            containerLayout.addView(transactionDetails)
            return containerLayout
        }
    }

    // 5. IdoszakFragment – Egy egyedi időszak adatai (csökkenő sorrendben)
    class IdoszakFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser

        private var customStart: LocalDate? = null
        private var customEnd: LocalDate? = null

        private lateinit var chart: BarChart

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            chart = requireActivity().findViewById(R.id.oszlopDiagram)
            showTimePeriodDialog()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun showTimePeriodDialog() {
            val context = requireContext()
            val today = LocalDate.now()
            val startPicker = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    customStart = LocalDate.of(year, month + 1, dayOfMonth)
                    val endPicker = DatePickerDialog(
                        context,
                        { _, endYear, endMonth, endDayOfMonth ->
                            customEnd = LocalDate.of(endYear, endMonth + 1, endDayOfMonth)
                            if (customStart!! <= customEnd!!) {
                                val periodString = "${customStart.toString()} - ${customEnd.toString()}"
                                Toast.makeText(context, "Kiválasztott időszak: $periodString", Toast.LENGTH_LONG).show()
                                loadPeriodData()
                            } else {
                                Toast.makeText(context, "A végdátum nem lehet korábbi a kezdőnél!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        today.year,
                        today.monthValue - 1,
                        today.dayOfMonth
                    )
                    endPicker.setTitle("Válassza ki a végdátumot")
                    endPicker.show()
                },
                today.year,
                today.monthValue - 1,
                today.dayOfMonth
            )
            startPicker.setTitle("Válassza ki a kezdő dátumot")
            startPicker.show()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun loadPeriodData() {
            if (currentUser == null || customStart == null || customEnd == null) return
            val uid = currentUser.uid
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.visibility = View.VISIBLE
            categoriesLayout.removeAllViews()

            firestore.collection("users")
                .document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), customStart.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), customEnd.toString())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        showNoDataOverlay("Nincs megjeleníthető adat a választott időszakra.", categoriesLayout)
                        chart.visibility = View.INVISIBLE
                        return@addOnSuccessListener
                    }
                    (activity as ElemzesActivity).removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id) }
                    for (doc in sortedDocs) {
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 4) }
                        }
                        categoriesLayout.addView(dateHeader)
                        try {
                            val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            val sortedTransactions = if (transactions.isNotEmpty() && transactions[0].containsKey("timestamp"))
                                transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                            else transactions.reversed()

                            for (trans in sortedTransactions) {
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                                val transactionView = createTransactionView(category, amount, type, trans, doc.id)
                                categoriesLayout.addView(transactionView)
                                if (type == "bevétel") totalRevenue += amount
                                else if (type == "kiadás") totalExpense += amount
                            }
                        } catch (e: Exception) {
                            Log.e("FirestoreDebug", "Error processing doc $dateStr: ${e.message}")
                        }
                    }
                    val revenueEntry = BarEntry(0f, totalRevenue)
                    val expenseEntry = BarEntry(1f, totalExpense)
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Időszak összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    val barData = BarData(dataSet)
                    chart.data = barData
                    chart.description.isEnabled = false
                    chart.setFitBars(true)
                    chart.invalidate()
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba az időszaki adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba az időszaki adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                }
        }

        private fun showNoDataOverlay(message: String, container: LinearLayout) {
            for (i in 0 until container.childCount) {
                if (container.getChildAt(i).tag == "noDataOverlay") return
            }
            val overlay = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    container.width.takeIf { it > 0 } ?: LinearLayout.LayoutParams.MATCH_PARENT,
                    container.height.takeIf { it > 0 } ?: LinearLayout.LayoutParams.MATCH_PARENT
                )
                tag = "noDataOverlay"
            }
            val topHalf = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val iv = ImageView(requireContext()).apply {
                setImageResource(R.drawable.no_data_icon)
                val size = (48 * resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
            }
            topHalf.addView(iv)
            val bottomHalf = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val tv = TextView(requireContext()).apply {
                text = message
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER }
            }
            bottomHalf.addView(tv)
            overlay.addView(topHalf)
            overlay.addView(bottomHalf)
            container.addView(overlay)
            overlay.bringToFront()
        }

        private fun createTransactionView(category: String, amount: Float, type: String, transaction: Map<String, Any>, docId: String): View {
            val context = requireContext()
            val containerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            // Bin ikon törléshez
            val binIcon = ImageView(context).apply {
                setImageResource(R.drawable.bin_icon)
                val size = (24 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setMargins(8, 0, 8, 0)
                }
                setOnClickListener {
                    (activity as ElemzesActivity).deleteTransaction(docId, transaction, {
                        Toast.makeText(context, "Tranzakció törölve", Toast.LENGTH_SHORT).show()
                        // Frissítjük az IdoszakFragment-et
                        requireActivity().supportFragmentManager.beginTransaction().detach(this@IdoszakFragment).attach(this@IdoszakFragment).commit()
                    }, { e ->
                        Toast.makeText(context, "Törlési hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                    })
                }
            }
            containerLayout.addView(binIcon)
            val transactionDetails = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val catTextView = TextView(context).apply {
                text = category
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 4, 0, 2)
                }
            }
            transactionDetails.addView(catTextView)
            val rowLayout = RelativeLayout(context).apply {
                layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            }
            val amountTextView = TextView(context).apply {
                id = View.generateViewId()
                textSize = 16f
                gravity = Gravity.START
                setTextColor(if (type == "bevétel") Color.GREEN else if (type == "kiadás") Color.RED else Color.BLACK)
                text = when (type) {
                    "bevétel" -> "+ $amount"
                    "kiadás" -> "- $amount"
                    else -> "$amount"
                }
            }
            val amountParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            amountTextView.layoutParams = amountParams
            rowLayout.addView(amountTextView)
            val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
            val iconView = ImageView(context).apply {
                id = View.generateViewId()
                setImageResource(iconRes)
            }
            val iconParams = RelativeLayout.LayoutParams((24 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt()).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
                setMargins(8, 0, 8, 0)
            }
            iconView.layoutParams = iconParams
            rowLayout.addView(iconView)
            transactionDetails.addView(rowLayout)
            containerLayout.addView(transactionDetails)
            return containerLayout
        }
    }

    // 6. DefaultFragment – fallback elrendezés
    class DefaultFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_alap, container, false)
        }
    }

    // --- Delete Transaction metódus ---
    fun deleteTransaction(docId: String, transaction: Map<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        // Számoljuk ki az összeget
        val amount = when (val a = transaction["mennyiseg"]) {
            is Double -> a.toFloat()
            is Long -> a.toFloat()
            else -> 0f
        }
        val type = (transaction["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
        // Ha bevétel törlésénél a fő egyenleg csökken, kiadásnál nő
        val delta = if (type == "bevétel") -amount else if (type == "kiadás") amount else 0f

        FirebaseFirestore.getInstance().runTransaction { trans ->
            // Először olvassuk be a felhasználói dokumentumot
            val userSnapshot = trans.get(userDocRef)
            val napDocRef = userDocRef.collection("nap").document(docId)
            // Frissítjük a tranzakciók listáját: távolítsuk el a törölt tranzakciót
            trans.update(napDocRef, "tranzakciok", FieldValue.arrayRemove(transaction))
            // Frissítjük a fő egyenleget
            val currentBalance = userSnapshot.getDouble("aktualisPenz") ?: 0.0
            val newBalance = currentBalance + delta
            trans.update(userDocRef, "aktualisPenz", newBalance)
            newBalance
        }.addOnSuccessListener { newBalance ->
            Log.d("ElemzesActivity", "Main balance updated to $newBalance after deletion")
            onSuccess()
        }.addOnFailureListener { e ->
            Log.e("ElemzesActivity", "Failed to delete transaction: ${e.message}")
            onFailure(e)
        }
    }
}
