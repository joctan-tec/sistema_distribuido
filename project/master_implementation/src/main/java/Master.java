import com.sun.net.httpserver.HttpServer;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class Master {

    private final List<Node> nodes = new ArrayList<>(); // Lista de nodos registrados
    private ArrayList<Task> tasks;
    private LoadBalancer loadBalancer; // Balanceador de carga
    private int taskAmount = 0;
    private final ConcurrentHashMap<String, CompletableFuture<String>> taskCompletionMap = new ConcurrentHashMap<>();

    public Master() {
        this.tasks = new ArrayList<>();
        this.loadBalancer = new LoadBalancer(nodes); // Inicializar con nodos vacíos
    }

    // Métodos para manipular el mapa
public CompletableFuture<String> createTaskFuture(String taskId) {
    CompletableFuture<String> future = new CompletableFuture<>();
    taskCompletionMap.put(taskId, future);
    return future;
}

public void completeTask(String taskId, String message) {
    CompletableFuture<String> future = taskCompletionMap.remove(taskId);
    if (future != null) {
        future.complete(message);
    }
}

    public void addNode(Node node) {
        synchronized (nodes) {
            nodes.add(node);
        }
    }

    public void removeNode(String nodeName) {
        synchronized (nodes) {
            nodes.removeIf(node -> node.getName().equals(nodeName));
        }
    }

    public Node getNextNode() {
        System.out.println("Getting next node...");
        Node nextNode = loadBalancer.getNextNode();
        System.out.println("Current node: " + nodes);
        return nextNode;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
        this.taskAmount++;
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        this.taskAmount--;
    }

    public int getTaskAmount() {
        return taskAmount;
    }
    
    // Método para inicializar y ejecutar el servidor HTTP
    public static void startHttpServer(Master master) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        HttpServer serverResponses = HttpServer.create(new InetSocketAddress(8082), 0);



        // Registrar el endpoint /registerNode
        server.createContext("/registerNode", new RegisterNodeHandler(master));
        server.createContext("/nodes", new NodesHandler(master));
        server.createContext("/getNewId", new GetNewIdHandler(master));
        server.createContext("/", new HomeHandler());

        // Configurar un pool de threads para manejar solicitudes
        server.setExecutor(Executors.newFixedThreadPool(10));

        System.out.println("Servidor HTTP del Cliente iniciado en el puerto 8081");
        server.start();

        serverResponses.createContext("/taskCompleted", new TaskCompletedHandler(master));
        serverResponses.setExecutor(Executors.newFixedThreadPool(10));
        System.out.println("Servidor HTTP interno iniciado en el puerto 8082");
        serverResponses.start();
    }

    //Manejador de tareas completadas: esta funcion, recibe un json de esta forma:
    /*
     * {
     *      "taskId": "1",
     *     "status": "completed",
     *     "nodeIp": "ip"
     *     "message": "Algun texto"
     * }
     */
    // Se encarga de buscar la tarea en la lista de tareas y cambiar su estado a completed
    // Ademas se la quitara de la lista de tareas del nodo mediante la ip del nodo

    static class TaskCompletedHandler implements HttpHandler {
        private final Master master;
    
        public TaskCompletedHandler(Master master) {
            this.master = master;
        }
    
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                JSONObject taskJson = new JSONObject(requestBody);
                String taskId = taskJson.getString("taskId");
                String status = taskJson.getString("status");
                String nodeIp = taskJson.getString("nodeIp");
                String message = taskJson.optString("message", "");

                Task task = master.getTasks().stream().filter(t -> t.getId().equals(taskId)).findFirst().orElse(null);
                if (task == null) {
                    String response = generateHtmlResponse("error", "Tarea no encontrada", null);
                    sendHtmlResponse(exchange, response, 404);
                    return;
                }

                
    
                if ("completed".equals(status)) {

                    Node node = master.getNodes().stream().filter(n -> n.getIp().equals(nodeIp)).findFirst().orElse(null);
                    
                    master.completeTask(taskId, message);
                    String response = generateHtmlResponse("success", "Tarea marcada como completada", null);
                    sendHtmlResponse(exchange, response, 200);
                } else {
                    String response = generateHtmlResponse("error", "Estado no reconocido", null);
                    sendHtmlResponse(exchange, response, 400);
                }
            }
        }
    }
    

static class GetNewIdHandler implements HttpHandler {
    private final Master master;

    public GetNewIdHandler(Master master) {
        this.master = master;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            synchronized (master.getNodes()) {
                if (master.getNodes().isEmpty()) {
                    String response = generateHtmlResponse("error", "No hay nodos registrados", null);
                    sendHtmlResponse(exchange, response, 500);
                    return;
                }

                Node selectedNode = master.getNextNode();
                String taskId = "Task-" + (master.getTaskAmount() + 1);
                Task task = new Task(taskId, "IdGenerator", "pending", selectedNode.getName(), selectedNode.getIp());
                master.addTask(task);

                // Crear una future para esperar la tarea
                CompletableFuture<String> taskFuture = master.createTaskFuture(taskId);

                // Enviar la tarea al nodo
                JSONObject taskJson = task.toJson();
                URL url = new URL("http://" + selectedNode.getIp() + ":8081/assignTask");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(taskJson.toString().getBytes("utf-8"));
                }

                if (con.getResponseCode() == 200) {
                    try {
                        // Esperar hasta que la tarea sea completada o timeout (30 segundos)
                        String result = taskFuture.get(30, TimeUnit.SECONDS);
                        String response = generateHtmlResponse("success", result, null);
                        sendHtmlResponse(exchange, response, 200);
                    } catch (Exception e) {
                        master.removeTask(task); // Cleanup en caso de fallo
                        String response = generateHtmlResponse("error", "Timeout o error en la tarea", null);
                        sendHtmlResponse(exchange, response, 500);
                    }
                } else {
                    master.removeTask(task);
                    String response = generateHtmlResponse("error", "Error al asignar tarea al nodo", null);
                    sendHtmlResponse(exchange, response, 500);
                }
            }
        }
    }
}
    

    // Manejador para el endpoint /
    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Solo aceptar solicitudes GET
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String response = generateHTML5toHomePage();
                sendHtmlResponse(exchange, response, 200);
            } else {
                String responseMessage = "Método no permitido";
                String response = generateHtmlResponse("error", responseMessage, null);
                sendHtmlResponse(exchange, response, 405);
            }
        }
    }

    // Manejador para el endpoint /nodes
    static class NodesHandler implements HttpHandler {

        private final Master master;

        public NodesHandler(Master master) {
            this.master = master;
        }


        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Solo aceptar solicitudes GET
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Crear una lista de nombres de nodos y direcciones IP
                StringBuilder responseBuilder = new StringBuilder();
                synchronized (master.getNodes()) {

                    if (master.getNodes().isEmpty()) {
                        String responseMessage = "No tenemos nodos registrados";
                        String response = generateHtmlResponse("warning", responseMessage, null);
                        sendHtmlResponse(exchange, response, 404);
                        return;
                    }

                    for (Node node : master.getNodes()) {
                        responseBuilder.append(node.getName()).append(": ").append(node.getIp() + "  Tasks: ").append(node.getTaskAmount()+"<br>");
                        System.out.println(node.toString());
                    }
                }

                String responseMessage = responseBuilder.toString();
                String response = generateHtmlResponse("success", responseMessage, null);

                sendHtmlResponse(exchange, response, 200);
            } else {
                String responseMessage = "Método no permitido";
                String response = generateHtmlResponse("error", responseMessage, null);
                sendHtmlResponse(exchange, response, 405);
            }
        }
    }

    // Manejador para el endpoint /registerNode
    static class RegisterNodeHandler implements HttpHandler {

        
        private final Master master;

        public RegisterNodeHandler(Master master) {
            this.master = master;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Solo aceptar solicitudes POST
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Leer el cuerpo de la solicitud
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                
                // Validar y registrar el nodo
                if (isValidIp(requestBody)) {
                    synchronized (master.getNodes()) {
                        String nodeName = "Node-" + (master.getNodes().size() + 1);
                        Node newNode = new Node(nodeName, requestBody);
                        master.addNode(newNode);
                    }

                    String response = "Nodo registrado exitosamente con IP: " + requestBody;
                    System.out.println(response);
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } else {
                    String response = "Formato de IP inválido";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                }
            } else {
                String response = "Método no permitido";
                exchange.sendResponseHeaders(405, response.getBytes().length);
            }
        }

        // Validar formato de IP
        private boolean isValidIp(String ip) {
            String ipPattern = 
                "^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$";
            return ip.matches(ipPattern);
        }
    }

    private static String generateHTML5toHomePage() {
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>Master Node</title>\n" +
                "<style>\n" +
                "  body {\n" +
                "    background-color: #121212;\n" +
                "    color: #e0e0e0;\n" +
                "    font-family: Arial, sans-serif;\n" +
                "    text-align: center;\n" +
                "    padding: 2em;\n" +
                "  }\n" +
                "  h1 {\n" +
                "    color: #bb86fc;\n" +
                "  }\n" +
                "  h2 {\n" +
                "    color: #03dac6;\n" +
                "  }\n" +
                "  a {\n" +
                "    color: #81d4fa;\n" +
                "    text-decoration: none;\n" +
                "    font-weight: bold;\n" +
                "  }\n" +
                "  a:hover {\n" +
                "    text-decoration: underline;\n" +
                "  }\n" +
                "  ul {\n" +
                "    list-style-type: none;\n" +
                "    padding: 0;\n" +
                "  }\n" +
                "  li {\n" +
                "    background-color: #1f1f1f;\n" +
                "    margin: 10px auto;\n" +
                "    padding: 15px;\n" +
                "    border-radius: 10px;\n" +
                "    max-width: 400px;\n" +
                "    transition: transform 0.2s;\n" +
                "  }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <h1>Master Node</h1>\n" +
                "  <h2>Endpoints</h2>\n" +
                "  <ul>\n" +
                "    <li><a href=\"/nodes\">GET /nodes</a> - Listar nodos registrados</li>\n" +
                "    <li><a href=\"/getNewId\">GET /getNewId</a> - Obtiene un nuevo carnet</li>\n" +
                "    <li><a href=\"#\">POST /registerNode</a> - Registrar un nodo</li>\n" +
                "  </ul>\n" +
                "</body>\n" +
                "</html>";
    
        return html;
    }

    private static void sendHtmlResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes("UTF-8").length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes("UTF-8"));
        }
    }

    private static String generateHtmlResponse(String status, String message, String details) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"es\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Respuesta del Servidor</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; background-color: #121212; color:#e0e0e0; text-align: center; padding: 2em; }");
        html.append(".container { background-color: #1f1f1f; border-radius: 10px; padding: 20px; max-width: 600px; margin: 0 auto; box-shadow: 0 0 20px rgba(0,0,0,0.1); }");
        html.append(".status { font-size: 1.5em; color: ").append(status.equals("success") ? "#4caf50" : status.equals("warning") ? "#cc9c2b" : "#f44336").append("; }");
        html.append(".message { font-size: 1.2em; margin: 20px 0; }");
        html.append(".details { background-color: #2f2f2f; border-radius: 5px; padding: 10px; margin-top: 10px; white-space: pre-wrap; text-align: left; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"container\">");
        html.append("<div class=\"status\">").append(status.equals("success") ? "✔ Éxito" : status.equals("warning") ? "☭ Advertencia" : "✖ Error" ).append("</div>");
        html.append("<div class=\"message\">").append(message).append("</div>");
        if (details != null) {
            html.append("<div class=\"details\">").append(details).append("</div>");
        }
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }




    

    public static void main(String[] args) {
        try {
            // Iniciar el servidor HTTP
            Master master = new Master();
            startHttpServer(master);
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor HTTP: " + e.getMessage());
        }
    }
}

// http:IP:8081/assignTask
/*
{
    taskId: "1",
    file_task: "generateId.java",
    status: "pending"
}
*/
