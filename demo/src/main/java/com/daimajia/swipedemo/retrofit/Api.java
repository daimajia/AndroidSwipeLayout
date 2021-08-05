package com.daimajia.swipedemo.retrofit;

import com.daimajia.swipedemo.models.StackApiResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface Api {

    @GET("answers")
    Call<StackApiResponse> getAnswers(@Query("page") int page, @Query("pagesize") int pagesize, @Query("site") String site);
}
