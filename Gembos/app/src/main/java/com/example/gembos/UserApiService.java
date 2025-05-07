package com.example.gembos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserApiService {
    @POST("/api/User/SyncUser")
    Call<Void> syncUsers(@Body List<UserModel> users);

    @POST("/api/Message/SyncMultipleMessage")
    Call<Void> syncMultipleMessage(@Body Message message);
}