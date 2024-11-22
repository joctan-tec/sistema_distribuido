import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {
    private final ReentrantReadWriteLock lock;
    private final Map<String, String> dataStorage;

    public ResourceManager() {
        this.lock = new ReentrantReadWriteLock();
        this.dataStorage = new HashMap<>();
    }

    // Método para escribir datos
    public boolean writeData(String key, String value) {
        lock.writeLock().lock();
        try {
            System.out.println("Writing data: " + key + " -> " + value);
            dataStorage.put(key, value);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Método para leer datos
    public String readData(String key) {
        lock.readLock().lock();
        try {
            System.out.println("Reading data for key: " + key);
            return dataStorage.getOrDefault(key, "Not Found");
        } finally {
            lock.readLock().unlock();
        }
    }

    // Método para manejar interacciones con nodos
    public boolean handleRequestFromNode(String endpoint, String key, String value) {
        if (endpoint.equalsIgnoreCase("GET")) {
            String result = readData(key);
            System.out.println("GET from Node: Key=" + key + ", Result=" + result);
            return result != null;
        } else if (endpoint.equalsIgnoreCase("POST")) {
            return writeData(key, value);
        } else {
            System.out.println("Invalid endpoint: " + endpoint);
            return false;
        }
    }
}
