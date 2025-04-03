package com.example.gembos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.Random;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DBName = "register.db";

    public DBHelper(@Nullable Context context) {
        super(context, DBName, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table users(phoneNumber TEXT primary key, name TEXT, surname TEXT, password TEXT, isVerified INTEGER, verificationCode TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    public boolean insertData(String phoneNumber, String name, String surname, String password){
        String verificationCode = generateVerificationCode();

        SQLiteDatabase myDB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("phoneNumber", phoneNumber);
        contentValues.put("name", name);
        contentValues.put("surname", surname);
        contentValues.put("password", password);
        contentValues.put("isVerified", 0);
        contentValues.put("verificationCode", verificationCode);
        long result = myDB.insert("users", null, contentValues);
        if(result == -1) return false;
        else return true;
    }

    public String generateVerificationCode(){
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ile 999999 arasında sayı üretir
        return String.valueOf(code);
    }

    public boolean checkPhoneNumber(String phoneNumber){
        if (phoneNumber.startsWith("0")) {
            phoneNumber = phoneNumber.substring(1);
        }
        
        SQLiteDatabase myDB = this.getWritableDatabase();
        Cursor cursor = myDB.rawQuery("select * from users where phoneNumber = ?", new String[]{phoneNumber});
        if(cursor.getCount()>0) {
            cursor.close();
            myDB.close();
            return true;
        }
        else return false;
    }

    public boolean verifyUser(String phoneNumber, String enteredCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE phoneNumber = ? AND verificationCode = ?",
                new String[]{phoneNumber, enteredCode});

        if (cursor.getCount() > 0) {
            // Kod doğru, kullanıcının isVerified değerini 1 yap
            ContentValues values = new ContentValues();
            values.put("isVerified", 1);
            db.update("users", values, "phoneNumber = ?", new String[]{phoneNumber});
            cursor.close();
            db.close();
            return true;
        } else {
            // Kod yanlış
            cursor.close();
            db.close();
            return false;
        }
    }

    public String getVerificationCode(String phoneNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        String verificationCode = null;

        Cursor cursor = db.rawQuery("SELECT verificationCode FROM users WHERE phoneNumber = ?",
                new String[]{phoneNumber});

        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex("verificationCode");
            if (columnIndex != -1) {
                verificationCode = cursor.getString(columnIndex);
            }
        }

        cursor.close();
        db.close();
        return verificationCode;
    }

}
