import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {

    private final List<Node> nodes;  // Lista de nodos registrados
    private int currentIndex = 0;    // Índice para implementar Round Robin
    private final int MAX_TASKS = 10; // Máximo de tareas por nodo

    public LoadBalancer(List<Node> nodes) {
        this.nodes = nodes;
    }

    public Node getNextNode() {
        if (nodes.isEmpty()) {
            System.out.println("Error: No hay nodos disponibles en el balanceador.");
            return null;
        }
    
        List<Node> availableNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (node != null && node.isAlive() && node.getTaskAmount() < MAX_TASKS) {
                availableNodes.add(node);
            }
        }
    
        if (availableNodes.isEmpty()) {
            System.out.println("Error: Todos los nodos están inactivos o sobrecargados.");
            return null;
        }
    
        Node selectedNode = availableNodes.get(currentIndex % availableNodes.size());
        currentIndex++;
        return selectedNode;
    }

    public void redistributeLoad(Node overloadedNode) {
        if (!nodes.contains(overloadedNode)) {
            System.out.println("Error: El nodo no pertenece al balanceador.");
            return;
        }

        if (!overloadedNode.isAlive() || overloadedNode.getTaskAmount() >= MAX_TASKS) {
            List<Node> availableNodes = new ArrayList<>();
            for (Node node : nodes) {
                if (node.isAlive() && node.getTaskAmount() < MAX_TASKS) {
                    availableNodes.add(node);
                }
            }

            if (availableNodes.isEmpty()) {
                System.out.println("No se puede redistribuir la carga. Todos los nodos están inactivos o llenos.");
                return;
            }

            // Redistribuir tareas del nodo sobrecargado entre nodos disponibles
            List<Task> tasksToRedistribute = overloadedNode.clearExcessTasks(MAX_TASKS);
            for (Task task : tasksToRedistribute) {
                Node targetNode = getNextNode();
                if (targetNode != null) {
                    targetNode.addTask(task);
                    System.out.println("Tarea redistribuida al nodo: " + targetNode.getName());
                }
            }
        }
    }
}