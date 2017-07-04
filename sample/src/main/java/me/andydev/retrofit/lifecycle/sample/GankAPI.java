package me.andydev.retrofit.lifecycle.sample;

import me.andydev.retrofit.lifecycle.common.RetrofitInterface;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Description:
 * Created by Andy on 2017/7/4
 */
@RetrofitInterface
public interface GankAPI {

    @GET("data/Android/10/1")
    Call<ResponseBody> getGankList();
}
