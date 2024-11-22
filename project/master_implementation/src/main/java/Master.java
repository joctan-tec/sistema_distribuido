import com.sun.net.httpserver.HttpServer;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class Master {

    private final HashMap<String, Node> nodes = new HashMap<>();
    private ArrayList<Task> tasks; 
    private LoadBalancer loadBalancer; // Balanceador de carga
    private int taskAmount = 0;

    public Master() {
        this.tasks = new ArrayList<>();
        this.loadBalancer = new LoadBalancer(new ArrayList<>(nodes.values())); // Inicializar con nodos vacíos
    }

    public void addNode(Node node) {
        synchronized (nodes) {
            nodes.put(node.getName(), node);
            updateLoadBalancer();
        }
    }

    public void removeNode(String nodeName) {
        synchronized (nodes) {
            nodes.remove(nodeName);
            updateLoadBalancer();
        }
    }

    private void updateLoadBalancer() {
        loadBalancer = new LoadBalancer(new ArrayList<>(nodes.values()));
    }

    public Node getNextNode() {
        return loadBalancer.getNextNode();
    }

    public HashMap<String, Node> getNodes() {
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

        // Registrar el endpoint /registerNode
        server.createContext("/registerNode", new RegisterNodeHandler(master));
        server.createContext("/nodes", new NodesHandler(master));
        server.createContext("/getNewId", new GetNewIdHandler(master));
        server.createContext("/", new HomeHandler());

        // Configurar un pool de threads para manejar solicitudes
        server.setExecutor(Executors.newFixedThreadPool(10));

        System.out.println("Servidor HTTP iniciado en el puerto 8081");
        server.start();
    }

    // Manejador para el endpoint /getNewId
    static class GetNewIdHandler implements HttpHandler {
        

        private final Master master;


        public GetNewIdHandler(Master master) {
            this.master = master;
        }   

        @Override                    
        public void handle(HttpExchange exchange) throws IOException {
            // Solo aceptar solicitudes GET
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Llamar al LoadBalancer para ver cual nodo tiene menos tareas y obtener su IP

                // Por ahora, simplemente seleccionar el primer nodo de la lista
                //String nodeIp;
                synchronized (master.getNodes()) {
                    if (master.getNodes().isEmpty()) {
                        String response = "No hay nodos registrados";
                        exchange.sendResponseHeaders(404, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                        return;
                    }

                    // Usar el LoadBalancer para obtener el siguiente nodo
                    Node selectedNode = master.getNextNode();
                    String nodeIp = selectedNode.getIp();
                    
                    nodeIp = master.getNodes().values().iterator().next().getIp();
                    int taskId = master.getTaskAmount() + 1;
                    Task task = new Task(taskId + "", "generateId.java", "pending", "Node-1", nodeIp);
                    master.addTask(task);

                    // Hacer solicitud POST al nodo para asignar una tarea
                    JSONObject taskJson = task.toJson();

                    URL url = new URL("http://" + nodeIp + ":8081/assignTask");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json");
                    con.setDoOutput(true);

                    try (OutputStream os = con.getOutputStream()) {
                        byte[] input = taskJson.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int responseCode = con.getResponseCode();
                    if (responseCode == 200) {
                        String response = "Tarea asignada exitosamente al nodo con IP: " + nodeIp;
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                    } else {
                        String response = "Error al asignar la tarea al nodo con IP: " + nodeIp;
                        exchange.sendResponseHeaders(500, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
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
    }
    

    // Manejador para el endpoint /
    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Solo aceptar solicitudes GET
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String response = generateHTML5toHomePage();
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                String response = "Método no permitido";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
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
                    for (Node node : master.getNodes().values()) {
                        responseBuilder.append(node.getName()).append(": ").append(node.getIp()).append("\n");
                    }
                }

                String response = responseBuilder.toString();

                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                String response = "Método no permitido";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
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
                        master.getNodes().put(nodeName, newNode);
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
                "  li:hover {\n" +
                "    transform: scale(1.05);\n" +
                "  }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <h1>Master Node</h1>\n" +
                "  <h2>Endpoints</h2>\n" +
                "  <ul>\n" +
                "    <li><a href=\"/nodes\">GET /nodes</a> - Listar nodos registrados</li>\n" +
                "    <li><a href=\"/registerNode\">POST /registerNode</a> - Registrar un nodo</li>\n" +
                "  </ul>\n" +
                "</body>\n" +
                "</html>";
    
        return html;
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
