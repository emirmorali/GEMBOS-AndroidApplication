package com.example.gembos;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class CheckNetwork extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_check_network);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            checkNetworkSecurity();
        }
    }

    private void checkNetworkSecurity() {
        String securityType = WifiSecurityUtils.getCurrentWifiSecurity(this);

        if ("OPEN".equals(securityType)) {
            ThreatAlert.showSecurityWarning(this);
        } else if ("SECURE".equals(securityType)) {
            Toast.makeText(this, "Ağ paylaşımlı ve güvenli. Derin Arama Yapıyorum", Toast.LENGTH_SHORT).show();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> networkList = wifi.getScanResults();

                WifiInfo wi = wifi.getConnectionInfo();
                String currentSSID = wi.getSSID();

                if (networkList != null) {
                    for (ScanResult network : networkList) {
                        if (currentSSID.equals(network.SSID)) {
                            String capabilities = network.capabilities;
                            Log.d(TAG, network.SSID + " capabilities : " + capabilities);

                            if (capabilities.contains("WPA2")) {
                                Log.d(TAG, network.SSID + " WPA2 güvenliği mevcut. : ");

                                Toast.makeText(this, "WPA2 güvenliği mevcut.", Toast.LENGTH_SHORT).show();
                            } else if (capabilities.contains("WPA")) {
                                Toast.makeText(this, "WPA güvenliği mevcut.", Toast.LENGTH_SHORT).show();
                            } else if (capabilities.contains("WEP")) {
                                Toast.makeText(this, "WEP güvenliği mevcut. Zayıf!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Belirsiz güvenlik tipi.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Wi-Fi ağları taranamadı.", Toast.LENGTH_SHORT).show();
                }
            }

        }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // İzin verildi
                checkNetworkSecurity();
            } else {
                // İzin reddedildi
                Toast.makeText(this, "Konum izni reddedildi. Ağ güvenliği taraması yapılamaz.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
