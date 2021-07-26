package mycroft.ai

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import mycroft.ai.shared.utilities.GuiUtilities.showToast
import org.json.JSONObject
import java.lang.Exception

class NumberParser {
    private var context : Context
    private var serverIp : String
    private var port : String
    private var queue : RequestQueue
    private var url : String

    constructor(context : Context, serverIp : String, port : String) {
        this.context = context
        this.serverIp = serverIp
        this.port = port
        queue = Volley.newRequestQueue(context)
        url = "http://$serverIp:$port"
    }

    fun parseNumber(message : String){
        val mUrl = "$url/?message=$message"

        val stringRequest = StringRequest(Request.Method.GET, mUrl,
            { response ->
                showToast(context, response)

            },
            {
                showToast(context, "That didn't work!")
            })
        queue.add(stringRequest)
    }
}