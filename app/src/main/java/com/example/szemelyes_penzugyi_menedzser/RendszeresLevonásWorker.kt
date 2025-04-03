package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RendszeresLevonásWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val adatbazis = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        val felhasznalo = auth.currentUser
        if (felhasznalo == null) {
            return Result.success()  // Ha nincs felhasználó, sikeresen befejezi a munkát
        }

        val felhasznaloId = felhasznalo.uid
        val aktualisIdoMillis = System.currentTimeMillis()

        try {
            // Lekérdezzük a rendszeres kifizetéseket
            val kifizetesek = adatbazis.collection("users")
                .document(felhasznaloId)
                .collection("kifizetesek")
                .get()
                .await()

            for (dokumentum in kifizetesek) {
                val utolsoLevonasMillis = dokumentum.getTimestamp("utolsoLevonas")?.toDate()?.time ?: 0L
                val period = dokumentum.getString("period") ?: continue
                val kifizetesOsszeg = dokumentum.getDouble("osszeg") ?: continue

                // Intervallumok millimásodpercben
                val intervallumMillis = when (period) {
                    "Naponta" -> 24 * 60 * 60 * 1000L
                    "Hetente" -> 7 * 24 * 60 * 60 * 1000L
                    "Havonta" -> 30 * 24 * 60 * 60 * 1000L
                    "Évente" -> 365 * 24 * 60 * 60 * 1000L
                    else -> 0L
                }

                // Ellenőrizzük, hogy elég idő telt-e el az utolsó levonás óta
                if (intervallumMillis > 0 && aktualisIdoMillis - utolsoLevonasMillis >= intervallumMillis) {
                    val felhasznaloDok = adatbazis.collection("users").document(felhasznaloId)

                    // Tranzakciót hajtunk végre az egyenleg frissítésére
                    adatbazis.runTransaction { tranzakcio ->
                        val snapshot = tranzakcio.get(felhasznaloDok)
                        val aktualisEgyenleg = snapshot.getDouble("aktualisPenz") ?: 0.0

                        // Ellenőrizzük, hogy van elegendő egyenleg
                        if (aktualisEgyenleg >= kifizetesOsszeg) {
                            tranzakcio.update(felhasznaloDok, "aktualisPenz", aktualisEgyenleg - kifizetesOsszeg)
                        }
                    }.addOnSuccessListener {
                        // Frissítjük a 'utolsoLevonas' mezőt
                        dokumentum.reference.update("utolsoLevonas", Timestamp.now())
                    }
                }
            }

            return Result.success()

        } catch (e: Exception) {
            // Ha bármi hiba történik, újrapróbálkozik
            return Result.retry()
        }
    }
}
