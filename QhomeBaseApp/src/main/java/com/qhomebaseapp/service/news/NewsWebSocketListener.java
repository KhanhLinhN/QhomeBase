package com.qhomebaseapp.service.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qhomebaseapp.dto.news.WebSocketNewsMessage;
import com.qhomebaseapp.repository.news.NewsRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NewsWebSocketListener {

    private static final Logger log = LoggerFactory.getLogger(NewsWebSocketListener.class);
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private final WebSocketStompClient stompClient;
    private final NewsRepository newsRepository;
    private final ObjectMapper objectMapper;
    @Value("${app.websocket.url}")
    private String websocketUrl;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private StompSession stompSession;
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(10);
    private static final int CONNECT_TIMEOUT_SECONDS = 10;

    public NewsWebSocketListener(WebSocketStompClient stompClient, NewsRepository newsRepository, ObjectMapper objectMapper) {
        this.stompClient = stompClient;
        this.newsRepository = newsRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        connectWithRetry(0);
    }

    private void connectWithRetry(long initialDelaySeconds) {
        scheduler.schedule(() -> {
            if (connected.get()) return;
            try {
                log.info("🔌 Connecting to WebSocket: {}", websocketUrl);
                ListenableFuture<StompSession> future = stompClient.connect(websocketUrl, new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        log.info("✅ Connected to WebSocket server");
                        connected.set(true);
                        stompSession = session;
                        subscribeTopic(session);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        log.error("⚠️ Transport error: {}", exception.getMessage());
                        connected.set(false);
                        scheduleReconnect();
                    }
                });
                stompSession = future.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ex) {
                log.error("❌ Failed to connect to WebSocket server: {}", ex.getMessage());
                connected.set(false);
                scheduleReconnect();
            }
        }, initialDelaySeconds, TimeUnit.SECONDS);
    }

    private void subscribeTopic(StompSession session) {
        if (session == null || !session.isConnected()) return;

        String destination = "/topic/news"; // bỏ dấu /
        log.info("📡 Subscribing to destination: {}", destination);

        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WebSocketNewsMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    WebSocketNewsMessage msg = (WebSocketNewsMessage) payload;
                    log.info("📰 Received WS news: '{}' (id={})", msg.getTitle(), msg.getNewsId());
                    handleMessage(msg);
                } catch (Exception e) {
                    log.error("❌ Error handling WebSocket payload", e);
                }
            }
        });
    }

    private void handleMessage(WebSocketNewsMessage msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            Instant now = Instant.now();
            Instant publishAt = msg.getTimestamp() != null ? msg.getTimestamp() : now;
            String status = (msg.getStatus() != null && !msg.getStatus().isEmpty())
                    ? msg.getStatus()
                    : "NORMAL";

            newsRepository.upsertWithJsonb(
                    msg.getNewsId(),
                    msg.getTitle(),
                    msg.getSummary(),
                    msg.getCoverImageUrl(),
                    msg.getDeepLink(),
                    now,
                    now,
                    now,
                    status,
                    publishAt,
                    json
            );

            WebSocketNewsMessage broadcastMsg = new WebSocketNewsMessage();
            broadcastMsg.setType("NEW_NEWS");
            broadcastMsg.setNewsId(msg.getNewsId());
            broadcastMsg.setTitle(msg.getTitle());
            broadcastMsg.setSummary(msg.getSummary());
            broadcastMsg.setCoverImageUrl(msg.getCoverImageUrl());
            broadcastMsg.setTimestamp(publishAt);
            broadcastMsg.setDeepLink(msg.getDeepLink());
            broadcastMsg.setStatus(status);

            messagingTemplate.convertAndSend("/topic/news", broadcastMsg);
            log.info("📢 Broadcast news {} with status={}", msg.getNewsId(), status);
            log.info("💾 Upserted news {} in qhomebaseapp.news", msg.getNewsId());
        } catch (Exception ex) {
            log.error("💥 Failed to save news to DB", ex);
        }
    }


    private void scheduleReconnect() {
        if (!connected.get()) {
            log.info("🔁 Scheduling reconnect in {}s", RECONNECT_DELAY.getSeconds());
            connectWithRetry(RECONNECT_DELAY.getSeconds());
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            log.info("🛑 Shutting down WebSocket listener...");
            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
            }
        } catch (Exception ignored) {
        }
        scheduler.shutdownNow();
    }
}
