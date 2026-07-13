package com.greenhouse.app.data.api;

import com.greenhouse.app.data.model.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

/**
 * 智慧大棚 API 接口定义
 * <p>
 * 所有接口与后端 API 文档一一对应。
 * 基础路径: /api/v1/
 * </p>
 */
public interface GreenhouseApiService {

    // ===== C1 用户认证 =====

    @POST("auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @GET("auth/me")
    Call<ApiResponse<UserInfo>> getCurrentUser();

    // ===== C3 大棚管理 =====

    @GET("greenhouses")
    Call<ApiResponse<List<Greenhouse>>> getGreenhouses();

    @GET("greenhouses/{id}")
    Call<ApiResponse<Greenhouse>> getGreenhouse(@Path("id") long id);

    // ===== C5 时序数据 =====

    @GET("sensor/realtime")
    Call<ApiResponse<SensorRealtimeData>> getRealtimeData(@Query("greenhouseId") long greenhouseId);

    @GET("sensor/history")
    Call<ApiResponse<List<SensorDataPoint>>> getHistoryData(
            @Query("greenhouseId") long greenhouseId,
            @Query("sensorType") String sensorType,
            @Query("startTime") long startTime,
            @Query("endTime") long endTime,
            @Query("interval") String interval
    );

    // ===== C6 预警 =====

    @GET("alerts")
    Call<ApiResponse<PageResult<AlertItem>>> getAlerts(
            @Query("greenhouseId") long greenhouseId,
            @Query("page") int page,
            @Query("size") int size
    );

    @PUT("alerts/{id}/read")
    Call<ApiResponse<Void>> markAlertRead(@Path("id") long id);

    @GET("alerts/unread-count")
    Call<ApiResponse<Integer>> getUnreadAlertCount(@Query("greenhouseId") long greenhouseId);

    // ===== C15 健康评分 =====

    @GET("health/score")
    Call<ApiResponse<HealthScoreData>> getHealthScore(@Query("greenhouseId") long greenhouseId);

    @GET("health/history")
    Call<ApiResponse<PageResult<HealthScoreData>>> getHealthHistory(
            @Query("greenhouseId") long greenhouseId,
            @Query("page") int page,
            @Query("size") int size
    );

    // ===== C22 作物生长周期 =====

    @GET("crop-cycles")
    Call<ApiResponse<List<CropCycleData>>> getCropCycles(@Query("greenhouseId") long greenhouseId);
}
