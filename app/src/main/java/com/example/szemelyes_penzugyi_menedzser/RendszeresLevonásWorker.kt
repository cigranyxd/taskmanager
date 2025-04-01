package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RendszeresLevonásWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val adatbazis = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        val felhasznalo = FirebaseAuth.getInstance().currentUser
        if (felhasznalo == null) return Result.success()
        val felhasznaloId = felhasznalo.uid
        val aktualisIdoMillis = System.currentTimeMillis()

        // Lekérjük a felhasználó rendszeres kifizetéseit
        adatbazis.collection("users").document(felhasznaloId)
            .collection("kifizetesek")
            .get()
            .addOnSuccessListener { dokumentumok ->
                for (dokumentum in dokumentumok) {
                    val utolsoLevonasMillis = dokumentum.getTimestamp("utolsoLevonas")?.toDate()?.time ?: 0L
                    val period = dokumentum.getString("period") ?: "N/A"
                    val kifizetesOsszeg = dokumentum.getDouble("osszeg") ?: 0.0

                    val intervallumMillis = when (period) {
                        "Naponta" -> 24 * 60 * 60 * 1000L
                        "Hetente" -> 7 * 24 * 60 * 60 * 1000L
                        "Havonta" -> 30 * 24 * 60 * 60 * 1000L
                        "Évente" -> 365 * 24 * 60 * 60 * 1000L
                        else -> 0L
                    }

                    if (intervallumMillis > 0 && aktualisIdoMillis - utolsoLevonasMillis >= intervallumMillis) {
                        val felhasznaloDok = adatbazis.collection("users").document(felhasznaloId)
                        adatbazis.runTransaction { tranzakcio ->
                            val snapshot = tranzakcio.get(felhasznaloDok)
                            val aktualisEgyenleg = snapshot.getDouble("aktualisPenz") ?: 0.0
                            tranzakcio.update(felhasznaloDok, "aktualisPenz", aktualisEgyenleg - kifizetesOsszeg)
                        }.addOnSuccessListener {
                            dokumentum.reference.update("utolsoLevonas", Timestamp.now())
                        }
                    }
                }
            }
        return Result.success()
    }
}
