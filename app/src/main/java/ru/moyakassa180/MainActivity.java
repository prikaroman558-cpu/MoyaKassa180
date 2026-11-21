package ru.moyakassa180;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new PrinterBridge(), "AndroidPrinter");

        setContentView(webView);

        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100
            );
        }

        webView.loadUrl("https://prikaroman558-cpu.github.io/MoyaKassa180/");
    }

    public class PrinterBridge {
        @JavascriptInterface
        public String printReceipt(final String receipt) {
            if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                return "Нет разрешения Bluetooth";
            }

            new Thread(() -> {
                BluetoothSocket socket = null;
                try {
                    BluetoothAdapter adapter =
                        BluetoothAdapter.getDefaultAdapter();

                    if (adapter == null || !adapter.isEnabled()) return;

                    BluetoothDevice device =
                        adapter.getRemoteDevice("DD:12:29:35:56:D3");

                    UUID uuid = UUID.fromString(
                        "00001101-0000-1000-8000-00805F9B34FB"
                    );

                    socket = device.createRfcommSocketToServiceRecord(uuid);
                    socket.connect();

                    OutputStream output = socket.getOutputStream();
                    output.write(new byte[]{0x1B, 0x40});
                    output.write(receipt.getBytes("UTF-8"));
                    output.write(new byte[]{0x0A, 0x0A, 0x0A});
                    output.flush();

                } catch (Exception e) {
                    android.util.Log.e("MoyaKassa180", "Ошибка печати", e);
                } finally {
                    try {
                        if (socket != null) socket.close();
                    } catch (Exception ignored) {}
                }
            }).start();

            return "Отправка чека запущена";
        }
    }
}
