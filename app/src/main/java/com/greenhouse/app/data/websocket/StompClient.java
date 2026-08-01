package com.greenhouse.app.data.websocket;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * 轻量 STOMP 协议 WebSocket 客户端 (F10)
 * <p>
 * 基于 OkHttp WebSocket 实现 STOMP over WebSocket 协议，
 * 用于专家咨询模块的实时消息通信。不引入第三方 STOMP 库。
 * </p>
 *
 * <p>使用方式：
 * <pre>
 * StompClient client = new StompClient("ws://host:port/ws/connect", token);
 * client.setListener(new StompClient.StompListener() { ... });
 * client.connect();
 * client.subscribe("/user/queue/chat");
 * client.send("/app/chat/message", jsonBody);
 * client.disconnect();
 * </pre>
 * </p>
 */
public class StompClient {

    private static final String TAG = "StompClient";

    private final String wsUrl;
    private final String authToken;
    private WebSocket webSocket;
    private OkHttpClient httpClient;
    private StompListener listener;
    private final Handler mainHandler;
    private boolean connected = false;

    // 订阅回调映射：destination → callback
    private final Map<String, String> subscriptions = new ConcurrentHashMap<>();

    // 心跳间隔（毫秒）
    private static final long HEARTBEAT_INTERVAL = 10000;
    private Handler heartbeatHandler;
    private HandlerThread heartbeatThread;
    private Runnable heartbeatRunnable;

    public StompClient(String wsUrl, String authToken) {
        this.wsUrl = wsUrl;
        this.authToken = authToken;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * STOMP 事件监听器
     */
    public interface StompListener {
        void onConnected();
        void onMessage(String destination, String body);
        void onError(String message);
        void onDisconnected();
    }

    public void setListener(StompListener listener) {
        this.listener = listener;
    }

    /**
     * 连接到 WebSocket 服务器
     */
    public void connect() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS) // 无限超时
                    .build();
        }

        Request request = new Request.Builder()
                .url(wsUrl)
                .header("Authorization", "Bearer " + authToken)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.d(TAG, "WebSocket opened");
                sendStompConnect();
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                handleStompFrame(text);
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                ws.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                connected = false;
                stopHeartbeat();
                notifyDisconnected();
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure: " + t.getMessage());
                connected = false;
                stopHeartbeat();
                notifyError("WebSocket连接失败: " + t.getMessage());
            }
        });
    }

    /**
     * 发送 STOMP CONNECT 帧
     */
    private void sendStompConnect() {
        StringBuilder frame = new StringBuilder();
        frame.append("CONNECT\n");
        frame.append("accept-version:1.1,1.2\n");
        frame.append("heart-beat:10000,10000\n");
        frame.append("login:").append(authToken).append("\n");
        frame.append("\n\0");

        webSocket.send(frame.toString());
    }

    /**
     * 订阅目标地址
     */
    public void subscribe(String destination) {
        String subId = "sub-" + subscriptions.size();
        subscriptions.put(destination, subId);

        StringBuilder frame = new StringBuilder();
        frame.append("SUBSCRIBE\n");
        frame.append("id:").append(subId).append("\n");
        frame.append("destination:").append(destination).append("\n");
        frame.append("\n\0");

        if (webSocket != null) {
            webSocket.send(frame.toString());
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String destination) {
        String subId = subscriptions.remove(destination);
        if (subId == null) return;

        StringBuilder frame = new StringBuilder();
        frame.append("UNSUBSCRIBE\n");
        frame.append("id:").append(subId).append("\n");
        frame.append("\n\0");

        if (webSocket != null) {
            webSocket.send(frame.toString());
        }
    }

    /**
     * 发送消息到指定目标
     */
    public void send(String destination, String body) {
        if (webSocket == null || !connected) return;

        StringBuilder frame = new StringBuilder();
        frame.append("SEND\n");
        frame.append("destination:").append(destination).append("\n");
        frame.append("content-type:application/json\n");
        frame.append("content-length:").append(body.length()).append("\n");
        frame.append("\n");
        frame.append(body);
        frame.append("\0");

        webSocket.send(frame.toString());
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        stopHeartbeat();

        if (webSocket != null) {
            // 发送 DISCONNECT 帧
            StringBuilder frame = new StringBuilder();
            frame.append("DISCONNECT\n");
            frame.append("receipt:disconnect-").append(UUID.randomUUID().toString().substring(0, 8)).append("\n");
            frame.append("\n\0");
            webSocket.send(frame.toString());

            webSocket.close(1000, "User disconnect");
            webSocket = null;
        }
        subscriptions.clear();
        connected = false;
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return connected;
    }

    // ===== 帧解析 =====

    private void handleStompFrame(String rawFrame) {
        try {
            if (rawFrame.startsWith("CONNECTED")) {
                connected = true;
                startHeartbeat();
                notifyConnected();
                return;
            }

            if (rawFrame.startsWith("MESSAGE")) {
                String[] lines = rawFrame.split("\n");
                String destination = null;
                int bodyStart = -1;

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.startsWith("destination:")) {
                        destination = line.substring("destination:".length()).trim();
                    }
                    if (line.isEmpty() && i + 1 < lines.length) {
                        bodyStart = i + 1;
                        break;
                    }
                }

                if (destination != null && bodyStart > 0 && bodyStart < lines.length) {
                    StringBuilder body = new StringBuilder();
                    for (int i = bodyStart; i < lines.length; i++) {
                        String line = lines[i];
                        if (line.endsWith("\0")) {
                            body.append(line, 0, line.length() - 1);
                            break;
                        }
                        body.append(line);
                    }
                    notifyMessage(destination, body.toString());
                }
                return;
            }

            if (rawFrame.startsWith("ERROR")) {
                notifyError("STOMP错误: " + rawFrame);
                return;
            }

            if (rawFrame.startsWith("RECEIPT")) {
                Log.d(TAG, "STOMP RECEIPT received");
            }

        } catch (Exception e) {
            Log.e(TAG, "STOMP frame parse error: " + e.getMessage());
        }
    }

    // ===== 心跳 =====

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatThread = new HandlerThread("stomp-heartbeat");
        heartbeatThread.start();
        heartbeatHandler = new Handler(heartbeatThread.getLooper());
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (webSocket != null && connected) {
                    webSocket.send("\n");
                    heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
                }
            }
        };
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL);
    }

    private void stopHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatHandler = null;
            heartbeatRunnable = null;
        }
    }

    // ===== 回调通知 =====

    private void notifyConnected() {
        if (listener != null) {
            mainHandler.post(() -> listener.onConnected());
        }
    }

    private void notifyMessage(String destination, String body) {
        if (listener != null) {
            mainHandler.post(() -> listener.onMessage(destination, body));
        }
    }

    private void notifyError(String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onError(message));
        }
    }

    private void notifyDisconnected() {
        if (listener != null) {
            mainHandler.post(() -> listener.onDisconnected());
        }
    }
}
