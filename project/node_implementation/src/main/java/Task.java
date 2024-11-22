public class Task {
    private final String id; // Identificador único de la tarea
    private final String fileName; // Nombre del archivo a procesar
    private String status; // Estado de la tarea
    //private long timestampAssigned; // Timestamp cuando se asigna la tarea
    //private long timestampCompleted; // Timestamp cuando se completa la tarea

    // Constructor
    public Task(String id, String fileName, String status) {
        this.id = id;
        this.fileName = fileName;
        this.status = status;
        //this.timestampAssigned = System.currentTimeMillis();
    }

    // Obtener el nombre del archivo
    public String getFileName() {
        return fileName;
    }

    // Obtener el estado de la tarea
    public String getStatus() {
        return status;
    }

    // Establecer el estado de la tarea
    public void setStatus(String status) {
        this.status = status;
    }



    // Obtener el ID de la tarea
    public String getId() {
        return id;
    }





    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}