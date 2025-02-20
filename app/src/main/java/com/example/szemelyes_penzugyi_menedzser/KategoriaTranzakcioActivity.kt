package com.example.szemelyes_penzugyi_menedzser

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class KategoriaTranzakciokActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var transactionsAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategoria_tranzakciok)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        listView = findViewById(R.id.tranzakciokListView)

        // Lekérjük az intentből a kiválasztott kategória nevét és az időszakot
        val categoryName = intent.getStringExtra("categoryName") ?: ""
        // Ha nincs explicit period extra, alapértelmezett érték: "Nap"
        val period = intent.getStringExtra("period")?.takeIf { it.isNotEmpty() } ?: "Nap"

        loadTransactions(categoryName, period)
    }

    /**
     * Az adott időszak (Nap, Het, Honap, Ev) kezdő és záró dátumát adja vissza (yyyy-MM-dd formátumban).
     */
    private fun idoszakKezelo(period: String): Pair<String, String> {
        val today = LocalDate.now()
        return when (period) {
            "Nap" -> Pair(today.toString(), today.toString())
            "Het" -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                Pair(start.toString(), end.toString())
            }
            "Honap" -> {
                val start = today.withDayOfMonth(1)
                val end = today.withDayOfMonth(today.lengthOfMonth())
                Pair(start.toString(), end.toString())
            }
            "Ev" -> {
                val start = today.withDayOfYear(1)
                val end = today.withDayOfYear(today.lengthOfYear())
                Pair(start.toString(), end.toString())
            }
            else -> Pair("", "")
        }
    }

    /**
     * Lekéri az adott kategóriához tartozó tranzakciókat az adott időszakban.
     */
    private fun loadTransactions(category: String, period: String) {
        // Normalizáljuk a kategória nevet (kisbetűs, trim)
        val normalizedCategory = category.trim().toLowerCase()
        val userId = auth.currentUser?.uid ?: return

        val (startDate, endDate) = idoszakKezelo(period)

        if (period == "Nap") {
            // Nap esetén egyetlen dokumentumot kérünk le, melynek ID-ja a nap dátuma
            firestore.collection("users")
                .document(userId)
                .collection("nap")
                .document(startDate)
                .get()
                .addOnSuccessListener { document ->
                    val transactionsList = mutableListOf<String>()
                    if (document.exists()) {
                        val trxList = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        for (trx in trxList) {
                            val trxCategory = (trx["kategoria"] as? String)?.trim()?.toLowerCase() ?: ""
                            if (trxCategory == normalizedCategory) {
                                val amount = (trx["mennyiseg"] as? Number)?.toFloat() ?: 0f
                                val description = trx["leiras"] as? String ?: ""
                                transactionsList.add("Dátum: $startDate\nLeírás: $description\nÖsszeg: ${amount.toInt()} Ft")
                            }
                        }
                    }
                    if (transactionsList.isEmpty()) {
                        transactionsList.add("Nincs tranzakció a kiválasztott napra.")
                    }
                    transactionsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, transactionsList)
                    listView.adapter = transactionsAdapter
                }
                .addOnFailureListener {
                    // Hibakezelés, pl. Toast üzenet
                }
        } else {
            // Más időszak esetén a "nap" kollekcióból kérünk le dokumentumokat, amelyek ID-ja a dátum
            firestore.collection("users")
                .document(userId)
                .collection("nap")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), startDate)
                .whereLessThanOrEqualTo(FieldPath.documentId(), endDate)
                .get()
                .addOnSuccessListener { documents ->
                    val transactionsList = mutableListOf<String>()
                    for (document in documents) {
                        val date = document.id
                        val trxList = document.get("tranzakciok") as? List<Map<String, Any>> ?: emptyList()
                        for (trx in trxList) {
                            val trxCategory = (trx["kategoria"] as? String)?.trim()?.toLowerCase() ?: ""
                            if (trxCategory == normalizedCategory) {
                                val amount = (trx["mennyiseg"] as? Number)?.toFloat() ?: 0f
                                val description = trx["leiras"] as? String ?: ""
                                transactionsList.add("Dátum: $date\nLeírás: $description\nÖsszeg: ${amount.toInt()} Ft")
                            }
                        }
                    }
                    if (transactionsList.isEmpty()) {
                        transactionsList.add("Nincs tranzakció a kiválasztott időszakban.")
                    }
                    transactionsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, transactionsList)
                    listView.adapter = transactionsAdapter
                }
                .addOnFailureListener {
                    // Hibakezelés, pl. Toast üzenet
                }
        }
    }
}
