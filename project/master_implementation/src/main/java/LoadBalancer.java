import java.util.ArrayList;
import java.util.Iterator;

public class LoadBalancer {

    private final ArrayList<Node> nodes; // Lista de nodos registrados
    private Iterator<Node> nodeIterator; // Iterador para implementar Round Robin

    public LoadBalancer(ArrayList<Node> nodes) {
        this.nodes = nodes;
        this.nodeIterator = nodes.iterator();
    }

    /**
     * Método para obtener el siguiente nodo disponible siguiendo el algoritmo Round Robin.
     * Si el iterador alcanza el final de la lista, reinicia desde el principio.
     */
    public synchronized Node getNextNode() {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("No hay nodos disponibles.");
        }

        if (!nodeIterator.hasNext()) {
            nodeIterator = nodes.iterator(); // Reiniciar el iterador al principio de la lista
        }

        return nodeIterator.next();
    }
}
