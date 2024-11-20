import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.net.InetSocketAddress;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;

import com.sun.net.httpserver.HttpServer;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import org.json.JSONArray;

public class Master { 
    private final ArrayList<Node> registeredNodes; // Nodos registrados en el sistema 
    private final ArrayList<Task> taskQueue; // Cola de tareas pendientes
    private final ScheduledExecutorService healthCheckScheduler; // Para el health check de nodos 
    private final ResourceManager resourceManager; // Administrador de recursos compartidos
    
    public Master() { 
        this.registeredNodes = new ArrayList<>(); 
        this.taskQueue = new ArrayList<>(); 
        this.healthCheckScheduler = Executors.newScheduledThreadPool(1); 
        this.resourceManager = new ResourceManager(); 
    }

    public static String listNodeApps() throws Exception {
    String tokenPath = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    String caCertPath = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    String kubeApiServer = "https://kubernetes.default.svc";
    String token;
    try (FileInputStream fis = new FileInputStream(tokenPath)) {
        token = new String(fis.readAllBytes());
    }
    String podsUrl = kubeApiServer + "/api/v1/pods";

    SSLContext sslContext = SSLContext.getInstance("TLS");
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (FileInputStream fis = new FileInputStream(caCertPath)) {
        keyStore.load(null, null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate caCert = cf.generateCertificate(fis);
        keyStore.setCertificateEntry("ca", caCert);
    }
    trustManagerFactory.init(keyStore);
    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

    URL url = new URL(podsUrl);
    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
    connection.setSSLSocketFactory(sslContext.getSocketFactory());
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", "Bearer " + token);
    connection.setRequestProperty("Accept", "application/json");

    int responseCode = connection.getResponseCode();
    if (responseCode == 200) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String response = reader.lines().collect(Collectors.joining("\n"));
        reader.close();
        return "Pods encontrados:\n" + response;
    } else {
        return "Error al listar pods. Código de respuesta: " + responseCode;
    }
}

    // Registra un nodo en el sistema
    public synchronized void registerNode(Node node) {
        registeredNodes.add(node);
        System.out.println("Node " + node.getName() + " registered successfully.");
    }

    // Main para pruebas rápidas 
    public static void main(String[] args) throws IOException { 
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        Master master = new Master();

        server.createContext("/registerNode", new RegisterNodeHandler(master));
        server.createContext("/test", new TestHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("Master running on port 8081");
    } 

    // Handler para probar la lista de pods
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response;
            System.out.println("GET /test 200 OK");
            try {
                response = listNodeApps();
                System.out.println(response);
            } catch (Exception e) {
                response = "Error retrieving node apps: " + e.getMessage();
                e.printStackTrace();
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Registra un nodo en el sistema
    static class RegisterNodeHandler implements HttpHandler {
        private final Master master;

        public RegisterNodeHandler(Master master) {
            this.master = master;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            if ("POST".equals(exchange.getRequestMethod())) {
                // Recibe el JSON con la información del nodo
                String json = new String(exchange.getRequestBody().readAllBytes());
                JSONObject jsonObject = new JSONObject(json);
                String name = jsonObject.getString("name");
                String ip = jsonObject.getString("ip");
                Node node = new Node(name, ip);
                master.registerNode(node);
                response = "Node registered successfully.";
            } else {
                response = "Invalid request.";
            }

            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}