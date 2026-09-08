package pl.nygus.live

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.mediaPlaybackRequiresUserGesture = false
        webView.webViewClient = WebViewClient()

        webView.loadUrl("file:///android_asset/dashboard.html")
    }
}
