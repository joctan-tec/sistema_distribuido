import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {

    private final List<Node> nodes;  // Lista de nodos registrados
    private int currentIndex = 0;    // Índice para implementar Round Robin

    public LoadBalancer(List<Node> nodes) {
        this.nodes = nodes;
    }

    public synchronized Node getNextNode() {
        if (nodes.isEmpty()) {
            System.out.println("Error: No hay nodos disponibles en el balanceador.");
            throw new IllegalStateException("No hay nodos disponibles.");
        }

        // Encontrar la menor carga
        Node minLoadNode = null;
        int minLoad = Integer.MAX_VALUE;

        for (Node node : nodes) {
            int currentLoad = node.getTaskAmount();
            if (currentLoad < minLoad) {
                minLoad = currentLoad;
                minLoadNode = node;
            }
        }

        // Crear una lista de nodos con la misma carga mínima
        List<Node> minLoadNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (node.getTaskAmount() == minLoad) {
                minLoadNodes.add(node);
            }
        }

        // Aplicar Round Robin entre los nodos con la misma carga mínima
        Node selectedNode = minLoadNodes.get(currentIndex % minLoadNodes.size());
        currentIndex++;  // Avanza al siguiente índice
        System.out.println("Nodo seleccionado: " + selectedNode.getName());
        return selectedNode;
    }
}