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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import com.example.szemelyes_penzugyi_menedzser.FirebaseManager

class ElemzesActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_elemzes)

        // Ellenőrizzük a bejelentkezést
        val currentUser = FirebaseManager.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Bejelentkezes::class.java))
            finish()
            return
        }

        // Navigációs gombok beállítása
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

        // Spinner beállítása
        val spinner: Spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegek)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // A hozzaadasGomb által történik a fő összeg frissítése (külön activity)
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

        // Alapértelmezettként a NapFragment jelenjen meg a FragmentContainer-ben
        supportFragmentManager.beginTransaction()
            .replace(R.id.oszlopDiagram, NapFragment())
            .commit()
    }

    // Segédfüggvény: A kategória alapján visszaadja a megfelelő ikon erőforrás ID-t.
    fun getIconResForCategory(category: String): Int {
        val norm = category.trim().toLowerCase(Locale.getDefault())
        return when (norm) {
            "ajándékok" -> R.drawable.ajandekok_icon
            "család" -> R.drawable.csalad_icon
            "edzés" -> R.drawable.edzes_icon
            "egészség" -> R.drawable.egeszseg_icon
            "élelmiszerek" -> R.drawable.elelmiszerek_icon
            "kávézó" -> R.drawable.kavezo_icon
            "közlekedés" -> R.drawable.kozlekedes_icon
            "oktatás" -> R.drawable.oktatas_icon
            "otthon" -> R.drawable.otthon_icon
            "szabadidő", "fizetési csekk" -> R.drawable.szabadido_icon
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

    // ------------------- Fragmentek -------------------

    // 1. NapFragment – Egy adott nap (ma) adatai és részletes listája
    class NapFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.removeAllViews()
            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return
            }
            val uid = currentUser.uid
            val today = LocalDate.now()
            val todayStr = today.toString()

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), todayStr)
                .whereLessThanOrEqualTo(FieldPath.documentId(), todayStr)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.documents.isNotEmpty()) {
                        try {
                            val dailyDoc = querySnapshot.documents[0]
                            val transactions = dailyDoc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            var totalRevenue = 0f
                            var totalExpense = 0f

                            // Iteráljunk a tranzakciókon, és hozzunk létre egy soronkénti elemet
                            transactions.forEach { trans ->
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                // Olvassuk ki a kategória nevét és a tranzakció típusát
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""

                                // Hozzunk létre egy vertikális LinearLayout-ot a tranzakcióhoz
                                val transactionLayout = LinearLayout(requireContext())
                                transactionLayout.orientation = LinearLayout.VERTICAL
                                transactionLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )

                                // Első sor: kategória neve
                                val catTextView = TextView(requireContext())
                                catTextView.text = category
                                catTextView.textSize = 16f
                                catTextView.setTextColor(Color.DKGRAY)
                                val catLp = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                catLp.setMargins(0, 4, 0, 2)
                                catTextView.layoutParams = catLp
                                transactionLayout.addView(catTextView)

                                // Második sor: egy vízszintes LinearLayout, amely tartalmazza az összeget és az ikont
                                val rowLayout = LinearLayout(requireContext())
                                rowLayout.orientation = LinearLayout.HORIZONTAL
                                rowLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )

                                val amountTextView = TextView(requireContext())
                                amountTextView.textSize = 16f
                                amountTextView.layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                                )

                                // Állapítsuk meg az előjelet és színt
                                if (type == "bevétel") {
                                    amountTextView.text = "+ $amount"
                                    amountTextView.setTextColor(Color.GREEN)
                                    totalRevenue += amount
                                } else if (type == "kiadás") {
                                    amountTextView.text = "- $amount"
                                    amountTextView.setTextColor(Color.RED)
                                    totalExpense += amount
                                }
                                rowLayout.addView(amountTextView)

                                // Ikon: az adott kategóriához tartozó ikon
                                val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
                                val iv = ImageView(requireContext())
                                iv.setImageResource(iconRes)
                                val ivSize = (24 * resources.displayMetrics.density).toInt()
                                val ivLp = LinearLayout.LayoutParams(ivSize, ivSize)
                                ivLp.gravity = Gravity.CENTER_VERTICAL
                                ivLp.setMargins(8, 0, 8, 0)
                                iv.layoutParams = ivLp
                                rowLayout.addView(iv)

                                // Adjuk hozzá a vízszintes row-t a vertikális transactionLayout-hoz
                                transactionLayout.addView(rowLayout)

                                // Adjuk hozzá az egyes tranzakciós elemeket a CategoriesLinearLayout-hoz
                                categoriesLayout.addView(transactionLayout)
                            }
                            // Diagram frissítése: két oszlop: bevétel és kiadás
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
                    } else {
                        Toast.makeText(context, "Nincs adat a mai napra.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba a napi adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba a napi adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 2. HetFragment – Az aktuális hét (hétfőtől vasárnapig) adatai és részletes listája
    class HetFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.removeAllViews()
            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return
            }
            val uid = currentUser.uid
            val today = LocalDate.now()
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), weekStart.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), weekEnd.toString())
                .orderBy(FieldPath.documentId())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    querySnapshot.documents.forEach { doc ->
                        val dateStr = doc.id
                        // Dátum fejléc
                        val dateHeader = TextView(requireContext())
                        dateHeader.text = dateStr
                        dateHeader.textSize = 16f
                        dateHeader.setTextColor(Color.DKGRAY)
                        val headerLp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        headerLp.setMargins(0, 8, 0, 4)
                        dateHeader.layoutParams = headerLp
                        categoriesLayout.addView(dateHeader)

                        try {
                            val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            transactions.forEach { trans ->
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                                // Készítsünk egy vertikális layoutot a tranzakcióhoz
                                val transactionLayout = LinearLayout(requireContext())
                                transactionLayout.orientation = LinearLayout.VERTICAL
                                transactionLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                // Első sor: a kategória neve
                                val catTV = TextView(requireContext())
                                catTV.text = category
                                catTV.textSize = 16f
                                catTV.setTextColor(Color.DKGRAY)
                                val catLp = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                catLp.setMargins(0, 4, 0, 2)
                                catTV.layoutParams = catLp
                                transactionLayout.addView(catTV)
                                // Második sor: a tranzakció összege és az ikon
                                val rowLayout = LinearLayout(requireContext())
                                rowLayout.orientation = LinearLayout.HORIZONTAL
                                rowLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                val tv = TextView(requireContext())
                                tv.textSize = 16f
                                tv.layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                                )
                                if (type == "bevétel") {
                                    tv.text = "+ $amount"
                                    tv.setTextColor(Color.GREEN)
                                    totalRevenue += amount
                                } else if (type == "kiadás") {
                                    tv.text = "- $amount"
                                    tv.setTextColor(Color.RED)
                                    totalExpense += amount
                                }
                                rowLayout.addView(tv)
                                val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
                                val iv = ImageView(requireContext())
                                iv.setImageResource(iconRes)
                                val ivSize = (24 * resources.displayMetrics.density).toInt()
                                val ivLp = LinearLayout.LayoutParams(ivSize, ivSize)
                                ivLp.gravity = Gravity.CENTER_VERTICAL
                                ivLp.setMargins(8, 0, 8, 0)
                                iv.layoutParams = ivLp
                                rowLayout.addView(iv)
                                transactionLayout.addView(rowLayout)
                                categoriesLayout.addView(transactionLayout)
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
    }

    // 3. HonapFragment – Az aktuális hónap adatai és részletes listája
    class HonapFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
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
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), monthStart.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), monthEnd.toString())
                .orderBy(FieldPath.documentId())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    var totalRevenue = 0f
                    var totalExpense = 0f
                    querySnapshot.documents.forEach { doc ->
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext())
                        dateHeader.text = dateStr
                        dateHeader.textSize = 16f
                        dateHeader.setTextColor(Color.DKGRAY)
                        val headerLp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        headerLp.setMargins(0, 8, 0, 4)
                        dateHeader.layoutParams = headerLp
                        categoriesLayout.addView(dateHeader)

                        try {
                            val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            transactions.forEach { trans ->
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                                val transactionLayout = LinearLayout(requireContext())
                                transactionLayout.orientation = LinearLayout.VERTICAL
                                transactionLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                // Kategória név
                                val catTV = TextView(requireContext())
                                catTV.text = category
                                catTV.textSize = 16f
                                catTV.setTextColor(Color.DKGRAY)
                                val catLp = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                catLp.setMargins(0, 4, 0, 2)
                                catTV.layoutParams = catLp
                                transactionLayout.addView(catTV)
                                // Összeg + ikon (vízszintes sor)
                                val rowLayout = LinearLayout(requireContext())
                                rowLayout.orientation = LinearLayout.HORIZONTAL
                                rowLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                val tv = TextView(requireContext())
                                tv.textSize = 16f
                                tv.layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                                )
                                if (type == "bevétel") {
                                    tv.text = "+ $amount"
                                    tv.setTextColor(Color.GREEN)
                                    totalRevenue += amount
                                } else if (type == "kiadás") {
                                    tv.text = "- $amount"
                                    tv.setTextColor(Color.RED)
                                    totalExpense += amount
                                }
                                rowLayout.addView(tv)
                                val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
                                val iv = ImageView(requireContext())
                                iv.setImageResource(iconRes)
                                val ivSize = (24 * resources.displayMetrics.density).toInt()
                                val ivLp = LinearLayout.LayoutParams(ivSize, ivSize)
                                ivLp.gravity = Gravity.CENTER_VERTICAL
                                ivLp.setMargins(8, 0, 8, 0)
                                iv.layoutParams = ivLp
                                rowLayout.addView(iv)
                                transactionLayout.addView(rowLayout)
                                categoriesLayout.addView(transactionLayout)
                            }
                        } catch (e: Exception) {
                            Log.e("FirestoreDebug", "Error processing doc $dateStr: ${e.message}")
                        }
                    }
                    val revenueEntry = BarEntry(0f, totalRevenue)
                    val expenseEntry = BarEntry(1f, totalExpense)
                    val dataSet = BarDataSet(listOf(revenueEntry, expenseEntry), "Hónap összesítés")
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
    }

    // 4. EvFragment – Az aktuális év adatai és részletes listája
    class EvFragment : Fragment() {
        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_empty, container, false)
        }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val chart = requireActivity().findViewById<BarChart>(R.id.oszlopDiagram)
            val categoriesLayout = requireActivity().findViewById<LinearLayout>(R.id.CategoriesLinearLayout)
            categoriesLayout.removeAllViews()
            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return
            }
            val uid = currentUser.uid
            val today = LocalDate.now()
            val yearStart = LocalDate.of(today.year, 1, 1)
            val yearEnd = LocalDate.of(today.year, 12, 31)

            firestore.collection("users").document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), yearStart.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), yearEnd.toString())
                .orderBy(FieldPath.documentId())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    var totalRevenue = 0f
                    var totalExpense = 0f
                    querySnapshot.documents.forEach { doc ->
                        val dateStr = doc.id
                        val dateHeader = TextView(requireContext())
                        dateHeader.text = dateStr
                        dateHeader.textSize = 16f
                        dateHeader.setTextColor(Color.DKGRAY)
                        val headerLp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        headerLp.setMargins(0, 8, 0, 4)
                        dateHeader.layoutParams = headerLp
                        categoriesLayout.addView(dateHeader)
                        try {
                            val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            transactions.forEach { trans ->
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""
                                val transactionLayout = LinearLayout(requireContext())
                                transactionLayout.orientation = LinearLayout.VERTICAL
                                transactionLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                val catTV = TextView(requireContext())
                                catTV.text = category
                                catTV.textSize = 16f
                                catTV.setTextColor(Color.DKGRAY)
                                val catLp = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                catLp.setMargins(0, 4, 0, 2)
                                catTV.layoutParams = catLp
                                transactionLayout.addView(catTV)
                                val rowLayout = LinearLayout(requireContext())
                                rowLayout.orientation = LinearLayout.HORIZONTAL
                                rowLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                val tv = TextView(requireContext())
                                tv.textSize = 16f
                                tv.layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                                )
                                if (type == "bevétel") {
                                    tv.text = "+ $amount"
                                    tv.setTextColor(Color.GREEN)
                                    totalRevenue += amount
                                } else if (type == "kiadás") {
                                    tv.text = "- $amount"
                                    tv.setTextColor(Color.RED)
                                    totalExpense += amount
                                }
                                rowLayout.addView(tv)
                                val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
                                val iv = ImageView(requireContext())
                                iv.setImageResource(iconRes)
                                val ivSize = (24 * resources.displayMetrics.density).toInt()
                                val ivLp = LinearLayout.LayoutParams(ivSize, ivSize)
                                ivLp.gravity = Gravity.CENTER_VERTICAL
                                ivLp.setMargins(8, 0, 8, 0)
                                iv.layoutParams = ivLp
                                rowLayout.addView(iv)
                                transactionLayout.addView(rowLayout)
                                categoriesLayout.addView(transactionLayout)
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
    }

    // 5. IdoszakFragment – Egy egyedi időszak, melyet a felhasználó DatePickerDialog-okkal választ ki,
    // majd az adott időszak adatait és tranzakcióit listázza a CategoriesLinearLayout ScrollView-ban.
    class IdoszakFragment : Fragment() {

        private val firestore = FirebaseManager.firestore
        private val currentUser = FirebaseManager.auth.currentUser

        // A felhasználó által kiválasztott időszak kezdő- és végdátuma
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
            categoriesLayout.removeAllViews()

            firestore.collection("users")
                .document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), customStart.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), customEnd.toString())
                .orderBy(FieldPath.documentId())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    var totalRevenue = 0f
                    var totalExpense = 0f

                    querySnapshot.documents.forEach { doc ->
                        val dateStr = doc.id
                        // Dátum fejléc
                        val dateHeader = TextView(requireContext())
                        dateHeader.text = dateStr
                        dateHeader.textSize = 16f
                        dateHeader.setTextColor(Color.DKGRAY)
                        val headerLp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        headerLp.setMargins(0, 8, 0, 4)
                        dateHeader.layoutParams = headerLp
                        categoriesLayout.addView(dateHeader)

                        try {
                            val transactions = doc.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            transactions.forEach { trans ->
                                val amount = when (val a = trans["mennyiseg"]) {
                                    is Double -> a.toFloat()
                                    is Long -> a.toFloat()
                                    else -> 0f
                                }
                                val category = (trans["kategoria"] as? String)?.trim() ?: "Egyéb"
                                val type = (trans["tipus"] as? String)?.trim()?.toLowerCase(Locale.getDefault()) ?: ""

                                // Vertikális layout a tranzakcióhoz
                                val transactionLayout = LinearLayout(requireContext())
                                transactionLayout.orientation = LinearLayout.VERTICAL
                                transactionLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                // Első sor: a kategória neve
                                val catTV = TextView(requireContext())
                                catTV.text = category
                                catTV.textSize = 16f
                                catTV.setTextColor(Color.DKGRAY)
                                val catLp = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                catLp.setMargins(0, 4, 0, 2)
                                catTV.layoutParams = catLp
                                transactionLayout.addView(catTV)
                                // Második sor: a tranzakció összege és ikon (vízszintes layout)
                                val rowLayout = LinearLayout(requireContext())
                                rowLayout.orientation = LinearLayout.HORIZONTAL
                                rowLayout.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                val tv = TextView(requireContext())
                                tv.textSize = 16f
                                tv.layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                                )
                                if (type == "bevétel") {
                                    tv.text = "+ $amount"
                                    tv.setTextColor(Color.GREEN)
                                    totalRevenue += amount
                                } else if (type == "kiadás") {
                                    tv.text = "- $amount"
                                    tv.setTextColor(Color.RED)
                                    totalExpense += amount
                                }
                                rowLayout.addView(tv)
                                val iconRes = (activity as ElemzesActivity).getIconResForCategory(category)
                                val iv = ImageView(requireContext())
                                iv.setImageResource(iconRes)
                                val ivSize = (24 * resources.displayMetrics.density).toInt()
                                val ivLp = LinearLayout.LayoutParams(ivSize, ivSize)
                                ivLp.gravity = Gravity.CENTER_VERTICAL
                                ivLp.setMargins(8, 0, 8, 0)
                                iv.layoutParams = ivLp
                                rowLayout.addView(iv)
                                transactionLayout.addView(rowLayout)
                                categoriesLayout.addView(transactionLayout)
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
    }

    // 6. DefaultFragment – fallback elrendezés
    class DefaultFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_alap, container, false)
        }
    }
}
