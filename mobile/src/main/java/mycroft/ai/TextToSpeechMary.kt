package mycroft.ai

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.volley.DefaultRetryPolicy
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
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.Base64

class TextToSpeechMary {
    private var serverIp : String
    private var context : Context
    private var queue : RequestQueue
    private var url : String
    private var port : String
    private var filePath : File
    private var mediaPlayer : MediaPlayer = MediaPlayer()
    private var files : Queue<File> = LinkedList<File>()
    private var currentOutput = 0

    constructor(context : Context, serverIp : String, port : String = "59125") {
        this.serverIp = serverIp
        this.context = context
        this.port = port
        queue = Volley.newRequestQueue(context)
        url = "http://$serverIp:$port"
        filePath = context.filesDir
    }

    fun sendTTSRequest(input_text : String) {
        // MaryTTS
        // var mUrl = "$url/process?INPUT_TEXT=$input_text&INPUT_TYPE=TEXT&OUTPUT_TYPE=AUDIO&AUDIO=WAVE_FILE&LOCALE=de&VOICE=bits3-hsmm"
        // Larynx with Eva-Voice
        // var mUrl = "$url/api/tts?text=$input_text&voice=de-de/eva_k-glow_tts&vocoder=hifi_gan/vctk_medium&denoiserStrength=0.005&noiseScale=0.333&lengthScale=1"
        // Larynx with Karlsson-Voice
        var mUrl = "$url/api/tts?text=$input_text&voice=de-de/karlsson-glow_tts&vocoder=hifi_gan/vctk_medium&denoiserStrength=0.005&noiseScale=0.333&lengthScale=1"
        var hashMap: HashMap<String, String> = HashMap()
        var request = InputStreamVolleyRequest(
            context, Request.Method.GET, mUrl,
            Response.Listener<ByteArray>() { response ->
                writeWavFile(response)
            },
            Response.ErrorListener { error ->
                showToast(context, error.toString()) },
            hashMap)
        request.retryPolicy = DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(request);
    }

    private fun writeWavFile(data : ByteArray) {
        val file = File(filePath, "output$currentOutput.wav")
        currentOutput = (currentOutput + 1).rem(10)
        FileOutputStream(file).use {
            it.write(data)
        }
        files.add(file)
        //playWav(file)
        play()
    }

    private fun playWav(file: File) {
        //val mediaPlayer = MediaPlayer.create(context, Uri.fromFile(file))

        if (!mediaPlayer.isPlaying) {
            mediaPlayer.setDataSource(file.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } else {
            showToast(context, "Waiting for MediaPlayer to finish")
        }
        mediaPlayer.reset()
        mediaPlayer.setDataSource(file.path)
        mediaPlayer.prepare()
        mediaPlayer.start()
    }

    private fun play() {
        if(!mediaPlayer.isPlaying && !files.isEmpty()) {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(files.remove().path)
            mediaPlayer.prepare()
            mediaPlayer.start()
        }

        // when WAV is received while audio is still playing
        if (mediaPlayer.isPlaying && !files.isEmpty()) {
            mediaPlayer.setOnCompletionListener(MediaPlayer.OnCompletionListener { play() })
        }
    }

}