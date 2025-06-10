package com.example.gembos;
import android.Manifest;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TextingActivity extends AppCompatActivity implements KeyExchangeCallback{
    private String phoneNumber;
    private boolean isEncrypted;
    private com.example.gembos.KeyExchangeManager keyExchangeManager;

    private RecyclerView recyclerView;
    private MessageItemAdapter adapter;
    private List<MessageItem> messageList;

    private EditText editMessageText;
    private Button btnSend;
    private LinearLayout animatedMenu;
    private Button option2;

    private BroadcastReceiver smsReceiver;

    @Override
    public void onKeyExchangeMessage(String message, boolean isSent) {
        // Add key exchange message to UI
        messageList.add(new MessageItem("___ PUBLIC KEY ___", isSent));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texting);

        keyExchangeManager = new KeyExchangeManager();
        KeyExchangeManager.setCallback(this);

        recyclerView = findViewById(R.id.recyclerView);
        editMessageText = findViewById(R.id.editMessageText);
        btnSend = findViewById(R.id.btnSend);
        option2 = findViewById(R.id.option2);

        messageList = new ArrayList<>();
        adapter = new MessageItemAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        animatedMenu = findViewById(R.id.animatedMenu);
        // Telefon numarası almak için pop-up göster
        //showPhoneNumberDialog();

        // Receive phone number from intent
        phoneNumber = getIntent().getStringExtra("phone_number");
        isEncrypted = getIntent().getBooleanExtra("is_encrypted", false);

        if (!isEncrypted) {
            // Show warning dialog for unencrypted user
            showWarningDialog();
        }

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number provided!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadSmsConversation(phoneNumber);

        // SMS gönder butonu tıklama işlemi
        btnSend.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(TextingActivity.this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(TextingActivity.this, new String[]{Manifest.permission.SEND_SMS}, 1);
            } else {
                //showAnimatedMenu(view); // Animasyonu başlat
                sendSMS();
            }
        });

        option2.setOnClickListener(view -> {
            Intent intent = new Intent(TextingActivity.this, CheckNetwork.class);
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
                            SmsMessage[] messages = new SmsMessage[pdus.length];
                            String sender = null;

                            // Construct all SmsMessage objects
                            for (int i = 0; i < pdus.length; i++) {
                                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            }

                            // Build full message from parts
                            StringBuilder fullMessageBuilder = new StringBuilder();
                            for (SmsMessage msg : messages) {
                                if (sender == null) {
                                    sender = msg.getDisplayOriginatingAddress();
                                }
                                String body = msg.getMessageBody();
                                if (body.startsWith(KeyExchangeManager.PUBLIC_KEY_PREFIX) || fullMessageBuilder.toString().contains(KeyExchangeManager.PUBLIC_KEY_PREFIX)) {
                                    fullMessageBuilder.append(body);
                                }
                            }

                            String fullMessage = fullMessageBuilder.toString();

                            // Check if this is a key exchange message
                            if (fullMessage.contains(KeyExchangeManager.PUBLIC_KEY_PREFIX) && keyExchangeManager.getSharedKey(sender) == null) {
                                keyExchangeManager.receivePublicKey(sender, fullMessage);
                                Toast.makeText(context, "Received key message from " + sender, Toast.LENGTH_SHORT).show();
                            }

                            // Check if the sender matches the saved phone number
                            if (phoneNumber != null && sender.equals(phoneNumber)) {
                                String displayText;

                                if (isEncrypted(fullMessage)) {
                                    SecretKey key = keyExchangeManager.getSharedKey(sender);
                                    if (key != null) {
                                        try {
                                            displayText = EncryptionHelper.decrypt(fullMessage, (SecretKeySpec) key);
                                        } catch (Exception e) {
                                            displayText = "[Failed to decrypt]";
                                        }
                                        messageList.add(new MessageItem(displayText, false));
                                        adapter.notifyItemInserted(messageList.size() - 1);
                                        recyclerView.scrollToPosition(messageList.size() - 1);
                                    } else {
                                        // Delay retrying decryption until key is ready
                                        String finalSender = sender;
                                        new android.os.Handler().postDelayed(() -> {
                                            SecretKey retryKey = keyExchangeManager.getSharedKey(finalSender);
                                            String retryText;
                                            if (retryKey != null) {
                                                try {
                                                    retryText = EncryptionHelper.decrypt(fullMessage, (SecretKeySpec) retryKey);
                                                } catch (Exception e) {
                                                    retryText = "[Failed to decrypt after retry]";
                                                }
                                            } else {
                                                retryText = "[Encrypted message – key not available after retry]";
                                            }

                                            messageList.add(new MessageItem(retryText, false));
                                            adapter.notifyItemInserted(messageList.size() - 1);
                                            recyclerView.scrollToPosition(messageList.size() - 1);
                                        }, 1000); // Delay 1 second (adjust as needed)
                                    }
                                } else if(isSecretKey(fullMessage)){
                                    displayText = "___ PUBLIC KEY ___";
                                } else{
                                    displayText = fullMessage;
                                }

                                // Add the message to the list and update the UI
                                messageList.add(new MessageItem(displayText, false)); // false indicates a received message
                                adapter.notifyItemInserted(messageList.size() - 1);
                                recyclerView.scrollToPosition(messageList.size() - 1);

                                //Toast.makeText(context, "Message received from: " + sender, Toast.LENGTH_SHORT).show();
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

    private void showWarningDialog() {
        // Create a custom view for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_encryption_warning, null);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set up the OK button
        Button btnOk = dialogView.findViewById(R.id.btnSubmitPhoneNumber);
        btnOk.setOnClickListener(v -> {
            // Dismiss the dialog when the button is clicked
            dialog.dismiss();
        });
    }

    private void loadSmsConversation(String number) {
        messageList.clear();
        Uri uri = Uri.parse("content://sms");
        Cursor cursor = getContentResolver().query(uri, null, null, null, "date ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type")); // 1=received, 2=sent

                if (address != null && address.equals(number)) {
                    boolean isSent = type == 2;
                    String displayText;

                    if (isEncrypted(body)) {
                        SecretKey key = keyExchangeManager.getSharedKey(number);
                        if (key != null) {
                            try {
                                displayText = EncryptionHelper.decrypt(body, (SecretKeySpec) key);
                            } catch (Exception e) {
                                displayText = "[Failed to decrypt]";
                            }
                        } else {
                            displayText = "[Encrypted message – key not exchanged yet]";
                        }
                    } else if (isSecretKey(body)){
                        displayText = "___ PUBLIC KEY ___";
                    } else {
                        displayText = body;
                    }

                    messageList.add(new MessageItem(displayText, isSent));
                }
            }
            cursor.close();
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
    }


    private void sendSMS() {
        String messageText = editMessageText.getText().toString();
        SecretKey sharedKey = keyExchangeManager.getSharedKey(phoneNumber);

        if (sharedKey == null) {
            // No shared key exists -> Start key exchange
            keyExchangeManager.initiateKeyExchange(this, phoneNumber);
            Toast.makeText(this, "Initiating key exchange with " + phoneNumber, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!messageText.isEmpty() && phoneNumber != null) {
            try {
                String encryptedSMS = EncryptionHelper.encrypt(messageText, (SecretKeySpec) sharedKey);

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, encryptedSMS, null, null);
                //Toast.makeText(this, "SMS sent!", Toast.LENGTH_SHORT).show();

                messageList.add(new MessageItem(messageText, true));
                adapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);

                editMessageText.setText("");
            } catch (Exception e) {
                Toast.makeText(this, "Error sending SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            if (phoneNumber == null)
                Toast.makeText(this, "Phone number unavailable!", Toast.LENGTH_SHORT).show();
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
        if (requestCode == 1) {
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

    public static boolean isEncrypted(String text) {
        return text.startsWith("[GEMBOS]");
    }
    public static boolean isSecretKey(String text) {
        return text.startsWith("[KEYX]");
    }

}