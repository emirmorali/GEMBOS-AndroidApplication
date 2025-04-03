package com.example.gembos;

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

        verifyBtn = findViewById(R.id.btnVerify);
        verificationCodeTxt = findViewById(R.id.edtVerificationCode);

        sendVerificationCode(phoneNumber);

        dbHelper = new DBHelper(this);

        verifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String verificationCode = verificationCodeTxt.getText().toString();

                if(dbHelper.verifyUser(phoneNumber, verificationCode)) {
                    Toast.makeText(VerificationActivity.this,"Verification successful!", Toast.LENGTH_LONG).show();
                    //Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                    //startActivity(i);
                }
                else{
                    Toast.makeText(VerificationActivity.this,"Entered code is not correct!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void sendVerificationCode(String phoneNumber){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        }

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
