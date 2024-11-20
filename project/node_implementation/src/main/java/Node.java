
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.stream.Collectors; 

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
//import org.json.JSONObject;
import org.json.JSONObject;


public class Node {
    
    private ArrayList<Task> tasks; // Lista de tareas asignadas al nodo
    private int taskAmount = 0; // Cantidad de tareas asignadas al nodo
    private final ExecutorService executor; // Pool de threads para ejecutar tareas
    private final AtomicBoolean busy; // Indica si el nodo está ocupado
    

    public Node() {
        this.tasks = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(10);
        this.busy = new AtomicBoolean(false);
    }



    

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        Node node = new Node();

        

        

        server.createContext("/test", new TestHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Node running on port 8081");
    }

    // Manejador para pruebas rápidas, GET /test y responde "Hello, world!"
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            System.out.println("GET /test 200 OK");
            String response = "Hello, world!";

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }



    
}