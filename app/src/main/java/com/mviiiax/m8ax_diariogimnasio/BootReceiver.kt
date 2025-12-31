package com.mviiiax.m8ax_diariogimnasio

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val hora = 16
            val minuto = 0
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (alarmManager.canScheduleExactAlarms()) MainActivity.programarAlarmaDiaria(
                    context, hora, minuto
                )
            } else MainActivity.programarAlarmaDiaria(context, hora, minuto)
        }
    }
}