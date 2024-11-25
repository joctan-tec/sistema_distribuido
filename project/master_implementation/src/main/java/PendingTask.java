// Clase que representa una tarea pendiente
// Debe de recibir esto: // Master master, fileName, taskStatus, exchange

import com.sun.net.httpserver.HttpExchange;

public class PendingTask {
    private final Master master;
    private final String fileName;
    private final String operation;
    private final String taskStatus;
    private final HttpExchange exchange;

    public PendingTask(Master master, String fileName, String operation,String taskStatus, HttpExchange exchange) {
        this.master = master;
        this.fileName = fileName;
        this.taskStatus = taskStatus;
        this.exchange = exchange;
        this.operation = operation;
    }

    

    public Master getMaster() {
        return master;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public HttpExchange getExchange() {
        return exchange;
    }

    public String getOperation() {
        return operation;
    }
}