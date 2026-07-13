package com.greenhouse.app.websocket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.greenhouse.app.BuildConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * STOMP over WebSocket 客户端
 * <p>
 * 使用 OkHttp WebSocket 实现 STOMP 1.2 协议。
 * 连接后端 WebSocket 端点，订阅主题，接收实时推送。
 * 符合开发规范：WebSocket 使用 STOMP over OkHttp。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * StompClient client = new StompClient();
 * client.connect(token, () -> {
 *     client.subscribe("/topic/greenhouse/1/realtime", message -> {
 *         // 处理实时数据
 *     });
 * });
 * </pre>
 */
public class StompClient {

    private static final String TAG = "StompClient";
    private static final String WS_URL = BuildConfig.WS_URL + "/websocket";

    private WebSocket webSocket;
    private OkHttpClient httpClient;
    private Handler mainHandler;
    private String authToken;

    // 订阅回调映射: destination -> callback
    private final Map<String, StompCallback> subscriptions = new ConcurrentHashMap<>();

    // 连接回调
    private StompCallback connectCallback;
    private StompCallback disconnectCallback;

    // STOMP 心跳
    private static final int HEARTBEAT_INTERVAL_MS = 10000;
    private final Runnable heartbeatTask = this::sendHeartbeat;
    private boolean connected = false;

    /**
     * 连接到 WebSocket 服务器
     *
     * @param token    JWT Token（用于 STOMP CONNECT 认证）
     * @param onReady  连接成功回调
     */
    public void connect(String token, StompCallback onReady) {
        this.authToken = token;
        this.mainHandler = new Handler(Looper.getMainLooper());

        httpClient = new OkHttpClient.Builder()
                .pingInterval(15, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(WS_URL)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.d(TAG, "WebSocket 已连接, 发送 STOMP CONNECT 帧");
                sendStompConnect();
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                handleStompFrame(text);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e(TAG, "WebSocket 连接失败: " + t.getMessage());
                connected = false;
                mainHandler.removeCallbacks(heartbeatTask);
                if (disconnectCallback != null) {
                    mainHandler.post(() -> disconnectCallback.onMessage("连接断开"));
                }
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                Log.d(TAG, "WebSocket 已关闭: " + reason);
                connected = false;
                mainHandler.removeCallbacks(heartbeatTask);
            }
        });
    }

    /**
     * 订阅主题
     *
     * @param destination 目标路径，如 /topic/greenhouse/1/realtime
     * @param callback    消息回调
     */
    public void subscribe(String destination, StompCallback callback) {
        subscriptions.put(destination, callback);

        String frame = "SUBSCRIBE\n" +
                "id:sub-" + destination.hashCode() + "\n" +
                "destination:" + destination + "\n" +
                "ack:auto\n" +
                "\n\u0000";

        sendFrame(frame);
        Log.d(TAG, "STOMP SUBSCRIBE: " + destination);
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String destination) {
        subscriptions.remove(destination);

        String frame = "UNSUBSCRIBE\n" +
                "id:sub-" + destination.hashCode() + "\n" +
                "\n\u0000";

        sendFrame(frame);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        connected = false;
        mainHandler.removeCallbacks(heartbeatTask);

        String frame = "DISCONNECT\n" +
                "receipt:disconnect-" + System.currentTimeMillis() + "\n" +
                "\n\u0000";

        sendFrame(frame);

        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
        }
    }

    /**
     * 设置断开连接回调
     */
    public void setOnDisconnect(StompCallback callback) {
        this.disconnectCallback = callback;
    }

    public boolean isConnected() {
        return connected;
    }

    // ===== 内部方法 =====

    private void sendStompConnect() {
        String frame = "CONNECT\n" +
                "accept-version:1.2\n" +
                "heart-beat:10000,10000\n" +
                "Authorization:Bearer " + authToken + "\n" +
                "\n\u0000";

        sendFrame(frame);
    }

    private void sendHeartbeat() {
        if (connected && webSocket != null) {
            webSocket.send("\n");
            mainHandler.postDelayed(heartbeatTask, HEARTBEAT_INTERVAL_MS);
        }
    }

    private void sendFrame(String frame) {
        if (webSocket != null) {
            webSocket.send(frame);
        }
    }

    /**
     * 处理 STOMP 帧
     */
    private void handleStompFrame(String frame) {
        Log.v(TAG, "收到 STOMP 帧: " + frame.substring(0, Math.min(200, frame.length())));

        if (frame.startsWith("CONNECTED")) {
            connected = true;
            mainHandler.postDelayed(heartbeatTask, HEARTBEAT_INTERVAL_MS);
            Log.d(TAG, "STOMP 连接成功");
            if (connectCallback != null) {
                mainHandler.post(() -> connectCallback.onMessage("connected"));
            }
            return;
        }

        if (frame.startsWith("MESSAGE")) {
            // 解析 destination 和 body
            String destination = extractHeader(frame, "destination");
            String body = extractBody(frame);

            StompCallback callback = subscriptions.get(destination);
            if (callback != null && body != null) {
                mainHandler.post(() -> callback.onMessage(body));
            }
            return;
        }

        if (frame.startsWith("ERROR")) {
            Log.e(TAG, "STOMP ERROR: " + frame);
            return;
        }

        // RECEIPT, HEARTBEAT 忽略
    }

    private String extractHeader(String frame, String headerName) {
        String marker = headerName + ":";
        int start = frame.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        int end = frame.indexOf('\n', start);
        return end == -1 ? frame.substring(start) : frame.substring(start, end).trim();
    }

    private String extractBody(String frame) {
        int bodyStart = frame.indexOf("\n\n");
        if (bodyStart == -1) return null;
        String body = frame.substring(bodyStart + 2);
        // 移除 STOMP 结尾的 \0
        if (body.endsWith("\u0000")) {
            body = body.substring(0, body.length() - 1);
        }
        return body.trim();
    }

    /**
     * STOMP 消息回调接口
     */
    public interface StompCallback {
        void onMessage(String message);
    }
}
