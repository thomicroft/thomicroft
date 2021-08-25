package mycroft.ai.services

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import mycroft.ai.MainActivity


class PorcupineService : Service() {

    private val channelId = "ForegroundService Kotlin"
    private lateinit var porcupineManager : PorcupineManager

    companion object {
        fun startService(context : Context, message : String) : Intent {
            val startIntent = Intent(context, PorcupineService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
            return startIntent
        }

        fun stopService(context: Context) : Intent {
            val stopIntent = Intent(context, PorcupineService::class.java)
            context.stopService(stopIntent)
            return stopIntent
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Thomicroft Foreground Service")
            .setContentText("input")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        try {
            porcupineManager = PorcupineManager.Builder()
                .setKeyword(Porcupine.BuiltInKeyword.COMPUTER)
                .setSensitivity(0.7f)
                .build(
                    applicationContext
                ) { keywordIndex: Int ->
                    //TODO callback for recognition
                    //porcupineServiceCallbacks?.showExampleToast()
                    var local = Intent("thomicroft.recognizeMicrophone")
                    this.sendBroadcast(local)
                }
            porcupineManager.start()
        } catch (e: PorcupineException) {
            Log.e("PORCUPINE_SERVICE", e.toString())
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        try {
            porcupineManager.stop()
            porcupineManager.delete()
        } catch (e: PorcupineException) {
            Log.e("PORCUPINE", e.toString())
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(channelId, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }
}