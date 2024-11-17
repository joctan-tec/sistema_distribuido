import io.fabric8.kubernetes.api.model.Pod; 
import io.fabric8.kubernetes.api.model.PodList; 
import io.fabric8.kubernetes.client.*;

import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors; 
 
public class KubernetesMonitor { 
    private final ExecutorService executorService; 
 
    public KubernetesMonitor() { 
        this.executorService = Executors.newSingleThreadExecutor(); 
    } 
 
    public void monitorCluster() { 
        executorService.submit(() -> { 
            try (KubernetesClient client = new KubernetesClientBuilder().build()) { 
                // List all pods in the cluster 
                System.out.println("=== List of Current Pods ==="); 
                PodList podList = client.pods().list(); 
                podList.getItems().forEach(this::printPodDetails); 
 
                // Watch for changes in pods (added, modified, deleted) 
                System.out.println("\n=== Watching Pod Events ==="); 
                client.pods().watch(new Watcher<Pod>() { 
                    @Override 
                    public void eventReceived(Action action, Pod pod) { 
                        System.out.println("\nEvent: " + action.name()); 
                        printPodDetails(pod); 
                    } 
 
                    @Override 
                    public void onClose(WatcherException e) { 
                        System.out.println("Watcher closed due to exception: " + e.getMessage()); 
                    } 
                }); 
            } catch (Exception e) { 
                System.err.println("Error connecting to Kubernetes cluster: " + e.getMessage()); 
            } 
        }); 
    } 
 
    private void printPodDetails(Pod pod) { 
        System.out.println("Pod Name: " + pod.getMetadata().getName()); 
        System.out.println("Namespace: " + pod.getMetadata().getNamespace()); 
        System.out.println("Status: " + pod.getStatus().getPhase()); 
        System.out.println("Node: " + pod.getSpec().getNodeName()); 
        System.out.println("IP: " + pod.getStatus().getPodIP()); 
        System.out.println("--------------------------"); 
    } 
 
    public void stopMonitoring() { 
        executorService.shutdownNow(); 
        System.out.println("KubernetesMonitor: Monitoring stopped."); 
    } 
 
    public static void main(String[] args) { 
        KubernetesMonitor monitor = new KubernetesMonitor(); 
        monitor.monitorCluster(); 
 
        // Simulate monitoring for 60 seconds before stopping 
        try { 
            Thread.sleep(60000); 
        } catch (InterruptedException e) { 
            e.printStackTrace(); 
        } 
        monitor.stopMonitoring(); 
    } 
}