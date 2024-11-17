import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;

public class Node {
    
    private final String name; // Nombre del nodo
    private final int ip; // Dirección IP del nodo
    private ArrayList<Task> tasks; // Lista de tareas asignadas al nodo
    private final ExecutorService executor; // Pool de threads para ejecutar tareas
    private final AtomicBoolean busy; // Indica si el nodo está ocupado
    

}