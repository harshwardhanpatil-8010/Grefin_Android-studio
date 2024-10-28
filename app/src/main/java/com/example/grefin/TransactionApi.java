package com.example.grefin;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TransactionApi {
    @POST("api/transactions")
    Call<Void> sendTransaction(@Body Transaction transaction) ;
}
