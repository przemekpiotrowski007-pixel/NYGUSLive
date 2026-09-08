package pl.nygus.live

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var toolbar: LinearLayout
    private lateinit var fileName: TextView

    private val prefs by lazy {
        getSharedPreferences("nygus_live", MODE_PRIVATE)
    }

    private val pickHtml =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->

            if (uri != null) {

                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                prefs.edit()
                    .putString("html_uri", uri.toString())
                    .apply()

                loadHtml(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true

            webViewClient = WebViewClient()
        }

        fileName = TextView(this).apply {

            text = "Nie wybrano pliku HTML"

            setPadding(
                24,
                18,
                24,
                18
            )
        }

        val chooseButton = Button(this).apply {

            text = "WYBIERZ / ZMIEŃ PLIK HTML"

            setOnClickListener {

                pickHtml.launch(
                    arrayOf(
                        "text/html",
                        "application/xhtml+xml"
                    )
                )
            }
        }

        val fullscreenButton = Button(this).apply {

            text = "UKRYJ PANEL"

            setOnClickListener {

                toolbar.visibility =
                    android.view.View.GONE
            }
        }

        toolbar = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            addView(fileName)

            addView(chooseButton)

            addView(fullscreenButton)
        }

        val root = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            addView(
                toolbar,
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

        val savedUri =
            prefs.getString(
                "html_uri",
                null
            )

        if (savedUri != null) {

            loadHtml(
                Uri.parse(savedUri)
            )

        } else {

            webView.loadDataWithBaseURL(
                null,
                """
                <html>
                <body style="
                    background:#080b0e;
                    color:white;
                    font-family:Arial;
                    display:flex;
                    align-items:center;
                    justify-content:center;
                    height:100vh;
                    margin:0;
                    text-align:center;
                ">

                <div>

                    <h1>NYGUS LIVE</h1>

                    <p>
                    Wybierz plik HTML z telefonu.
                    </p>

                </div>

                </body>
                </html>
                """.trimIndent(),
                "text/html",
                "UTF-8",
                null
            )
        }
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
                    ?: throw Exception(
                        "Nie można odczytać pliku"
                    )

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

            fileName.text =
                "Błąd: ${e.message}"
        }
    }

    private fun getDisplayName(
        uri: Uri
    ): String {

        var result =
            "wybrany plik.html"

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

                    result =
                        cursor.getString(index)
                }
            }
        }

        return result
    }

    override fun onBackPressed() {

        if (
            toolbar.visibility ==
            android.view.View.GONE
        ) {

            toolbar.visibility =
                android.view.View.VISIBLE

        } else if (
            webView.canGoBack()
        ) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }
}
