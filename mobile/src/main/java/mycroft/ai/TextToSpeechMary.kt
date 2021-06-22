package mycroft.ai

import android.content.Context
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
import java.util.*
import java.util.Base64

class TextToSpeechMary {
    private var serverIp : String
    private var context : Context
    private var queue : RequestQueue
    private var url : String
    private var port : String

    constructor(context : Context, serverIp : String, port : String = "59125") {
        this.serverIp = serverIp
        this.context = context
        this.port = port
        queue = Volley.newRequestQueue(context)
        url = "http://$serverIp:$port"
    }

    fun sendTTSRequest(input_text : String) {
        var requestUrl = "$url/process?INPUT_TEXT=$input_text&INPUT_TYPE=TEXT&OUTPUT_TYPE=AUDIO&AUDIO=WAVE_FILE&LOCALE=de&VOICE=bits3-hsmm"
        // var requestUrl = "192.168.0.31:59125/version"
        val stringRequest = StringRequest(
            Request.Method.GET, requestUrl,
            Response.Listener<String> { response ->
                val text = response;
                showToast(context, "tts ist daaa")
            },
            Response.ErrorListener { val errorText = "That didn't work!" })
        queue.add(stringRequest)
    }

}