package com.example.gembos;
import android.Manifest;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 1;
    private String phoneNumber;

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messageList;

    private EditText editMessageText;
    private Button btnSend;
    private LinearLayout animatedMenu;
    private Button option2;

    private BroadcastReceiver smsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        editMessageText = findViewById(R.id.editMessageText);
        btnSend = findViewById(R.id.btnSend);
        option2 = findViewById(R.id.option2);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        animatedMenu = findViewById(R.id.animatedMenu);
        // Telefon numarası almak için pop-up göster
        showPhoneNumberDialog();

        // Check and request SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);
        }

        // SMS gönder butonu tıklama işlemi
        btnSend.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
            } else {
                //showAnimatedMenu(view); // Animasyonu başlat
                sendSMS();
            }
        });

        option2.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CheckNetwork.class);
            startActivity(intent);
        });

        animatedMenu.setOnClickListener(view -> {
            animatedMenu.setVisibility(View.GONE);
        });

        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        if (pdus != null) {
                            for (Object pdu : pdus) {
                                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                                String sender = smsMessage.getDisplayOriginatingAddress();
                                String message = smsMessage.getMessageBody();

                                // Check if the sender matches the saved phone number
                                if (phoneNumber != null && sender.equals(phoneNumber)) {
                                    // Add the message to the list and update the UI
                                    messageList.add(new Message(message, false)); // false indicates a received message
                                    adapter.notifyItemInserted(messageList.size() - 1);
                                    recyclerView.scrollToPosition(messageList.size() - 1);

                                    Toast.makeText(context, "Message received from: " + sender, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
            }
        };

        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    private void showPhoneNumberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_phone_number, null);
        builder.setView(dialogView);

        EditText editPhoneNumber = dialogView.findViewById(R.id.editPhoneNumber);
        Button btnSubmitPhoneNumber = dialogView.findViewById(R.id.btnSubmitPhoneNumber);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();

        btnSubmitPhoneNumber.setOnClickListener(view -> {
            String inputPhoneNumber = editPhoneNumber.getText().toString();
            if (!inputPhoneNumber.isEmpty()) {
                phoneNumber = inputPhoneNumber;
                dialog.dismiss();
                Toast.makeText(this, "Phone number saved: " + phoneNumber, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Enter phone number.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSMS() {
        String messageText = editMessageText.getText().toString();

        if (!messageText.isEmpty() && phoneNumber != null) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, messageText, null, null);
                Toast.makeText(this, "SMS sent!", Toast.LENGTH_SHORT).show();

                messageList.add(new Message(messageText, true));
                adapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);

                editMessageText.setText("");
            } catch (Exception e) {
                Toast.makeText(this, "Error sending SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Check message content or phone number.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAnimatedMenu(View anchorView) {
        LinearLayout animatedMenu = findViewById(R.id.animatedMenu);
        animatedMenu.setVisibility(View.VISIBLE);

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int centerX = location[0] + (anchorView.getWidth() / 2);
        int centerY = location[1] + (anchorView.getHeight() / 2);

        int finalRadius = Math.max(animatedMenu.getWidth(), animatedMenu.getHeight());

        Animator animator = ViewAnimationUtils.createCircularReveal(animatedMenu, centerX, centerY, 0, finalRadius);
        animator.setDuration(500);
        animator.start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSMS();
            } else {
                Toast.makeText(this, "Need SMS sending permission.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the activity is destroyed
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
        }
    }
}