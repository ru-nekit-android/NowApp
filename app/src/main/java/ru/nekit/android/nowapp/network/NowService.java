package ru.nekit.android.nowapp.network;

import com.fasterxml.jackson.databind.node.ObjectNode;

import retrofit.http.GET;
import retrofit.http.Query;
import ru.nekit.android.nowapp.mvvm.ApiCallResult;
import ru.nekit.android.nowapp.mvvm.EventListApiCallResult;
import rx.Observable;

/**
 * Created by MacOS on 29.09.15.
 */


public interface NowService {

    @GET("api/v2/device/register")
    Observable<ObjectNode> registerDevice(@Query("city_id") int cityId, @Query("device_id") String deviceId, @Query("platform") String platform, @Query("version") int version);

    @GET("api/v2/events")
    Observable<EventListApiCallResult> getEvents(@Query("token") String token, @Query("n") int n);

    @GET("api/v2/events")
    Observable<Object> getEventsRaw(@Query("token") String token, @Query("n") int n);

    @GET("api/v2/events")
    Observable<ApiCallResult> getNewEvents(@Query("token") String token, @Query("start_at") long startAt, @Query("n") int n);


}
