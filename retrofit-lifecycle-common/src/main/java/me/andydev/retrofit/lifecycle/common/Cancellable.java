package me.andydev.retrofit.lifecycle.common;

import retrofit2.Call;

/**
 * Description:
 * Created by Andy on 2017/7/4
 */

public interface Cancellable {
    void cancelAll(Call... excludes);
}
