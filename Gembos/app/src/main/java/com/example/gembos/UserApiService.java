package com.example.gembos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserApiService {
    @POST("/api/users/sync") // .NET backend endpoint'in bu şekilde olacak
    Call<Void> syncUsers(@Body List<UserModel> users);
}