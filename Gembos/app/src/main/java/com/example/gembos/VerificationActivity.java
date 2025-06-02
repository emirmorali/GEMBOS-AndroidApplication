package com.example.gembos;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VerificationActivity extends AppCompatActivity {

    EditText otp1, otp2, otp3, otp4, otp5, otp6;
    Button verifyBtn;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        String phoneNumber = getIntent().getStringExtra("phone_number");
        Log.d("RegisterActivity", "Starting VerificationActivity with phone number: " + phoneNumber);

        // OTP alanları tanımlanıyor
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);

        verifyBtn = findViewById(R.id.btnVerify);
        dbHelper = new DBHelper(this);

        sendVerificationCode(phoneNumber);

        verifyBtn.setOnClickListener(v -> {
            String verificationCode =
                    otp1.getText().toString().trim() +
                            otp2.getText().toString().trim() +
                            otp3.getText().toString().trim() +
                            otp4.getText().toString().trim() +
                            otp5.getText().toString().trim() +
                            otp6.getText().toString().trim();

            if (verificationCode.length() < 6) {
                Toast.makeText(this, "Please enter all 6 digits.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.verifyUser(phoneNumber, verificationCode)) {
                Toast.makeText(this, "Verification successful!", Toast.LENGTH_LONG).show();
                //boolean registeredSuccess = dbHelper.insertData(phoneNumber, name, surname, password);
                startActivity(new Intent(VerificationActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Entered code is not correct!", Toast.LENGTH_LONG).show();
            }
        });

        // Otomatik geçiş için yardımcı fonksiyon (isteğe bağlı ama önerilir)
        setupOtpAutoMove();
    }

    private void sendVerificationCode(String phoneNumber) {
        String verificationCode = dbHelper.getVerificationCode(phoneNumber);

        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = "Your GEMBOS verification code: " + verificationCode;
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d("SMS", "Verification code sent: " + message);
        } catch (Exception e) {
            Log.e("SMS", "Error: " + e.getMessage());
        }
    }

    private void setupOtpAutoMove() {
        EditText[] otpFields = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[index].addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        otpFields[index - 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) { }
            });
        }
    }
}
