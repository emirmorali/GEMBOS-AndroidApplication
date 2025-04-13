package com.example.gembos;

import android.content.Context;
import android.util.Log;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private Context context;

    public SyncManager(Context context) {
        this.context = context;
    }

    public void sendUnsyncedUsersToServer() {
        DBHelper dbHelper = new DBHelper(context);
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
}
