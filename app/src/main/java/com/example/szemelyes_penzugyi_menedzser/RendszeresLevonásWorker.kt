package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class RendszeresLevonásWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val adatbazis = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        val felhasznalo = auth.currentUser
        if (felhasznalo == null) {
            Log.d("RendszeresLevonásWorker", "Nincs bejelentkezett felhasználó.")
            return Result.success()
        }

        val felhasznaloId = felhasznalo.uid
        val docId = inputData.getString("docId")
        val aktualisIdoMillis = System.currentTimeMillis()

        try {
            val kifizetesek = if (docId != null) {
                listOf(
                    adatbazis.collection("users")
                        .document(felhasznaloId)
                        .collection("kifizetesek")
                        .document(docId)
                        .get()
                        .await()
                )
            } else {
                adatbazis.collection("users")
                    .document(felhasznaloId)
                    .collection("kifizetesek")
                    .get()
                    .await()
            }

            for (dokumentum in kifizetesek) {
                if (!dokumentum.exists()) continue

                val docAzonosito = dokumentum.id
                val utolsoLevonas = dokumentum.getTimestamp("utolsoLevonas")?.toDate() ?: Date(0)
                val utolsoLevonasMillis = utolsoLevonas.time
                val period = dokumentum.getString("period") ?: continue
                val osszeg = dokumentum.getDouble("osszeg") ?: continue

                val intervallumMillis = when (period) {
                    "Naponta" -> 24 * 60 * 60 * 1000L
                    "Hetente" -> 7 * 24 * 60 * 60 * 1000L
                    "Havonta" -> 30L * 24 * 60 * 60 * 1000
                    "Évente" -> 365L * 24 * 60 * 60 * 1000
                    else -> 0L
                }

                val eltelt = aktualisIdoMillis - utolsoLevonasMillis

                Log.d("RendszeresLevonásWorker", "Dokumentum: $docAzonosito, Period: $period, Eltelt: $eltelt ms")

                if (intervallumMillis > 0 && eltelt >= intervallumMillis) {
                    val userRef = adatbazis.collection("users").document(felhasznaloId)

                    val sikeres = adatbazis.runTransaction { tranzakcio ->
                        val userSnapshot = tranzakcio.get(userRef)
                        val aktualisEgyenleg = userSnapshot.getDouble("aktualisPenz") ?: 0.0

                        // Levonás mínuszba is engedve
                        tranzakcio.update(userRef, "aktualisPenz", aktualisEgyenleg - osszeg)
                        true
                    }.await()

                    if (sikeres) {
                        dokumentum.reference.update("utolsoLevonas", Timestamp.now()).await()

                        // Következő levonás időzítése
                        RendszeresLevonasHelper.scheduleNext(applicationContext, docAzonosito, period)

                        Log.d("RendszeresLevonásWorker", "Sikeres levonás (mínuszba is lehet menni): $docAzonosito")
                    }
                } else {
                    Log.d("RendszeresLevonásWorker", "Nem telt el elég idő. Skip: $docAzonosito")
                }
            }

            return Result.success()

        } catch (e: Exception) {
            Log.e("RendszeresLevonásWorker", "Hiba történt a levonás során", e)
            return Result.retry()
        }
    }
}
