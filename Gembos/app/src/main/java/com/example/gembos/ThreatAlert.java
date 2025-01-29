package com.example.gembos;
import android.app.AlertDialog;
import android.content.Context;

public class ThreatAlert {

    public static void showSecurityWarning(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Unsafe network detected!")
                .setMessage("The Wi-Fi network you are connected is unsafe.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
