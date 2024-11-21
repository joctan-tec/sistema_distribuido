import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Node {

    private static final String MASTER_POD_PATTERN = "^master-deployment.*";
    private static final int RETRY_DELAY_MS = 5000; // Intervalo de reintento en milisegundos

    public static void main(String[] args) throws IOException {
        // Iniciar el servidor HTTP local del nodo
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        Node node = new Node();

        server.createContext("/test", new TestHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Node running on port 8081");

        // Intentar registrar el nodo con el maestro hasta que tenga éxito
        while (true) {
            try {
                // Obtener la IP del pod maestro
                String masterIp = getMasterIp();
                if (masterIp != null) {
                    System.out.println("Master IP found: " + masterIp);

                    // Obtener la IP local del nodo
                    InetAddress localHost = InetAddress.getLocalHost();
                    String nodeIp = localHost.getHostAddress();

                    // Registrar este nodo con el maestro
                    if (registerWithMaster(masterIp, nodeIp)) {
                        System.out.println("Node successfully registered with master at " + masterIp);
                        break; // Salir del bucle si el registro es exitoso
                    }
                } else {
                    System.out.println("No master pod found matching pattern: " + MASTER_POD_PATTERN);
                }
            } catch (Exception e) {
                System.out.println("Error during registration attempt: " + e.getMessage());
            }

            // Esperar antes de reintentar
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                System.out.println("Retry delay interrupted: " + e.getMessage());
            }
        }
    }

    // Método para buscar la IP del pod maestro
    private static String getMasterIp() {
        try {
            // Inicializar el cliente de Kubernetes
            ApiClient client = Config.defaultClient();
            CoreV1Api api = new CoreV1Api(client);

            // Obtener la lista de pods
            V1PodList podList = api.listPodForAllNamespaces(
                    null, null, null, null, null, null, null, null, null, false);

            // Filtrar los pods que cumplen con el patrón
            List<V1Pod> filteredPods = podList.getItems().stream()
                    .filter(pod -> Pattern.matches(MASTER_POD_PATTERN, pod.getMetadata().getName()))
                    .collect(Collectors.toList());

            // Retornar la IP del primer pod encontrado
            if (!filteredPods.isEmpty()) {
                return filteredPods.get(0).getStatus().getPodIP();
            }

        } catch (Exception e) {
            System.out.println("Error al obtener la IP del maestro: " + e.getMessage());
        }
        return null;
    }

    // Método para registrar este nodo con el maestro
    private static boolean registerWithMaster(String masterIp, String nodeIp) {
        try {
            // Construir la URL del endpoint /registerNode
            URL url = new URL("http://" + masterIp + ":8081/registerNode");

            // Configurar la conexión HTTP
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "text/plain");

            // Enviar la IP del nodo en el cuerpo de la solicitud
            try (OutputStream os = con.getOutputStream()) {
                os.write(nodeIp.getBytes());
            }

            // Leer la respuesta del servidor
            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                return true; // Registro exitoso
            } else {
                System.out.println("Failed to register node. Response code: " + responseCode);
            }
        } catch (IOException e) {
            System.out.println("Error al registrar el nodo con el maestro: " + e.getMessage());
        }
        return false; // Registro fallido
    }

    // Manejador para pruebas rápidas, GET /test y responde "Hello, world!"
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            System.out.println("GET /test 200 OK");
            String response = "Hello, world!";
            // Obtener la dirección IP del nodo
            InetAddress ip = InetAddress.getLocalHost();
            response += "\nIP: " + ip.getHostAddress();

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
