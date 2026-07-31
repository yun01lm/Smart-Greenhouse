package com.greenhouse.app.data.api;

import com.greenhouse.app.BuildConfig;
import com.greenhouse.app.data.local.TokenManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API 客户端
 * <p>
 * 封装 Retrofit，自动添加 JWT Token 到请求头。
 * 提供 ExecutorService 用于后台任务执行（符合规范：异步使用 Java ExecutorService + Handler）。
 * </p>
 */
public class ApiClient {

    private static final String BASE_URL = BuildConfig.API_BASE_URL + "/api/v1/";

    private static Retrofit retrofit;
    private static GreenhouseApiService apiService;
    private static OkHttpClient okHttpClient;

    /** 后台任务线程池（规范要求：ExecutorService） */
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void init() {
        // 已初始化则跳过（单例模式，连接池复用）
        if (apiService != null) {
            return;
        }
        // 日志拦截器（Debug 模式）
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        // Token 拦截器：自动添加 Authorization 头
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                String token = TokenManager.getToken();

                if (token != null && !token.isEmpty()) {
                    Request authenticated = original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build();
                    return chain.proceed(authenticated);
                }

                return chain.proceed(original);
            }
        };

        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build();
                    .build();
        }

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(GreenhouseApiService.class);
    }

    public static GreenhouseApiService getApiService() {
        if (apiService == null) {
            init();
        }
        return apiService;
    }

    /** 获取后台线程池 */
    public static ExecutorService getExecutor() {
        return executor;
    }

    public static Retrofit getRetrofit() {
        return retrofit;
    }

    /** 获取基础 URL（用于 WebSocket 连接等） */
    public static String getBaseUrl() {
        return BuildConfig.API_BASE_URL;
    }

    /** 获取认证 Token */
    public static String getAuthToken() {
        return TokenManager.getToken();
    }
}
