import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoadBalancer {
    private final Map<String, Node> registeredNodes; // Mapa de nodos registrados (id -> nodo)
    private final Queue<String> nodeQueue; // Cola para implementar Round Robin
    private final Master master; // Referencia al Master
    private int taskCounter; // Contador de tareas asignadas para generar IDs únicos

    public LoadBalancer(Master master) {
        this.registeredNodes = new ConcurrentHashMap<>();
        this.nodeQueue = new LinkedList<>();
        this.master = master;
        this.taskCounter = 0;
    }

    // Registra un nodo en el balanceador
    public synchronized void registerNode(Node node) {
        if (!registeredNodes.containsKey(node.getId())) {
            registeredNodes.put(node.getId(), node);
            nodeQueue.add(node.getId());
            System.out.println("LoadBalancer: Nodo " + node.getId() + " registrado.");
        }
    }

    // Elimina un nodo del balanceador (por ejemplo, si falló)
    public synchronized void deregisterNode(String nodeId) {
        if (registeredNodes.containsKey(nodeId)) {
            registeredNodes.remove(nodeId);
            nodeQueue.remove(nodeId);
            System.out.println("LoadBalancer: Nodo " + nodeId + " eliminado del balanceador.");
        }
    }

    // Asigna una tarea utilizando Round Robin
    public synchronized void assignTask(Task task) {
        if (nodeQueue.isEmpty()) {
            System.out.println("LoadBalancer: No hay nodos disponibles para asignar la tarea.");
            return;
        }

        String nodeId = nodeQueue.poll(); // Obtener el siguiente nodo en la cola
        Node node = registeredNodes.get(nodeId);

        if (node != null && node.isAlive()) {
            task.setTimestampAssigned(System.currentTimeMillis());
            node.addTask(task); // Asignar la tarea al nodo
            System.out.println("LoadBalancer: Tarea " + task.getId() + " asignada al nodo " + node.getId());
        } else {
            System.out.println("LoadBalancer: Nodo " + nodeId + " no está disponible. Redistribuyendo...");
            deregisterNode(nodeId);
            assignTask(task); // Intentar reasignar la tarea
        }

        nodeQueue.add(nodeId); // Agregar el nodo nuevamente al final de la cola
    }

    // Método para redistribuir tareas de un nodo fallido
    public synchronized void redistributeTasks(String failedNodeId) {
        Node failedNode = registeredNodes.get(failedNodeId);

        if (failedNode != null) {
            List<Task> tasksToRedistribute = failedNode.getTasks(); // Obtener tareas del nodo fallido
            deregisterNode(failedNodeId); // Eliminar el nodo del balanceador

            System.out.println("LoadBalancer: Redistribuyendo " + tasksToRedistribute.size() + " tareas.");
            for (Task task : tasksToRedistribute) {
                assignTask(task); // Reasignar cada tarea
            }
        }
    }

    // Generar una nueva tarea y asignarla automáticamente
    public synchronized void createAndAssignTask(long executionTime) {
        String taskId = "Task-" + (++taskCounter);
        Task task = new Task(taskId, executionTime);
        assignTask(task);
    }

    // Obtener la lista de nodos activos
    public List<Node> getActiveNodes() {
        return new ArrayList<>(registeredNodes.values());
    }
}