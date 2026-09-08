package pl.nygus.live

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var panel: LinearLayout
    private lateinit var fileName: TextView
    private lateinit var rtmpInput: EditText
    private lateinit var audioSpinner: Spinner

    private var pendingEndpoint = ""
    private var pendingAudioMode = 0

    private val prefs by lazy {
        getSharedPreferences("nygus_live", MODE_PRIVATE)
    }

    private val pickHtml =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {

                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                prefs.edit()
                    .putString("html_uri", uri.toString())
                    .apply()

                loadHtml(uri)
            }
        }

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {
                requestCapture()
            } else {
                Toast.makeText(
                    this,
                    "Brak zgody na audio",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK && result.data != null) {

                val intent = Intent(
                    this,
                    StreamService::class.java
                ).apply {

                    action = StreamService.ACTION_START

                    putExtra(
                        StreamService.EXTRA_RESULT_CODE,
                        result.resultCode
                    )

                    putExtra(
                        StreamService.EXTRA_DATA,
                        result.data
                    )

                    putExtra(
                        StreamService.EXTRA_ENDPOINT,
                        pendingEndpoint
                    )

                    putExtra(
                        StreamService.EXTRA_AUDIO_MODE,
                        pendingAudioMode
                    )
                }

                ContextCompat.startForegroundService(
                    this,
                    intent
                )

                panel.visibility = View.GONE

                Toast.makeText(
                    this,
                    "START LIVE",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Anulowano przechwytywanie ekranu",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false

            webViewClient = WebViewClient()
        }

        fileName = TextView(this).apply {
            text = "Nie wybrano HTML"
            setPadding(20, 15, 20, 15)
        }

        val chooseHtml = Button(this).apply {

            text = "WYBIERZ HTML"

            setOnClickListener {

                pickHtml.launch(
                    arrayOf(
                        "text/html",
                        "application/xhtml+xml"
                    )
                )
            }
        }

        rtmpInput = EditText(this).apply {

            hint = "RTMP / RTMPS + stream key"

            setSingleLine(true)

            setText(
                prefs.getString(
                    "rtmp",
                    ""
                )
            )
        }

        audioSpinner = Spinner(this)

        val audioOptions = arrayOf(
            "Dźwięk telefonu / muzyka",
            "Mikrofon",
            "Mix: muzyka + mikrofon"
        )

        audioSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                audioOptions
            )

        val startButton = Button(this).apply {

            text = "🔴 START LIVE"

            setOnClickListener {

                startLive()
            }
        }

        val stopButton = Button(this).apply {

            text = "⏹ STOP LIVE"

            setOnClickListener {

                val intent =
                    Intent(
                        this@MainActivity,
                        StreamService::class.java
                    ).apply {

                        action =
                            StreamService.ACTION_STOP
                    }

                startService(intent)

                panel.visibility =
                    View.VISIBLE

                Toast.makeText(
                    this@MainActivity,
                    "LIVE zatrzymany",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val hideButton = Button(this).apply {

            text = "UKRYJ PANEL"

            setOnClickListener {

                panel.visibility =
                    View.GONE
            }
        }

        panel = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                10,
                10,
                10,
                10
            )

            addView(fileName)

            addView(chooseHtml)

            addView(rtmpInput)

            addView(audioSpinner)

            addView(startButton)

            addView(stopButton)

            addView(hideButton)
        }

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                addView(
                    panel,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                addView(
                    webView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                )
            }

        setContentView(root)

        val saved =
            prefs.getString(
                "html_uri",
                null
            )

        if (saved != null) {

            loadHtml(
                Uri.parse(saved)
            )

        } else {

            webView.loadData(
                """
                <html>
                <body style="
                background:#05090c;
                color:white;
                font-family:Arial;
                text-align:center;
                padding-top:100px;
                ">

                <h1>NYGUS LIVE</h1>

                <p>Wybierz plik HTML</p>

                </body>
                </html>
                """.trimIndent(),
                "text/html",
                "UTF-8"
            )
        }
    }

    private fun startLive() {

        val endpoint =
            rtmpInput.text
                .toString()
                .trim()

        if (
            !endpoint.startsWith("rtmp://") &&
            !endpoint.startsWith("rtmps://")
        ) {

            Toast.makeText(
                this,
                "Wpisz poprawny RTMP/RTMPS",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        prefs.edit()
            .putString(
                "rtmp",
                endpoint
            )
            .apply()

        pendingEndpoint =
            endpoint

        pendingAudioMode =
            audioSpinner.selectedItemPosition

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            micPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )

        } else {

            requestCapture()
        }
    }

    private fun requestCapture() {

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            )
                    as android.media.projection.MediaProjectionManager

        captureLauncher.launch(
            manager.createScreenCaptureIntent()
        )
    }

    private fun loadHtml(uri: Uri) {

        try {

            val html =
                contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader(
                        Charsets.UTF_8
                    )
                    ?.use {
                        it.readText()
                    }
                    ?: return

            fileName.text =
                "HTML: ${getDisplayName(uri)}"

            webView.loadDataWithBaseURL(
                uri.toString(),
                html,
                "text/html",
                "UTF-8",
                null
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Błąd HTML: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getDisplayName(
        uri: Uri
    ): String {

        var name =
            "plik.html"

        contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (index >= 0) {

                    name =
                        cursor.getString(index)
                }
            }
        }

        return name
    }

    override fun onBackPressed() {

        if (
            panel.visibility ==
            View.GONE
        ) {

            panel.visibility =
                View.VISIBLE

        } else {

            super.onBackPressed()
        }
    }
}
