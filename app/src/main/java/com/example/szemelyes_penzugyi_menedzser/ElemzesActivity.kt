package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

@Suppress("IMPLICIT_CAST_TO_ANY")
class ElemzesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_elemzes)

        auth = FirebaseAuth.getInstance()

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
        if (currentUser != null) {
            // Felhasználó bejelentkezve
        } else {
            // visszadob a bejelentkezési képernyőre
            val intent = Intent(this, Bejelentkezes::class.java)
            startActivity(intent)
            finish()
        }



        //Ez az osztály tartalmazza a felugró ablak logikáját. A felhasználó itt megadhatja az összeget, a kategóriát és a dátumot.

        class HozzadasDialogFragment : DialogFragment() {

            @SuppressLint("NewApi")
            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                val view = inflater.inflate(R.layout.dialog_hozzadas, container, false)

                val osszegInput = view.findViewById<EditText>(R.id.osszegInput)
                val kategoriakSpinner = view.findViewById<Spinner>(R.id.kategoriakSpinner)
                val mentesGomb = view.findViewById<Button>(R.id.mentesGomb)
                val leirasInput = view.findViewById<EditText>(R.id.leirasInput)

                // Spinner beállítása
                val kategoriak = listOf("Bevétel", "Kiadás")
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    kategoriak
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                kategoriakSpinner.adapter = adapter

                // A mentés gomb lenyomása után az adatok elmentésre kerülnek az adatbázisban.
                mentesGomb.setOnClickListener {
                    val osszeg = osszegInput.text.toString().toDoubleOrNull()
                    val kategoria = kategoriakSpinner.selectedItem.toString()
                    val leiras = leirasInput.text.toString()  // A leírás mező szövege
                    val firestore = FirebaseFirestore.getInstance()
                    val dokNev = LocalDate.now().toString()  // A dokumentum neve: a mai nap dátuma

                    if (osszeg != null && leiras.isNotBlank()) {

                        // A tranzakció létrehozása HashMap-ben
                        val transaction = hashMapOf<String, Any>(
                            "mennyiseg" to osszeg,  // Összeg elmentése
                            "kategoria" to kategoria,  // Kategória elmentése
                            "datum" to Timestamp.now(),  // Dátum elmentése
                            "leiras" to leiras  // Leírás elmentése
                        )

                        // Aznapra már létező dokumentumban tároljuk a tranzakciókat egy listában
                        firestore.collection("nap")  // A kollekció neve
                            .document(dokNev)  // Dokumentum neve (a nap dátuma)
                            .get()  // Lekérjük az aktuális dokumentumot
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // Ha létezik már dokumentum, akkor hozzáadjuk a tranzakciót a listához
                                    val existingTransactions = document.get("tranzakciok") as? List<HashMap<String, Any>> ?: mutableListOf()
                                    val mutableTransactions = existingTransactions.toMutableList() // Listát módosíthatóvá tesszük
                                    mutableTransactions.add(transaction)  // Hozzáadjuk az új tranzakciót

                                    // Frissítjük a dokumentumot a tranzakciók listájával
                                    firestore.collection("nap")
                                        .document(dokNev)
                                        .update("tranzakciok", mutableTransactions)
                                        .addOnSuccessListener {
                                            Toast.makeText(activity, "Sikeresen mentve: $dokNev", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(activity, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    // Ha nem létezik dokumentum, akkor új dokumentumot hozunk létre a tranzakcióval
                                    val transactions = mutableListOf(transaction)

                                    firestore.collection("nap")
                                        .document(dokNev)
                                        .set(mapOf("tranzakciok" to transactions))
                                        .addOnSuccessListener {
                                            Toast.makeText(activity, "Sikeresen mentve: $dokNev", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(activity, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(activity, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Ha nincs megadva érvényes összeg vagy leírás
                        Toast.makeText(context, "Érvényes összeget és leírást adj meg!", Toast.LENGTH_SHORT).show()
                    }
                }

                return view
            }
        }





        val hozzadasGomb = findViewById<Button>(R.id.hozzaadasGomb)

        hozzadasGomb.setOnClickListener {
            val dialog = HozzadasDialogFragment()
            dialog.show(supportFragmentManager, "HozzadasDialog")
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

            vonalDiagram = gyokerNezet.findViewById(R.id.vonalDiagram)

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
        private lateinit var vonalDiagram: LineChart
        private val firestore = FirebaseFirestore.getInstance()

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_nap, container, false)

            vonalDiagram = gyokerNezet.findViewById(R.id.vonalDiagram)

            // Napi adatok lekérdezése az aktuális dátum alapján
            val maiDatum = LocalDate.now().toString() // Pl. "2025-01-10"
            firestore.collection("nap").document(maiDatum).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Lekérdezett adatok feldolgozása
                        val tranzakciok = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        val bevetelAdatok = mutableListOf<Entry>()
                        val kiadasAdatok = mutableListOf<Entry>()

                        tranzakciok.forEachIndexed { index, tranzakcio ->
                            val mennyiseg = (tranzakcio["mennyiseg"] as Double).toFloat()
                            val kategoria = tranzakcio["kategoria"] as String
                            if (kategoria == "Bevétel") {
                                bevetelAdatok.add(Entry(index.toFloat(), mennyiseg))
                            } else if (kategoria == "Kiadás") {
                                kiadasAdatok.add(Entry(index.toFloat(), mennyiseg))
                            }
                        }

                        // Diagram beállítása az adatok alapján
                        setupChart(bevetelAdatok, kiadasAdatok)
                    } else {
                        Toast.makeText(context, "Nincs adat a mai napra.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Hiba az adatok lekérdezése során: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            return gyokerNezet
        }

        private fun setupChart(bevetelAdatok: List<Entry>, kiadasAdatok: List<Entry>) {
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
        }
    }


    class HetFragment : Fragment() {
        private lateinit var vonalDiagram: LineChart
        private val firestore = FirebaseFirestore.getInstance()

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_het, container, false)

            vonalDiagram = gyokerNezet.findViewById(R.id.vonalDiagram)

            // Heti adatok lekérdezése
            val kalendar = Calendar.getInstance()
            val datumformatum = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Hét első napja (hétfő)
            kalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val hetKezdete = kalendar.time

            // Hét utolsó napja (vasárnap)
            kalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            val hetVege = kalendar.time

            val bevetelAdatok = mutableListOf<Entry>()
            val kiadasAdatok = mutableListOf<Entry>()

            // Iterálunk az összes napra a hét folyamán
            val napokSzama = 7
            for (i in 0 until napokSzama) {
                val nap = kalendar.time
                val napStr = datumformatum.format(nap)  // A napi dátum stringje

                // Lekérdezzük a napi tranzakciókat a Firestore-ból
                firestore.collection("nap")
                    .document(napStr) // A dokumentum neve a napi dátum
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val tranzakciok = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()

                            tranzakciok.forEachIndexed { index, tranzakcio ->
                                val mennyiseg = (tranzakcio["mennyiseg"] as? Double)?.toFloat() ?: 0f
                                val kategoria = tranzakcio["kategoria"] as? String ?: ""

                                if (kategoria == "Bevétel") {
                                    bevetelAdatok.add(Entry(index.toFloat(), mennyiseg))
                                } else if (kategoria == "Kiadás") {
                                    kiadasAdatok.add(Entry(index.toFloat(), mennyiseg))
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Hiba történt a tranzakciók lekérésekor: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                // Növeljük a napot a következő napra
                kalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Várakozás az összes adat lekérdezésére (asynchronous)
            Handler(Looper.getMainLooper()).postDelayed({
                // Diagram beállítása az összegyűjtött adatokkal
                chartBeallitas(bevetelAdatok, kiadasAdatok)
            }, 2000) // 2 másodperc várakozás

            return gyokerNezet
        }

        private fun chartBeallitas(bevetelAdatok: List<Entry>, kiadasAdatok: List<Entry>) {
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
        }
    }



    class HonapFragment : Fragment() {
        private lateinit var vonalDiagram: LineChart
        private val firestore = FirebaseFirestore.getInstance()

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_honap, container, false)

            vonalDiagram = gyokerNezet.findViewById(R.id.vonalDiagram)

            // Havi adatok lekérdezése
            val kalendar = Calendar.getInstance()
            val datumformatum = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // A hónap első napja
            kalendar.set(Calendar.DAY_OF_MONTH, 1)
            val honapKezdete = kalendar.time

            // A hónap utolsó napja
            kalendar.set(Calendar.DAY_OF_MONTH, kalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val honapVege = kalendar.time

            val bevetelAdatok = mutableListOf<Entry>()
            val kiadasAdatok = mutableListOf<Entry>()

            // Iterálunk az összes napra a hónap folyamán
            val napokSzama = kalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in 0 until napokSzama) {
                val nap = kalendar.time
                val napStr = datumformatum.format(nap)  // A napi dátum stringje

                // Lekérdezzük a napi tranzakciókat a Firestore-ból
                firestore.collection("nap")
                    .document(napStr) // A dokumentum neve a napi dátum
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val tranzakciok = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()

                            tranzakciok.forEachIndexed { index, tranzakcio ->
                                val mennyiseg = (tranzakcio["mennyiseg"] as? Double)?.toFloat() ?: 0f
                                val kategoria = tranzakcio["kategoria"] as? String ?: ""

                                if (kategoria == "Bevétel") {
                                    bevetelAdatok.add(Entry(index.toFloat(), mennyiseg))
                                } else if (kategoria == "Kiadás") {
                                    kiadasAdatok.add(Entry(index.toFloat(), mennyiseg))
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Hiba történt a tranzakciók lekérésekor: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                // Növeljük a napot a következő napra
                kalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Várakozás az összes adat lekérdezésére (asynchronous)
            Handler(Looper.getMainLooper()).postDelayed({
                // Diagram beállítása az összegyűjtött adatokkal
                chartBeallitas(bevetelAdatok, kiadasAdatok)
            }, 2000) // 2 másodperc várakozás

            return gyokerNezet
        }

        private fun chartBeallitas(bevetelAdatok: List<Entry>, kiadasAdatok: List<Entry>) {
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
        }
    }






    class EvFragment : Fragment() {
        private lateinit var vonalDiagram: LineChart
        private val firestore = FirebaseFirestore.getInstance()

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val gyokerNezet = inflater.inflate(R.layout.fragment_ev, container, false)

            vonalDiagram = gyokerNezet.findViewById(R.id.vonalDiagram)

            // Éves adatok lekérdezése
            val kalendar = Calendar.getInstance()
            val datumformatum = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Az év első napja
            kalendar.set(Calendar.MONTH, Calendar.JANUARY)
            kalendar.set(Calendar.DAY_OF_MONTH, 1)
            val evKezdete = kalendar.time

            // Az év utolsó napja
            kalendar.set(Calendar.MONTH, Calendar.DECEMBER)
            kalendar.set(Calendar.DAY_OF_MONTH, 31)
            val evVege = kalendar.time

            val bevetelAdatok = mutableListOf<Entry>()
            val kiadasAdatok = mutableListOf<Entry>()

            // Iterálunk az összes hónapra az év folyamán
            for (i in 0 until 12) {
                kalendar.set(Calendar.MONTH, i)
                val honapKezdete = kalendar.time
                val honapStr = datumformatum.format(honapKezdete)  // A havi dátum stringje

                // Lekérdezzük a havi tranzakciókat a Firestore-ból
                firestore.collection("honap")
                    .document(honapStr) // A dokumentum neve a havi dátum
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val tranzakciok = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()

                            tranzakciok.forEachIndexed { index, tranzakcio ->
                                val mennyiseg = (tranzakcio["mennyiseg"] as? Double)?.toFloat() ?: 0f
                                val kategoria = tranzakcio["kategoria"] as? String ?: ""

                                if (kategoria == "Bevétel") {
                                    bevetelAdatok.add(Entry(index.toFloat(), mennyiseg))
                                } else if (kategoria == "Kiadás") {
                                    kiadasAdatok.add(Entry(index.toFloat(), mennyiseg))
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Hiba történt a tranzakciók lekérésekor: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            // Várakozás az összes adat lekérdezésére (asynchronous)
            Handler(Looper.getMainLooper()).postDelayed({
                // Diagram beállítása az összegyűjtött adatokkal
                chartBeallitas(bevetelAdatok, kiadasAdatok)
            }, 2000) // 2 másodperc várakozás

            return gyokerNezet
        }

        private fun chartBeallitas(bevetelAdatok: List<Entry>, kiadasAdatok: List<Entry>) {
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
