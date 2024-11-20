
import java.util.concurrent.ConcurrentHashMap; 
import java.util.concurrent.locks.Lock; 
import java.util.concurrent.locks.ReentrantLock; 
 
public class ResourceManager { 
    private final ConcurrentHashMap<String, Resource> resources; // Recursos compartidos identificados por un ID 
    private final Lock lock; // Lock global para sincronizar el acceso a los recursos 
 
    public ResourceManager() { 
        this.resources = new ConcurrentHashMap<>(); 
        this.lock = new ReentrantLock(); 
    } 
 
    // Clase interna que representa un recurso compartido 
    public static class Resource { 
        private final String id; 
        private String value; // Valor del recurso (puede ser modificado por los nodos) 
 
        public Resource(String id, String value) { 
            this.id = id; 
            this.value = value; 
        } 
 
        public String getId() { 
            return id; 
        } 
 
        public synchronized String getValue() { 
            return value; 
        } 
 
        public synchronized void setValue(String value) { 
            this.value = value; 
        } 
    } 
 
    // Método para agregar un recurso nuevo al administrador 
    public void addResource(String resourceId, String initialValue) { 
        lock.lock(); 
        try { 
            if (!resources.containsKey(resourceId)) { 
                resources.put(resourceId, new Resource(resourceId, initialValue)); 
                System.out.println("ResourceManager: Recurso " + resourceId + " agregado."); 
            } else { 
                System.out.println("ResourceManager: Recurso " + resourceId + " ya existe."); 
            } 
        } finally { 
            lock.unlock(); 
        } 
    } 
 
    // Método para eliminar un recurso 
    public void removeResource(String resourceId) { 
        lock.lock(); 
        try { 
            if (resources.containsKey(resourceId)) { 
                resources.remove(resourceId); 
                System.out.println("ResourceManager: Recurso " + resourceId + " eliminado."); 
            } else { 
                System.out.println("ResourceManager: Recurso " + resourceId + " no existe."); 
            } 
        } finally { 
            lock.unlock(); 
        } 
    } 
 
    // Método para acceder de forma segura a un recurso 
    public String readResource(String resourceId) { 
        Resource resource = resources.get(resourceId); 
        if (resource != null) { 
            return resource.getValue(); 
        } else { 
            System.out.println("ResourceManager: Recurso " + resourceId + " no encontrado."); 
            return null; 
        } 
    } 
 
    // Método para actualizar de forma segura un recurso 
    public void writeResource(String resourceId, String newValue) { 
        Resource resource = resources.get(resourceId); 
        if (resource != null) { 
            resource.setValue(newValue); 
            System.out.println("ResourceManager: Recurso " + resourceId + " actualizado a " + newValue); 
        } else { 
            System.out.println("ResourceManager: Recurso " + resourceId + " no encontrado."); 
        } 
    } 
 
    // Obtener el estado actual de todos los recursos 
    public void printAllResources() { 
        lock.lock(); 
        try { 
            System.out.println("Estado actual de los recursos:"); 
            for (Resource resource : resources.values()) { 
                System.out.println("Recurso ID: " + resource.getId() + ", Valor: " + resource.getValue()); 
            } 
        } finally { 
            lock.unlock(); 
        } 
    } 
}