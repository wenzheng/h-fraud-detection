package com.vincent.fraud.ingest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vincent.fraud.ingest.config.TcpIngressProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TcpTransactionServerLifecycleTest {

    @Test
    void shouldStartAcceptConnectionsAndStop() throws Exception {
        CountDownLatch handled = new CountDownLatch(1);
        ImmediateExecutorService executorService = new ImmediateExecutorService();
        StubTcpTransactionRequestHandler requestHandler = new StubTcpTransactionRequestHandler() {
            @Override
            String handlePayload(String payload) {
                handled.countDown();
                return "{\"status\":\"QUEUED\",\"queueMessageId\":\"msg-1\",\"transactionId\":\"tx-1\"}";
            }
        };
        FakeSocket socket = new FakeSocket("{\"accountId\":\"acct-1\"}\n");
        TestableTcpTransactionServerLifecycle lifecycle = new TestableTcpTransactionServerLifecycle(
                new TcpIngressProperties(0, 2),
                executorService,
                requestHandler,
                new FakeServerSocket(18000, socket)
        );

        lifecycle.start();
        assertThat(lifecycle.localPort()).isEqualTo(18000);
        assertThat(socket.responseBody()).contains("\"status\":\"QUEUED\"");

        assertThat(handled.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void shouldReturnEmptyPayloadError() throws Exception {
        TestableTcpTransactionServerLifecycle lifecycle = new TestableTcpTransactionServerLifecycle(
                new TcpIngressProperties(8000, 1),
                new StoredExecutorService(),
                new StubTcpTransactionRequestHandler(),
                new FakeServerSocket(18000)
        );
        FakeSocket socket = new FakeSocket("");

        lifecycle.handleClient(socket);

        assertThat(socket.responseBody()).contains("\"error\":\"EMPTY_PAYLOAD\"");
    }

    @Test
    void shouldIgnoreSecondStartCall() throws Exception {
        StoredExecutorService executorService = new StoredExecutorService();
        TestableTcpTransactionServerLifecycle lifecycle = new TestableTcpTransactionServerLifecycle(
                new TcpIngressProperties(0, 1),
                executorService,
                new StubTcpTransactionRequestHandler(),
                new FakeServerSocket(18000)
        );

        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();
        lifecycle.start();
        lifecycle.stop();

        assertThat(executorService.submittedTasks).hasSize(1);
    }

    @Test
    void shouldFailStartWhenServerSocketCreationFails() {
        TestableTcpTransactionServerLifecycle lifecycle = new TestableTcpTransactionServerLifecycle(
                new TcpIngressProperties(18000, 1),
                new StoredExecutorService(),
                new StubTcpTransactionRequestHandler(),
                null
        ) {
            @Override
            ServerSocket createServerSocket() throws IOException {
                throw new IOException("bind failed");
            }
        };

        assertThatThrownBy(lifecycle::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to start TCP transaction listener");
        assertThat(lifecycle.isRunning()).isFalse();
    }

    private static class StubTcpTransactionRequestHandler extends TcpTransactionRequestHandler {

        private StubTcpTransactionRequestHandler() {
            super(null, null, null);
        }

        @Override
        public String handle(String payload) {
            return handlePayload(payload);
        }

        String handlePayload(String payload) {
            return "{\"status\":\"QUEUED\"}";
        }
    }

    private static class TestableTcpTransactionServerLifecycle extends TcpTransactionServerLifecycle {

        private final ServerSocket testServerSocket;

        private TestableTcpTransactionServerLifecycle(
                TcpIngressProperties properties,
                AbstractExecutorService executorService,
                TcpTransactionRequestHandler requestHandler,
                ServerSocket testServerSocket
        ) {
            super(properties, executorService, requestHandler);
            this.testServerSocket = testServerSocket;
        }

        @Override
        ServerSocket createServerSocket() throws IOException {
            return testServerSocket;
        }

        @Override
        void handleClient(Socket socket) {
            super.handleClient(socket);
            stop();
        }
    }

    private static final class ImmediateExecutorService extends AbstractExecutorService {

        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static final class StoredExecutorService extends AbstractExecutorService {

        private final List<Runnable> submittedTasks = new ArrayList<>();
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            submittedTasks.add(command);
        }
    }

    private static final class FakeServerSocket extends ServerSocket {

        private final int localPort;
        private final List<Socket> acceptedSockets = new ArrayList<>();
        private boolean closed;

        private FakeServerSocket(int localPort, Socket... sockets) throws IOException {
            super();
            this.localPort = localPort;
            this.acceptedSockets.addAll(List.of(sockets));
        }

        @Override
        public void bind(java.net.SocketAddress endpoint) {
            // no-op
        }

        @Override
        public Socket accept() throws IOException {
            if (acceptedSockets.isEmpty()) {
                throw new IOException("no socket");
            }
            return acceptedSockets.remove(0);
        }

        @Override
        public synchronized void close() {
            closed = true;
        }

        @Override
        public int getLocalPort() {
            return localPort;
        }
    }

    private static final class FakeSocket extends Socket {

        private final InputStream inputStream;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private FakeSocket(String requestBody) {
            this.inputStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        String responseBody() {
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }
}
