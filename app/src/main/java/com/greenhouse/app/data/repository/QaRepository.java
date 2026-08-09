package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.*;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * AI问答数据仓库
 * <p>负责文字问答、语音问答、问答历史。</p>
 */
public class QaRepository extends BaseRepository {

    public void ask(String question, long greenhouseId, Callback<QaResponse> callback) {
        execute(() -> {
            try {
                QaRequest request = new QaRequest(question, greenhouseId);
                Response<ApiResponse<QaResponse>> response = apiService.ask(request).execute();
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

    public void askVoice(File audioFile, long greenhouseId, Callback<QaResponse> callback) {
        execute(() -> {
            try {
                RequestBody requestFile = RequestBody.create(audioFile, MediaType.parse("audio/*"));
                MultipartBody.Part body = MultipartBody.Part.createFormData("audio", audioFile.getName(), requestFile);
                RequestBody ghIdPart = RequestBody.create(String.valueOf(greenhouseId),
                        MediaType.parse("text/plain"));
                Response<ApiResponse<QaResponse>> response = apiService.askVoice(body, ghIdPart).execute();
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

    public void getQaHistory(int page, int size, Callback<PageResult<QaHistoryItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<QaHistoryItem>>> response =
                        apiService.getQaHistory(page, size).execute();
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
