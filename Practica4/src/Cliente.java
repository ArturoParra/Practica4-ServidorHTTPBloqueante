import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Cliente {
    private static final int DEFAULT_PORT = 8000;
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        mostrarBienvenida();
        
        while (true) {
            mostrarMenu();
            int opcion = leerOpcion();
            
            switch (opcion) {
                case 1:
                    descargarArchivo();
                    break;
                case 2:
                    mostrarAyuda();
                    break;
                case 3:
                    System.out.println("\n¡Gracias por usar el cliente HTTP!");
                    System.out.println("Cerrando aplicación...");
                    return;
                default:
                    System.out.println("❌ Opción inválida. Por favor, seleccione una opción válida.");
                    break;
            }
            
            System.out.println("\nPresione Enter para continuar...");
            scanner.nextLine();
        }
    }
    
    private static void mostrarBienvenida() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    CLIENTE HTTP WGET                         ║");
        System.out.println("║                  Descargador de archivos                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    private static void mostrarMenu() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                        MENÚ PRINCIPAL                       │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. Descargar archivo desde URL                             │");
        System.out.println("│  2. Ayuda y ejemplos                                        │");
        System.out.println("│  3. Salir                                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.print("\nSeleccione una opción [1-3]: ");
    }
    
    private static int leerOpcion() {
        try {
            String input = scanner.nextLine().trim();
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private static void descargarArchivo() {
        System.out.println("\nDESCARGA DE ARCHIVO");
        System.out.println("─────────────────────────");
        
        System.out.print("Ingrese la URL del archivo: ");
        String url = scanner.nextLine().trim();
        
        if (url.isEmpty()) {
            System.out.println("Error: La URL no puede estar vacía.");
            return;
        }
        
        System.out.print("Directorio de destino (Enter para directorio actual): ");
        String outputDir = scanner.nextLine().trim();
        if (outputDir.isEmpty()) {
            outputDir = ".";
        }
        
        procesarDescarga(url, outputDir);
    }
    
    private static void mostrarAyuda() {
        System.out.println("\n AYUDA Y EJEMPLOS");
        System.out.println("─────────────────────");
        System.out.println("Este cliente permite descargar archivos desde un servidor HTTP.");
        System.out.println();
        System.out.println("Formato de URL:");
        System.out.println("   http://servidor:puerto/ruta/archivo");
        System.out.println();
        System.out.println("Ejemplos de uso:");
        System.out.println("   • http://localhost:8000/index.html");
        System.out.println("   • http://localhost:8000/imagenes/foto.jpg");
        System.out.println("   • http://localhost:8000/documentos/archivo.pdf");
        System.out.println();
        System.out.println("Directorios de destino:");
        System.out.println("   • .              (directorio actual)");
        System.out.println("   • ./downloads    (carpeta downloads)");
        System.out.println("   • C:\\Descargas   (ruta absoluta)");
        System.out.println();
        System.out.println("Notas:");
        System.out.println("   • El puerto por defecto es 8000");
        System.out.println("   • Se crearán automáticamente los directorios necesarios");
        System.out.println("   • El progreso se muestra durante la descarga");
    }
    
    private static void procesarDescarga(String url, String outputDir) {
        try {
            // Crear directorio de destino si no existe
            Path dirPath = Paths.get(outputDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("Directorio creado: " + dirPath.toAbsolutePath());
            }
              // Analizar la URL
            URI parsedUri = new URI(url);
            String host = parsedUri.getHost();
            int port = parsedUri.getPort() == -1 ? DEFAULT_PORT : parsedUri.getPort();
            String path = parsedUri.getPath().isEmpty() ? "/" : parsedUri.getPath();
            
            System.out.println("\nConectando a " + host + ":" + port);
            System.out.println("Solicitando recurso: " + path);
            
            // Extraer nombre de archivo de la ruta
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) {
                fileName = "index.html";
            }
            
            // Ruta completa del archivo a guardar
            String outputPath = Paths.get(outputDir, fileName).toString();
              // Establecer conexión y descargar el archivo
            descargarArchivo(host, port, path, outputPath);
              } catch (URISyntaxException e) {
            System.err.println("URL inválida. Formato correcto: http://host:puerto/ruta");
        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
        }
    }    private static void descargarArchivo(String host, int port, String path, String outputPath) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            // Configurar timeout de conexión
            socket.setSoTimeout(10000);
            
            // Usar DataInputStream para lectura más eficiente
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            // Enviar la solicitud HTTP
            String request = "GET " + path + " HTTP/1.1\r\n" +
                           "Host: " + host + "\r\n" +
                           "Connection: close\r\n" +
                           "\r\n";
            out.writeBytes(request);
            out.flush();
            
            System.out.println("📡 Solicitud enviada, esperando respuesta...");
            
            // Leer cabeceras usando un enfoque híbrido más eficiente
            ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
            int consecutiveCRLF = 0;
            
            while (true) {
                int b = in.read();
                if (b == -1) {
                    throw new IOException("Conexión cerrada inesperadamente");
                }
                
                headerBytes.write(b);
                
                // Contar secuencias \r\n para encontrar el final de las cabeceras
                if (b == '\r') {
                    consecutiveCRLF++;
                } else if (b == '\n' && consecutiveCRLF == 1) {
                    consecutiveCRLF++;
                } else if (b == '\r' && consecutiveCRLF == 2) {
                    consecutiveCRLF++;
                } else if (b == '\n' && consecutiveCRLF == 3) {
                    // Encontramos \r\n\r\n - fin de cabeceras
                    break;
                } else {
                    consecutiveCRLF = 0;
                }
            }
            
            // Convertir cabeceras a string
            String headers = headerBytes.toString("UTF-8");
            String[] headerLines = headers.split("\r\n");
            
            // Verificar la línea de estado
            if (headerLines.length == 0 || !headerLines[0].startsWith("HTTP/1.")) {
                throw new IOException("Respuesta HTTP inválida: " + (headerLines.length > 0 ? headerLines[0] : "Sin respuesta"));
            }
            
            String[] statusParts = headerLines[0].split(" ", 3);
            if (statusParts.length < 2) {
                throw new IOException("Formato de respuesta HTTP inválido");
            }
            
            int statusCode = Integer.parseInt(statusParts[1]);
            if (statusCode != 200) {
                throw new IOException("Error en la respuesta HTTP: " + headerLines[0]);
            }
            
            System.out.println("✅ Respuesta: " + headerLines[0]);
            
            // Procesar las cabeceras
            Map<String, String> headersMap = new HashMap<>();
            for (int i = 1; i < headerLines.length; i++) {
                if (!headerLines[i].trim().isEmpty()) {
                    int colonPos = headerLines[i].indexOf(':');
                    if (colonPos > 0) {
                        String headerName = headerLines[i].substring(0, colonPos).trim().toLowerCase();
                        String headerValue = headerLines[i].substring(colonPos + 1).trim();
                        headersMap.put(headerName, headerValue);
                    }
                }
            }
            
            // Obtener el tamaño del contenido si está disponible
            long contentLength = -1;
            if (headersMap.containsKey("content-length")) {
                contentLength = Long.parseLong(headersMap.get("content-length"));
                System.out.println("📊 Tamaño del archivo: " + formatFileSize(contentLength));
            }
            
            // Leer el cuerpo de la respuesta y guardar el archivo
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                long startTime = System.currentTimeMillis();
                
                System.out.println("💾 Descargando a: " + outputPath);
                
                // Leer el contenido del archivo
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Mostrar progreso si conocemos el tamaño total
                    if (contentLength > 0) {
                        double progress = (double) totalBytesRead / contentLength * 100;
                        System.out.printf("\r🔄 Progreso: %.1f%% completado", progress);
                    } else {
                        System.out.printf("\r📈 Bytes descargados: %s", formatFileSize(totalBytesRead));
                    }
                    
                    // Si conocemos el tamaño y hemos leído todo, salir
                    if (contentLength > 0 && totalBytesRead >= contentLength) {
                        break;
                    }
                }
                
                long endTime = System.currentTimeMillis();
                double seconds = (endTime - startTime) / 1000.0;
                
                System.out.println();
                
                if (totalBytesRead > 0) {
                    double speed = totalBytesRead / seconds;
                    System.out.println("🎉 Descarga completada: " + formatFileSize(totalBytesRead) + 
                                      " en " + String.format("%.1f", seconds) + " segundos" +
                                      " (" + formatFileSize((long)speed) + "/s)");
                } else {
                    System.out.println("❌ No se descargaron datos");
                }
            }
            
        } catch (SocketTimeoutException e) {
            throw new IOException("Tiempo de espera agotado al conectar con el servidor", e);
        }
    }
    
    private static String formatFileSize(long bytes) {
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
}