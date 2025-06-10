package com.example.gembos;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

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

    public void sendUnsyncedMessagesToServer(byte[] localKeyBytes, byte[] masterKeyBytes) {
        Log.d("SYNC", "sendUnsyncedMessagesToServer() çağrıldı");

        List<Message> unsyncedMessages = dbHelper.getUnsyncedMessages();

        if (unsyncedMessages.isEmpty()) {
            Log.d("SYNC", "Gönderilecek mesaj yok.");
            return;
        }


        List<Message> encryptedMessagesForAPI = new ArrayList<>();

        try {
            SecretKeySpec localKeySpec = EncryptionHelper.deriveAESKey(localKeyBytes);
            SecretKeySpec masterKeySpec = EncryptionHelper.deriveAESKey(masterKeyBytes);

            for (Message msg : unsyncedMessages) {
                String decryptedText;

                if (EncryptionHelper.isEncrypted(msg.getBody())) {
                    decryptedText = EncryptionHelper.decrypt(msg.getBody(), localKeySpec);
                } else {
                    decryptedText = msg.getBody();
                }

                String encryptedForAPI = EncryptionHelper.encrypt(decryptedText, masterKeySpec);

                Message encryptedMsg = new Message(msg.getSender(), encryptedForAPI, msg.getDate());
                encryptedMsg.setId(msg.getId());
                encryptedMessagesForAPI.add(encryptedMsg);
            }

        } catch (Exception e) {
            Log.e("SYNC", "Şifreleme sırasında hata oluştu: " + e.getMessage());
            e.printStackTrace();
            return;

        }

        // API gönderimi
        UserApiService apiService = ApiClient.getRetrofit().create(UserApiService.class);
        Call<Void> call = apiService.syncMultipleMessage(encryptedMessagesForAPI);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("SYNC", "Mesajlar başarıyla senkronize edildi.");
                    for (Message msg : encryptedMessagesForAPI) {
                        dbHelper.markMessageAsSynced(msg);
                    }
                } else {
                    Log.e("SYNC", "Mesaj senkronizasyonu başarısız. Sunucu hatası: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SYNC", "Mesaj senkronizasyonunda hata: " + t.getMessage());
            }
        });
    }





}
