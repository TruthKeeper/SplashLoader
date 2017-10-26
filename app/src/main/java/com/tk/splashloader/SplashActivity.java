package com.tk.splashloader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class SplashActivity extends AppCompatActivity {
    private AppCompatButton btnSkip;

    private SplashLoader loader;
    private int delay = 5;
    private Disposable timeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        btnSkip = (AppCompatButton) findViewById(R.id.btn_skip);
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loader.doSkip();
            }
        });
        init();
        start();
    }

    private void start() {
        //倒计时
        Observable.intervalRange(0, delay + 1, 0, 1_000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        timeDisposable = d;
                    }

                    @Override
                    public void onNext(Long value) {
                        btnSkip.setText("点击跳过广告 " + (delay - value) + "s");
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                    }
                });
        //加载
        loader.process(new Action() {
            @Override
            public void run() throws Exception {
                //进入主页面
                Log.e("SplashLoader", "onComplete：" + System.currentTimeMillis());
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e("SplashLoader", "onError：" + System.currentTimeMillis());
                Toast.makeText(SplashActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void init() {
        Observable<Object> localConfig = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                Thread.sleep(500);
//                e.onError(new NullPointerException("本地配置异常"));
                e.onNext(new Object());
                e.onComplete();
            }
        }).map(new Function<Object, Object>() {
            @Override
            public Object apply(Object result) throws Exception {
                Log.e("local", "本地配置加载完毕：" + System.currentTimeMillis());
                return result;
            }
        }).subscribeOn(Schedulers.io());

        Observable<Object> necessaryNetworkConfig = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                Thread.sleep(2_000);
//                e.onError(new NullPointerException("必要网络异常"));
                e.onNext(new Object());
                e.onComplete();
            }
        }).map(new Function<Object, Object>() {
            @Override
            public Object apply(Object result) throws Exception {
                Log.e("necessaryNetwork", "必要网络配置加载完毕：" + System.currentTimeMillis());
                return result;
            }
        }).subscribeOn(Schedulers.io());

        Observable<Object> unnecessaryNetworkConfig = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                Thread.sleep(8_000);
//                e.onError(new NullPointerException("非必要网络异常"));
                e.onNext(new Object());
                e.onComplete();
            }
        }).map(new Function<Object, Object>() {
            @Override
            public Object apply(Object result) throws Exception {
                Log.e("unnecessaryNetwork", "非必要网络配置加载完毕：" + System.currentTimeMillis());
                return result;
            }
        }).subscribeOn(Schedulers.io());

        Observable<Object> offlineConfig = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                Thread.sleep(1_000);
                e.onNext(new Object());
                e.onComplete();
            }
        }).map(new Function<Object, Object>() {
            @Override
            public Object apply(Object result) throws Exception {
                Log.e("offline", "离线配置加载完毕：" + System.currentTimeMillis());
                return result;
            }
        }).subscribeOn(Schedulers.io());

        loader = new SplashLoader.Builder()
                .delayMilli(delay * 1000)
                .localConfig(localConfig)
                .necessaryNetworkConfig(necessaryNetworkConfig)
                .unnecessaryNetworkConfig(unnecessaryNetworkConfig)
                .offlineConfig(offlineConfig)
                .build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //可用Rx Lifecycle优化
        if (timeDisposable != null && !timeDisposable.isDisposed()) {
            timeDisposable.dispose();
        }
        //回收
        loader.recycle();
    }
}
