package com.tugaz.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.Date

class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val taskId = intent.getLongExtra("id", 0L)
        val title = intent.getStringExtra("title").orEmpty()
        val deadline = intent.getLongExtra("deadline", 0L)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(Channel, "Pengingat deadline", NotificationManager.IMPORTANCE_HIGH))
        val openApp = PendingIntent.getActivity(context, taskId.toInt(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(deadline))
        val notification = NotificationCompat.Builder(context, Channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Deadline tugas mendekat")
            .setContentText("$title - $date")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Tugas \"$title\" deadline pada $date."))
            .setContentIntent(openApp).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }
    companion object { const val Channel = "deadline_reminders" }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) TaskRepository.load(context).forEach { DeadlineScheduler.schedule(context, it) }
    }
}

object DeadlineScheduler {
    private const val Day = 24L * 60L * 60L * 1000L
    fun schedule(context: Context, task: Task) {
        if (task.deadlineMillis <= System.currentTimeMillis()) return
        val trigger = maxOf(System.currentTimeMillis() + 5_000L, task.deadlineMillis - Day)
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, task)
        if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) manager.set(AlarmManager.RTC_WAKEUP, trigger, pending)
        else manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }
    fun cancel(context: Context, id: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context, Task(id, "", 0L)))
    }
    private fun pendingIntent(context: Context, task: Task): PendingIntent {
        val intent = Intent(context, DeadlineReceiver::class.java).setAction("com.tugaz.app.DEADLINE")
            .putExtra("id", task.id).putExtra("title", task.title).putExtra("deadline", task.deadlineMillis)
        return PendingIntent.getBroadcast(context, task.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
