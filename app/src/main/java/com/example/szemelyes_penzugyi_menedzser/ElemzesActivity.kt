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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.FieldPath
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import com.example.szemelyes_penzugyi_menedzser.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import android.annotation.SuppressLint as SuppressLint1

class ElemzesActivity : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private val aktualisOldal = "Elemzés"

    companion object {
        fun formatNumber(value: Int): String {
            val nf = NumberFormat.getIntegerInstance(Locale.US) as DecimalFormat
            val symbols = nf.decimalFormatSymbols
            symbols.groupingSeparator = ' '
            nf.decimalFormatSymbols = symbols
            return nf.format(value)
        }
    }

    @SuppressLint1("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elemzes)

        val currentUser = FirebaseManager.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Bejelentkezes::class.java))
            finish()
            return
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

        // Navigációs feliratok
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

        // Spinner inicializálása
        spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegek)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        findViewById<Button>(R.id.hozzaadasGomb).setOnClickListener {
            startActivity(Intent(this, HozzaadasActivity::class.java))
        }

        // Az Elemzés fül legyen az alapértelmezett, amikor az activity megnyílik
        spinner.setSelection(1)
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
                    "Főoldal" -> startActivity(Intent(this@ElemzesActivity, Telefonszam::class.java))
                    "Kategóriák" -> startActivity(Intent(this@ElemzesActivity, Kategoriak::class.java))
                    "Rendszeres kifizetések" -> startActivity(Intent(this@ElemzesActivity, RendszeresKifizetesek::class.java))
                    "Beállítások" -> startActivity(Intent(this@ElemzesActivity, BeallitasokActivity::class.java))
                    "Kijelentkezés" -> Kijelentkezes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        applyFontSizeToCurrentActivity()

        // Alapértelmezett Fragment betöltése (NapFragment)
        supportFragmentManager.beginTransaction()
            .replace(R.id.oszlopDiagram, NapFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Ha az Elemzés activity-be visszatérünk normál módon, akkor ez a fül marad Elemzés.
        spinner.setSelection(1)
    }

    // Grafikon stílusának beállítása, hogy az alsó tengely vonal folyamatos legyen
    fun styleBarChart(chart: BarChart) {
        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.animateY(1000)
        chart.setScaleEnabled(false)
        // Háttér és segédvonalak kikapcsolása
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        // Engedélyezzük az xAxis-t, hogy alsó vonal legyen látható
        chart.xAxis.isEnabled = true
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawLabels(false)
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.axisLineColor = Color.DKGRAY
        chart.xAxis.axisLineWidth = 2f
        // Eltávolítjuk az első/utolsó elem klippelését, hogy a vonal a teljes szélességet lefedje
        chart.xAxis.setAvoidFirstLastClipping(false)
        // Biztosítjuk, hogy a bal tengely 0-tól induljon
        chart.axisLeft.axisMinimum = 0f
        // Bal oldali tengely testreszabása
        val leftAxis = chart.axisLeft
        leftAxis.textColor = Color.DKGRAY
        leftAxis.textSize = 12f
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawGridLines(false)
        // Jobb oldali tengely kikapcsolása
        chart.axisRight.isEnabled = false

        // Chart háttér és extra margók
        chart.setBackgroundColor(Color.WHITE)
        chart.setExtraOffsets(10f, 10f, 10f, 10f)

        // Jelmagyarázat (legend) testreszabása
        val legend = chart.legend
        legend.textColor = Color.DKGRAY
        legend.textSize = 14f
    }

    // Overlay eltávolítása a BarChart szülőjéből
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

    // showNoDataOverlay metódus, amit fragmentek hívhatnak
    fun showNoDataOverlay(message: String, chart: BarChart, container: LinearLayout) {
        if (!this.isFinishing) {
            val parent = chart.parent as? ViewGroup ?: return
            if (parent.findViewWithTag<View>("noDataOverlay") == null) {
                val overlay = createNoDataOverlay(message)
                overlay?.let {
                    parent.addView(it)
                    it.bringToFront()
                }
            }
            chart.visibility = View.INVISIBLE
        }
    }

    private fun createNoDataOverlay(message: String): View? {
        val ctx = this
        val overlay = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            tag = "noDataOverlay"
        }
        val topHalf = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val iv = ImageView(ctx).apply {
            setImageResource(R.drawable.no_data_icon)
            val size = (48 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
        }
        topHalf.addView(iv)
        val bottomHalf = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val tv = TextView(ctx).apply {
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

    // Public függvény a tranzakciós nézet létrehozásához
    fun createTransactionView(
        category: String,
        amount: Float,
        type: String,
        transaction: Map<String, Any>,
        docId: String
    ): View {
        val ctx = this
        val containerLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val binIcon = ImageView(ctx).apply {
            setImageResource(R.drawable.bin_icon)
            val size = (24 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_VERTICAL
                setMargins(8, 0, 8, 0)
            }
            setOnClickListener {
                deleteTransaction(docId, transaction, {
                    Toast.makeText(ctx, "Tranzakció törölve", Toast.LENGTH_SHORT).show()
                }, { e ->
                    Toast.makeText(ctx, "Törlési hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                })
            }
        }
        containerLayout.addView(binIcon)

        val transactionDetails = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
            isFocusable = true
        }
        val catTextView = TextView(ctx).apply {
            text = category
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 0, 2) }
        }
        transactionDetails.addView(catTextView)

        val rowLayout = RelativeLayout(ctx).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val amountTextView = TextView(ctx).apply {
            id = View.generateViewId()
            textSize = 16f
            gravity = Gravity.START
            setTextColor(if (type == "bevétel") Color.GREEN else if (type == "kiadás") Color.RED else Color.BLACK)
            text = when (type) {
                "bevétel" -> "+ ${formatNumber(amount.toInt())}"
                "kiadás" -> "- ${formatNumber(amount.toInt())}"
                else -> formatNumber(amount.toInt())
            }
        }
        val amountParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.CENTER_VERTICAL)
        }
        amountTextView.layoutParams = amountParams
        rowLayout.addView(amountTextView)

        val iconRes = getIconResForCategory(category)
        val iconView = ImageView(ctx).apply {
            id = View.generateViewId()
            setImageResource(iconRes)
        }
        val iconParams = RelativeLayout.LayoutParams(
            (24 * resources.displayMetrics.density).toInt(),
            (24 * resources.displayMetrics.density).toInt()
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.CENTER_VERTICAL)
            setMargins(8, 0, 8, 0)
        }
        iconView.layoutParams = iconParams
        rowLayout.addView(iconView)
        transactionDetails.addView(rowLayout)

        val description = transaction["leiras"] as? String ?: ""
        val descriptionTextView = TextView(ctx).apply {
            text = "Leírás: $description"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            visibility = View.GONE
            setPadding(0, 8, 0, 8)
        }
        transactionDetails.addView(descriptionTextView)
        transactionDetails.setOnClickListener {
            descriptionTextView.visibility =
                if (descriptionTextView.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        containerLayout.addView(transactionDetails)
        return containerLayout
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
        // Betűméret módosítása itt szükség esetén
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

    // --- Módosított onBackPressed ---
    // Ha a felhasználó a telefon vissza gombját nyomja, navigáljunk a Telefonszam activity-be.
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, Telefonszam::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra("spinnerSelection", 0)
        startActivity(intent)
        finish()
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

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return
            }
            val uid = currentUser.uid
            val todayStr = LocalDate.now().toString()

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereEqualTo(FieldPath.documentId(), todayStr)
                .addSnapshotListener { querySnapshot, e ->
                    if (!isAdded) return@addSnapshotListener
                    categoriesLayout.removeAllViews()
                    if (e != null) {
                        Log.e("FirestoreDebug", "Hiba a napi adatok lekérdezése során: ${e.message}")
                        Toast.makeText(context, "Hiba a napi adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (querySnapshot == null || querySnapshot.isEmpty) {
                        if (categoriesLayout.findViewWithTag<View>("noDataOverlay") == null) {
                            (activity as? ElemzesActivity)?.showNoDataOverlay(
                                "Nincs megjeleníthető adat a választott időszakra.",
                                chart, categoriesLayout
                            )
                            chart.visibility = View.INVISIBLE
                        }
                        return@addSnapshotListener
                    }
                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    for (doc in querySnapshot.documents) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        val sortedTransactions = transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                        for (trans in sortedTransactions) {
                            val amount = when (val a = trans["mennyiseg"]) {
                                is Double -> a.toFloat()
                                is Long -> a.toFloat()
                                else -> 0f
                            }
                            val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                            val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                            val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
                            categoriesLayout.addView(transactionView)
                            if (type == "bevétel") totalRevenue += amount
                            else if (type == "kiadás") totalExpense += amount
                        }
                    }
                    // Always add both entries to maintain positions
                    val revenueEntry = BarEntry(0f, totalRevenue)
                    val expenseEntry = BarEntry(1f, totalExpense)
                    val entries = listOf(revenueEntry, expenseEntry)
                    val dataSet = BarDataSet(entries, "Napi összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return if (barEntry?.y == 0f) "" else ElemzesActivity.formatNumber(barEntry!!.y.toInt())
                        }
                    }
                    val barData = BarData(dataSet)
                    barData.barWidth = 0.45f
                    chart.data = barData
                    (activity as? ElemzesActivity)?.styleBarChart(chart)
                    chart.invalidate()
                }
        }
    }

    // 2. HetFragment – Az aktuális hét adatai
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
                .addSnapshotListener { querySnapshot, e ->
                    if (!isAdded) return@addSnapshotListener
                    categoriesLayout.removeAllViews()
                    if (e != null) {
                        Log.e("FirestoreDebug", "Hiba a heti adatok lekérdezése során: ${e.message}")
                        Toast.makeText(context, "Hiba a heti adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (querySnapshot == null || querySnapshot.isEmpty) {
                        if (categoriesLayout.findViewWithTag<View>("noDataOverlay") == null) {
                            (activity as? ElemzesActivity)?.showNoDataOverlay(
                                "Nincs megjeleníthető adat a választott időszakra.",
                                chart, categoriesLayout
                            )
                            chart.visibility = View.INVISIBLE
                        }
                        return@addSnapshotListener
                    }
                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    for (doc in querySnapshot.documents) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        val sortedTransactions = transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                        for (trans in sortedTransactions) {
                            val amount = when (val a = trans["mennyiseg"]) {
                                is Double -> a.toFloat()
                                is Long -> a.toFloat()
                                else -> 0f
                            }
                            val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                            val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                            val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
                            categoriesLayout.addView(transactionView)
                            if (type == "bevétel") totalRevenue += amount
                            else if (type == "kiadás") totalExpense += amount
                        }
                    }
                    val revenueEntry = BarEntry(0f, totalRevenue)
                    val expenseEntry = BarEntry(1f, totalExpense)
                    val entries = listOf(revenueEntry, expenseEntry)
                    val dataSet = BarDataSet(entries, "Heti összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return if (barEntry?.y == 0f) "" else ElemzesActivity.formatNumber(barEntry!!.y.toInt())
                        }
                    }
                    val barData = BarData(dataSet)
                    barData.barWidth = 0.45f
                    chart.data = barData
                    (activity as? ElemzesActivity)?.styleBarChart(chart)
                    chart.invalidate()
                }
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
            if (!isAdded) return
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.visibility = View.VISIBLE
            chart.visibility = View.VISIBLE

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
                .addSnapshotListener { querySnapshot, e ->
                    if (!isAdded) return@addSnapshotListener
                    categoriesLayout.removeAllViews()
                    if (e != null) {
                        Log.e("FirestoreDebug", "Hiba a havi adatok lekérdezése során: ${e.message}")
                        Toast.makeText(context, "Hiba a havi adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (querySnapshot == null || querySnapshot.isEmpty) {
                        if (categoriesLayout.findViewWithTag<View>("noDataOverlay") == null) {
                            (activity as? ElemzesActivity)?.showNoDataOverlay(
                                "Nincs megjeleníthető adat a választott időszakra.",
                                chart, categoriesLayout
                            )
                            chart.visibility = View.INVISIBLE
                        }
                        return@addSnapshotListener
                    }
                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id, formatter) }
                    for (doc in sortedDocs) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        if (transactions.isEmpty()) {
                            firestore.collection("users").document(uid)
                                .collection("nap").document(doc.id).delete()
                            continue
                        }
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 4) }
                        }
                        categoriesLayout.addView(dateHeader, 0)
                        try {
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
                                val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
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
                    val entries = listOf(revenueEntry, expenseEntry)
                    val dataSet = BarDataSet(entries, "Havi összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return if (barEntry?.y == 0f) "" else ElemzesActivity.formatNumber(barEntry!!.y.toInt())
                        }
                    }
                    val barData = BarData(dataSet)
                    barData.barWidth = 0.45f
                    chart.data = barData
                    (activity as? ElemzesActivity)?.styleBarChart(chart)
                    chart.invalidate()
                }
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
            if (!isAdded) return
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
                    if (!isAdded) return@addOnSuccessListener
                    if (querySnapshot.isEmpty) {
                        if (categoriesLayout.findViewWithTag<View>("noDataOverlay") == null) {
                            (activity as? ElemzesActivity)?.showNoDataOverlay(
                                "Nincs megjeleníthető adat a választott időszakra.",
                                chart, categoriesLayout
                            )
                            chart.visibility = View.INVISIBLE
                        }
                        return@addOnSuccessListener
                    }
                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id, formatter) }
                    for (doc in sortedDocs) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        if (transactions.isEmpty()) {
                            firestore.collection("users").document(uid)
                                .collection("nap").document(doc.id).delete()
                            continue
                        }
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 4) }
                        }
                        categoriesLayout.addView(dateHeader, 0)
                        try {
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
                                val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
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
                    val entries = listOf(revenueEntry, expenseEntry)
                    val dataSet = BarDataSet(entries, "Év összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return if (barEntry?.y == 0f) "" else ElemzesActivity.formatNumber(barEntry!!.y.toInt())
                        }
                    }
                    val barData = BarData(dataSet)
                    barData.barWidth = 0.45f
                    chart.data = barData
                    (activity as? ElemzesActivity)?.styleBarChart(chart)
                    chart.invalidate()
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba az éves adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba az éves adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                }
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
                        if (categoriesLayout.findViewWithTag<View>("noDataOverlay") == null) {
                            (activity as? ElemzesActivity)?.showNoDataOverlay("Nincs megjeleníthető adat a választott időszakra.", chart, categoriesLayout)
                            chart.visibility = View.INVISIBLE
                        }
                        return@addOnSuccessListener
                    }
                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id) }
                    for (doc in sortedDocs) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        if (transactions.isEmpty()) {
                            firestore.collection("users").document(uid)
                                .collection("nap").document(doc.id).delete()
                            continue
                        }
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 4) }
                        }
                        categoriesLayout.addView(dateHeader)
                        try {
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
                                val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
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
                    val entries = listOf(revenueEntry, expenseEntry)
                    val dataSet = BarDataSet(entries, "Időszak összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return if (barEntry?.y == 0f) "" else ElemzesActivity.formatNumber(barEntry!!.y.toInt())
                        }
                    }
                    val barData = BarData(dataSet)
                    barData.barWidth = 0.45f
                    chart.data = barData
                    (activity as? ElemzesActivity)?.styleBarChart(chart)
                    chart.invalidate()
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba az időszaki adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba az időszaki adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 6. DefaultFragment – fallback elrendezés
    inner class DefaultFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_alap, container, false)
        }
    }

    // --- Módosított deleteTransaction metódus ---
    // Ha több azonos tranzakció van, csak az első példány kerül törlésre,
    // és a fő összeg csak egyszer módosul.
    fun deleteTransaction(
        docId: String,
        transaction: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        val amount = when (val a = transaction["mennyiseg"]) {
            is Double -> a.toFloat()
            is Long -> a.toFloat()
            else -> 0f
        }
        val type = (transaction["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
        val delta = if (type == "bevétel") -amount else if (type == "kiadás") amount else 0f

        FirebaseFirestore.getInstance().runTransaction { trans ->
            val userSnapshot = trans.get(userDocRef)
            val napDocRef = userDocRef.collection("nap").document(docId)
            val napSnapshot = trans.get(napDocRef)
            val currentTransactions = napSnapshot.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()

            // Csak az első egyező tranzakció törlése
            val mutableTransactions = currentTransactions.toMutableList()
            val indexToRemove = mutableTransactions.indexOfFirst {
                (it["timestamp"] == transaction["timestamp"]) &&
                        (it["mennyiseg"] == transaction["mennyiseg"]) &&
                        (it["kategoria"] == transaction["kategoria"]) &&
                        (it["tipus"] == transaction["tipus"])
            }
            if (indexToRemove != -1) {
                mutableTransactions.removeAt(indexToRemove)
            }
            if (mutableTransactions.isEmpty()) {
                trans.delete(napDocRef)
            } else {
                trans.update(napDocRef, "tranzakciok", mutableTransactions)
            }
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

    // --- Új addTransaction metódus ---
    // Ez a metódus felelős egy új tranzakció hozzáadásáért, és a fő egyenleg (aktualisPenz) megfelelő frissítéséért.
    // A delta kiszámítása itt fordított: ha a tranzakció típusa "bevétel", akkor a tranzakció összegét hozzáadjuk,
    // ha "kiadás", akkor levonjuk.
    fun addTransaction(
        docId: String,
        transaction: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        val amount = when (val a = transaction["mennyiseg"]) {
            is Double -> a.toFloat()
            is Long -> a.toFloat()
            else -> 0f
        }
        val type = (transaction["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
        // Itt: bevétel esetén hozzáadjuk, kiadás esetén levonjuk
        val delta = if (type == "bevétel") amount else if (type == "kiadás") -amount else 0f

        FirebaseFirestore.getInstance().runTransaction { trans ->
            val userSnapshot = trans.get(userDocRef)
            val napDocRef = userDocRef.collection("nap").document(docId)
            // Próbáljuk lekérdezni a meglévő tranzakciókat (ha létezik a dokumentum)
            val napSnapshot = try {
                trans.get(napDocRef)
            } catch (e: Exception) {
                null
            }
            val currentTransactions = napSnapshot?.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
            val mutableTransactions = currentTransactions.toMutableList()
            mutableTransactions.add(transaction)
            if (currentTransactions.isEmpty()) {
                trans.set(napDocRef, mapOf("tranzakciok" to mutableTransactions))
            } else {
                trans.update(napDocRef, "tranzakciok", mutableTransactions)
            }
            val currentBalance = userSnapshot.getDouble("aktualisPenz") ?: 0.0
            val newBalance = currentBalance + delta
            trans.update(userDocRef, "aktualisPenz", newBalance)
            newBalance
        }.addOnSuccessListener { newBalance ->
            Log.d("ElemzesActivity", "Main balance updated to $newBalance after addition")
            onSuccess()
        }.addOnFailureListener { e ->
            Log.e("ElemzesActivity", "Failed to add transaction: ${e.message}")
            onFailure(e)
        }
    }
}
