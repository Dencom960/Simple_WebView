package com.example.baragundaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ClientCertRequest;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    private X509Certificate[] mCertificates;
    private PrivateKey mPrivateKey;

    private void loadCertificateAndPrivateKey() {
        try {
            InputStream certificateFileStream = getClass().getResourceAsStream("/assets/cert.pfx");

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = "password";
            keyStore.load(certificateFileStream, password != null ? password.toCharArray() : null);

            Enumeration<String> aliases = keyStore.aliases();
            String alias = aliases.nextElement();

            Key key = keyStore.getKey(alias, password.toCharArray());
            if (key instanceof PrivateKey) {
                mPrivateKey = (PrivateKey)key;
                Certificate cert = keyStore.getCertificate(alias);
                mCertificates = new X509Certificate[1];
                mCertificates[0] = (X509Certificate)cert;
            }

            certificateFileStream.close();

        } catch (Exception ignored) {

        }
    }

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions =
                {Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.INTERNET};

        ActivityCompat.requestPermissions(
                this,
                permissions,
                200);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://baragunda.ru/easyeng/auth.php");
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        WebViewClient webViewClient = new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onReceivedClientCertRequest(WebView view, final ClientCertRequest request) {
                if (mCertificates == null || mPrivateKey == null) {
                    loadCertificateAndPrivateKey();
                }
                request.proceed(mPrivateKey, mCertificates);
            }
        };

        webView.setWebViewClient(webViewClient);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}