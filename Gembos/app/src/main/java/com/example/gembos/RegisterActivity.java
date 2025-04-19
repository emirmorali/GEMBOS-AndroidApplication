package com.example.gembos;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class RegisterActivity extends AppCompatActivity {

    EditText edtName, edtSurname, edtPhoneNumber, edtPassword, edtPasswordAgain;
    Button btnRegister, btnLogin;
    DBHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtName = findViewById(R.id.edtName);
        edtSurname = findViewById(R.id.edtSurname);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtPassword = findViewById(R.id.edtPassword);
        edtPasswordAgain = findViewById(R.id.edtPasswordAgain);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);

        dbHelper = new DBHelper(this);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name,surname,phoneNumber,password,passwordAgain;
                name = edtName.getText().toString();
                surname = edtSurname.getText().toString();
                phoneNumber = edtPhoneNumber.getText().toString();
                password = edtPassword.getText().toString();
                passwordAgain = edtPasswordAgain.getText().toString();

                if(name.equals("") || surname.equals("") || phoneNumber.equals("") || password.equals("") || passwordAgain.equals("")){
                    Toast.makeText(RegisterActivity.this,"Please fill all the fields", Toast.LENGTH_LONG).show();
                }else{
                    if (!isValidPhoneNumber(phoneNumber)) {
                        Toast.makeText(RegisterActivity.this, "Invalid phone number.", Toast.LENGTH_LONG).show();
                        return; // Geçersizse işlemi durdur
                    }
                    if(password.equals(passwordAgain)){
                        if(dbHelper.checkPhoneNumber(phoneNumber)){
                            Toast.makeText(RegisterActivity.this,"Phone number already exists", Toast.LENGTH_LONG).show();
                            return;
                        }
                        boolean registeredSuccess = dbHelper.insertData(phoneNumber, name, surname, password);
                        if(registeredSuccess){
                            Toast.makeText(RegisterActivity.this,"User registered successfully", Toast.LENGTH_LONG).show();
                            Intent i = new Intent(RegisterActivity.this, VerificationActivity.class);
                            i.putExtra("phone_number", phoneNumber);
                            startActivity(i);
                        }else{
                            Toast.makeText(RegisterActivity.this,"User registration failed", Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(RegisterActivity.this,"Passwords must be same", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        //Login ekranına gönder.
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });

    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^05[0-9]{9}$"); // 5 ile başlamalı ve toplam 10 rakam içermeli
    }
}
