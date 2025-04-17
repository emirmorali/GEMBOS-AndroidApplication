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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

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

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();

        adapter = new MessageAdapter(messageList, phoneNumber -> {
            Intent intent = new Intent(MessagesActivity.this, TextingActivity.class);
            intent.putExtra("phone_number", phoneNumber);
            startActivity(intent);
        });
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

        Uri uriSms = Uri.parse("content://sms/inbox");

        // Use try-with-resources to automatically close the cursor
        try (Cursor cursor = getContentResolver().query(uriSms, null, null, null, "date DESC")) {
            if (cursor != null) {
                // Map to hold messages grouped by sender
                Map<String, Message> latestMessages = new HashMap<>();

                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
/*
                    // Fetch contact name if available
                    String contactName = getContactName(address);

                    // If contact name is not available, use the phone number
                    if (contactName == null || contactName.isEmpty()) {
                        contactName = address; // Use the phone number if contact name is not found
                    }
*/
                    Message message = new Message(address, body, date);

                    // Only keep the latest message for each sender
                    if (!latestMessages.containsKey(address) || Long.parseLong(latestMessages.get(address).getDate()) < Long.parseLong(date)) {
                        latestMessages.put(address, message);
                    }
                }

                // Add the latest message for each sender
                messageList.addAll(latestMessages.values());

                // Sort the messages by date in descending order
                Collections.sort(messageList, new Comparator<Message>() {
                    @Override
                    public int compare(Message m1, Message m2) {
                        // Compare by date, with the most recent messages first
                        return Long.compare(Long.parseLong(m2.getDate()), Long.parseLong(m1.getDate()));
                    }
                });

                // Notify the adapter about the change
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception for debugging
            Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
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
