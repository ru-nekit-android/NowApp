package ru.nekit.android.nowapp.mvvm.viewModel;

import android.provider.Settings;
import android.text.TextUtils;

import com.github.pwittchen.reactivenetwork.library.ConnectivityStatus;
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.squareup.okhttp.OkHttpClient;

import retrofit.JacksonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import ru.nekit.android.nowapp.NowApplication;
import ru.nekit.android.nowapp.network.NowService;
import ru.nekit.android.nowapp.utils.VTAG;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by MacOS on 28.09.15.
 */
public class SplashScreenViewModel {

    private final PublishSubject<Void> mConnectivityProblemPublisher = PublishSubject.create();

    private String deviceToken;

    public void init() {
        ReactiveNetwork reactiveNetwork = new ReactiveNetwork();
        reactiveNetwork.enableInternetCheck();
        reactiveNetwork.observeConnectivity(NowApplication.getInstance())
                .subscribeOn(Schedulers.io())
                /*for test
                .doOnNext(value -> {
                            VTAG.call(value.toString());
                        }
                )*/
                .filter(ConnectivityStatus.isNotEqualTo(ConnectivityStatus.UNKNOWN))
                .filter(ConnectivityStatus.isNotEqualTo(ConnectivityStatus.WIFI_CONNECTED_HAS_INTERNET))
                .filter(ConnectivityStatus.isNotEqualTo(ConnectivityStatus.MOBILE_CONNECTED))
                        //.flatMap(value -> Observable.empty())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> mConnectivityProblemPublisher.onNext(null));

        //get event test code
        /*
        register.subscribeOn(Schedulers.io())
                .flatMap(value -> service.getEvents(value, 12))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    VTAG.call("OK " + result);
                }, throwable -> {
                    VTAG.call("ERROR");
                });
        */

        //NowApplication.getInstance().getEventModel().loadEventsRaw().subscribe(value -> {
        //    VTAG.call("" + value);
        //});

test().subscribeOn(Schedulers.io())

        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(result -> {
            VTAG.call("OK " + result);
        }, throwable -> {
            VTAG.call("ERROR");
        });
    }

    private Observable<String> test() {
        VTAG.call("start:: " + deviceToken);
        Observable<String> register = getService().registerDevice(1, Settings.Secure.getString(NowApplication.getInstance().getContentResolver(),
                Settings.Secure.ANDROID_ID), "android", NowApplication.VERSION)
                //.map(result -> result.data.get("token").asText())
                .doOnNext(value -> {
                    deviceToken = value.toString();
                })
                .map(Object::toString)
                .cache();

        return Observable
                .concat(Observable.just(deviceToken), register)
                .first(value -> !TextUtils.isEmpty(value));
    }

    private NowService getService() {
        OkHttpClient client = new OkHttpClient();

        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://nowapp.ru")
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        return retrofit.create(NowService.class);
    }

    public Observable<Void> onConnectivityProblem() {
        return mConnectivityProblemPublisher.asObservable();
    }

}


