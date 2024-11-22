import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class Node {

    private static final String MASTER_POD_PATTERN = "^master-deployment.*";
    private static final int RETRY_DELAY_MS = 5000; // Intervalo de reintento en milisegundos
    private ArrayList<Task> tasks; // Lista de tareas asignadas al nodo


    public Node() {
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public static void main(String[] args) throws IOException {
        // Iniciar el servidor HTTP local del nodo
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        Node node = new Node();

        server.createContext("/test", new TestHandler());
        server.createContext("/assignTask", new AssignTaskHandler(node));
        server.setExecutor(null);
        server.start();
        System.out.println("Node running on port 8081");

        // Intentar registrar el nodo con el maestro hasta que tenga éxito
        while (true) {
            try {
                // Obtener la IP del pod maestro
                System.out.println("Searching for master pod...");
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

    // Manejador para el endpoint /assignTask
    static class AssignTaskHandler implements HttpHandler {
        private final Node node;

        public AssignTaskHandler(Node node) {
            this.node = node;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                // Leer el cuerpo de la solicitud
                String requestBody = new String(t.getRequestBody().readAllBytes());
                System.out.println("POST /assignTask 200 OK");
                System.out.println("Task assigned: " + requestBody);

                // Parsear el cuerpo de la solicitud como una tarea
                /*Cuerpo:
                 * 
                 * {
                 *    "id": string,
                 *    "fileName": string
                 *  "status": string
                 * }
                 */
                // Parsear el cuerpo de la solicitud como una tarea
                JSONObject taskJson = new JSONObject(requestBody);
                String id = taskJson.getString("id");
                String fileName = taskJson.getString("fileName");
                String status = taskJson.getString("status");


                

                // Crear una nueva tarea y agregarla a la lista de tareas del nodo
                Task task = new Task(id, fileName, status);
                node.addTask(task);

                // Responder con un mensaje de éxito
                String response = "Task assigned to node";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                System.out.println("POST /assignTask 405 Method Not Allowed");
                String response = "Method not allowed";
                t.sendResponseHeaders(405, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // Método para buscar la IP del pod maestro
    private static String getMasterIp() {
        try {
            // Inicializar el cliente de Kubernetes
            System.out.println("Initializing Kubernetes client...");
            ApiClient client = Config.defaultClient();
            System.out.println("Kubernetes client initialized");
            CoreV1Api api = new CoreV1Api(client);
            System.out.println("CoreV1Api initialized");

            Pattern pattern = Pattern.compile("^master-deployment.*");

            // Obtener la lista de pods
            V1PodList podList = api.listPodForAllNamespaces(
                null, null, null, null, null, null, null, null, null, false);

            // Filtrar y mapear los pods que cumplen con el patrón
            List<V1Pod> filteredPods = podList.getItems().stream()
                .filter(pod -> pattern.matcher(pod.getMetadata().getName()).matches())
                .collect(Collectors.toList());


            for (V1Pod pod : podList.getItems()) {
                System.out.println("Pod: " + pod.getMetadata().getName());
            }
            System.out.println("Pods found: " + podList.getItems().size());

            // Filtrar los pods que cumplen con el patrón
            /*List<V1Pod> filteredPods = podList.getItems().stream()
                    .filter(pod -> Pattern.matches(MASTER_POD_PATTERN, pod.getMetadata().getName()))
                    .collect(Collectors.toList());*/
            

            // Retornar la IP del primer pod encontrado
            if (!filteredPods.isEmpty()) {
                return filteredPods.get(0).getStatus().getPodIP();
            }

        } 
        catch (ApiException e) {
            System.out.println("Error al listar los pods: " + e.getResponseBody());
            return null;
        }
        
        catch (IOException e) {
            System.out.println("Error al inicializar el cliente de Kubernetes: " + e.getMessage());
        }
        
        catch (Exception e) {
            System.out.println("Error al buscar el pod maestro: " + e.getMessage());
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
