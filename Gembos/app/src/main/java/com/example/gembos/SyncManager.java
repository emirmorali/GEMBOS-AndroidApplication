package com.example.gembos;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private Context context;
    private final DBHelper dbHelper;

    public SyncManager(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public void sendUnsyncedUsersToServer() {
        List<UserModel> unsyncedUsers = dbHelper.getUnsyncedUsers();

        if (unsyncedUsers.isEmpty()) {
            Log.d("SYNC", "Gönderilecek kullanıcı yok.");
            return;
        }


        UserApiService apiService = ApiClient.getRetrofit().create(UserApiService.class);
        Call<Void> call = apiService.syncUsers(unsyncedUsers);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("SYNC", "Kullanıcılar başarıyla senkronize edildi.");
                    for (UserModel user : unsyncedUsers) {
                        dbHelper.markUserAsSynced(user.getPhoneNumber());
                    }
                } else {
                    Log.e("SYNC", "Sunucu hatası: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SYNC", "Hata: " + t.getMessage());
            }
        });
    }

    public void sendUnsyncedMessagesToServer() {
        Log.d("SYNC", "sendUnsyncedMessagesToServer() çağrıldı");
        List<Message> unsyncedMessages = dbHelper.getUnsyncedMessages();

        if (unsyncedMessages.isEmpty()) {
            Log.d("SYNC", "Gönderilecek mesaj yok.");
            return;
        }

        // Mesajları yeniden şifrele
        List<Message> encryptedMessages = new ArrayList<>();
        for (Message msg : unsyncedMessages) {
            //String encryptedBody = EncryptionHelper.encrypt(msg.getBody());
            Message encryptedMsg = new Message(msg.getSender(), msg.getBody(), msg.getDate());
            encryptedMsg.setId(msg.getId()); // ID'yi de koru
            encryptedMessages.add(encryptedMsg);
        }

        UserApiService apiService = ApiClient.getRetrofit().create(UserApiService.class);

        // Mesajları API'ye gönder
        Call<Void> call = apiService.syncMultipleMessage(encryptedMessages);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("SYNC", "Tüm mesajlar senkronize edildi.");
                    // Başarıyla gönderilen mesajları senkronize olarak işaretle
                    for (Message msg : unsyncedMessages) {
                        dbHelper.markMessageAsSynced(msg); // Veritabanında 'synced' olarak işaretle
                    }
                } else {
                    Log.e("SYNC", "Mesaj senkronizasyonu başarısız. Kod: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SYNC", "Mesaj senkronizasyonu sırasında hata oluştu: " + t.getMessage());
                t.printStackTrace();  // Hatanın detaylarını logla
            }
        });
    }



}
