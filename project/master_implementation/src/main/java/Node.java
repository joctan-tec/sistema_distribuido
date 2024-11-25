
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
    private final String ip; // Dirección IP del nodo
    private ArrayList<Task> tasks; // Lista de tareas asignadas al nodo
    private int taskAmount = 0; // Cantidad de tareas asignadas al nodo
    private int tasksCompleted = 0; // Cantidad de tareas completadas
    private final ExecutorService executor; // Pool de threads para ejecutar tareas
    private final AtomicBoolean busy; // Indica si el nodo está ocupado
    private long lastAliveTimestamp; // Timestamp de la última vez que el nodo estuvo vivo
    private boolean isAlive = true; // Indica si el nodo está vivo


    public Node(String name, String ip) {
        this.name = name;
        this.ip = ip;
        this.tasks = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(10);
        this.busy = new AtomicBoolean(false);
        this.lastAliveTimestamp = System.currentTimeMillis(); // Inicializar al momento de la creación
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
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

    public int getTasksCompleted() {
        return tasksCompleted;
    }


    public void incrementTasksCompleted() {
        tasksCompleted++;
    }

    public void setBusy() {
        busy.set(true);
    }

    public void setFree() {
        busy.set(false);
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getLastAliveTimestamp() {
        return lastAliveTimestamp;
    }

    public void setLastAliveTimestamp(long lastAliveTimestamp) {
        this.lastAliveTimestamp = lastAliveTimestamp;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAsDead() {
        isAlive = false;
    }

    public List<Task> clearExcessTasks(int maxTasks) {
        List<Task> excessTasks = new LinkedList<>();
        while (tasks.size() > maxTasks) {
            excessTasks.add(tasks.remove(tasks.size() - 1));
        }
        return excessTasks;
    }

    /*
    public void executeTask(Task task) {
        executor.execute(task);
    }*/
    
    

}