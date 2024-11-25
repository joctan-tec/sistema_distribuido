import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class Node {

    private String dataIp;
    private String nodeIp;
    private String masterIp;
    private static final String MASTER_POD_PATTERN = "^master-deployment.*";
    private static final String DATA_POD_PATTERN = "^data-deployment.*";
    private static final int RETRY_DELAY_MS = 5000; // Intervalo de reintento en milisegundos
    private ArrayList<Task> tasks; // Lista de tareas asignadas al nodo
    
    // Cola bloqueante para almacenar las tareas
    private BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();
    
    public Node() {
        // Inicia el hilo trabajador al crear el nodo
        new Thread(this::processTasks).start();
    }

    public void addTask(Task task) {
        taskQueue.offer(task);  // Agrega la tarea a la cola
        System.out.println("Tarea agregada a la cola: " + task.getId());
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        Node node = new Node();

        server.createContext("/test", new TestHandler());
        server.createContext("/assignTask", new AssignTaskHandler(node));
        server.setExecutor(null);
        server.start();
        System.out.println("Node running on port 8081");

        System.out.println("Node waiting responses on port 8082");

        // Inicia el hilo para procesar tareas
        new Thread(node::processTasks).start();  // Inicia el hilo trabajador

        // Bucle de registro con el maestro (como estaba)
        while (true) {
            try {
                node.dataIp = getDataIp();
                if (node.dataIp != null) {
                    System.out.println("Data IP found: " + node.dataIp);
                    break;
                } else {
                    System.out.println("No data pod found matching pattern: " + DATA_POD_PATTERN);
                }
            } catch (Exception e) {
                System.out.println("Error during registration attempt: " + e.getMessage());
            }
            
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    System.out.println("Sleep interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            
        }

        while (true) {
            try {
                node.masterIp = getMasterIp();
                if (node.masterIp != null) {
                    System.out.println("Master IP found: " + node.masterIp);
                    InetAddress localHost = InetAddress.getLocalHost();
                    node.nodeIp = localHost.getHostAddress();

                    if (registerWithMaster(node.masterIp, node.nodeIp)) {
                        System.out.println("Node successfully registered with master at " + node.masterIp);
                        break;
                    }
                } else {
                    System.out.println("No master pod found matching pattern: " + MASTER_POD_PATTERN);
                }
            } catch (Exception e) {
                System.out.println("Error during registration attempt: " + e.getMessage());
            }

            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                System.out.println("Sleep interrupted: " + e.getMessage());
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
            
        }
        node.startHealthCheck();
    }




    // Hilo trabajador que procesa las tareas en orden
    private void processTasks() {
        while (true) {
            try {
                Task task = taskQueue.take();  // Extrae y bloquea si la cola está vacía
                System.out.println("Procesando tarea: " + task.getId());

                String result = executeTask(this, task);  // Ejecuta la tarea
                

                
                if (result != null) {
                    notifyMaster(task.getId(), result);  // Notifica al maestro
                } else {
                    notifyMaster(task.getId(), "Error al procesar la tarea");
                }
                
            } catch (InterruptedException e) {
                System.out.println("El hilo de procesamiento fue interrumpido.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.println("Error en el procesamiento de tareas: " + e.getMessage());
            }
        }
    }

    // Método para notificar al maestro
    private void notifyMaster(String taskId, String response) {
        try {
            URL url = new URL("http://" + masterIp + ":8082/taskCompleted");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("taskId", taskId);
            json.put("nodeIp", nodeIp);
            json.put("status", "completed");
            json.put("message", response);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.toString().getBytes());
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Respuesta enviada al maestro: " + responseCode);

        } catch (IOException e) {
            System.out.println("Error al notificar al maestro: " + e.getMessage());
        }
    }

    private static String executeTask(Node node, Task task) {
        try {
                    
            System.out.println("Ejecutando IdGenerator en " + node.dataIp);
            ProcessBuilder builder = new ProcessBuilder(
                "java", "-cp", "/app/tasks", task.getFileName(), node.dataIp
            );
            builder.redirectErrorStream(true); // Redirige stderr a stdout para capturar todo
            Process process = builder.start();
            String processOutput;
            // Leer la salida del proceso
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator()); // Agrega cada línea y un salto de línea
                }
                processOutput = output.toString(); // Convierte el StringBuilder a un String
                System.out.println("Salida del proceso: " + processOutput);
            }
            // Esperar a que el proceso termine y obtener su código de salida
            int exitCode = process.waitFor();
            System.out.println("Código de salida: " + exitCode);
            if (exitCode == 0){
                URL url = new URL("http://" + node.masterIp + ":8081/request");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                
                JSONObject taskJson = new JSONObject();
                taskJson.put("ip", node.nodeIp);
                taskJson.put("operation", task.getOperation());
                taskJson.put("content", processOutput);
                
                try (OutputStream os = con.getOutputStream()) {
                    os.write(taskJson.toString().getBytes());
                }
                int responseCode = con.getResponseCode();
                System.out.println("Response code: " + responseCode);
                return processOutput;
            }else{
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
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
                String operation = taskJson.getString("operation");


                

                // Crear una nueva tarea y agregarla a la lista de tareas del nodo
                Task task = new Task(id, fileName, status, operation);
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
        private static String getDataIp() {
            try {
                // Inicializar el cliente de Kubernetes
                ApiClient client = Config.defaultClient();
                CoreV1Api api = new CoreV1Api(client);
                Pattern pattern = Pattern.compile("^data-deployment.*");
    
                // Obtener la lista de pods
                V1PodList podList = api.listPodForAllNamespaces(
                    null, null, null, null, null, null, null, null, null, false);
    
                // Filtrar y mapear los pods que cumplen con el patrón
                List<V1Pod> filteredPods = podList.getItems().stream()
                    .filter(pod -> pattern.matcher(pod.getMetadata().getName()).matches())
                    .collect(Collectors.toList());
            
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

    // Método para buscar la IP del pod maestro
    private static String getMasterIp() {
        try {
            // Inicializar el cliente de Kubernetes
            ApiClient client = Config.defaultClient();
            CoreV1Api api = new CoreV1Api(client);
            Pattern pattern = Pattern.compile("^master-deployment.*");

            // Obtener la lista de pods
            V1PodList podList = api.listPodForAllNamespaces(
                null, null, null, null, null, null, null, null, null, false);

            // Filtrar y mapear los pods que cumplen con el patrón
            List<V1Pod> filteredPods = podList.getItems().stream()
                .filter(pod -> pattern.matcher(pod.getMetadata().getName()).matches())
                .collect(Collectors.toList());
        
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

    public void startHealthCheck() {
        // Crear un hilo que realice el healthcheck cada 10 segundos
        new Thread(() -> {
            while (true) {
                try {
                    // Enviar solicitud HTTP al Master en el puerto 8082
                    //Log de healthcheck con hora formateada con hora de costa rica
                    System.out.println("Healthcheck para el nodo " + nodeIp + " a las " + java.time.LocalDateTime.now());
                    URL url = new URL("http://" + masterIp + ":8082/healthcheck");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.getResponseCode(); // Solo para disparar la solicitud
                    Thread.sleep(10000); // Espera 10 segundos antes de enviar otra solicitud
                } catch (Exception e) {
                    System.out.println("Error en el healthcheck para el nodo " + nodeIp);
                }
            }
        }).start();
    }
}
