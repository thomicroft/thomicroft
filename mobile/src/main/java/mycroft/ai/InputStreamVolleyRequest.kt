package mycroft.ai

import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser

class InputStreamVolleyRequest : Request<ByteArray> {
    private var context : Context
    private var mListener : Response.Listener<ByteArray>
    private var mParams : Map<String, String>
    private var responseHeaders: Map<String, String>? = null


    constructor(context : Context, method : Int , mUrl : String, listener : Response.Listener<ByteArray>, errorListener : Response.ErrorListener, params : HashMap<String, String> ) :
            super(method, mUrl, errorListener) {
        this.context = context
        setShouldCache(false)
        this.mListener = listener
        this.mParams = params
    }



    @Throws(AuthFailureError::class)
    override fun getParams(): Map<String, String>? {
        return mParams
    }

    @Throws(AuthFailureError::class)
    override fun getHeaders(): Map<String, String>? {
        val params: HashMap<String, String> = HashMap()
        params["Content-Type"] = "application/json"
        return params
    }

    protected override fun deliverResponse(response: ByteArray) {
        mListener.onResponse(response)
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray>? {
        //Initialise local responseHeaders map with response headers received
        responseHeaders = response.headers

        //Pass the response data here
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response))
    }
}