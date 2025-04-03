package com.example.szemelyes_penzugyi_menedzser

import Kifizetesitem
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.work.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class RendszeresKifizetesek : BaseActivity() {

    private lateinit var adatbazis: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var osszeg: Double = 0.0
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: KifizetesAdapter
    private val kifizetesLista = mutableListOf<Kifizetesitem>()
    private lateinit var navigaciosSpinner: Spinner
    private lateinit var idoszakSpinner: Spinner

    private var kifizetesekRegistration: ListenerRegistration? = null

    private fun formatOsszeg(osszeg: Double): String {
        val szamjel = DecimalFormatSymbols(Locale("hu", "HU"))
        szamjel.groupingSeparator = ' '
        val formatter = DecimalFormat("#,###", szamjel)
        return formatter.format(osszeg)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rendszeres_kifizetesek)

        adatbazis = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val felhasznalo = auth.currentUser ?: run {
            finish()
            return
        }
        val felhasznaloId = felhasznalo.uid

        val osszegTextView: TextView = findViewById(R.id.osszegTextView)
        val nevEditText: EditText = findViewById(R.id.nevEditText)
        val osszegEditText: EditText = findViewById(R.id.osszegEditText)
        val hozzaadGomb: Button = findViewById(R.id.hozzaadButton)
        val torlesGomb: Button = findViewById(R.id.torlesButton)


        navigaciosSpinner = findViewById(R.id.lenyilo_menu)
        val lehetosegek = listOf("Főoldal", "Elemzés", "Kategóriák", "Rendszeres kifizetések", "Beállítások", "Kijelentkezés")
        val navigaciosAdapter = CustomFontSizeSpinnerAdapter(this, R.layout.spinner_item, lehetosegek)
        navigaciosAdapter.setDropDownViewResource(R.layout.spinner_item)
        navigaciosSpinner.adapter = navigaciosAdapter
        navigaciosSpinner.setSelection(3)
        var elsoFutas = true
        navigaciosSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (elsoFutas) {
                    elsoFutas = false
                    return
                }
                when (parent.getItemAtPosition(position).toString()) {
                    "Főoldal" -> startActivity(Intent(this@RendszeresKifizetesek, Telefonszam::class.java))
                    "Elemzés" -> startActivity(Intent(this@RendszeresKifizetesek, ElemzesActivity::class.java))
                    "Kategóriák" -> startActivity(Intent(this@RendszeresKifizetesek, Kategoriak::class.java))
                    "Beállítások" -> startActivity(Intent(this@RendszeresKifizetesek, BeallitasokActivity::class.java))
                    "Kijelentkezés" -> kijelentkezes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        idoszakSpinner = findViewById(R.id.periodSpinner)
        val idoszakOpciók = listOf("Naponta", "Hetente", "Havonta", "Évente")
        val idoszakAdapter = CustomFontSizeSpinnerAdapter(this, R.layout.spinner_item, idoszakOpciók)
        idoszakAdapter.setDropDownViewResource(R.layout.spinner_item)
        idoszakSpinner.adapter = idoszakAdapter
        idoszakSpinner.setSelection(2)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = KifizetesAdapter(kifizetesLista)
        recyclerView.adapter = adapter

        egyenlegBetoltes(felhasznaloId, osszegTextView)
        hallgatoKifizetesek(felhasznaloId)

        hozzaadGomb.setOnClickListener {
            val nev = nevEditText.text.toString().trim()
            val osszegInput = osszegEditText.text.toString().trim().toDoubleOrNull()
            val idoszak = idoszakSpinner.selectedItem.toString()
            if (nev.isNotEmpty() && osszegInput != null) {
                val ujKifizetes = hashMapOf(
                    "nev" to nev,
                    "osszeg" to osszegInput,
                    "period" to idoszak,
                    "utolsoLevonas" to com.google.firebase.Timestamp.now()
                )
                adatbazis.collection("users").document(felhasznaloId)
                    .collection("kifizetesek")
                    .add(ujKifizetes)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Kifizetés hozzáadva!", Toast.LENGTH_SHORT).show()
                        nevEditText.text.clear()
                        osszegEditText.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("RendszeresKifizetesek", "Hiba a kifizetés hozzáadásakor", e)
                    }
            } else {
                Toast.makeText(this, "Kérlek töltsd ki a nevet és az összeget helyesen!", Toast.LENGTH_SHORT).show()
            }
        }

        torlesGomb.setOnClickListener {
            val kijeloltElemek = adapter.getKijeloltElemek()
            for (kifizetes in kijeloltElemek) {
                torolKifizetes(felhasznaloId, kifizetes.docId)
            }
        }



        val levonasMunkafutas = PeriodicWorkRequestBuilder<RendszeresLevonásWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RendszeresLevonas",
            ExistingPeriodicWorkPolicy.KEEP,
            levonasMunkafutas
        )
    }

    private fun egyenlegBetoltes(felhasznaloId: String, osszegTextView: TextView) {
        adatbazis.collection("users").document(felhasznaloId)
            .get()
            .addOnSuccessListener {
                val aktualis = it.getDouble("aktualisPenz") ?: 0.0
                osszeg = aktualis
                osszegTextView.text = "Fő összeg: ${formatOsszeg(aktualis)} Ft"
            }
            .addOnFailureListener {
                osszegTextView.text = "Hiba az egyenleg betöltésekor!"
            }
    }

    private fun hallgatoKifizetesek(felhasznaloId: String) {
        kifizetesekRegistration?.remove()
        kifizetesekRegistration = adatbazis.collection("users").document(felhasznaloId)
            .collection("kifizetesek")
            .addSnapshotListener { eredmeny, e ->
                if (e != null) {
                    Log.e("RendszeresKifizetesek", "Hiba a kifizetések lekérésekor", e)
                    return@addSnapshotListener
                }
                if (eredmeny != null) {
                    kifizetesLista.clear()
                    for (dokumentum in eredmeny) {
                        val docId = dokumentum.id
                        val nev = dokumentum.getString("nev") ?: "N/A"
                        val osszeg = dokumentum.getDouble("osszeg") ?: 0.0
                        val period = dokumentum.getString("period") ?: "N/A"
                        kifizetesLista.add(Kifizetesitem(docId, nev, osszeg, period))
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun torolKifizetes(felhasznaloId: String, docId: String) {
        adatbazis.collection("users").document(felhasznaloId)
            .collection("kifizetesek").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Kifizetés törölve!", Toast.LENGTH_SHORT).show()
                egyenlegBetoltes(felhasznaloId, findViewById(R.id.osszegTextView))
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Törlés sikertelen: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("RendszeresKifizetesek", "Hiba a kifizetés törlésekor", e)
            }
    }

    private fun kijelentkezes() {
        auth.signOut()
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isLoggedIn", false)
            .apply()
        Toast.makeText(this, "Kijelentkezve!", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, Telefonszam::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
