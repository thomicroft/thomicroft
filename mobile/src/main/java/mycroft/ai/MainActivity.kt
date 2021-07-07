/*
 *  Copyright (c) 2017. Mycroft AI, Inc.
 *
 *  This file is part of Mycroft-Android a client for Mycroft Core.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mycroft.ai

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import mycroft.ai.Constants.MycroftMobileConstants.VERSION_CODE_PREFERENCE_KEY
import mycroft.ai.Constants.MycroftMobileConstants.VERSION_NAME_PREFERENCE_KEY
import mycroft.ai.adapters.MycroftAdapter
import mycroft.ai.receivers.NetworkChangeReceiver
import mycroft.ai.services.PorcupineService
import mycroft.ai.shared.utilities.GuiUtilities
import mycroft.ai.shared.utilities.GuiUtilities.showToast
import mycroft.ai.shared.wear.Constants.MycroftSharedConstants.MYCROFT_WEAR_REQUEST
import mycroft.ai.shared.wear.Constants.MycroftSharedConstants.MYCROFT_WEAR_REQUEST_MESSAGE
import mycroft.ai.utils.NetworkUtil
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException


class MainActivity : AppCompatActivity(), RecognitionListener {
    // Mycroft Part
    private val logTag = "Mycroft"
    private val utterances = mutableListOf<Utterance>()
    private val reqCodeSpeechInput = 100
    private var maximumRetries = 1
    private var currentItemPosition = -1

    private var isNetworkChangeReceiverRegistered = false
    private var isWearBroadcastRevieverRegistered = false
    private var launchedFromWidget = false
    private var autoPromptForSpeech = false
    private var resultText = ""


    // private lateinit var ttsManager: TTSManager
    private lateinit var mycroftAdapter: MycroftAdapter
    private lateinit var wsip: String
    private lateinit var sharedPref: SharedPreferences
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var wearBroadcastReceiver: BroadcastReceiver
    private lateinit var marytts : TextToSpeechMary;

    var webSocketClient: WebSocketClient? = null

    private val STATE_START = 0
    private val STATE_READY = 1
    private val STATE_DONE = 2
    private val STATE_FILE = 3
    private val STATE_MIC = 4

    /* Used to handle permission request */
    private final val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

    private lateinit var model : Model
    private var speechService: SpeechService? = null
    private var speechStreamService : SpeechStreamService? = null
    private lateinit var resultView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mycroft
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar?)


        loadPreferences()


        marytts = TextToSpeechMary(this, wsip, "5002")

        //ttsManager = TTSManager(this)
        mycroftAdapter = MycroftAdapter(utterances, applicationContext, menuInflater)
        mycroftAdapter.setOnLongItemClickListener(object: MycroftAdapter.OnLongItemClickListener {
            override fun itemLongClicked(v: View, position: Int) {
                currentItemPosition = position
                v.showContextMenu()
            }
        })

        kbMicSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            editor.putBoolean("kbMicSwitch", isChecked)
            editor.apply()

            if (isChecked) {
                // Switch to mic
                micButton.visibility = View.VISIBLE
                utteranceInput.visibility = View.INVISIBLE
                sendUtterance.visibility = View.INVISIBLE
            } else {
                // Switch to keyboard
                micButton.visibility = View.INVISIBLE
                utteranceInput.visibility = View.VISIBLE
                sendUtterance.visibility = View.VISIBLE
            }
        }

        // Textinput
        utteranceInput.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                sendUtterance()
                true
            } else {
                false
            }
        })
        micButton.setOnClickListener { recognizeMicrophone() }
        sendUtterance.setOnClickListener { sendUtterance() }

        registerForContextMenu(cardList)

        //attach a listener to check for changes in state
        voxswitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            editor.putBoolean("appReaderSwitch", isChecked)
            editor.apply()

            // stop tts from speaking if app reader disabled
            //if (!isChecked) ttsManager.initQueue("")
        }

        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        llm.orientation = LinearLayoutManager.VERTICAL
        with (cardList) {
            setHasFixedSize(true)
            layoutManager = llm
            adapter = mycroftAdapter
        }

        registerReceivers()

        // start the discovery activity (testing only)
        // startActivity(new Intent(this, DiscoveryActivity.class));

        resultView = findViewById(R.id.result_text)
        setUiState(STATE_START)
        LibVosk.setLogLevel(LogLevel.INFO)

        var permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,  arrayOf(Manifest.permission.RECORD_AUDIO) , PERMISSIONS_REQUEST_RECORD_AUDIO)
        } else {
            startPorcupine()
            initModel()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_setup, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        var consumed = false
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                consumed = true
            }
            R.id.action_home_mycroft_ai -> {
                val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.mycroft_website_url)))
                startActivity(intent)
            }
        }

        return consumed && super.onOptionsItemSelected(item)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        super.onContextItemSelected(item)
        if (item.itemId == R.id.user_resend) {
            // Resend user utterance
            sendMessage(utterances[currentItemPosition].utterance)
        } else if (item.itemId == R.id.user_copy || item.itemId == R.id.mycroft_copy) {
            // Copy utterance to clipboard
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = ClipData.newPlainText("text", utterances[currentItemPosition].utterance)
            clipboardManager.setPrimaryClip(data)
            showToast("Copied to clipboard")
        } else if (item.itemId == R.id.mycroft_share) {
            // Share utterance
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, utterances[currentItemPosition].utterance)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.action_share)))
        } else {
            return super.onContextItemSelected(item)
        }

        return true
    }

    fun sendUtterance() {
        val utterance = utteranceInput.text.toString()
        if (utterance != "") {
            sendMessage(utterance)
            utteranceInput.text.clear()
        }
    }

    fun connectWebSocket() {
        val uri = deriveURI()

        if (uri != null) {
            webSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(serverHandshake: ServerHandshake) {
                    Log.i("Websocket", "Opened")
                }

                override fun onMessage(s: String) {
                    // Log.i(TAG, s);
                    runOnUiThread(MessageParser(s, object : SafeCallback<Utterance> {
                        override fun call(param: Utterance) {
                            addData(param)
                        }
                    }))
                }

                override fun onClose(i: Int, s: String, b: Boolean) {
                    Log.i("Websocket", "Closed $s")

                }

                override fun onError(e: Exception) {
                    Log.i("Websocket", "Error " + e.message)
                }
            }
            webSocketClient!!.connect()
        }
    }

    private fun addData(mycroftUtterance: Utterance) {
        utterances.add(mycroftUtterance)
        defaultMessageTextView.visibility = View.GONE
        mycroftAdapter.notifyItemInserted(utterances.size - 1)
        if (voxswitch.isChecked) {
            if (mycroftUtterance.from.toString() != "USER") {
                marytts.sendTTSRequest(mycroftUtterance.utterance)
            }
        }
        cardList.smoothScrollToPosition(mycroftAdapter.itemCount - 1)
    }

    private fun registerReceivers() {
        registerNetworkReceiver()
        registerWearBroadcastReceiver()
    }

    private fun registerNetworkReceiver() {
        if (!isNetworkChangeReceiverRegistered) {
            // set up the dynamic broadcast receiver for maintaining the socket
            networkChangeReceiver = NetworkChangeReceiver()
            networkChangeReceiver.setMainActivityHandler(this)

            // set up the intent filters
            val connChange = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
            val wifiChange = IntentFilter("android.net.wifi.WIFI_STATE_CHANGED")
            registerReceiver(networkChangeReceiver, connChange)
            registerReceiver(networkChangeReceiver, wifiChange)

            isNetworkChangeReceiverRegistered = true
        }
    }

    private fun registerWearBroadcastReceiver() {
        if (!isWearBroadcastRevieverRegistered) {
            wearBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val message = intent.getStringExtra(MYCROFT_WEAR_REQUEST_MESSAGE)
                    // send to mycroft
                    if (message != null) {
                        Log.d(logTag, "Wear message received: [$message] sending to Mycroft")
                        sendMessage(message)
                    }
                }
            }

            LocalBroadcastManager.getInstance(this).registerReceiver(wearBroadcastReceiver, IntentFilter(MYCROFT_WEAR_REQUEST))
            isWearBroadcastRevieverRegistered = true
        }
    }

    private fun unregisterReceivers() {
        unregisterBroadcastReceiver(networkChangeReceiver)
        unregisterBroadcastReceiver(wearBroadcastReceiver)

        isNetworkChangeReceiverRegistered = false
        isWearBroadcastRevieverRegistered = false
    }

    private fun unregisterBroadcastReceiver(broadcastReceiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    /**
     * This method will attach the correct path to the
     * [.wsip] hostname to allow for communication
     * with a Mycroft instance at that address.
     *
     *
     * If [.wsip] cannot be used as a hostname
     * in a [URI] (e.g. because it's null), then
     * this method will return null.
     *
     *
     * @return a valid uri, or null
     */
    private fun deriveURI(): URI? {
        return if (wsip.isNotEmpty()) {
            try {
                URI("ws://$wsip:8181/core")
            } catch (e: URISyntaxException) {
                Log.e(logTag, "Unable to build URI for websocket", e)
                null
            }
        } else {
            null
        }
    }

    fun sendMessage(msg: String) {
        // let's keep it simple eh?
        //final String json = "{\"message_type\":\"recognizer_loop:utterance\", \"context\": null, \"metadata\": {\"utterances\": [\"" + msg + "\"]}}";
        val json = "{\"data\": {\"utterances\": [\"$msg\"]}, \"type\": \"recognizer_loop:utterance\", \"context\": null}"

        try {
            if (webSocketClient == null || webSocketClient!!.connection.isClosed) {
                // try and reconnect
                if (NetworkUtil.getConnectivityStatus(this) == NetworkUtil.NETWORK_STATUS_WIFI) { //TODO: add config to specify wifi only.
                    connectWebSocket()
                }
            }

            val handler = Handler()
            handler.postDelayed({
                // Actions to do after 1 seconds
                try {
                    webSocketClient!!.send(json)
                    addData(Utterance(msg, UtteranceFrom.USER))
                } catch (exception: WebsocketNotConnectedException) {
                    showToast(resources.getString(R.string.websocket_closed))
                } catch (exception: KotlinNullPointerException) {
                    showToast(resources.getString(R.string.websocket_null))
                }
            }, 1000)

        } catch (exception: WebsocketNotConnectedException) {
            showToast(resources.getString(R.string.websocket_closed))
        }

    }

    private fun recognizeMicrophone() {
        if (speechService != null) {
            speechService!!.stop()
            speechService = null
            sendMessage(resultText)
            setUiState(STATE_READY)
            resultText = ""
        } else {
            setUiState(STATE_MIC)
            try {
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService!!.startListening(this)
            } catch (e: IOException) {
                setErrorState(e.message!!)
            }
        }
    }

    /**
     * Receiving speech input
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            reqCodeSpeechInput -> {
                if (resultCode == Activity.RESULT_OK && null != data) {

                    val result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                    sendMessage(result[0])
                }
            }
        }
        setUiState(STATE_READY)
    }

    public override fun onDestroy() {
        super.onDestroy()
        //ttsManager.shutDown()
        isNetworkChangeReceiverRegistered = false
        isWearBroadcastRevieverRegistered = false

        if (speechService != null) {
            speechService!!.stop()
            speechService!!.shutdown()
        }

        if (speechStreamService != null) {
            speechStreamService!!.stop()
        }

        stopPorcupine()
    }

    public override fun onStart() {
        super.onStart()
        recordVersionInfo()
        registerReceivers()
    }

    public override fun onStop() {
        super.onStop()

        unregisterReceivers()

        if (launchedFromWidget) {
            autoPromptForSpeech = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun loadPreferences() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // get mycroft-core ip address
        wsip = sharedPref.getString("ip", "")!!
        if (wsip!!.isEmpty()) {
            // eep, show the settings intent!
            startActivity(Intent(this, SettingsActivity::class.java))
        } else if (webSocketClient == null || webSocketClient!!.connection.isClosed) {
            connectWebSocket()
        }

        kbMicSwitch.isChecked = sharedPref.getBoolean("kbMicSwitch", true)
        if (kbMicSwitch.isChecked) {
            // Switch to mic
            micButton.visibility = View.VISIBLE
            utteranceInput.visibility = View.INVISIBLE
            sendUtterance.visibility = View.INVISIBLE
        } else {
            // Switch to keyboard
            micButton.visibility = View.INVISIBLE
            utteranceInput.visibility = View.VISIBLE
            sendUtterance.visibility = View.VISIBLE
        }

        // set app reader setting
        voxswitch.isChecked = sharedPref.getBoolean("appReaderSwitch", true)

        maximumRetries = Integer.parseInt(sharedPref.getString("maximumRetries", "1")!!)
    }

    private fun recordVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val editor = sharedPref.edit()
            editor.putInt(VERSION_CODE_PREFERENCE_KEY, packageInfo.versionCode)
            editor.putString(VERSION_NAME_PREFERENCE_KEY, packageInfo.versionName)
            editor.apply()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(logTag, "Couldn't find package info", e)
        }
    }

    private fun showToast(message: String) {
        GuiUtilities.showToast(applicationContext, message)
    }

    private fun initModel() {
        StorageService.unpack(this, "vosk-model-small-de-0.15", "model",
            { model: Model? ->
                this.model = model!!
                setUiState(STATE_READY)
            }
        ) { exception: IOException ->
            setErrorState(
                "Failed to unpack the model" + exception.message
            )
        }

    }

    private fun setErrorState(message: String) {
        resultView.text = message
        // setViewId stuff was deleted here
    }

    private fun setUiState(state: Int) {
        when (state) {
            STATE_START -> {
            }
           STATE_READY -> {
               textfeld.visibility = View.INVISIBLE
               result_text.visibility = View.INVISIBLE
            }
            STATE_DONE -> {
            }
            STATE_FILE -> {
            }
            STATE_MIC -> {
                resultView.text = ""
                textfeld.visibility = View.VISIBLE
                result_text.visibility = View.VISIBLE
            }
            else -> throw IllegalStateException("Unexpected value: $state")
        }

    }

    override fun onPartialResult(hypothesis: String) {
        //resultView.append(hypothesis + "\n")
        // resultView.text = hypothesis
    }

    override fun onResult(hypothesis : String) {
        var hypothesisText = JSONObject(hypothesis)["text"].toString()
        resultView.append(" $hypothesisText")
        // resultView.append(hypothesis + "\n")
        // resultView.text = hypothesisText["text"].toString()
        resultText += " $hypothesisText"

    }

    override fun onFinalResult(hypothesis: String) {
        // var hypothesisText = JSONObject(hypothesis)["text"].toString()
        setUiState(STATE_READY)
        if (speechStreamService != null) {
            speechStreamService = null
        }

    }

    override fun onError(e: java.lang.Exception) {
        setErrorState(e.message!!)
    }

    override fun onTimeout() {
        setUiState(STATE_READY)
    }

    private fun pause(checked : Boolean) {
        if (speechService != null) {
            speechService!!.setPause(checked)
        }
    }

    // Porcupine functions

    private fun startPorcupine() {
        //val serviceIntent = Intent(this, PorcupineService::class.java)
        //ContextCompat.startForegroundService(this, serviceIntent)
        PorcupineService.startService(this, "Thomicroft Service is running")
    }

    private fun stopPorcupine() {
        //val serviceIntent = Intent(this, PorcupineService::class.java)
        //stopService(serviceIntent)
        PorcupineService.stopService(this)
    }



}
