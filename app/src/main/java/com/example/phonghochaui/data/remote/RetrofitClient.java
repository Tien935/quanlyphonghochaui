package com.example.phonghochaui.data.remote;

import android.content.Context;

import com.example.phonghochaui.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static volatile SupabaseApiService apiService;

    private RetrofitClient() {
        // Không cho phép tạo đối tượng RetrofitClient.
    }

    public static SupabaseApiService getApiService(
            Context context
    ) {
        if (apiService == null) {
            synchronized (RetrofitClient.class) {
                if (apiService == null) {
                    apiService = createApiService(
                            context.getApplicationContext()
                    );
                }
            }
        }

        return apiService;
    }

    private static SupabaseApiService createApiService(
            Context context
    ) {
        if (!SupabaseConfig.isConfigured()) {
            throw new IllegalStateException(
                    "Supabase chưa được cấu hình trong local.properties."
            );
        }

        HttpLoggingInterceptor loggingInterceptor =
                new HttpLoggingInterceptor();

        loggingInterceptor.redactHeader("Authorization");
        loggingInterceptor.redactHeader("apikey");

        loggingInterceptor.setLevel(
                BuildConfig.DEBUG
                        ? HttpLoggingInterceptor.Level.BASIC
                        : HttpLoggingInterceptor.Level.NONE
        );

        SessionManager sessionManager =
                new SessionManager(context);

        OkHttpClient okHttpClient =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                new SupabaseInterceptor(
                                        sessionManager
                                )
                        )
                        .authenticator(
                                new SupabaseAuthenticator(
                                        sessionManager
                                )
                        )
                        .addInterceptor(loggingInterceptor)
                        .build();

        Gson gson = new GsonBuilder().create();

        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(
                                SupabaseConfig.getBaseUrl()
                        )
                        .client(okHttpClient)
                        .addConverterFactory(
                                GsonConverterFactory.create(gson)
                        )
                        .build();

        return retrofit.create(
                SupabaseApiService.class
        );
    }
}