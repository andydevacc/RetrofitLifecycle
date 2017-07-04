package me.andydev.retrofit.lifecycle.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private GankAPI mGankAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGankAPI = RetrofitHelper.get(GankAPI.class);
        mGankAPI.getGankList().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse() called with: call = [" + call + "], response = [" + response + "]");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure() called with: call = [" + call + "], t = [" + t + "]");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RetrofitHelper.cancel(mGankAPI);
    }
}
