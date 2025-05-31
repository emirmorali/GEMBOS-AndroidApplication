package com.example.gembos;

import static com.example.gembos.EncryptionHelper.decrypt;
import static com.example.gembos.EncryptionHelper.encrypt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DBName = "register.db";

    public DBHelper(@Nullable Context context) {
        super(context, DBName, null, 6);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table users(phoneNumber TEXT primary key, name TEXT, surname TEXT, password TEXT, isVerified INTEGER, isSynced INTEGER, verificationCode TEXT, profileImage TEXT, about TEXT)");

        db.execSQL("create table messages(id INTEGER primary key AUTOINCREMENT, sender TEXT NOT NULL, body TEXT NOT NULL, date TEXT NOT NULL, synced INTEGER DEFAULT 0, UNIQUE(sender, body, date))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS messages");
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
        contentValues.put("isSynced", 0);
        contentValues.put("verificationCode", verificationCode);
        long result = myDB.insert("users", null, contentValues);
        if(result == -1) return false;
        else return true;
    }

    public void insertMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sender", message.getSender());
        //values.put("body", encrypt(message.getBody()));
        values.put("body", message.getBody());
        values.put("date", String.valueOf(message.getDate()));
        values.put("synced", 0); // yeni mesaj senkronize edilmedi

        Log.d("DB", "Mesaj veritabanına ekleniyor: " + message.getBody());
        long result = db.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_IGNORE);

        if (result == -1) {
            Log.e("DB", "Mesaj veritabanına EKLENEMEDİ: " + message.getBody());
        } else {
            Log.d("DB", "Mesaj veritabanına eklendi: " + message.getBody());
        }
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

    public boolean checkUser(String phoneNumber, String pw){
        SQLiteDatabase myDB = this.getWritableDatabase();
        Cursor cursor = myDB.rawQuery("select * from users where phoneNumber = ? and password = ?", new String[]{phoneNumber, pw});
        if(cursor.getCount()>0)
            return true;
        else return false;
    }


    public List<UserModel> getUnsyncedUsers() {
        List<UserModel> unsyncedUsers = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE isSynced = 0 AND isVerified = 1", null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                try {
                    String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String surname = cursor.getString(cursor.getColumnIndexOrThrow("surname"));
                    String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));

                    UserModel user = new UserModel(phoneNumber, name, surname, password);
                    unsyncedUsers.add(user);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        return unsyncedUsers;
    }

    public List<Message> getUnsyncedMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                "messages",
                new String[]{"id", "sender", "body", "date"},
                "synced = ?",
                new String[]{"0"},
                null, null, null
        );

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String sender = cursor.getString(cursor.getColumnIndexOrThrow("sender"));
                String encryptedBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                long timestamp = 0;
                try {
                    timestamp = Long.parseLong(dateStr);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                String formattedDate = timestamp > 0 ? sdf.format(new Date(timestamp)) : dateStr;

                // Mesaj oluştur, tarih olarak formatlanmış date verisini veriyoruz
                Message msg = new Message(id, sender, encryptedBody, formattedDate);
                messages.add(msg);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return messages;
    }



    public void markUserAsSynced(String phoneNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isSynced", 1);
        db.update("users", values, "phoneNumber = ?", new String[]{phoneNumber});
        db.close();
    }

    public void markMessageAsSynced(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("synced", 1);

        db.update("messages", values, "id = ?", new String[]{String.valueOf(message.getId())});
    }

    public ProfileModel getUserByPhone() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users LIMIT 1", null);

        ProfileModel user = null;
        if (cursor != null && cursor.moveToFirst()) {
            String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String surname = cursor.getString(cursor.getColumnIndexOrThrow("surname"));
            String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            String about = cursor.getString(cursor.getColumnIndexOrThrow("about"));
            String profileImage = cursor.getString(cursor.getColumnIndexOrThrow("profileImage"));
            user = new ProfileModel(phoneNumber, name, surname, password, about, profileImage);

            cursor.close();
        }
        return user;
    }

    public void updateUserProfile(String phoneNumber, String about, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("about", about);
        values.put("profileImage", imageUri);
        db.update("users", values, "phoneNumber = ?", new String[]{phoneNumber});
        db.close();
    }




}
