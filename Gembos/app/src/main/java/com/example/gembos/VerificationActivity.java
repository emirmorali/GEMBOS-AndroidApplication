package com.example.gembos;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VerificationActivity extends AppCompatActivity {
    EditText verificationCodeTxt;
    Button verifyBtn;
    DBHelper dbHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        String phoneNumber = getIntent().getStringExtra("phone_number");
        Log.d("RegisterActivity", "Starting VerificationActivity with phone number: " + phoneNumber);

        verifyBtn = findViewById(R.id.btnVerify);
        verificationCodeTxt = findViewById(R.id.edtVerificationCode);

        dbHelper = new DBHelper(this);
        sendVerificationCode(phoneNumber);

        verifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String verificationCode = verificationCodeTxt.getText().toString();

                if(dbHelper.verifyUser(phoneNumber, verificationCode)) {
                    Toast.makeText(VerificationActivity.this,"Verification successful!", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(VerificationActivity.this, LoginActivity.class);
                    startActivity(i);
                }
                else{
                    Toast.makeText(VerificationActivity.this,"Entered code is not correct!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void sendVerificationCode(String phoneNumber){
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
}
