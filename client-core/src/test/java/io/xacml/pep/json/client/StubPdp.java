package io.xacml.pep.json.client;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An in-process HTTP server standing in for a Policy Decision Point, shared by the client test suites.
 * <p>
 * It remembers the last request it received so tests can assert on what the client sent over the wire.
 */
public class StubPdp implements AutoCloseable {

    public static final String PERMIT_1_1 = "{\"Response\":[{\"Decision\":\"Permit\"}]}";
    public static final String DENY_1_0_WITH_OBLIGATION = "{\"Response\":{\"Decision\":\"Deny\"," +
            "\"Obligations\":{\"Id\":\"notify\",\"AttributeAssignment\":[{\"AttributeId\":\"email\",\"Value\":\"a@b.c\"}]}}}";

    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(16);

    private volatile Headers lastHeaders;
    private volatile String lastBody;

    public StubPdp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        respond("/permit", 200, "application/xacml+json", PERMIT_1_1);
        respond("/deny-1.0", 200, "application/xacml+json", DENY_1_0_WITH_OBLIGATION);
        respond("/error", 500, "application/json", "{\"error\":\"boom\"}");
        server.start();
    }

    public String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    public String lastHeader(String name) {
        return lastHeaders == null ? null : lastHeaders.getFirst(name);
    }

    public int lastHeaderCount(String name) {
        return lastHeaders == null || lastHeaders.get(name) == null ? 0 : lastHeaders.get(name).size();
    }

    public String lastBody() {
        return lastBody;
    }

    private void respond(String path, int status, String contentType, String body) {
        server.createContext(path, exchange -> {
            lastHeaders = exchange.getRequestHeaders();
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
