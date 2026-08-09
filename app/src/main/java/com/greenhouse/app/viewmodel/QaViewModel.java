package com.greenhouse.app.viewmodel;

import android.speech.tts.TextToSpeech;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.QaHistoryItem;
import com.greenhouse.app.data.model.QaResponse;
import com.greenhouse.app.data.model.PageResult;
import com.greenhouse.app.data.repository.QaRepository;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI 智能问答业务逻辑
 * <p>
 * 符合规范：ViewModel 不持有 Context，TTS 由 Fragment 注入。
 * 历史记录与 Web 端一致：默认加载最近 30 条；时间显示规则
 * （今天 HH:mm、今年 MM-DD HH:mm、往年 YYYY-MM-DD HH:mm）。
 * </p>
 */
public class QaViewModel extends ViewModel {

    private final QaRepository repository;

    // 当前对话消息列表（用于聊天气泡展示）
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    // 问答历史列表
    private final MutableLiveData<List<QaHistoryItem>> historyList = new MutableLiveData<>();
    // 加载状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // 错误信息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    // TTS 播放状态
    private final MutableLiveData<Boolean> isSpeaking = new MutableLiveData<>(false);

    private long currentGreenhouseId;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 30;   // 与 Web 端一致：默认加载最近 30 条

    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public QaViewModel() {
        this.repository = new QaRepository();
    }

    // ===== LiveData =====

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<List<QaHistoryItem>> getHistoryList() { return historyList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsSpeaking() { return isSpeaking; }

    public void setCurrentGreenhouseId(long id) { this.currentGreenhouseId = id; }
    public long getCurrentGreenhouseId() { return currentGreenhouseId; }

    // ===== 历史记录（与 Web 端一致） =====

    /**
     * 加载最近 30 条历史并恢复为聊天气泡
     */
    public void loadHistoryIntoMessages() {
        repository.getQaHistory(1, PAGE_SIZE, new QaRepository.Callback<PageResult<QaHistoryItem>>() {
            @Override
            public void onSuccess(PageResult<QaHistoryItem> data) {
                List<QaHistoryItem> list = data.getList();
                List<ChatMessage> msgs = new ArrayList<>();
                if (list != null) {
                    for (QaHistoryItem item : list) {
                        msgs.add(ChatMessage.userFromHistory(item));
                        if (item.getAnswer() != null && !item.getAnswer().isEmpty()) {
                            msgs.add(ChatMessage.aiFromHistory(item));
                        }
                    }
                }
                messages.postValue(msgs);
                historyList.postValue(list);
            }

            @Override
            public void onError(String message) {
                // 历史加载失败不阻塞问答，仅提示
                errorMessage.postValue("历史记录加载失败: " + message);
            }
        });
    }

    // ===== 文字问答 =====

    /**
     * 发送文字问题
     */
    public void askQuestion(String question) {
        if (question == null || question.trim().isEmpty()) return;

        // 添加用户消息到列表
        List<ChatMessage> list = messages.getValue();
        if (list == null) list = new ArrayList<>();
        list.add(ChatMessage.user(question));
        messages.postValue(list);

        isLoading.setValue(true);
        repository.ask(question.trim(), currentGreenhouseId, new QaRepository.Callback<QaResponse>() {
            @Override
            public void onSuccess(QaResponse data) {
                isLoading.postValue(false);
                List<ChatMessage> msgs = messages.getValue();
                if (msgs == null) msgs = new ArrayList<>();
                msgs.add(ChatMessage.ai(data));
                messages.postValue(msgs);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                List<ChatMessage> msgs = messages.getValue();
                if (msgs == null) msgs = new ArrayList<>();
                msgs.add(ChatMessage.error(message));
                messages.postValue(msgs);
            }
        });
    }

    // ===== 语音问答 =====

    /**
     * 发送语音问题
     */
    public void askVoice(File audioFile) {
        isLoading.setValue(true);
        repository.askVoice(audioFile, currentGreenhouseId, new QaRepository.Callback<QaResponse>() {
            @Override
            public void onSuccess(QaResponse data) {
                isLoading.postValue(false);
                List<ChatMessage> msgs = messages.getValue();
                if (msgs == null) msgs = new ArrayList<>();
                // 显示语音转文字结果作为用户消息
                String question = data.getRawDialectText() != null
                        ? data.getRawDialectText() : data.getQuestion();
                msgs.add(ChatMessage.userVoice(question));
                msgs.add(ChatMessage.ai(data));
                messages.postValue(msgs);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // ===== 问答历史（分页，保留给后续扩展） =====

    public void loadHistory() {
        currentPage = 1;
        loadHistoryPage(currentPage);
    }

    public void loadMoreHistory() {
        currentPage++;
        loadHistoryPage(currentPage);
    }

    private void loadHistoryPage(int page) {
        isLoading.setValue(true);
        repository.getQaHistory(page, PAGE_SIZE, new QaRepository.Callback<PageResult<QaHistoryItem>>() {
            @Override
            public void onSuccess(PageResult<QaHistoryItem> data) {
                isLoading.postValue(false);
                List<QaHistoryItem> list = historyList.getValue();
                if (list == null || page == 1) list = new ArrayList<>();
                if (data.getList() != null) list.addAll(data.getList());
                historyList.postValue(list);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                if (page > 1) currentPage--;
                errorMessage.postValue(message);
            }
        });
    }

    // ===== 时间格式化（与 Web 端 formatTime 一致） =====

    /**
     * 今天只显示 HH:mm；今年显示 MM-DD HH:mm；往年显示 YYYY-MM-DD HH:mm
     */
    public static String formatMessageTime(String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(createdAt, ISO_DT);
            LocalDateTime now = LocalDateTime.now();
            String hm = String.format(Locale.CHINA, "%02d:%02d", dt.getHour(), dt.getMinute());
            if (dt.toLocalDate().equals(LocalDate.now())) return hm;
            String md = String.format(Locale.CHINA, "%02d-%02d", dt.getMonthValue(), dt.getDayOfMonth());
            if (dt.getYear() == now.getYear()) return md + " " + hm;
            return dt.getYear() + "-" + md + " " + hm;
        } catch (Exception e) {
            return "";
        }
    }

    // ===== TTS =====

    private TextToSpeech tts;

    public void setTts(TextToSpeech tts) {
        this.tts = tts;
    }

    public void speakAnswer(String text) {
        if (tts == null || text == null) return;
        isSpeaking.setValue(true);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "qa_tts_" + System.currentTimeMillis());
    }

    public void stopSpeaking() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
        isSpeaking.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (tts != null) {
            tts.shutdown();
        }
    }

    // ===== 聊天气泡数据模型 =====

    /**
     * 聊天消息（用于 RecyclerView 适配器）
     */
    public static class ChatMessage {
        public static final int TYPE_USER = 0;
        public static final int TYPE_AI = 1;
        public static final int TYPE_ERROR = 2;

        private final int type;
        private final String text;
        private final QaResponse qaResponse;
        private final boolean isVoice;
        private final String timeText;

        private ChatMessage(int type, String text, QaResponse qaResponse, boolean isVoice, String timeText) {
            this.type = type;
            this.text = text;
            this.qaResponse = qaResponse;
            this.isVoice = isVoice;
            this.timeText = timeText;
        }

        public static ChatMessage user(String text) {
            return new ChatMessage(TYPE_USER, text, null, false, formatMessageTime(LocalDateTime.now().toString()));
        }

        public static ChatMessage userVoice(String text) {
            return new ChatMessage(TYPE_USER, "[语音] " + text, null, true, formatMessageTime(LocalDateTime.now().toString()));
        }

        public static ChatMessage userFromHistory(QaHistoryItem item) {
            String text = item.isVoiceInput() ? "[语音] " + item.getQuestion() : item.getQuestion();
            return new ChatMessage(TYPE_USER, text, null, item.isVoiceInput(),
                    formatMessageTime(item.getCreatedAt()));
        }

        public static ChatMessage ai(QaResponse response) {
            return new ChatMessage(TYPE_AI, response.getAnswer(), response, false,
                    formatMessageTime(response.getCreatedAt()));
        }

        public static ChatMessage aiFromHistory(QaHistoryItem item) {
            return new ChatMessage(TYPE_AI, item.getAnswer(), QaResponse.fromHistory(item), false,
                    formatMessageTime(item.getCreatedAt()));
        }

        public static ChatMessage error(String text) {
            return new ChatMessage(TYPE_ERROR, text, null, false, "");
        }

        public int getType() { return type; }
        public String getText() { return text; }
        public QaResponse getQaResponse() { return qaResponse; }
        public boolean isVoice() { return isVoice; }
        public String getTimeText() { return timeText; }
    }
}