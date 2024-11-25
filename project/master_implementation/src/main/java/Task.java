import org.json.JSONObject;


public class Task {
    private final String id; // Identificador único de la tarea
    private final String fileName; // Nombre del archivo a procesar
    private final String operation; // Operación a realizar
    private String status; // Estado de la tarea
    private String node; // Nodo al que se asigna la tarea
    private final String ip; // Dirección IP del nodo

    //private long timestampAssigned; // Timestamp cuando se asigna la tarea
    //private long timestampCompleted; // Timestamp cuando se completa la tarea

    // Constructor
    public Task(String id, String fileName, String status, String node, String ip, String operation) {
        this.id = id;
        this.fileName = fileName;
        this.status = status;
        this.node = node;
        this.operation = operation;
        this.ip = ip;
        //this.timestampAssigned = System.currentTimeMillis();
    }

    // Obtener el nombre del archivo
    public String getFileName() {
        return fileName;
    }

    public String getOperation() {
        return operation;
    }

    public void setNode(String node){
        this.node = node;
    }

    // Obtener el estado de la tarea
    public String getStatus() {
        return status;
    }

    // Establecer el estado de la tarea
    public void setStatus(String status) {
        this.status = status;
    }

    // Obtener el nodo al que se asigna la tarea
    public String getNode() {
        return node;
    }

    // Obtener la dirección IP del nodo
    public String getIp() {
        return ip;
    }



    // Obtener el ID de la tarea
    public String getId() {
        return id;
    }

    public JSONObject completeJson(){
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("fileName", fileName);
        json.put("status", status);
        json.put("node", node);
        json.put("ip", ip);
        json.put("operation", operation);
        return json;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("fileName", fileName);
        json.put("status", status);
        json.put("operation", operation);
        return json;
    }





    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", status='" + status + '\'' +
                ", node='" + node + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}