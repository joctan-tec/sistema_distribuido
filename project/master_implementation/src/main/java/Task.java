public class Task {
    private final String id; // Identificador único de la tarea
    private final long executionTime; // Tiempo de ejecución simulado en milisegundos
    private long timestampAssigned; // Timestamp cuando se asigna la tarea
    private long timestampCompleted; // Timestamp cuando se completa la tarea

    public Task(String id, long executionTime) {
        this.id = id;
        this.executionTime = executionTime;
    }

    // Obtener el ID de la tarea
    public String getId() {
        return id;
    }

    // Obtener el tiempo de ejecución
    public long getExecutionTime() {
        return executionTime;
    }

    // Asignar el timestamp de asignación
    public void setTimestampAssigned(long timestampAssigned) {
        this.timestampAssigned = timestampAssigned;
    }

    // Obtener el timestamp de asignación
    public long getTimestampAssigned() {
        return timestampAssigned;
    }

    // Asignar el timestamp de finalización
    public void setTimestampCompleted(long timestampCompleted) {
        this.timestampCompleted = timestampCompleted;
    }

    // Obtener el timestamp de finalización
    public long getTimestampCompleted() {
        return timestampCompleted;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", executionTime=" + executionTime +
                ", timestampAssigned=" + timestampAssigned +
                ", timestampCompleted=" + timestampCompleted +
                '}';
    }
}