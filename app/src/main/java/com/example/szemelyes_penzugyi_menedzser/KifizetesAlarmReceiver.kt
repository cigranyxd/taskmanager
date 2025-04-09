package com.example.szemelyes_penzugyi_menedzser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.szemelyes_penzugyi_menedzser.RendszeresLevonásWorker

class KifizetesAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val docId = intent.getStringExtra("docId")

        if (docId == null) {
            Log.w("KifizetesAlarmReceiver", "docId hiányzik, nem indítjuk a levonást.")
            return
        }

        Log.d("KifizetesAlarmReceiver", "Levonás indítása dokumentumhoz: $docId")

        val inputData = Data.Builder()
            .putString("docId", docId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<RendszeresLevonásWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
