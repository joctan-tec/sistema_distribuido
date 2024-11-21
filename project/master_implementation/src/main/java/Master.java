import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;

public class Master {

    private static final HashMap<String, Node> nodes = new HashMap<>();
    private ArrayList<Task> tasks;  
    // Método para inicializar y ejecutar el servidor HTTP
    public static void startHttpServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        // Registrar el endpoint /registerNode
        server.createContext("/registerNode", new RegisterNodeHandler());

        // Configurar un pool de threads para manejar solicitudes
        server.setExecutor(Executors.newFixedThreadPool(10));

        System.out.println("Servidor HTTP iniciado en el puerto 8081");
        server.start();
    }

    // Manejador para el endpoint /registerNode
    static class RegisterNodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Solo aceptar solicitudes POST
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Leer el cuerpo de la solicitud
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                
                // Validar y registrar el nodo
                if (isValidIp(requestBody)) {
                    synchronized (nodes) {
                        String nodeName = "Node-" + (nodes.size() + 1); // Crear un nombre único
                        Node newNode = new Node(nodeName, requestBody);
                        nodes.put(nodeName, newNode);
                    }

                    String response = "Nodo registrado exitosamente con IP: " + requestBody;
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else {
                    String response = "Formato de IP inválido";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            } else {
                String response = "Método no permitido";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }

        // Validar formato de IP
        private boolean isValidIp(String ip) {
            String ipPattern = 
                "^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$";
            return ip.matches(ipPattern);
        }
    }

    public static void main(String[] args) {
        try {
            // Iniciar el servidor HTTP
            startHttpServer();
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor HTTP: " + e.getMessage());
        }
    }
}
