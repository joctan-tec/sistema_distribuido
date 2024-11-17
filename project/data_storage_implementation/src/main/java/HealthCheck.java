import java.util.concurrent.*;

public class HealthCheck {
    private final Node node; // Nodo asociado al health check
    private final Master master; // Referencia al Master para reportar el estado del nodo
    private final ScheduledExecutorService scheduler; // Scheduler para ejecutar el health check
    private final int checkInterval; // Intervalo de tiempo entre cada verificación (en segundos)

    public HealthCheck(Node node, Master master, int checkInterval) {
        this.node = node;
        this.master = master;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.checkInterval = checkInterval;
    }

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
    }

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
        Master master = new Master();
        Node node = new Node("Node1", 3);

        // Crear una instancia de HealthCheck
        HealthCheck healthCheck = new HealthCheck(node, master, 5);

        // Iniciar el health check
        healthCheck.start();

        // Simular que el nodo está activo durante 10 segundos, luego marcarlo como muerto
        try {
            Thread.sleep(10000);
            node.markAsDead();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}