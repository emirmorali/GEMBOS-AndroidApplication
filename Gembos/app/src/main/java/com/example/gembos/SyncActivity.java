package com.example.gembos;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SyncActivity extends AppCompatActivity {

    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        syncManager = new SyncManager(this);

        Button syncButton = findViewById(R.id.btnStartSync);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncManager.sendUnsyncedUsersToServer();
                Toast.makeText(SyncActivity.this, "Synchronization Started", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
