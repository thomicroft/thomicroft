package mycroft.ai

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import mycroft.ai.shared.utilities.GuiUtilities.showToast
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import java.util.Base64

class TextToSpeechMary {
    private var serverIp : String
    private var context : Context
    private var queue : RequestQueue
    private var url : String
    private var port : String
    private var path : File
    private var file : File

    constructor(context : Context, serverIp : String, port : String = "59125") {
        this.serverIp = serverIp
        this.context = context
        this.port = port
        queue = Volley.newRequestQueue(context)
        url = "http://$serverIp:$port"
        path = context.filesDir
        file = File(path, "output.wav")
    }

    fun sendTTSRequest(input_text : String) {
        var mUrl = "$url/process?INPUT_TEXT=$input_text&INPUT_TYPE=TEXT&OUTPUT_TYPE=AUDIO&AUDIO=WAVE_FILE&LOCALE=de&VOICE=bits3-hsmm"
        //var mUrl = "$url/api/tts?text=$input_text&voice=de-de%2Fthorsten-glow_tts&vocoder=hifi_gan%2Fvctk_medium&denoiserStrength=0.005&noiseScale=0.333&lengthScale=1"
        var hashMap: HashMap<String, String> = HashMap()
        var request = InputStreamVolleyRequest(
            context, Request.Method.GET, mUrl,
            Response.Listener<ByteArray>() { response ->
                writeWavFile(response)
            },
            Response.ErrorListener { showToast(context, "That didn't work!") },
            hashMap)
        queue.add(request);
    }

    private fun writeWavFile(data : ByteArray) {
        file = File(path, "output.wav")
        FileOutputStream(file).use {
            it.write(data)
        }
        playWav(file)
    }

    private fun playWav(file : File) {
        var mediaPlayer = MediaPlayer.create(context, Uri.fromFile(file))
        if (mediaPlayer.isPlaying) {

            mediaPlayer.setOnCompletionListener {
                try{
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(file.path)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                    file.delete()
                } catch (e : Exception) {

                }
            }
        } else {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(file.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            file.delete()
        }

    }

}