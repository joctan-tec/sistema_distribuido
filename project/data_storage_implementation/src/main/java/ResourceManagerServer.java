import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.json.JSONObject;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ResourceManagerServer {
    private final ReentrantReadWriteLock lock;
    private final Path dataFilePath = Paths.get("/app/data/carnets.txt");
    private final Path logFilePath = Paths.get("/app/logs/carnets.log");

    public ResourceManagerServer() throws IOException {
        this.lock = new ReentrantReadWriteLock();
        // Crear archivos si no existen
        Files.createDirectories(dataFilePath.getParent());
        Files.createDirectories(logFilePath.getParent());
        if (!Files.exists(dataFilePath)) {
            Files.createFile(dataFilePath);
        }
        if (!Files.exists(logFilePath)) {
            Files.createFile(logFilePath);
        }
    }

    // Método para manejar escritura segura en archivo
    public boolean writeData(String content) {
        lock.writeLock().lock();
        try {
            Files.write(dataFilePath, (content + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.APPEND);
            writeLog("Escritura exitosa: " + content);
            System.out.println("Escritura exitosa: " + content);
            return true;
        } catch (IOException e) {
            writeLog("Error de escritura: " + e.getMessage());
            System.err.println("Error al escribir en el archivo de datos: " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Método para leer contenido del archivo de datos
    public String readData() {
        lock.readLock().lock();
        try {
            String data = Files.readString(dataFilePath, StandardCharsets.UTF_8);
            writeLog("Lectura realizada");
            System.out.println("Lectura realizada");
            return data.isEmpty() ? "Archivo vacío" : data;
        } catch (IOException e) {
            writeLog("Error de lectura: " + e.getMessage());
            System.err.println("Error al leer el archivo de datos: " + e.getMessage());
            return "Error al leer el archivo.";
        } finally {
            lock.readLock().unlock();
        }
    }

    // Escribir un registro en el archivo de logs
    private void writeLog(String message) {
        ZoneId zonaCostaRica = ZoneId.of("America/Costa_Rica");

        // Obtener la fecha y hora actual en la zona horaria de Costa Rica
        ZonedDateTime fechaHoraCR = ZonedDateTime.now(zonaCostaRica);

        // Formatear la fecha y hora en el formato deseado
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String fechaHoraFormateada = fechaHoraCR.format(formato);
        String logEntry = "["+fechaHoraFormateada + "] " + message;
        try {
            Files.write(logFilePath, (logEntry + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error al escribir en el log: " + e.getMessage());
        }
    }

    // Método principal para iniciar el servidor
    public void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/resource", new RequestHandler(this));
        // Crear un contexto para la consulta de carnets con parametro de path "carnet" y metodo GET
        server.createContext("/query", new QueryHandler(dataFilePath));
        server.createContext("/logs", new LogHandler());
        server.setExecutor(Executors.newFixedThreadPool(4)); // Pool de hilos
        server.start();
        writeLog("Servidor iniciado en el puerto 8081");
        System.out.println("Servidor iniciado en el puerto 8081");
    }

    private static boolean consultarSiCarnetExiste(String carnet, Path filePath){
        // Buscar el carnet en el archivo de datos
        try {
            String data = Files.readString(filePath, StandardCharsets.UTF_8);
            return data.contains(carnet);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de datos: " + e.getMessage());
            return false;
        }
    }

    static class QueryHandler implements HttpHandler {
        private final Path dataFilePath;

        public QueryHandler(Path dataFilePath) {
            this.dataFilePath = dataFilePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {

                String query = exchange.getRequestURI().getQuery();
                String carnet = query.split("=")[1];
                boolean exists = consultarSiCarnetExiste(carnet, dataFilePath);
                String response = String.valueOf(exists);
                System.out.println("Consulta de carnet " + carnet + ": " + response);
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "Método no permitido";
                exchange.sendResponseHeaders(405, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // Handler para manejar las solicitudes HTTP (POST)
    static class RequestHandler implements HttpHandler {
        private final ResourceManagerServer server;

        public RequestHandler(ResourceManagerServer server) {
            this.server = server;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                
                JSONObject requestBody = new JSONObject(body);
                String operation = requestBody.getString("operation");
                String ip = requestBody.getString("ip");
                String content = requestBody.optString("content", "");

                System.out.println("Operación: " + operation + ", IP: " + ip + ", Contenido: " + content);

                String response = processRequest(operation, ip, content);
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "Método no permitido";
                exchange.sendResponseHeaders(405, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        private String processRequest(String operation, String ip, String content) {
            if ("Leer".equalsIgnoreCase(operation)) {
                return server.readData();
            } else if ("Escribir".equalsIgnoreCase(operation)) {
                boolean success = server.writeData(ip + ": " + content);
                return success ? "Escritura exitosa desde IP " + ip : "Error al escribir";
            } else {
                server.writeLog("Operación no válida desde IP " + ip);
                return "Operación no válida";
            }
        }
    }

    // Handler para manejar las solicitudes de logs (GET)
    private class LogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String logs = Files.readString(logFilePath, StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, logs.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(logs.getBytes());
                os.close();
            } else {
                String response = "Método no permitido";
                exchange.sendResponseHeaders(405, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    public static void main(String[] args) {
        try {
            ResourceManagerServer server = new ResourceManagerServer();
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
