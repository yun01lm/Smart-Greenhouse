package com.greenhouse.app.data.api;

import com.greenhouse.app.data.model.*;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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

    @GET("auth/profile")
    Call<ApiResponse<UserInfo>> getCurrentUser();

    @PUT("auth/password")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequest request);

    // ===== C3 大棚管理 =====

    @GET("greenhouses")
    Call<ApiResponse<List<Greenhouse>>> getGreenhouses();

    @GET("greenhouses/{id}")
    Call<ApiResponse<Greenhouse>> getGreenhouse(@Path("id") long id);

    // ===== C5 时序数据 =====

    @GET("sensors/realtime")
    Call<ApiResponse<SensorRealtimeData>> getRealtimeData(@Query("greenhouseId") long greenhouseId);

    // ===== C6 预警 =====

    @GET("alerts")
    Call<ApiResponse<PageResult<AlertItem>>> getAlerts(
            @Query("greenhouseId") long greenhouseId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("alerts")
    Call<ApiResponse<PageResult<AlertItem>>> getAlertsByLevel(
            @Query("greenhouseId") long greenhouseId,
            @Query("page") int page,
            @Query("size") int size,
            @Query("level") String level
    );

    @PUT("alerts/{id}/read")
    Call<ApiResponse<Void>> markAlertRead(@Path("id") long id);

    @GET("alerts/unread-count")
    Call<ApiResponse<Map<String, Object>>> getUnreadAlertCount(@Query("greenhouseId") long greenhouseId);

    // ===== C21 自定义阈值 =====

    @GET("alerts/thresholds")
    Call<ApiResponse<List<ThresholdItem>>> getThresholds(
            @Query("greenhouseId") long greenhouseId
    );

    @POST("alerts/thresholds")
    Call<ApiResponse<ThresholdItem>> setThreshold(@Body ThresholdItem request);

    @DELETE("alerts/thresholds/{id}")
    Call<ApiResponse<Void>> deleteThreshold(@Path("id") long id);

    // ===== C15 健康评分 =====

    @GET("health/score")
    Call<ApiResponse<HealthScoreData>> getHealthScore(@Query("greenhouseId") long greenhouseId);

    @GET("health/history")
    Call<ApiResponse<PageResult<HealthScoreData>>> getHealthHistory(
            @Query("greenhouseId") long greenhouseId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("health/detail/{id}")
    Call<ApiResponse<HealthScoreData>> getHealthDetail(@Path("id") long id);

    // ===== C22 作物生长周期 =====

    @GET("crop-cycles")
    Call<ApiResponse<List<CropCycleData>>> getCropCycles(@Query("greenhouseId") long greenhouseId);

    // ===== C8 病虫害诊断 =====

    @Multipart
    @POST("diagnosis/recognize")
    Call<ApiResponse<DiagnosisResponse>> diagnose(
            @Part MultipartBody.Part image,
            @Part("greenhouseId") RequestBody greenhouseId
    );

    @GET("diagnosis/records")
    Call<ApiResponse<List<DiagnosisHistoryItem>>> getDiagnosisHistory(
            @Query("page") int page,
            @Query("size") int size
    );

    // ===== C9 AI问答 =====

    @POST("qa/ask")
    Call<ApiResponse<QaResponse>> ask(@Body QaRequest request);

    @Multipart
    @POST("qa/ask/voice")
    Call<ApiResponse<QaResponse>> askVoice(
            @Part MultipartBody.Part audio,
            @Part("greenhouseId") RequestBody greenhouseId
    );

    @GET("qa/records")
    Call<ApiResponse<PageResult<QaHistoryItem>>> getQaHistory(
            @Query("page") int page,
            @Query("size") int size
    );

    // ===== C7 设备控制 =====

    @POST("control/actuator")
    Call<ApiResponse<DeviceControlResult>> controlActuator(@Body ControlRequest request);

    @GET("control/scenes")
    Call<ApiResponse<List<SceneInfo>>> getScenes(
            @Query("greenhouseId") long greenhouseId
    );

    @GET("control/logs")
    Call<ApiResponse<PageResult<ControlLogItem>>> getControlLogs(
            @Query("greenhouseId") long greenhouseId,
            @Query("source") String source,
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("control/scenes")
    Call<ApiResponse<SceneInfo>> createScene(@Query("greenhouseId") long greenhouseId,
                                              @Body CreateSceneRequest request);
    @POST("control/scenes/{id}/execute")
    Call<ApiResponse<List<DeviceControlResult>>> executeScene(@Path("id") long sceneId);

    @GET("greenhouses/{greenhouseId}/devices")
    Call<ApiResponse<List<DeviceInfo>>> getDevices(
            @Path("greenhouseId") long greenhouseId,
            @Query("type") String type
    );

    // ===== C5 历史数据（与后端 POST /api/v1/sensors/history 对齐）=====

    @POST("sensors/history")
    Call<ApiResponse<List<SensorDataPoint>>> getHistory(
            @Query("greenhouseId") long greenhouseId,
            @Body SensorHistoryRequest request
    );

    // ===== F7 长势评估 =====

    @GET("growth/latest")
    Call<ApiResponse<GrowthAssessment>> getLatestGrowthAssessment(
            @Query("greenhouseId") long greenhouseId
    );

    @GET("growth/history")
    Call<ApiResponse<PageResult<GrowthAssessment>>> getGrowthHistory(
            @Query("greenhouseId") long greenhouseId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("growth/images")
    Call<ApiResponse<PageResult<GrowthImage>>> getGrowthImages(
            @Query("greenhouseId") long greenhouseId,
            @Query("date") String date,
            @Query("page") int page,
            @Query("size") int size
    );

    // ===== F10 专家咨询 — 专家列表 =====

    @GET("experts")
    Call<ApiResponse<List<ExpertInfo>>> getExperts(
            @Query("specialty") String specialty,
            @Query("onlineOnly") boolean onlineOnly
    );

    // ===== F10 专家咨询 — 对话管理 =====

    @POST("chat/conversations")
    Call<ApiResponse<ConversationInfo>> createConversation(@Body CreateConversationRequest request);

    @GET("chat/conversations")
    Call<ApiResponse<List<ConversationInfo>>> getConversations(
            @Query("status") String status,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("chat/conversations/{id}/messages")
    Call<ApiResponse<List<ChatMessage>>> getMessages(
            @Path("id") long conversationId,
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("chat/messages")
    Call<ApiResponse<ChatMessage>> sendMessage(@Body SendMessageRequest request);

    @Multipart
    @POST("chat/messages")
    Call<ApiResponse<ChatMessage>> sendImageMessage(
            @Part("conversationId") RequestBody conversationId,
            @Part MultipartBody.Part image
    );

    @Multipart
    @POST("chat/messages")
    Call<ApiResponse<ChatMessage>> sendVideoMessage(
            @Part("conversationId") RequestBody conversationId,
            @Part MultipartBody.Part video
    );

    @POST("chat/snapshot")
    Call<ApiResponse<ChatMessage>> sendSnapshot(@Body SnapshotRequest request);

    @PUT("chat/conversations/{id}/close")
    Call<ApiResponse<Void>> closeConversation(@Path("id") long conversationId);

    @GET("chat/unread")
    Call<ApiResponse<UnreadResponse>> getUnreadCount();

    // ===== F10 专家咨询 — 授权管理 =====

    @POST("expert/authorize/request")
    Call<ApiResponse<AuthorizationInfo>> requestAuthorization(@Body RequestAuthorizationRequest request);

    @GET("expert/authorize/pending")
    Call<ApiResponse<List<AuthorizationInfo>>> getPendingAuthorizations();

    @PUT("expert/authorize/{id}/approve")
    Call<ApiResponse<AuthorizationInfo>> approveAuthorization(@Path("id") long authId);

    @PUT("expert/authorize/{id}/reject")
    Call<ApiResponse<Void>> rejectAuthorization(@Path("id") long authId);

    @PUT("expert/authorize/{id}/revoke")
    Call<ApiResponse<Void>> revokeAuthorization(@Path("id") long authId);

    @GET("expert/authorize/active")
    Call<ApiResponse<List<AuthorizationInfo>>> getActiveAuthorizations();

    // ===== R26 棚主员工管理 =====

    @GET("owner/employees")
    Call<ApiResponse<List<EmployeeItem>>> getEmployees();

    @POST("owner/employees")
    Call<ApiResponse<EmployeePermissionItem>> addEmployee(@Body AddEmployeeRequest request);

    @PUT("owner/employees/{id}/password")
    Call<ApiResponse<Void>> resetEmployeePassword(@Path("id") long employeeId, @Body ResetPasswordRequest request);

    @GET("owner/employees/{id}/permissions")
    Call<ApiResponse<List<EmployeePermissionItem>>> getEmployeePermissions(@Path("id") long employeeId);

    @PUT("owner/employees/{id}/permissions")
    Call<ApiResponse<EmployeePermissionItem>> updateEmployeePermission(
            @Path("id") long employeeId, @Body UpdatePermissionRequest request);

    @DELETE("owner/employees/{id}")
    Call<ApiResponse<Void>> removeEmployee(@Path("id") long employeeId);
}
