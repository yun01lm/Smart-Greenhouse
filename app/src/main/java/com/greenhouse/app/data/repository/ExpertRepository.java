package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * 专家咨询数据仓库
 * <p>负责专家列表、聊天会话、消息收发、快照、数据授权。</p>
 */
public class ExpertRepository extends BaseRepository {

    public void getExperts(String specialty, boolean onlineOnly, Callback<List<ExpertInfo>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<ExpertInfo>>> response =
                        apiService.getExperts(specialty, onlineOnly).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void createConversation(CreateConversationRequest request, Callback<ConversationInfo> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<ConversationInfo>> response =
                        apiService.createConversation(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void getConversations(String status, int page, int size,
                                 Callback<List<ConversationInfo>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<ConversationInfo>>> response =
                        apiService.getConversations(status, page, size).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void getMessages(long conversationId, int page, int size,
                            Callback<List<ChatMessage>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<ChatMessage>>> response =
                        apiService.getMessages(conversationId, page, size).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void sendMessage(SendMessageRequest request, Callback<ChatMessage> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<ChatMessage>> response = apiService.sendMessage(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void sendImageMessage(long conversationId, File imageFile, Callback<ChatMessage> callback) {
        execute(() -> {
            try {
                RequestBody convBody = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(conversationId));
                RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
                MultipartBody.Part part = MultipartBody.Part.createFormData("image", imageFile.getName(), fileBody);
                Response<ApiResponse<ChatMessage>> response =
                        apiService.sendImageMessage(convBody, part).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void sendVideoMessage(long conversationId, File videoFile, Callback<ChatMessage> callback) {
        execute(() -> {
            try {
                RequestBody convBody = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(conversationId));
                RequestBody fileBody = RequestBody.create(MediaType.parse("video/*"), videoFile);
                MultipartBody.Part part = MultipartBody.Part.createFormData("video", videoFile.getName(), fileBody);
                Response<ApiResponse<ChatMessage>> response =
                        apiService.sendVideoMessage(convBody, part).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }
    public void sendSnapshot(SnapshotRequest request, Callback<ChatMessage> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<ChatMessage>> response = apiService.sendSnapshot(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void closeConversation(long conversationId, Callback<Void> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<Void>> response = apiService.closeConversation(conversationId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void getUnreadCount(Callback<UnreadResponse> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<UnreadResponse>> response = apiService.getUnreadCount().execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void requestAuthorization(long userId, long greenhouseId, String reason,
                                     Callback<AuthorizationInfo> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<AuthorizationInfo>> response =
                        apiService.requestAuthorization(
                                new RequestAuthorizationRequest(userId, greenhouseId, reason)).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void getPendingAuthorizations(Callback<List<AuthorizationInfo>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<AuthorizationInfo>>> response =
                        apiService.getPendingAuthorizations().execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void approveAuthorization(long authId, Callback<AuthorizationInfo> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<AuthorizationInfo>> response =
                        apiService.approveAuthorization(authId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void rejectAuthorization(long authId, Callback<Void> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<Void>> response = apiService.rejectAuthorization(authId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void revokeAuthorization(long authId, Callback<Void> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<Void>> response = apiService.revokeAuthorization(authId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void getActiveAuthorizations(Callback<List<AuthorizationInfo>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<AuthorizationInfo>>> response =
                        apiService.getActiveAuthorizations().execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }
}
