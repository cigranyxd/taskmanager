package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters


@Suppress("IMPLICIT_CAST_TO_ANY")
class ElemzesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_elemzes)



        auth = FirebaseAuth.getInstance()



        findViewById<TextView>(R.id.NapFelirat).setOnClickListener { SzovegreKattint(it) }
        findViewById<TextView>(R.id.HetFelirat).setOnClickListener { SzovegreKattint(it) }
        findViewById<TextView>(R.id.HonapFelirat).setOnClickListener { SzovegreKattint(it) }
        findViewById<TextView>(R.id.EvFelirat).setOnClickListener { SzovegreKattint(it) }
        findViewById<TextView>(R.id.IdoszakFelirat).setOnClickListener { SzovegreKattint(it) }

        // Az ablak margóinak beállítása a rendszer sávokhoz
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Lenyíló menü inicializálása
        val spinner: Spinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Rendszeres kifizetések", "Kijelentkezés")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lehetosegek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (currentUser != null) {
            // Felhasználó bejelentkezve
        } else {
            // visszadob a bejelentkezési képernyőre
            val intent = Intent(this, Bejelentkezes::class.java)
            startActivity(intent)
            finish()
        }



        val hozzaadasGomb = findViewById<Button>(R.id.hozzaadasGomb)
        hozzaadasGomb.setOnClickListener {
            val intent = Intent(this, HozzaadasActivity::class.java)
            startActivity(intent)
        }


        // Az "Elemzés" menüpont alapértelmezett kiválasztása
        spinner.setSelection(1)

        // Lenyíló menü kiválasztásának figyelése
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                when (selectedItem) {
                    "Főoldal" -> {
                        val intent = Intent(this@ElemzesActivity, Telefonszam::class.java)
                        startActivity(intent)
                    }
                    "Rendszeres kifizetések" -> {
                        Toast.makeText(
                            this@ElemzesActivity,
                            "Rendszeres kifizetések még nem implementáltak",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    "Kijelentkezés" -> {
                        Kijelentkezes()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Semmi sem történt
            }
        }
        applyFontSizeToCurrentActivity()
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
        if (view is TextView) {
            view.textSize = fontSize
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextViewsFontSize(view.getChildAt(i), fontSize)
            }
        }
    }



    private fun Kijelentkezes() {
        auth.signOut()
        getSharedPreferences("UserPreferences", MODE_PRIVATE)
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
            .replace(R.id.TartalomFrame, fragment)
            .commit()
    }



    class IdoszakFragment : Fragment() {
        private lateinit var vonalDiagram: LineChart
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_nap, container, false)

          //  vonalDiagram = gyokerNezet.findViewById(R.id.vonalDiagram)

            val bevetelAdatok = listOf(
                Entry(0f, 1000f),
                Entry(1f, 1200f),
                Entry(2f, 800f),
                Entry(3f, 1500f)
            )
            val kiadasAdatok = listOf(
                Entry(0f, 500f),
                Entry(1f, 700f),
                Entry(2f, 600f),
                Entry(3f, 900f)
            )

            val bevetelSor = LineDataSet(bevetelAdatok, "Bevételek").apply {
                color = Color.GREEN
                lineWidth = 2f
                setCircleColor(Color.GREEN)
                circleRadius = 4f
                valueTextColor = Color.BLACK
            }

            val kiadasSor = LineDataSet(kiadasAdatok, "Kiadások").apply {
                color = Color.RED
                lineWidth = 2f
                setCircleColor(Color.RED)
                circleRadius = 4f
                valueTextColor = Color.BLACK
            }

            val diagramAdatok = LineData(bevetelSor, kiadasSor)
            vonalDiagram.data = diagramAdatok

            vonalDiagram.description.isEnabled = false
            vonalDiagram.animateX(1000)
            vonalDiagram.invalidate()

            return gyokerNezet
        }
    }

    class NapFragment : Fragment() {
        private lateinit var oszlopDiagram: BarChart
        private val firestore = FirebaseFirestore.getInstance()
        private val currentUser = FirebaseAuth.getInstance().currentUser

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_nap, container, false)
            oszlopDiagram = gyokerNezet.findViewById(R.id.oszlopDiagram)

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return gyokerNezet
            }

            // Az aktuális dátum
            val maiDatum = LocalDate.now().toString()
            val uid = currentUser.uid

            firestore.collection("users")
                .document(uid)
                .collection("nap")
                .document(maiDatum)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        try {
                            val tranzakciok = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                            var osszBevetel = 0f
                            var osszKiadas = 0f

                            tranzakciok.forEach { tranzakcio ->
                                val mennyiseg = when (val mennyisegValue = tranzakcio["mennyiseg"]) {
                                    is Double -> mennyisegValue.toFloat()
                                    is Long -> mennyisegValue.toFloat()
                                    else -> {
                                        Log.e("FirestoreDebug", "Hibás típusú mennyiseg érték: $mennyisegValue")
                                        return@forEach
                                    }
                                }

                                val kategoria = tranzakcio["tipus"] as String
                                if (kategoria == "Bevétel") {
                                    osszBevetel += mennyiseg
                                } else if (kategoria == "Kiadás") {
                                    osszKiadas += mennyiseg
                                }
                            }

                            // Oszlopdiagram beállítása
                            setupChart(osszBevetel, osszKiadas)
                        } catch (e: Exception) {
                            Log.e("FirestoreDebug", "Hiba a tranzakciók feldolgozása során: ${e.message}")
                            Toast.makeText(context, "Hiba történt az adatok feldolgozása közben.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Nincs adat a mai napra.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDebug", "Hiba az adatok lekérdezése során: ${e.message}")
                    Toast.makeText(context, "Hiba az adatok lekérdezése során: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            return gyokerNezet
        }

        private fun setupChart(osszBevetel: Float, osszKiadas: Float) {
            val bevetelOszlop = BarEntry(0f, osszBevetel)
            val kiadasOszlop = BarEntry(1f, osszKiadas)

            val bevetelSor = BarDataSet(listOf(bevetelOszlop), "Bevételek").apply {
                color = Color.GREEN
                valueTextColor = Color.BLACK
            }

            val kiadasSor = BarDataSet(listOf(kiadasOszlop), "Kiadások").apply {
                color = Color.RED
                valueTextColor = Color.BLACK
            }

            val diagramAdatok = BarData(bevetelSor, kiadasSor)
            oszlopDiagram.data = diagramAdatok
            oszlopDiagram.description.isEnabled = false
            oszlopDiagram.setFitBars(true)
            oszlopDiagram.animateY(1000)
            oszlopDiagram.invalidate()
        }
    }

    class HetFragment : Fragment() {
        private lateinit var oszlopDiagram: BarChart
        private lateinit var progressBar: ProgressBar
        private val firestore = FirebaseFirestore.getInstance()
        private val currentUser = FirebaseAuth.getInstance().currentUser

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_het, container, false)

            oszlopDiagram = gyokerNezet.findViewById(R.id.oszlopDiagram)
            progressBar = gyokerNezet.findViewById(R.id.progressBar)

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return gyokerNezet
            }

            val maiDatum = LocalDate.now()
            val hetElsoNapja = maiDatum.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val hetUtsoNapja = maiDatum.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

            progressBar.visibility = View.VISIBLE
            val uid = currentUser.uid

            firestore.collection("users")
                .document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), hetElsoNapja.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), hetUtsoNapja.toString())
                .get()
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        var osszesBevetel = 0f
                        var osszesKiadas = 0f

                        task.result?.documents?.forEach { document ->
                            try {
                                val tranzakciok = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                                tranzakciok.forEach { tranzakcio ->
                                    val mennyiseg = (tranzakcio["mennyiseg"] as? Number)?.toFloat() ?: 0f
                                    val kategoria = tranzakcio["tipus"] as? String

                                    when (kategoria) {
                                        "Bevétel" -> osszesBevetel += mennyiseg
                                        "Kiadás" -> osszesKiadas += mennyiseg
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirestoreDebug", "Hiba a dokumentum feldolgozása során: ${e.message}")
                            }
                        }

                        if (osszesBevetel == 0f && osszesKiadas == 0f) {
                            Toast.makeText(context, "Nincs adat az aktuális hétre.", Toast.LENGTH_SHORT).show()
                            oszlopDiagram.clear()
                            oszlopDiagram.invalidate()
                        } else {
                            setupChart(osszesBevetel, osszesKiadas)
                        }
                    } else {
                        Log.e("FirestoreDebug", "Hiba az adatok lekérdezése során: ${task.exception?.message}")
                        Toast.makeText(context, "Hiba az adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                    }
                }

            return gyokerNezet
        }

        private fun setupChart(osszesBevetel: Float, osszesKiadas: Float) {
            val entries = listOf(
                BarEntry(0f, osszesBevetel), // 0. oszlop: Bevételek
                BarEntry(1f, osszesKiadas)  // 1. oszlop: Kiadások
            )

            val dataSet = BarDataSet(entries, "Heti összegzés").apply {
                colors = listOf(Color.GREEN, Color.RED) // Zöld a bevétel, piros a kiadás
                valueTextSize = 14f
            }

            val data = BarData(dataSet)
            oszlopDiagram.data = data
            oszlopDiagram.description.isEnabled = false
            oszlopDiagram.setFitBars(true)
            oszlopDiagram.animateY(1000)
            oszlopDiagram.invalidate()
        }
    }





    class HonapFragment : Fragment() {
        private lateinit var oszlopDiagram: BarChart
        private lateinit var progressBar: ProgressBar
        private val firestore = FirebaseFirestore.getInstance()
        private val currentUser = FirebaseAuth.getInstance().currentUser

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_honap, container, false)

            oszlopDiagram = gyokerNezet.findViewById(R.id.oszlopDiagram)
            progressBar = gyokerNezet.findViewById(R.id.progressBar)

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return gyokerNezet
            }

            // Az aktuális hónap első és utolsó napjának meghatározása
            val maiDatum = LocalDate.now()
            val honapElsoNapja = maiDatum.withDayOfMonth(1)
            val honapUtsoNapja = maiDatum.withDayOfMonth(maiDatum.lengthOfMonth())

            progressBar.visibility = View.VISIBLE
            val uid = currentUser.uid

            firestore.collection("users")
                .document(uid)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), honapElsoNapja.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), honapUtsoNapja.toString())
                .get()
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        var osszesBevetel = 0f
                        var osszesKiadas = 0f

                        task.result?.documents?.forEach { document ->
                            try {
                                val tranzakciok = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                                tranzakciok.forEach { tranzakcio ->
                                    val mennyiseg = (tranzakcio["mennyiseg"] as? Number)?.toFloat() ?: 0f
                                    val kategoria = tranzakcio["tipus"] as? String

                                    when (kategoria) {
                                        "Bevétel" -> osszesBevetel += mennyiseg
                                        "Kiadás" -> osszesKiadas += mennyiseg
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirestoreDebug", "Hiba a dokumentum feldolgozása során: ${e.message}")
                            }
                        }

                        if (osszesBevetel == 0f && osszesKiadas == 0f) {
                            Toast.makeText(context, "Nincs adat az aktuális hónapra.", Toast.LENGTH_SHORT).show()
                            oszlopDiagram.clear()
                            oszlopDiagram.invalidate()
                        } else {
                            setupChart(osszesBevetel, osszesKiadas)
                        }
                    } else {
                        Log.e("FirestoreDebug", "Hiba az adatok lekérdezése során: ${task.exception?.message}")
                        Toast.makeText(context, "Hiba az adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                    }
                }

            return gyokerNezet
        }

        private fun setupChart(osszesBevetel: Float, osszesKiadas: Float) {
            val entries = listOf(
                BarEntry(0f, osszesBevetel), // 0. oszlop: Bevételek
                BarEntry(1f, osszesKiadas)  // 1. oszlop: Kiadások
            )

            val dataSet = BarDataSet(entries, "Havi összegzés").apply {
                colors = listOf(Color.GREEN, Color.RED) // Zöld a bevétel, piros a kiadás
                valueTextSize = 14f
            }

            val data = BarData(dataSet)
            oszlopDiagram.data = data
            oszlopDiagram.description.isEnabled = false
            oszlopDiagram.setFitBars(true)
            oszlopDiagram.animateY(1000)
            oszlopDiagram.invalidate()
        }
    }



    class EvFragment : Fragment() {
        private lateinit var oszlopDiagram: BarChart
        private lateinit var progressBar: ProgressBar
        private val firestore = FirebaseFirestore.getInstance()
        private val currentUser = FirebaseAuth.getInstance().currentUser

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_ev, container, false)

            oszlopDiagram = gyokerNezet.findViewById(R.id.oszlopDiagram)
            progressBar = gyokerNezet.findViewById(R.id.progressBar)

            if (currentUser == null) {
                Toast.makeText(context, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show()
                return gyokerNezet
            }

            // Az aktuális év meghatározása
            val maiDatum = LocalDate.now()
            val aktualisEv = maiDatum.year

            // Lekérdezés az aktuális év napjaira (dokumentum nevek alapján)
            progressBar.visibility = View.VISIBLE
            val uid = currentUser.uid

            firestore.collection("users")
                .document(uid)  // Felhasználói dokumentum
                .collection("nap")  // Napi tranzakciók gyűjteménye
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), "${aktualisEv}-01-01")
                .whereLessThanOrEqualTo(FieldPath.documentId(), "${aktualisEv}-12-31")
                .get()
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        var osszesBevetel = 0f
                        var osszesKiadas = 0f

                        task.result?.documents?.forEach { document ->
                            try {
                                // Dokumentum neve = nap dátum (pl. "2025-01-10")
                                val datumString = document.id
                                val datum = LocalDate.parse(datumString)

                                // Ha az év megegyezik az aktuális évvel, folytatjuk
                                if (datum.year == aktualisEv) {
                                    // Tranzakciók ellenőrzése
                                    val tranzakciok = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                                    tranzakciok.forEach { tranzakcio ->
                                        val mennyisegValue = tranzakcio["mennyiseg"]
                                        if (mennyisegValue != null) {
                                            val mennyiseg = when (mennyisegValue) {
                                                is Double -> mennyisegValue.toFloat() // Ha Double, akkor Float-ra konvertáljuk
                                                is Long -> mennyisegValue.toFloat()  // Ha Long, akkor Float-ra konvertáljuk
                                                else -> {
                                                    Log.e("FirestoreDebug", "Hibás típusú mennyiseg érték: $mennyisegValue")
                                                    return@forEach // Ha nem Double vagy Long, akkor kilépünk
                                                }
                                            }

                                            val kategoria = tranzakcio["tipus"] as? String
                                            if (kategoria == null) {
                                                Log.e("FirestoreDebug", "Hiányzó 'kategoria' mező a tranzakcióban: $tranzakcio")
                                                return@forEach
                                            }

                                            if (kategoria == "Bevétel") {
                                                osszesBevetel += mennyiseg
                                            } else if (kategoria == "Kiadás") {
                                                osszesKiadas += mennyiseg
                                            }
                                        } else {
                                            Log.w("FirestoreDebug", "Hiányzó 'mennyiseg' mező a tranzakcióban: $tranzakcio")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirestoreDebug", "Hiba a dokumentum feldolgozása során: ${e.message}")
                            }
                        }

                        // Diagram beállítása az adatok alapján
                        if (osszesBevetel == 0f && osszesKiadas == 0f) {
                            Toast.makeText(context, "Nincs adat az aktuális évre.", Toast.LENGTH_SHORT).show()
                            oszlopDiagram.clear()
                            oszlopDiagram.invalidate()
                        } else {
                            setupChart(osszesBevetel, osszesKiadas)
                        }
                    } else {
                        Log.e("FirestoreDebug", "Hiba az adatok lekérdezése során: ${task.exception?.message}")
                        Toast.makeText(context, "Hiba az adatok lekérdezése során.", Toast.LENGTH_SHORT).show()
                    }
                }

            return gyokerNezet
        }

        private fun setupChart(osszesBevetel: Float, osszesKiadas: Float) {
            val entries = listOf(
                BarEntry(0f, osszesBevetel), // 0. oszlop: Bevételek
                BarEntry(1f, osszesKiadas)  // 1. oszlop: Kiadások
            )

            val dataSet = BarDataSet(entries, "Éves összegzés").apply {
                colors = listOf(Color.GREEN, Color.RED) // Zöld a bevétel, piros a kiadás
                valueTextSize = 14f
            }

            val data = BarData(dataSet)
            oszlopDiagram.data = data
            oszlopDiagram.description.isEnabled = false
            oszlopDiagram.setFitBars(true)
            oszlopDiagram.animateY(1000)
            oszlopDiagram.invalidate()
        }
    }




    class DefaultFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_alap, container, false)
        }
    }
}
