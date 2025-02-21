package com.example.szemelyes_penzugyi_menedzser

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    // Firebase Auth és Firestore példányok lazy inicializálása
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Aktuális felhasználó azonosító lekérése
    fun getCurrentUserUID(): String? = auth.currentUser?.uid

    // Egyenleg lekérése a Firestore-ból
    fun loadBalance(
        uid: String,
        onComplete: (Double) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                // Ha a dokumentum létezik, kiolvassuk az aktuális egyenleget, különben 0.0-t adunk vissza
                val balance = document.getDouble("aktualisPenz") ?: 0.0
                onComplete(balance)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    // Egyenleg mentése a Firestore-ba
    fun saveBalance(
        uid: String,
        balance: Double,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val balanceData = hashMapOf("aktualisPenz" to balance)
        firestore.collection("users").document(uid)
            .set(balanceData)
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { exception -> onError(exception) }
    }

    // Felhasználó kijelentkeztetése
    fun signOut() {
        auth.signOut()
    }
}
