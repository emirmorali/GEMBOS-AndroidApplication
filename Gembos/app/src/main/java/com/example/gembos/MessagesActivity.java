package com.example.gembos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

public class MessagesActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_READ_SMS = 100;
    private RecyclerView messageRecyclerView;
    private List<Message> messageList;
    private MessageAdapter adapter;
    private FloatingActionButton syncButton;
    private SyncManager syncManager;
    DBHelper dbHelper;

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadSmsInbox(); // reload messages when a new SMS is received
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        dbHelper = new DBHelper(this);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        syncButton = findViewById(R.id.syncButton);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();

        syncManager = new SyncManager(this);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncManager.sendUnsyncedUsersToServer();
                Toast.makeText(MessagesActivity.this, "Synchronization Started", Toast.LENGTH_SHORT).show();
                Log.d("DEBUG", "Mesaj senkronizasyonu başlatılıyor.");

                // Mesajları sunucuya şifreleyip gönder
                //syncManager.sendUnsyncedMessagesToServer(MasterKey.getMasterKey());
                syncManager.sendUnsyncedMessagesToServer();
            }
        });

        adapter = new MessageAdapter(messageList, (phoneNumber, isEncrypted) -> {
            Intent intent = new Intent(MessagesActivity.this, TextingActivity.class);
            intent.putExtra("phone_number", phoneNumber);
            intent.putExtra("is_encrypted", isEncrypted);
            startActivity(intent);
        }, null);
        messageRecyclerView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    PERMISSION_REQUEST_READ_SMS);
        } else {
            loadSmsInbox();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter);

        loadSmsInbox();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(smsReceiver); // to avoid memory leaks
    }


    private void loadSmsInbox() {
        messageList.clear(); // clear old messages

        // Get both inbox and sent messages
        List<Message> allMessages = new ArrayList<>();
        fetchMessagesFromUri(Uri.parse("content://sms/inbox"), allMessages);
        fetchMessagesFromUri(Uri.parse("content://sms/sent"), allMessages);

        // Map to hold latest message per address
        Map<String, Message> latestMessages = new HashMap<>();

        for (Message msg : allMessages) {
            String address = msg.getSender();
            String date = msg.getDate();

            if (!latestMessages.containsKey(address) ||
                    Long.parseLong(latestMessages.get(address).getDate()) < Long.parseLong(date)) {
                latestMessages.put(address, msg);
            }
        }

        messageList.addAll(latestMessages.values());

        // Sort the messages by date descending
        Collections.sort(messageList, (m1, m2) ->
                Long.compare(Long.parseLong(m2.getDate()), Long.parseLong(m1.getDate()))
        );

        adapter.notifyDataSetChanged();

    }

    private void fetchMessagesFromUri(Uri uri, List<Message> targetList) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, "date DESC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                    Message message = new Message(address, body, date);
                    targetList.add(message);

                    if (body != null && body.startsWith("[GEMBOS]")) {
                        dbHelper.insertMessage(message);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(this, "Failed to load SMS from " + uri.toString(), Toast.LENGTH_SHORT).show();
        }
    }




    // Helper method to get contact name from phone number
    private String getContactName(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            cursor.close();
            return contactName;
        }

        return null; // Return null if no contact name is found
    }

    // Handle user permission response
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSmsInbox();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
