package com.tk.splashloader;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Timed;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * <pre>
 *      author : TK
 *      time : 2017/10/26
 *      desc :
 * </pre>
 */

public class SplashLoader {
    private final Subject<Boolean> skipSubject = BehaviorSubject.create();
    private final static Object OBJ = new Object();
    /**
     * 本地配置
     */
    private Observable<Object> localConfig;
    /**
     * 有网络时的必要配置
     */
    private Observable<Object> necessaryNetworkConfig;
    /**
     * 有网络时的不必要配置
     */
    private Observable<Object> unnecessaryNetworkConfig;
    /**
     * 无网络时的配置
     */
    private Observable<Object> offlineConfig;
    /**
     * 延迟时长
     */
    private long delayMilli;
    /**
     * 跳过
     */
    private boolean doSkip;
    /**
     * 桥梁
     */
    private Disposable processDisposable;

    private SplashLoader(Builder builder) {
        localConfig = builder.localConfig;
        necessaryNetworkConfig = builder.necessaryNetworkConfig;
        unnecessaryNetworkConfig = builder.unnecessaryNetworkConfig;
        offlineConfig = builder.offlineConfig;
        delayMilli = builder.delayMilli;
    }

    /**
     * 开始执行
     *
     * @param onComplete
     * @param onError
     */
    public void process(@NonNull final Action onComplete, @NonNull final Consumer<? super Throwable> onError) {
        if (localConfig == null) {
            localConfig = Observable.just(OBJ);
        }
        if (necessaryNetworkConfig == null) {
            necessaryNetworkConfig = Observable.just(OBJ);
        }
        if (offlineConfig == null) {
            offlineConfig = Observable.just(OBJ);
        }
        localConfig
                //记录开始的时间
                .timestamp()
                .flatMap(new Function<Timed<Object>, ObservableSource<Timed<Object>>>() {
                    @Override
                    public ObservableSource<Timed<Object>> apply(final Timed<Object> timedResult) throws Exception {
                        if (NetworkUtils.isConnected()) {
                            //有网络时
                            if (unnecessaryNetworkConfig != null) {
                                //直接订阅非必要的网络请求，内部请用全局Application
                                unnecessaryNetworkConfig.subscribe();
                            }
                            return delayAndResponseSkip(timedWrapper(necessaryNetworkConfig, timedResult), delayMilli, skipSubject);
                        } else {
                            //无网络时
                            return delayAndResponseSkip(timedWrapper(offlineConfig, timedResult), delayMilli, skipSubject);
                        }
                    }
                })
                .map(new Function<Timed<Object>, Object>() {
                    @Override
                    public Object apply(Timed<Object> timedResult) throws Exception {
                        return timedResult.value();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {

                    }
                }, onError, onComplete, new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        processDisposable = disposable;
                    }
                });
    }

    /**
     * 外部跳过
     */
    public void doSkip() {
        if (!doSkip) {
            doSkip = true;
            skipSubject.onNext(true);
        }
    }

    /**
     * 界面onDestory时回收
     */
    public void recycle() {
        if (processDisposable != null && !processDisposable.isDisposed()) {
            //SplashLoader不支持Rx Lifecycle，因为会回调onComplete事件干扰结果
            //非必要网络请求SplashLoader中直接做了订阅处理，因此生命周期不会被结束
            processDisposable.dispose();
        }
    }

    /**
     * 操作后延迟、并且延迟期间可被跳过
     *
     * @param observable
     * @param delayMilli
     * @param skipObservable
     * @param <T>
     * @return
     */
    private static <T> Observable<Timed<T>> delayAndResponseSkip(final Observable<Timed<T>> observable, final long delayMilli, final Observable<?> skipObservable) {
        return observable.flatMap(new Function<Timed<T>, ObservableSource<Timed<T>>>() {
            @Override
            public ObservableSource<Timed<T>> apply(Timed<T> tTimed) throws Exception {
                return Observable.just(tTimed)
                        .delay(new Function<Timed<T>, ObservableSource<T>>() {
                            @Override
                            public ObservableSource<T> apply(Timed<T> tTimed) throws Exception {
                                //获取到消费的时长
                                long consume = System.currentTimeMillis() - tTimed.time();
                                Observable<T> result = Observable.just(tTimed.value());
                                if (consume >= delayMilli) {
                                    //耗时超过delayMilli
                                    return result;
                                }
                                return result.delay(delayMilli - consume, TimeUnit.MILLISECONDS);
                            }
                        }).takeUntil(skipObservable);
            }
        });
    }

    /**
     * 修饰时间
     *
     * @param observable
     * @param timed
     * @param <T>
     * @return
     */
    private static <T> Observable<Timed<T>> timedWrapper(final Observable<T> observable, final Timed<T> timed) {
        return observable.map(new Function<T, Timed<T>>() {
            @Override
            public Timed<T> apply(T t) throws Exception {
                return new Timed<T>(t, timed.time(), timed.unit());
            }
        });
    }

    public static final class Builder {
        private Observable<Object> localConfig;
        private Observable<Object> necessaryNetworkConfig;
        private Observable<Object> unnecessaryNetworkConfig;
        private Observable<Object> offlineConfig;
        private long delayMilli;

        public Builder() {
        }

        /**
         * 本地配置
         *
         * @param localConfig
         * @return
         */
        public Builder localConfig(@Nullable Observable<Object> localConfig) {
            this.localConfig = localConfig;
            return this;
        }

        /**
         * 必要网络请求
         *
         * @param necessaryNetworkConfig
         * @return
         */
        public Builder necessaryNetworkConfig(@Nullable Observable<Object> necessaryNetworkConfig) {
            this.necessaryNetworkConfig = necessaryNetworkConfig;
            return this;
        }

        /**
         * 非必要网络请求
         *
         * @param unnecessaryNetworkConfig
         * @return
         */
        public Builder unnecessaryNetworkConfig(@Nullable Observable<Object> unnecessaryNetworkConfig) {
            this.unnecessaryNetworkConfig = unnecessaryNetworkConfig;
            return this;
        }

        /**
         * 离线配置
         *
         * @param offlineConfig
         * @return
         */
        public Builder offlineConfig(@Nullable Observable<Object> offlineConfig) {
            this.offlineConfig = offlineConfig;
            return this;
        }

        /**
         * 界面延迟时长
         *
         * @param delayMilli
         * @return
         */
        public Builder delayMilli(long delayMilli) {
            this.delayMilli = delayMilli;
            return this;
        }

        public SplashLoader build() {
            return new SplashLoader(this);
        }
    }
}
