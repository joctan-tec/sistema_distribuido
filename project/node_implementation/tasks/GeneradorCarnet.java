import java.io.*;
import java.net.*;
import java.util.*;

public class GeneradorCarnet {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Debe proporcionar la IP del nodo administrador de datos como argumento.");
            System.exit(1); // Fallo
        }

        String ipNodoAdministrador = args[0]; // IP del nodo administrador
        String baseUrl = "http://" + ipNodoAdministrador + ":8081";

        try {
            // Generar el carnet y comprobar si ya existe a través del nodo administrador
            String carnet = generarCarnetConRetraso();
            while (consultarSiCarnetExiste(carnet, baseUrl)) {
                carnet = generarCarnetConRetraso(); // Si ya existe, generar otro
            }

            // Guardar el carnet en el nodo administrador
            if (guardarCarnetEnNodo(carnet, baseUrl)) {
                System.out.println("Carnet generado y guardado: " + carnet);
                System.exit(0); // Éxito
            } else {
                System.err.println("Error al guardar el carnet en el nodo administrador.");
                System.exit(1); // Fallo
            }

        } catch (Exception e) {
            // Captura cualquier error y lo imprime en stderr
            System.err.println("Error: " + e.getMessage());
            System.exit(1); // Fallo
        }
    }

    // Método para generar un carnet con un año aleatorio dentro de un rango reciente y simular una tarea pesada
    public static String generarCarnetConRetraso() throws InterruptedException {
        Random random = new Random();
        int delay = 1000 + random.nextInt(10000); // Generar un tiempo entre 1000ms (1s) y 10000ms (10s)
        Thread.sleep(delay);

        // Generar el carnet
        int añoMinimo = 2010;
        int añoMaximo = 2025;
        int añoAleatorio = añoMinimo + random.nextInt(añoMaximo - añoMinimo + 1);
        int numeroAleatorio = 100000 + random.nextInt(900000); // Entre 100000 y 999999
        return String.format("%04d%06d", añoAleatorio, numeroAleatorio);
    }

    // Método para consultar si el carnet ya existe a través de una solicitud HTTP GET
    public static boolean consultarSiCarnetExiste(String carnet, String baseUrl) {
        try {
            URL url = new URL(baseUrl + "/consultar?carnet=" + URLEncoder.encode(carnet, "UTF-8"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Leer la respuesta del servidor
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String response = reader.readLine();
                    return Boolean.parseBoolean(response); // El servidor responde "true" o "false"
                }
            } else {
                System.err.println("Error al consultar el carnet: Código de respuesta HTTP " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("Error en la consulta HTTP: " + e.getMessage());
        }
        return false; // Por defecto asumimos que no existe si hay un error
    }

    // Método para guardar el carnet en el nodo administrador a través de una solicitud HTTP POST
    public static boolean guardarCarnetEnNodo(String carnet, String baseUrl) {
        try {
            URL url = new URL(baseUrl + "/guardar");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Escribir los datos en el cuerpo de la solicitud
            String postData = "carnet=" + URLEncoder.encode(carnet, "UTF-8");
            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return true; // Éxito
            } else {
                System.err.println("Error al guardar el carnet: Código de respuesta HTTP " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("Error en la solicitud HTTP: " + e.getMessage());
        }
        return false; // Fallo por defecto
    }
}
