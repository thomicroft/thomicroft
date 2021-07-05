package mycroft.ai.services

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import mycroft.ai.shared.utilities.GuiUtilities.showToast


class PorcupineService : Service() {

    private lateinit var porcupineManager : PorcupineManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            porcupineManager = PorcupineManager.Builder()
                .setKeyword(Porcupine.BuiltInKeyword.COMPUTER)
                .setSensitivity(0.7f)
                .build(
                    applicationContext
                ) { keywordIndex: Int ->
                    //TODO callback for recognition
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
}