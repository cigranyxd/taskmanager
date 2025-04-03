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
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import com.example.szemelyes_penzugyi_menedzser.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.math.BigDecimal
import android.annotation.SuppressLint as SuppressLint1

class ElemzesActivity : BaseActivity() {

    private lateinit var spinner: Spinner
    private val aktualisOldal = "Elemzés"

    companion object {
        fun formatNumber(value: Double): String {
            val nf = NumberFormat.getNumberInstance(Locale.US) as DecimalFormat
            val symbols = nf.decimalFormatSymbols
            symbols.groupingSeparator = ' '
            nf.decimalFormatSymbols = symbols
            nf.minimumFractionDigits = 0
            nf.maximumFractionDigits = 2
            return nf.format(value) + " Ft"
        }

        fun scaleValueForChart(value: Double): Float {
            return (value / 1_000_000).toFloat()
        }

        fun unscaleValueForLabel(value: Float): Double {
            return value.toDouble() * 1_000_000
        }


        fun addTransaction(
            context: Context,
            docId: String,
            transaction: Map<String, Any>,
            onSuccess: () -> Unit,
            onFailure: (Exception) -> Unit
        ) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            val amount = when (val a = transaction["mennyiseg"]) {
                is Double -> BigDecimal.valueOf(a)
                is Long -> BigDecimal.valueOf(a.toDouble())
                is String -> BigDecimal(a)
                else -> BigDecimal.ZERO
            }
            val type = (transaction["tipus"] as? String)?.trim()?.lowercase(Locale.getDefault()) ?: ""
            val delta = when (type) {
                "bevétel" -> amount
                "kiadás" -> amount.negate()
                else -> BigDecimal.ZERO
            }

            FirebaseFirestore.getInstance().runTransaction { trans ->
                val userSnapshot = trans.get(userDocRef)
                val napDocRef = userDocRef.collection("nap").document(docId)
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

                val currentBalance = BigDecimal.valueOf(userSnapshot.getDouble("aktualisPenz") ?: 0.0)
                val newBalance = currentBalance.add(delta)
                trans.update(userDocRef, "aktualisPenz", newBalance.toDouble())
                newBalance
            }.addOnSuccessListener {
                Log.d("ElemzesActivity", "Main balance updated successfully.")
                onSuccess()
            }.addOnFailureListener { e ->
                Log.e("ElemzesActivity", "Failed to add transaction: ${e.message}")
                onFailure(e)
            }
        }
    }

    @SuppressLint1("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elemzes)
        applyFontSizeToCurrentActivity()

        val currentUser = FirebaseManager.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Bejelentkezes::class.java))
            finish()
            return
        }

        fun SzovegreKattint(view: View) {
            // Töröljük az összes időszak TextView kiválasztottságát
            val ids = listOf(
                R.id.NapFelirat,
                R.id.HetFelirat,
                R.id.HonapFelirat,
                R.id.EvFelirat,
                R.id.IdoszakFelirat
            )
            for (id in ids) {
                findViewById<TextView>(id).isSelected = false
            }
            // Az aktuálisan megnyomott TextView legyen selected
            (view as? TextView)?.isSelected = true

            // Fragment váltás a kiválasztott elem alapján
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

        spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val spinnerAdapter = CustomFontSizeSpinnerAdapter(this, R.layout.spinner_item, lehetosegek)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
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

    // Grafikon stílusának beállítása
    fun styleBarChart(chart: BarChart) {
        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.animateY(1000)
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.xAxis.isEnabled = true
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawLabels(false)
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.axisLineColor = Color.DKGRAY
        chart.xAxis.axisLineWidth = 2f
        chart.xAxis.setAvoidFirstLastClipping(false)
        chart.axisLeft.axisMinimum = 0f
        val leftAxis = chart.axisLeft
        leftAxis.textColor = Color.DKGRAY
        leftAxis.textSize = 12f
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.setBackgroundColor(Color.WHITE)
        chart.setExtraOffsets(10f, 10f, 10f, 10f)
        val legend = chart.legend
        legend.textColor = Color.DKGRAY
        legend.textSize = 14f


    }

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

    // A tranzakciós nézet létrehozása – amount paraméter Double típusú
    fun createTransactionView(
        category: String,
        amount: Double,
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
            setOnClickListener { view ->
                // Az első kattintás után letiltjuk a gombot
                view.isEnabled = false
                deleteTransaction(docId, transaction, {
                    // Siker esetén eltávolítjuk a tranzakció nézetét
                    (containerLayout.parent as? ViewGroup)?.removeView(containerLayout)
                    Toast.makeText(ctx, "Tranzakció törölve", Toast.LENGTH_SHORT).show()
                }, { e ->
                    // Hiba esetén újra engedélyezzük a gombot
                    view.isEnabled = true
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
            setTextColor(
                when (type) {
                    "bevétel" -> Color.GREEN
                    "kiadás" -> Color.RED
                    else -> Color.BLACK
                }
            )
            text = when (type) {
                "bevétel" -> "+ ${formatNumber(amount)}"
                "kiadás" -> "- ${formatNumber(amount)}"
                else -> formatNumber(amount)
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

    fun getIconResForCategory(category: String): Int {
        val norm = category.trim().lowercase(Locale.getDefault())
        return when {
            norm.contains("ajándék") -> R.drawable.ajandekok_icon
            norm.contains("családi") || norm.contains("csalad") -> R.drawable.csalad_icon
            norm.contains("edzés") || norm.contains("edzes") || norm.contains("sport") -> R.drawable.edzes_icon
            norm.contains("egészség") || norm.contains("egeszseg") -> R.drawable.egeszseg_icon
            norm.contains("élelmiszer") || norm.contains("elelmiszer") -> R.drawable.elelmiszerek_icon
            norm.contains("kávézó") || norm.contains("kavezo") -> R.drawable.kavezo_icon
            norm.contains("közlekedés") || norm.contains("kozlekedes") -> R.drawable.kozlekedes_icon
            norm.contains("oktatás") || norm.contains("oktatas") -> R.drawable.oktatas_icon
            norm.contains("otthon") -> R.drawable.otthon_icon
            norm.contains("szabadidő") || norm.contains("szabadido") || norm.contains("csekk") -> R.drawable.szabadido_icon
            norm.contains("fizetés") || norm.contains("fizetes") -> R.drawable.szabadido_icon
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
        private val scale = 1_000_000.0
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

                    var totalRevenue = 0.0
                    var totalExpense = 0.0

                    for (doc in querySnapshot.documents) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        val sortedTransactions = transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }

                        // Fordított sorrendű tranzakciók feldolgozása
                        for (trans in sortedTransactions.reversed()) {
                            val amount = when (val a = trans["mennyiseg"]) {
                                is Double -> a
                                is Long -> a.toDouble()
                                else -> 0.0
                            }
                            val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                            val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                            val transactionView = (activity as ElemzesActivity)
                                .createTransactionView(category, amount, type, trans, doc.id)
                            categoriesLayout.addView(transactionView, 0)
                            if (type == "bevétel") totalRevenue += amount
                            else if (type == "kiadás") totalExpense += amount
                        }
                    }
                    val revenueEntry = BarEntry(0f, (totalRevenue / scale).toFloat())
                    val expenseEntry = BarEntry(1f, (totalExpense / scale).toFloat())
                    val entries = listOf(revenueEntry, expenseEntry)
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Napi összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return when (barEntry?.x?.toInt()) {
                                0 -> ElemzesActivity.formatNumber(totalRevenue)
                                1 -> ElemzesActivity.formatNumber(totalExpense)
                                else -> ""
                            }
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
        private val scale = 1_000_000.0
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
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), weekStart.format(formatter))
                .whereLessThanOrEqualTo(FieldPath.documentId(), weekEnd.format(formatter))
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!isAdded) return@addOnSuccessListener

                    if (querySnapshot.isEmpty) {
                        (activity as? ElemzesActivity)?.showNoDataOverlay(
                            "Nincs megjeleníthető adat a választott időszakra.", chart, categoriesLayout
                        )
                        chart.visibility = View.INVISIBLE
                        return@addOnSuccessListener
                    }

                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)
                    var totalRevenue = 0.0
                    var totalExpense = 0.0

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id, formatter) }

                    for (doc in sortedDocs) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        if (transactions.isEmpty()) continue

                        val dateStr = doc.id
                        val dailyContainer = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 12, 0, 12) }
                            tag = "container_$dateStr"
                        }

                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 8) }
                        }
                        dailyContainer.addView(dateHeader)

                        val sortedTransactions = transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                        for (trans in sortedTransactions) {
                            val amount = when (val a = trans["mennyiseg"]) {
                                is Double -> a
                                is Long -> a.toDouble()
                                else -> 0.0
                            }
                            val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                            val type = (trans["tipus"] as? String)?.trim()?.lowercase(Locale.getDefault()) ?: ""
                            val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
                            dailyContainer.addView(transactionView)

                            if (type == "bevétel") totalRevenue += amount
                            else if (type == "kiadás") totalExpense += amount
                        }

                        categoriesLayout.addView(dailyContainer)
                    }

                    val revenueEntry = BarEntry(0f, (totalRevenue / scale).toFloat())
                    val expenseEntry = BarEntry(1f, (totalExpense / scale).toFloat())
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Heti összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return when (barEntry?.x?.toInt()) {
                                0 -> ElemzesActivity.formatNumber(totalRevenue)
                                1 -> ElemzesActivity.formatNumber(totalExpense)
                                else -> ""
                            }
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
        private val scale = 1_000_000.0
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
            val today = LocalDate.now()
            val monthStart = today.withDayOfMonth(1)
            val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), monthStart.format(formatter))
                .whereLessThanOrEqualTo(FieldPath.documentId(), monthEnd.format(formatter))
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!isAdded) return@addOnSuccessListener

                    if (querySnapshot.isEmpty) {
                        (activity as? ElemzesActivity)?.showNoDataOverlay(
                            "Nincs megjeleníthető adat a választott időszakra.",
                            chart, categoriesLayout
                        )
                        chart.visibility = View.INVISIBLE
                        return@addOnSuccessListener
                    }

                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)
                    var totalRevenue = 0.0
                    var totalExpense = 0.0

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id, formatter) }

                    for (doc in sortedDocs) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        if (transactions.isEmpty()) continue

                        val dateStr = doc.id
                        val dailyContainer = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 12, 0, 12) }
                        }

                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 8) }
                        }
                        dailyContainer.addView(dateHeader)

                        val sortedTransactions = transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                        for (trans in sortedTransactions) {
                            val amount = when (val a = trans["mennyiseg"]) {
                                is Double -> a
                                is Long -> a.toDouble()
                                else -> 0.0
                            }
                            val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                            val type = (trans["tipus"] as? String)?.trim()?.lowercase(Locale.getDefault()) ?: ""
                            val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
                            dailyContainer.addView(transactionView)

                            if (type == "bevétel") totalRevenue += amount else if (type == "kiadás") totalExpense += amount
                        }
                        categoriesLayout.addView(dailyContainer)
                    }

                    val revenueEntry = BarEntry(0f, (totalRevenue / scale).toFloat())
                    val expenseEntry = BarEntry(1f, (totalExpense / scale).toFloat())
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Havi összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return when (barEntry?.x?.toInt()) {
                                0 -> ElemzesActivity.formatNumber(totalRevenue)
                                1 -> ElemzesActivity.formatNumber(totalExpense)
                                else -> ""
                            }
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
        private val scale = 1_000_000.0
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
                        (activity as? ElemzesActivity)?.showNoDataOverlay("Nincs megjeleníthető adat a választott időszakra.", chart, categoriesLayout)
                        chart.visibility = View.INVISIBLE
                        return@addOnSuccessListener
                    }

                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)
                    var totalRevenue = 0.0
                    var totalExpense = 0.0

                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id, formatter) }

                    for (doc in sortedDocs) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        if (transactions.isEmpty()) continue

                        val dateStr = doc.id
                        val dailyContainer = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 12, 0, 12) }
                        }

                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 8) }
                        }
                        dailyContainer.addView(dateHeader)

                        val sortedTransactions = transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                        for (trans in sortedTransactions) {
                            val amount = when (val a = trans["mennyiseg"]) {
                                is Double -> a
                                is Long -> a.toDouble()
                                else -> 0.0
                            }
                            val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                            val type = (trans["tipus"] as? String)?.trim()?.lowercase(Locale.getDefault()) ?: ""
                            val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
                            dailyContainer.addView(transactionView)

                            if (type == "bevétel") totalRevenue += amount else if (type == "kiadás") totalExpense += amount
                        }
                        categoriesLayout.addView(dailyContainer)
                    }

                    val revenueEntry = BarEntry(0f, (totalRevenue / scale).toFloat())
                    val expenseEntry = BarEntry(1f, (totalExpense / scale).toFloat())
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Éves összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return when (barEntry?.x?.toInt()) {
                                0 -> ElemzesActivity.formatNumber(totalRevenue)
                                1 -> ElemzesActivity.formatNumber(totalExpense)
                                else -> ""
                            }
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



    // 5. IdoszakFragment – Egy egyedi időszak adatai (csökkenő sorrendben)
    class IdoszakFragment : Fragment() {
        private val scale = 1_000_000.0
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
                        today.year, today.monthValue - 1, today.dayOfMonth
                    )
                    endPicker.setTitle("Válassza ki a végdátumot")
                    endPicker.show()
                },
                today.year, today.monthValue - 1, today.dayOfMonth
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
                        (activity as? ElemzesActivity)?.showNoDataOverlay("Nincs megjeleníthető adat a választott időszakra.", chart, categoriesLayout)
                        chart.visibility = View.INVISIBLE
                        return@addOnSuccessListener
                    }
                    (activity as? ElemzesActivity)?.removeNoDataOverlay(chart)

                    var totalRevenue = 0.0
                    var totalExpense = 0.0

                    // Fordított sorrendbe rakjuk a dokumentumokat, hogy a legújabbak legyenek elöl
                    val sortedDocs = querySnapshot.documents.sortedByDescending { LocalDate.parse(it.id) }

                    for (doc in sortedDocs) {
                        val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        if (transactions.isEmpty()) continue

                        val dateStr = doc.id
                        val dailyContainer = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 12, 0, 12) }
                        }

                        val dateHeader = TextView(requireContext()).apply {
                            text = dateStr
                            textSize = 16f
                            setTextColor(Color.DKGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 8) }
                        }
                        dailyContainer.addView(dateHeader)

                        // A tranzakciók rendezése a legújabb tranzakcióval elöl
                        val sortedTransactions = transactions.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                        for (trans in sortedTransactions) {
                            val amount = when (val a = trans["mennyiseg"]) {
                                is Double -> a
                                is Long -> a.toDouble()
                                else -> 0.0
                            }
                            val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                            val type = (trans["tipus"] as? String)?.trim()?.lowercase(Locale.getDefault()) ?: ""
                            val transactionView = (activity as ElemzesActivity).createTransactionView(category, amount, type, trans, doc.id)
                            dailyContainer.addView(transactionView)

                            if (type == "bevétel") totalRevenue += amount else if (type == "kiadás") totalExpense += amount
                        }
                        categoriesLayout.addView(dailyContainer)
                    }

                    val revenueEntry = BarEntry(0f, (totalRevenue / scale).toFloat())
                    val expenseEntry = BarEntry(1f, (totalExpense / scale).toFloat())
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Időszaki összesítés")
                    dataSet.colors = listOf(Color.GREEN, Color.RED)
                    dataSet.valueTextColor = Color.BLACK
                    dataSet.valueTextSize = 16f
                    dataSet.valueFormatter = object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry?): String {
                            return when (barEntry?.x?.toInt()) {
                                0 -> ElemzesActivity.formatNumber(totalRevenue)
                                1 -> ElemzesActivity.formatNumber(totalExpense)
                                else -> ""
                            }
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
    fun deleteTransaction(
        docId: String,
        transaction: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        val amount = when (val a = transaction["mennyiseg"]) {
            is Double -> BigDecimal.valueOf(a)
            is Long -> BigDecimal.valueOf(a.toDouble())
            is String -> BigDecimal(a)
            else -> BigDecimal.ZERO
        }
        val type = (transaction["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
        val delta = when (type) {
            "bevétel" -> amount.negate()
            "kiadás" -> amount
            else -> BigDecimal.ZERO
        }

        FirebaseFirestore.getInstance().runTransaction { trans ->
            val userSnapshot = trans.get(userDocRef)
            val napDocRef = userDocRef.collection("nap").document(docId)
            val napSnapshot = trans.get(napDocRef)
            val currentTransactions = napSnapshot.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()

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
            val currentBalance = BigDecimal.valueOf(userSnapshot.getDouble("aktualisPenz") ?: 0.0)
            val newBalance = currentBalance.add(delta)
            trans.update(userDocRef, "aktualisPenz", newBalance.toDouble())
            newBalance
        }.addOnSuccessListener { newBalance ->
            Log.d("ElemzesActivity", "Main balance updated to $newBalance after deletion")
            onSuccess()

            // ÚJ: ha a tranzakciós blokk (napiContainer) üres maradt, távolítsuk el azt is a nézetből
            val mainLayout = findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            val parentView = currentFocus?.parent as? LinearLayout ?: return@addOnSuccessListener
            val container = parentView.parent as? LinearLayout ?: return@addOnSuccessListener
            if (container.childCount <= 1) {
                // csak a dátum TextView maradt
                (container.parent as? LinearLayout)?.removeView(container)
            }

        }.addOnFailureListener { e ->
            Log.e("ElemzesActivity", "Failed to delete transaction: ${e.message}")
            onFailure(e)
        }
    }

    // --- Új addTransaction metódus ---
    fun addTransaction(
        docId: String,
        transaction: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        val amount = when (val a = transaction["mennyiseg"]) {
            is Double -> BigDecimal.valueOf(a)
            is Long -> BigDecimal.valueOf(a.toDouble())
            is String -> BigDecimal(a)
            else -> BigDecimal.ZERO
        }
        val type = (transaction["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
        val delta = when (type) {
            "bevétel" -> amount
            "kiadás" -> amount.negate()
            else -> BigDecimal.ZERO
        }

        FirebaseFirestore.getInstance().runTransaction { trans ->
            val userSnapshot = trans.get(userDocRef)
            val napDocRef = userDocRef.collection("nap").document(docId)
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

            val currentBalance = BigDecimal.valueOf(userSnapshot.getDouble("aktualisPenz") ?: 0.0)
            val newBalance = currentBalance.add(delta)
            trans.update(userDocRef, "aktualisPenz", newBalance.toDouble())
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
