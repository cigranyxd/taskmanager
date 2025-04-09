package com.example.szemelyes_penzugyi_menedzser

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.util.*

object RendszeresLevonasHelper {

    fun scheduleNext(context: Context, docId: String, period: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, KifizetesAlarmReceiver::class.java).apply {
            putExtra("docId", docId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            docId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ⚙️ 1 perc múlva (teszteléshez)
        val nextTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
        }

        // 🔐 Ellenőrizd az exact alarm engedélyt Android 12+ rendszeren
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    context,
                    "A pontos időzítéshez engedélyezned kell az 'exact alarm'-ot a beállításokban!",
                    Toast.LENGTH_LONG
                ).show()

                val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intentSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intentSettings)

                return
            }
        }

        // ✅ Időzítés beállítása
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTime.timeInMillis,
            pendingIntent
        )

        Log.d("RendszeresLevonasHelper", "Levonás időzítve 1 perccel későbbre: $docId - ${nextTime.time}")
    }
}
