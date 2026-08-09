package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.*;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * 病虫害诊断数据仓库
 * <p>负责图像上传诊断、诊断历史查询。</p>
 */
public class DiagnosisRepository extends BaseRepository {

    public void diagnose(File imageFile, long greenhouseId, Callback<DiagnosisResponse> callback) {
        execute(() -> {
            try {
                RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/*"));
                MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);
                RequestBody ghIdPart = RequestBody.create(String.valueOf(greenhouseId),
                        MediaType.parse("text/plain"));
                Response<ApiResponse<DiagnosisResponse>> response =
                        apiService.diagnose(body, ghIdPart).execute();
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

    public void getDiagnosisHistory(long greenhouseId, int page, int size,
                                    Callback<PageResult<DiagnosisHistoryItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<DiagnosisHistoryItem>>> response =
                        apiService.getDiagnosisHistory(greenhouseId, page, size).execute();
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
