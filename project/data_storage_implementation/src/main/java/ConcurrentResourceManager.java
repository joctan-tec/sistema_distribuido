import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConcurrentResourceManager {
    private final ResourceManager resourceManager;
    private final BlockingQueue<Request> requestQueue;

    public ConcurrentResourceManager() {
        this.resourceManager = new ResourceManager();
        this.requestQueue = new LinkedBlockingQueue<>();
        startProcessingRequests();
    }

    private void startProcessingRequests() {
        new Thread(() -> {
            while (true) {
                try {
                    Request request = requestQueue.take(); // Bloquea hasta que haya una solicitud en la cola
                    if (request.operation.equalsIgnoreCase("POST")) {
                        resourceManager.writeData(request.key, request.value);
                        System.out.println("Processed WRITE request from " + request.ip);
                    } else if (request.operation.equalsIgnoreCase("GET")) {
                        String result = resourceManager.readData(request.key);
                        System.out.println("Processed READ request from " + request.ip + ": " + result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Request processing interrupted");
                }
            }
        }).start();
    }

    public void enqueueRequest(Request request) {
        requestQueue.add(request);
    }

    public static void main(String[] args) throws IOException {
        ConcurrentResourceManager manager = new ConcurrentResourceManager();
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    String[] parts = body.split("&");
                    String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
                    String operation = null, key = null, value = null;

                    for (String part : parts) {
                        String[] pair = part.split("=");
                        if (pair.length == 2) {
                            switch (pair[0]) {
                                case "operation":
                                    operation = pair[1];
                                    break;
                                case "key":
                                    key = pair[1];
                                    break;
                                case "value":
                                    value = pair[1];
                                    break;
                            }
                        }
                    }

                    if (operation != null && key != null) {
                        Request request = new Request(ip, operation, key, value);
                        manager.enqueueRequest(request);
                        String response = "Request enqueued successfully.";
                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    } else {
                        String response = "Invalid request format.";
                        exchange.sendResponseHeaders(400, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } else {
                    String response = "Only POST requests are supported.";
                    exchange.sendResponseHeaders(405, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        });
        server.setExecutor(null);
        server.start();
        System.out.println("Server is running on port 8081...");
    }
}
