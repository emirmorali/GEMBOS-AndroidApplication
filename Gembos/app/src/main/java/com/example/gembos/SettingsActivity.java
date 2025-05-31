package com.example.gembos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    private Switch notificationSwitch;
    private Switch darkModeSwitch;
    private CheckBox encryptionCheckBox;
    private Switch autoSyncSwitch;
    private Button editProfileButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        notificationSwitch = findViewById(R.id.switch_notifications);
        darkModeSwitch = findViewById(R.id.switch_dark_mode);
        encryptionCheckBox = findViewById(R.id.checkbox_encryption);
        autoSyncSwitch = findViewById(R.id.switch_auto_sync);
        editProfileButton = findViewById(R.id.btn_edit_profile);
        logoutButton = findViewById(R.id.btn_logout);

        // Notification Switch
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "Notifications on" : "Notifications off";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();

            // Buraya bildirimleri aktif/pasif etme kodunu ekleyebilirsin
        });

        // Dark Mode Switch
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "Dark mode enabled" : "Dark mode disabled";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();

            // Burada tema değiştirme kodu olabilir
        });

        // Encryption Checkbox
        encryptionCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "Message encryption enabled" : "Message encryption disabled";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();

            // Mesaj şifrelemeyi aç/kapa işlemi burada yapılabilir
        });

        // Auto Sync Switch
        autoSyncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "Auto sync enabled" : "Auto sync disabled";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();

            // Otomatik senkronizasyon aktif/pasif yapılabilir
        });

        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
