import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Node {
    private final String id; // Identificador único del nodo
    private final int capacity; // Capacidad máxima de tareas que puede manejar
    private int taskAssigned = 0; // Número de tareas asignadas al nodo
    private final String ipAddress; // Dirección IP del nodo
    private long registeredTimestamp; // Timestamp de registro del nodo
    private final Queue<Task> taskQueue; // Cola de tareas asignadas al nodo
    private final AtomicBoolean isAlive; // Indica si el nodo está activo
    private final ScheduledExecutorService healthCheckScheduler; // Health check para informar al Master
    private final Master master; // Referencia al Master


    public Node(String id, int capacity, String ipAddress, Master master) {
        this.id = id;
        this.capacity = capacity;
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.isAlive = new AtomicBoolean(true);
        this.healthCheckScheduler = Executors.newScheduledThreadPool(1);
        this.master = master;
        this.ipAddress = ipAddress;
    }

    // Obtener el ID del nodo
    public String getId() {
        return id;
    }



    public int getTaskAssigned() {
        return taskAssigned;
    }


    public static void main(String[] args) {
        // Obtener puerto random del sistema host
        
    }



    
}