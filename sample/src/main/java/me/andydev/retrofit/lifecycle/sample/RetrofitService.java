package me.andydev.retrofit.lifecycle.sample;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Description: Retrofit Initializer
 * Created by Andy on 2017/7/4
 */

public class RetrofitService {
    private Retrofit mRetrofit;

    private RetrofitService() {
        if (mRetrofit == null) {
            mRetrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("http://gank.io/api/")
                    .build();
        }
    }

    public static Retrofit getRetrofit() {
        return RetrofitServiceHolder.INSTANCE.mRetrofit;
    }

    private static class RetrofitServiceHolder {
        private static RetrofitService INSTANCE = new RetrofitService();
    }
}
