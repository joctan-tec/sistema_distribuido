import java.util.concurrent.*;

public class HealthCheck {
    private final Node node; // Nodo asociado al health check
    private final ScheduledExecutorService scheduler; // Scheduler para ejecutar el health check
    private final int checkInterval; // Intervalo de tiempo entre cada verificación (en segundos)

    public HealthCheck(Node node, int checkInterval) {
        this.node = node;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.checkInterval = checkInterval;
    }
/* 
    // Inicia el proceso de health check
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
               // Inicia el proceso de health check
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            if (node.isAlive()) {
                System.out.println("HealthCheck: Nodo " + node.getId() + " está vivo.");
                master.updateNodeStatus(node.getId(), true);
            } else {
                System.out.println("HealthCheck: Nodo " + node.getId() + " está inactivo. Notificando al Master...");
                master.updateNodeStatus(node.getId(), false);
                stop(); // Detener el health check si el nodo está inactivo
            }
        }, 0, checkInterval, TimeUnit.SECONDS);
    } if (node.isAlive()) {
                System.out.println("HealthCheck: Nodo " + node.getId() + " está vivo.");
                master.updateNodeStatus(node.getId(), true);
            } else {
                System.out.println("HealthCheck: Nodo " + node.getId() + " está inactivo. Notificando al Master...");
                master.updateNodeStatus(node.getId(), false);
                stop(); // Detener el health check si el nodo está inactivo
            }
        }, 0, checkInterval, TimeUnit.SECONDS);
    }
*/
    // Detiene el proceso de health check
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    // Main para pruebas rápidas
    public static void main(String[] args) {

        // Crear una instancia de HealthCheck

        // Iniciar el health check

        // Simular que el nodo está activo durante 10 segundos, luego marcarlo como muerto
        
    }
}