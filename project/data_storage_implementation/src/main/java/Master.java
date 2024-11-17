import java.util.*; 
import java.util.concurrent.*; 
import java.util.stream.Collectors; 

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;


 
public class Master { 
    private final ArrayList<Node> registeredNodes; // Nodos registrados en el sistema 
    private final ArrayList<Task> taskQueue; // Cola de tareas pendientes
    private final ScheduledExecutorService healthCheckScheduler; // Para el health check de nodos 
    private final LoadBalancer loadBalancer; // Balanceador de carga para asignar tareas a los nodos
    private final ResourceManager resourceManager; // Administrador de recursos compartidos
    private final Kuber


    public Master() { 
        registeredNodes = new ArrayList<>(); 
        taskQueue = new ArrayList<>(); 
        healthCheckScheduler = Executors.newScheduledThreadPool(1);
        loadBalancer = new LoadBalancer(this);
        resourceManager = new ResourceManager();
    } 

    // Registra un nodo en el sistema
    public synchronized void registerNode(Node node) {
        registeredNodes.add(node);
        System.out.println("Master: Nodo " + node.getId() + " registrado.");
    }

    // Main para pruebas rápidas 
    public static void main(String[] args) throws IOException { 
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        Master master = new Master();

        server.createContext("/registerNode", new RegisterNodeHandler(master));
        // server.createContext("/readIDs", new ReadIDsHandler(master));
        // server.createContext("/writeID", new WriteIDHandler(master));

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
                master.registerNode(new Node("Node" + master.registeredNodes.size(), 3));
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