package com.vincent.fraud.ingest.service;

import com.vincent.fraud.ingest.config.TcpIngressProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class TcpTransactionServerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TcpTransactionServerLifecycle.class);

    private final TcpIngressProperties properties;
    private final ExecutorService tcpClientExecutorService;
    private final TcpTransactionRequestHandler requestHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ServerSocket serverSocket;

    public TcpTransactionServerLifecycle(
            TcpIngressProperties properties,
            ExecutorService tcpClientExecutorService,
            TcpTransactionRequestHandler requestHandler
    ) {
        this.properties = properties;
        this.tcpClientExecutorService = tcpClientExecutorService;
        this.requestHandler = requestHandler;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            ServerSocket createdServerSocket = new ServerSocket();
            createdServerSocket.bind(new InetSocketAddress(properties.port()));
            serverSocket = createdServerSocket;
            log.info("Started TCP transaction listener on port={}", properties.port());
            tcpClientExecutorService.submit(this::acceptLoop);
        } catch (IOException exception) {
            running.set(false);
            throw new IllegalStateException("Failed to start TCP transaction listener", exception);
        }
    }

    void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                tcpClientExecutorService.submit(() -> handleClient(socket));
            } catch (IOException exception) {
                if (running.get()) {
                    log.error("Failed to accept TCP transaction connection", exception);
                }
            }
        }
    }

    void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String payload = reader.readLine();
            if (payload == null || payload.isBlank()) {
                writer.println("{\"error\":\"EMPTY_PAYLOAD\",\"message\":\"Request payload must not be empty\"}");
                return;
            }
            writer.println(requestHandler.handle(payload));
        } catch (Exception exception) {
            log.error("Failed to handle TCP transaction request", exception);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException exception) {
                log.warn("Failed to close TCP transaction listener", exception);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
