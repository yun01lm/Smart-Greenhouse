package com.greenhouse.app.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.AuthorizationInfo;
import com.greenhouse.app.data.model.ChatMessage;
import com.greenhouse.app.data.model.ConversationInfo;
import com.greenhouse.app.data.model.CreateConversationRequest;
import com.greenhouse.app.data.model.ExpertInfo;
import com.greenhouse.app.data.model.PageResult;
import com.greenhouse.app.data.model.SendMessageRequest;
import com.greenhouse.app.data.model.SnapshotRequest;
import com.greenhouse.app.data.model.UnreadResponse;
import com.greenhouse.app.data.repository.GreenhouseRepository;
import com.greenhouse.app.data.websocket.StompClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 专家咨询 ViewModel (F10)
 * <p>
 * 管理专家列表、对话管理、实时聊天、授权管理等全部业务逻辑。
 * 双通道设计：REST API + WebSocket STOMP 实时通信。
 * </p>
 */
public class ExpertViewModel extends ViewModel {

    private final GreenhouseRepository repository;
    private StompClient stompClient;

    // 专家列表
    private final MutableLiveData<List<ExpertInfo>> experts = new MutableLiveData<>(new ArrayList<>());
    // 对话列表
    private final MutableLiveData<List<ConversationInfo>> conversations = new MutableLiveData<>(new ArrayList<>());
    // 当前对话消息
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    // 待处理授权
    private final MutableLiveData<List<AuthorizationInfo>> pendingAuthorizations = new MutableLiveData<>(new ArrayList<>());
    // 有效授权
    private final MutableLiveData<List<AuthorizationInfo>> activeAuthorizations = new MutableLiveData<>(new ArrayList<>());
    // 未读消息数
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    // WebSocket 连接状态
    private final MutableLiveData<Boolean> wsConnected = new MutableLiveData<>(false);
    // 加载状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // 错误信息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long currentConversationId;
    private long currentGreenhouseId;
    private int conversationPage = 1;
    private int messagePage = 1;
    private static final int PAGE_SIZE = 20;
    private static final long POLL_INTERVAL = 3000; // REST 轮询间隔（WebSocket不可用时）

    private Handler pollHandler;
    private Runnable pollRunnable;
    private boolean polling = false;

    public ExpertViewModel() {
        this.repository = new GreenhouseRepository();
        this.pollHandler = new Handler(Looper.getMainLooper());
    }

    // ===== LiveData =====

    public LiveData<List<ExpertInfo>> getExperts() { return experts; }
    public LiveData<List<ConversationInfo>> getConversations() { return conversations; }
    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<List<AuthorizationInfo>> getPendingAuthorizations() { return pendingAuthorizations; }
    public LiveData<List<AuthorizationInfo>> getActiveAuthorizations() { return activeAuthorizations; }
    public LiveData<Integer> getUnreadCount() { return unreadCount; }
    public LiveData<Boolean> getWsConnected() { return wsConnected; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void setCurrentGreenhouseId(long id) { this.currentGreenhouseId = id; }
    public long getCurrentGreenhouseId() { return currentGreenhouseId; }

    // ===== 专家列表 =====

    public void loadExperts() {
        loadExperts(null, false);
    }

    public void loadExperts(String specialty, boolean onlineOnly) {
        isLoading.setValue(true);
        repository.getExperts(specialty, onlineOnly, new GreenhouseRepository.Callback<List<ExpertInfo>>() {
            @Override
            public void onSuccess(List<ExpertInfo> data) {
                isLoading.postValue(false);
                experts.postValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("加载专家列表失败: " + message);
            }
        });
    }

    // ===== 对话管理 =====

    public void createConversation(long expertId, String subject) {
        isLoading.setValue(true);
        CreateConversationRequest request = new CreateConversationRequest(expertId, currentGreenhouseId, subject);
        repository.createConversation(request, new GreenhouseRepository.Callback<ConversationInfo>() {
            @Override
            public void onSuccess(ConversationInfo data) {
                isLoading.postValue(false);
                currentConversationId = data.getId();
                // 刷新对话列表
                refreshConversations();
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("发起求助失败: " + message);
            }
        });
    }

    public void refreshConversations() {
        conversationPage = 1;
        loadConversationsPage();
    }

    private void loadConversationsPage() {
        repository.getConversations(null, conversationPage, PAGE_SIZE,
                new GreenhouseRepository.Callback<PageResult<ConversationInfo>>() {
                    @Override
                    public void onSuccess(PageResult<ConversationInfo> data) {
                        List<ConversationInfo> list = data.getList();
                        if (list == null) list = new ArrayList<>();
                        conversations.postValue(list);
                    }

                    @Override
                    public void onError(String message) {
                        errorMessage.postValue("加载对话列表失败: " + message);
                    }
                });
    }

    // ===== 聊天消息 =====

    public void enterConversation(long conversationId) {
        this.currentConversationId = conversationId;
        messagePage = 1;
        loadMessages();
        // 尝试连接 WebSocket
        connectWebSocket();
    }

    public void loadMessages() {
        if (currentConversationId == 0) return;
        isLoading.setValue(true);
        repository.getMessages(currentConversationId, messagePage, PAGE_SIZE,
                new GreenhouseRepository.Callback<PageResult<ChatMessage>>() {
                    @Override
                    public void onSuccess(PageResult<ChatMessage> data) {
                        isLoading.postValue(false);
                        List<ChatMessage> list = data.getList();
                        if (list == null) list = new ArrayList<>();
                        messages.postValue(list);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("加载消息失败: " + message);
                    }
                });
    }

    /**
     * 发送文字消息（REST + WebSocket 双通道）
     */
    public void sendTextMessage(String content) {
        if (currentConversationId == 0 || content.isEmpty()) return;

        SendMessageRequest request = new SendMessageRequest(currentConversationId, content);

        // REST 方式发送（保证送达）
        repository.sendMessage(request, new GreenhouseRepository.Callback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage data) {
                // 追加到消息列表
                addMessageToList(data);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("发送失败: " + message);
            }
        });

        // WebSocket 方式发送（实时通道，如已连接）
        if (stompClient != null && stompClient.isConnected()) {
            String json = "{\"conversationId\":" + currentConversationId + ",\"messageType\":\"TEXT\",\"content\":\"" + content + "\"}";
            stompClient.send("/app/chat/message", json);
        }
    }

    /**
     * 发送图片消息
     */
    public void sendImageMessage(File imageFile) {
        if (currentConversationId == 0 || imageFile == null) return;
        isLoading.setValue(true);

        repository.sendImageMessage(currentConversationId, imageFile,
                new GreenhouseRepository.Callback<ChatMessage>() {
                    @Override
                    public void onSuccess(ChatMessage data) {
                        isLoading.postValue(false);
                        addMessageToList(data);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("发送图片失败: " + message);
                    }
                });
    }

    /**
     * 发送视频消息
     */
    public void sendVideoMessage(File videoFile) {
        if (currentConversationId == 0 || videoFile == null) return;
        isLoading.setValue(true);

        repository.sendVideoMessage(currentConversationId, videoFile,
                new GreenhouseRepository.Callback<ChatMessage>() {
                    @Override
                    public void onSuccess(ChatMessage data) {
                        isLoading.postValue(false);
                        addMessageToList(data);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("发送视频失败: " + message);
                    }
                });
    }

    /**
     * 发送环境快照
     */
    public void sendSnapshot() {
        if (currentConversationId == 0) return;
        isLoading.setValue(true);

        SnapshotRequest request = new SnapshotRequest(currentConversationId, currentGreenhouseId);
        repository.sendSnapshot(request, new GreenhouseRepository.Callback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage data) {
                isLoading.postValue(false);
                addMessageToList(data);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("发送环境快照失败: " + message);
            }
        });
    }

    // ===== WebSocket 连接 =====

    private void connectWebSocket() {
        // 获取 API 基础 URL 转换为 WS URL
        String baseUrl = com.greenhouse.app.data.api.ApiClient.getBaseUrl();
        if (baseUrl == null) return;

        String wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "ws/connect";
        String token = com.greenhouse.app.data.api.ApiClient.getAuthToken();
        if (token == null || token.isEmpty()) return;

        if (stompClient != null) {
            stompClient.disconnect();
        }

        stompClient = new StompClient(wsUrl, token);
        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                wsConnected.postValue(true);
                // 订阅对话消息通道
                stompClient.subscribe("/topic/chat/" + currentConversationId);
                stompClient.subscribe("/user/queue/chat");
            }

            @Override
            public void onMessage(String destination, String body) {
                // 收到新消息，刷新消息列表
                loadMessages();
            }

            @Override
            public void onError(String message) {
                wsConnected.postValue(false);
                // WebSocket 失败，启动 REST 轮询
                startPolling();
            }

            @Override
            public void onDisconnected() {
                wsConnected.postValue(false);
            }
        });
        stompClient.connect();
    }

    /**
     * WebSocket 不可用时的 REST 轮询
     */
    private void startPolling() {
        if (polling) return;
        polling = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!polling) return;
                loadMessages();
                loadUnreadCount();
                pollHandler.postDelayed(this, POLL_INTERVAL);
            }
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    private void stopPolling() {
        polling = false;
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    // ===== 授权管理 =====

    public void loadPendingAuthorizations() {
        repository.getPendingAuthorizations(new GreenhouseRepository.Callback<List<AuthorizationInfo>>() {
            @Override
            public void onSuccess(List<AuthorizationInfo> data) {
                pendingAuthorizations.postValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("加载待处理授权失败: " + message);
            }
        });
    }

    public void loadActiveAuthorizations() {
        repository.getActiveAuthorizations(new GreenhouseRepository.Callback<List<AuthorizationInfo>>() {
            @Override
            public void onSuccess(List<AuthorizationInfo> data) {
                activeAuthorizations.postValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("加载有效授权失败: " + message);
            }
        });
    }

    public void approveAuthorization(long authId) {
        isLoading.setValue(true);
        repository.approveAuthorization(authId, new GreenhouseRepository.Callback<AuthorizationInfo>() {
            @Override
            public void onSuccess(AuthorizationInfo data) {
                isLoading.postValue(false);
                loadPendingAuthorizations();
                loadActiveAuthorizations();
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("授权失败: " + message);
            }
        });
    }

    public void rejectAuthorization(long authId) {
        repository.rejectAuthorization(authId, new GreenhouseRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadPendingAuthorizations();
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("操作失败: " + message);
            }
        });
    }

    public void revokeAuthorization(long authId) {
        repository.revokeAuthorization(authId, new GreenhouseRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadActiveAuthorizations();
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("撤销失败: " + message);
            }
        });
    }

    // ===== 未读消息 =====

    public void loadUnreadCount() {
        repository.getUnreadCount(new GreenhouseRepository.Callback<UnreadResponse>() {
            @Override
            public void onSuccess(UnreadResponse data) {
                unreadCount.postValue(data.getTotalUnread());
            }

            @Override
            public void onError(String message) {
                // 静默处理
            }
        });
    }

    public void closeConversation() {
        if (currentConversationId == 0) return;
        repository.closeConversation(currentConversationId, new GreenhouseRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                refreshConversations();
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("关闭对话失败: " + message);
            }
        });
    }

    // ===== 辅助 =====

    private void addMessageToList(ChatMessage message) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(message);
        messages.postValue(current);
    }

    public void clearError() {
        errorMessage.postValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling();
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
    }
}
