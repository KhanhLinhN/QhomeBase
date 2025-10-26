package com.qhomebaseapp.service.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qhomebaseapp.dto.news.WebSocketNewsMessage;
import com.qhomebaseapp.model.NewsTest;
import com.qhomebaseapp.repository.news.NewsTestRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class NewsWebSocketListener {

    private static final Logger log = LoggerFactory.getLogger(NewsWebSocketListener.class);

    private final WebSocketStompClient stompClient;
    private final NewsTestRepository newsRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.websocket.url}")
    private String websocketUrl;

    @Value("${app.websocket.tenant-id}")
    private String tenantId;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private StompSession stompSession;

    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(10);
    private static final int CONNECT_TIMEOUT_SECONDS = 10;

    @PostConstruct
    public void start() {
        log.info("🚀 Starting WebSocket listener for tenantId={}", tenantId);
        connectWithRetry(0);
    }

    private void connectWithRetry(long initialDelaySeconds) {
        scheduler.schedule(() -> {
            if (connected.get()) {
                log.info("🟢 Already connected, skipping reconnect attempt.");
                return;
            }
            try {
                log.info("🔌 Attempting to connect to WebSocket: {}", websocketUrl);
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

                    @Override
                    public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                                byte[] payload, Throwable exception) {
                        log.error("❌ STOMP exception", exception);
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
        String destination = "/topic/news/" + tenantId;
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

            newsRepository.insertWithJsonb(
                    msg.getNewsId(),
                    msg.getTitle(),
                    msg.getSummary(),
                    msg.getCoverImageUrl(),
                    msg.getDeepLink(),
                    msg.getTimestamp() != null ? msg.getTimestamp() : Instant.now(),
                    json
            );

            log.info("💾 Saved news {} to database (news_test)", msg.getNewsId());
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
