import java.io.*;
import java.net.*;
import java.util.*;

public class GetAllIDs {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Debe proporcionar la IP del nodo administrador de datos como argumento.");
            System.exit(1); // Fallo
        }

        String ipNodoAdministrador = args[0]; // IP del nodo administrador
        String baseUrl = "http://" + ipNodoAdministrador + ":8081";

        try {
            // Consultar y obtener todos los carnets
            String carnets = obtenerTodosLosCarnets(baseUrl);
            
            
            System.out.println(carnets);
              
            System.exit(0); // Éxito

        } catch (Exception e) {
            // Captura cualquier error y lo imprime en stderr
            System.err.println("Error: " + e.getMessage());
            System.exit(1); // Fallo
        }
    }

    // Método para consultar todos los carnets a través de una solicitud HTTP GET
    public static String obtenerTodosLosCarnets(String baseUrl) throws InterruptedException {
        String carnets;
        try {
            URL url = new URL(baseUrl + "/resource"); // Endpoint para obtener todos los carnets
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String body = String.format("{\"operation\":\"Leer\",\"ip\":\"%s\",\"content\":\"%s\"}", InetAddress.getLocalHost().getHostAddress(), "");
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            Random random = new Random();
            int delay = 0 + random.nextInt(10000); // Generar un tiempo entre 1000ms (1s) y 10000ms (10s)
            Thread.sleep(delay);

            StringBuilder jsonResponse = new StringBuilder();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Leer la respuesta del servidor
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        jsonResponse.append(line + "<br>");
                    }

                    return jsonResponse.toString();

                }
            } else {
                System.err.println("Error al obtener los carnets: Código de respuesta HTTP " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("Error en la solicitud HTTP: " + e.getMessage());
        }
        return ""; // Por defecto asumimos que no existe si hay un error
    }

    
}
