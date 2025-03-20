package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val TAG = "EgyediKategoriak"


// Singleton az egyedi kategóriák kezelésére
object EgyediKategoriak {
    val kategoriak: MutableList<EgyediKategoria> = mutableListOf()

    // Betölti a kategóriákat SharedPreferences-ből (fallback vagy első futáskor)
    fun betolt(context: Context) {
        val prefs = context.getSharedPreferences("egyedi_kategoriak", Context.MODE_PRIVATE)
        val json = prefs.getString("kategoriak", null)
        if (json != null) {
            try {
                val tipusToken = object : TypeToken<List<EgyediKategoria>>() {}.type
                val lista: List<EgyediKategoria> = Gson().fromJson(json, tipusToken)
                kategoriak.clear()
                kategoriak.addAll(lista)
                Log.d(TAG, "Custom kategóriák betöltve SharedPreferences-ből.")
            } catch (e: Exception) {
                Log.e(TAG, "Hiba SharedPreferences betöltésénél: ${e.message}")
            }
        } else {
            Log.d(TAG, "Nincs custom kategória adat SharedPreferences-ben.")
        }
    }

    // Elmenti a kategóriákat SharedPreferences-be
    fun ment(context: Context) {
        val prefs = context.getSharedPreferences("egyedi_kategoriak", Context.MODE_PRIVATE)
        val szerkeszto = prefs.edit()
        val json = Gson().toJson(kategoriak)
        szerkeszto.putString("kategoriak", json)
        szerkeszto.apply()
        Log.d(TAG, "Custom kategóriák elmentve SharedPreferences-be.")
    }

    // Betölti a kategóriákat Firestore-ból a felhasználóhoz rendelve
    fun betoltFirestore(context: Context, felhasznaloId: String, onBefejez: () -> Unit = {}) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(felhasznaloId)
            .collection("egyedi_kategoriak")
            .get()
            .addOnSuccessListener { querySnapshot ->
                kategoriak.clear()
                for (doc in querySnapshot.documents) {
                    val egyediKat = doc.toObject(EgyediKategoria::class.java)
                    if (egyediKat != null) {
                        kategoriak.add(egyediKat)
                    }
                }
                Log.d(TAG, "Custom kategóriák betöltve Firestore-ból.")
                onBefejez()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Hiba Firestore betöltésénél: ${e.message}")
                // Hibakezelés: fallbackként SharedPreferences-ből töltjük be
                betolt(context)
                onBefejez()
            }
    }

    // Elmenti a kategóriákat Firestore-ba a felhasználóhoz rendelve
    fun mentFirestore(context: Context, felhasznaloId: String, onBefejez: (() -> Unit)? = null) {
        val firestore = FirebaseFirestore.getInstance()
        val batch = firestore.batch()
        val egyediKatRef = firestore.collection("users").document(felhasznaloId)
            .collection("egyedi_kategoriak")
        for (egyediKat in kategoriak) {
            val docRef = egyediKatRef.document(egyediKat.nev)
            batch.set(docRef, egyediKat)
        }
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Custom kategóriák elmentve Firestore-ba.")
                onBefejez?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Hiba Firestore mentésénél: ${e.message}")
            }
    }

    // Törli a kategóriát Firestore-ból és frissíti a lokális listát
    fun torolKategoriat(context: Context, felhasznaloId: String, category: String, callback: (Boolean) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(felhasznaloId)
            .collection("egyedi_kategoriak")
            .document(category)
            .delete()
            .addOnSuccessListener {
                // Töröljük a kategóriát a lokális listából is
                kategoriak.removeAll { it.nev == category }
                // Frissítjük a SharedPreferences-ben is az adatot
                ment(context)
                Log.d(TAG, "Kategória '$category' törölve Firestore-ból.")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Hiba a kategória törlésekor: ${e.message}")
                callback(false)
            }
    }
}
