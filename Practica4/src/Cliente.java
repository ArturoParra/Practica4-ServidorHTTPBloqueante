import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class Cliente {
    private static final int NUM_HILOS = 10;
    private static Scanner scanner = new Scanner(System.in);
    private static Set<String> urlsProcesadas = new HashSet<>();
    private static ExecutorService executor;
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
                    descargarRecursivamente();
                    break;
                case 3:
                    mostrarAyuda();
                    break;
                case 4:
                    System.out.println("\n¡Gracias por usar el cliente HTTP!");
                    System.out.println("Cerrando aplicación...");
                    return;
                default:
                    System.out.println("Opción inválida. Por favor, seleccione una opción válida.");
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
        System.out.println("│  2. Descargar recursivamente (estilo wget)                  │");
        System.out.println("│  3. Ayuda y ejemplos                                        │");
        System.out.println("│  4. Salir                                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.print("\nSeleccione una opción [1-4]: ");
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
        System.out.println("   http://servidor:puerto/ruta/archivo");        System.out.println();
        System.out.println("Ejemplos de uso:");
        System.out.println("   • http://localhost/index.html");
        System.out.println("   • https://example.com/imagenes/foto.jpg");
        System.out.println("   • http://148.204.58.221/documentos/archivo.pdf");
        System.out.println();
        System.out.println("Directorios de destino:");
        System.out.println("   • .              (directorio actual)");
        System.out.println("   • ./downloads    (carpeta downloads)");
        System.out.println("   • C:\\Descargas   (ruta absoluta)");        System.out.println();
        System.out.println("Descarga recursiva:");
        System.out.println("   • Nivel 0: Solo descarga el archivo especificado");
        System.out.println("   • Nivel 1: Descarga todo el directorio de la URL base");
        System.out.println("   • Nivel 2+: Descarga también los archivos referenciados recursivamente");
        System.out.println("   • Se utilizan " + NUM_HILOS + " hilos para acelerar la descarga");        System.out.println();
        System.out.println("Notas:");
        System.out.println("   • Se utilizan los puertos estándar: 80 para HTTP y 443 para HTTPS");
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
            }            // Analizar la URL
            URI parsedUri = new URI(url);
            String host = parsedUri.getHost();
            int port = parsedUri.getPort();
            
            // Si no se especificó puerto y el esquema es http o https, usamos el puerto estándar
            if (port == -1) {
                if ("https".equalsIgnoreCase(parsedUri.getScheme())) {
                    port = 443; // Puerto por defecto para HTTPS
                } else {
                    port = 80; // Puerto por defecto para HTTP
                }
            }
            
            String path = parsedUri.getPath().isEmpty() ? "/" : parsedUri.getPath();
            
            System.out.println("\nConectando a " + host + (port != 80 && port != 443 ? ":" + port : ""));
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
                           "User-Agent: ClienteHTTP-ESCOM/1.0\r\n" +
                           "Accept: */*\r\n" +
                           "Connection: close\r\n" +
                           "\r\n";
            out.writeBytes(request);
            out.flush();
            
            System.out.println("Solicitud enviada, esperando respuesta...");
              // Leer cabeceras usando un enfoque híbrido más eficiente
            ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
            int consecutiveCRLF = 0;
            int b;
            
            while ((b = in.read()) != -1) {
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
            
            System.out.println("Respuesta: " + headerLines[0]);
              // Procesar las cabeceras
            Map<String, String> headersMap = new HashMap<>();
            for (int i = 1; i < headerLines.length; i++) {
                String line = headerLines[i].trim();
                if (!line.isEmpty()) {
                    int colonPos = line.indexOf(':');
                    if (colonPos > 0) {
                        String headerName = line.substring(0, colonPos).trim().toLowerCase();
                        String headerValue = colonPos < line.length() - 1 ? 
                                            line.substring(colonPos + 1).trim() : "";
                        headersMap.put(headerName, headerValue);
                    }
                }
            }
              // Obtener el tamaño del contenido si está disponible
            long contentLength = -1;
            if (headersMap.containsKey("content-length")) {
                try {
                    contentLength = Long.parseLong(headersMap.get("content-length").trim());
                    System.out.println("Tamaño del archivo: " + formatFileSize(contentLength));
                } catch (NumberFormatException e) {
                    System.out.println("Error al leer el tamaño del archivo: " + e.getMessage());
                    contentLength = -1;
                }
            } else {
                System.out.println("Tamaño del archivo: desconocido");
            }
            
            // Leer el cuerpo de la respuesta y guardar el archivo
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                long startTime = System.currentTimeMillis();
                
                System.out.println("Descargando a: " + outputPath);
                
                // Leer el contenido del archivo
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Mostrar progreso si conocemos el tamaño total
                    if (contentLength > 0) {
                        double progress = (double) totalBytesRead / contentLength * 100;
                        System.out.printf("\rProgreso: %.1f%% completado", progress);
                    } else {
                        System.out.printf("\rBytes descargados: %s", formatFileSize(totalBytesRead));
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
                    System.out.println("Descarga completada: " + formatFileSize(totalBytesRead) + 
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
      private static void descargarRecursivamente() {
        System.out.println("\nDESCARGA RECURSIVA DE ARCHIVOS");
        System.out.println("─────────────────────────────────");
        
        System.out.print("Ingrese la URL base: ");
        String urlBase = scanner.nextLine().trim();
        
        if (urlBase.isEmpty()) {
            System.out.println("Error: La URL no puede estar vacía.");
            return;
        }
        
        System.out.print("Directorio de destino (Enter para directorio actual): ");
        String outputDir = scanner.nextLine().trim();
        if (outputDir.isEmpty()) {
            outputDir = ".";
        }
        
        System.out.print("Nivel de profundidad (0-n): ");
        System.out.println("\n  Nivel 0: Solo el archivo especificado");
        System.out.println("  Nivel 1: Todo el directorio de la URL base");
        System.out.println("  Nivel 2+: Todo lo referenciado desde los archivos");
        System.out.print("\nNivel: ");
        int nivelMax = 0;
        try {
            nivelMax = Integer.parseInt(scanner.nextLine().trim());
            if (nivelMax < 0) {
                System.out.println("El nivel debe ser mayor o igual a 0. Se usará nivel 0.");
                nivelMax = 0;
            }
        } catch (NumberFormatException e) {
            System.out.println("Nivel inválido. Se usará nivel 0.");
        }
        
        // Inicializar el pool de hilos
        executor = Executors.newFixedThreadPool(NUM_HILOS);
        urlsProcesadas.clear();
        
        try {
            // Crear directorio de destino si no existe
            Path dirPath = Paths.get(outputDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("Directorio creado: " + dirPath.toAbsolutePath());
            }            // Procesar URL
            URI parsedUri = new URI(urlBase);
            String host = parsedUri.getHost();
            int port = parsedUri.getPort();
            
            // Si no se especificó puerto, usamos el puerto estándar según el esquema
            if (port == -1) {
                if ("https".equalsIgnoreCase(parsedUri.getScheme())) {
                    port = 443; // Puerto por defecto para HTTPS
                } else {
                    port = 80; // Puerto por defecto para HTTP
                }
            }
            
            String path = parsedUri.getPath().isEmpty() ? "/" : parsedUri.getPath();
            
            // Construimos la URL base respetando el puerto original
            String baseUrl;
            if (parsedUri.getPort() == -1) {
                // Si no hay puerto especificado, no lo incluimos en la URL
                baseUrl = parsedUri.getScheme() + "://" + host;
            } else {
                // Si hay puerto especificado, lo incluimos
                baseUrl = parsedUri.getScheme() + "://" + host + ":" + port;
            }
            
            System.out.println("\nIniciando descarga recursiva desde " + urlBase);
            System.out.println("Nivel máximo de profundidad: " + nivelMax);
            System.out.println("Usando " + NUM_HILOS + " hilos para la descarga");
            
            // Iniciar descarga recursiva
            CountDownLatch latch = new CountDownLatch(1);
            descargarRecursivamente(baseUrl, path, outputDir, 0, nivelMax, latch);
            
            // Esperar a que terminen todas las descargas
            try {
                latch.await();
                System.out.println("\nEsperando a que terminen todas las descargas...");
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                System.err.println("Proceso interrumpido: " + e.getMessage());
            }
            
            System.out.println("\n¡Descarga recursiva completada!");
            System.out.println("Se procesaron " + urlsProcesadas.size() + " URLs");
            
        } catch (URISyntaxException e) {
            System.err.println("URL inválida. Formato correcto: http://host:puerto/ruta");
        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
        } finally {
            // Asegurar que el executor se cierre
            if (executor != null && !executor.isTerminated()) {
                executor.shutdownNow();
            }
        }
    }    private static void descargarRecursivamente(String baseUrl, String path, String outputDir, 
                                               int nivelActual, int nivelMax, CountDownLatch parentLatch) {
        // URL completa para control de duplicados
        String fullUrl;
        if (path.startsWith("http")) {
            // Ya es una URL completa
            fullUrl = path;
        } else {
            // Es una ruta relativa
            fullUrl = baseUrl + path;
        }
        
        // Evitar procesar URLs duplicadas
        synchronized (urlsProcesadas) {
            if (urlsProcesadas.contains(fullUrl)) {
                parentLatch.countDown();
                return;
            }
            urlsProcesadas.add(fullUrl);
        }
        
        executor.submit(() -> {
            try {
                // Extraer nombre de archivo de la ruta
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                if (fileName.isEmpty()) {
                    fileName = "index.html";
                }
                
                // Crear subruta en el directorio de destino si es necesario
                String relativePath = "";
                if (path.contains("/")) {
                    relativePath = path.substring(0, path.lastIndexOf('/'));
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }
                }
                
                String subDir = outputDir;
                if (!relativePath.isEmpty()) {
                    subDir = Paths.get(outputDir, relativePath).toString();
                    Files.createDirectories(Paths.get(subDir));
                }
                
                // Ruta completa del archivo a guardar
                String outputPath = Paths.get(subDir, fileName).toString();
                  // Parsear la URL para obtener host y puerto
                URI uri = new URI(baseUrl);
                String host = uri.getHost();
                int port = uri.getPort();
                
                // Si no se especificó puerto, usamos el puerto estándar según el esquema
                if (port == -1) {
                    if ("https".equalsIgnoreCase(uri.getScheme())) {
                        port = 443; // Puerto por defecto para HTTPS
                    } else {
                        port = 80; // Puerto por defecto para HTTP
                    }
                }
                
                System.out.println("[Nivel " + nivelActual + "] Descargando: " + fullUrl + " -> " + outputPath);
                
                // Descargar el archivo
                byte[] contenido = descargarContenido(host, port, path, outputPath);
                
                // Lista para almacenar enlaces que procesaremos
                List<String> enlaces = new ArrayList<>();
                
                // NIVEL 1: Descargar todo lo alojado en la URL base
                if (nivelActual == 0 && nivelMax >= 1 && esArchivoHtml(fileName, contenido)) {
                    // Si estamos en nivel 0 y queremos llegar al menos a nivel 1,
                    // extraemos todos los enlaces para descargar el directorio completo
                    enlaces.addAll(extraerEnlaces(new String(contenido, "UTF-8")));
                      // Especial: Si es el index.html o página principal, buscamos todos los subdirectorios
                    // que pertenezcan al mismo dominio
                    if (path.equals("/") || path.endsWith("/index.html")) {
                        // Estamos en la raíz o en un directorio, tratamos de explorar subdirectorios
                        for (String enlace : new ArrayList<>(enlaces)) {
                            if (enlace.startsWith("/") || 
                                (!enlace.contains("://") && !enlace.startsWith("mailto:") && 
                                 !enlace.startsWith("javascript:") && !enlace.startsWith("#"))) {
                                
                                // Es un enlace relativo del mismo dominio, lo procesamos en nivel 1
                                // independientemente de si son recursos o no
                                continue; // Ya está en la lista de enlaces, lo procesaremos después
                            }
                        }
                    }
                }
                
                // NIVEL 2+: Descargar contenido referenciado
                if (nivelActual >= 1 && nivelActual < nivelMax && esArchivoHtml(fileName, contenido)) {
                    // En nivel 2+, extraemos enlaces para continuar la descarga recursiva
                    enlaces.addAll(extraerEnlaces(new String(contenido, "UTF-8")));
                }
                
                if (!enlaces.isEmpty()) {
                    // Crear un latch para controlar las descargas hijas
                    CountDownLatch childLatch = new CountDownLatch(enlaces.size());
                    
                    // Procesar cada enlace
                    for (String enlace : enlaces) {
                        // Normalizar el enlace
                        if (enlace.startsWith("http")) {
                            // URL absoluta, la procesamos directamente
                            try {                                URI enlaceUri = new URI(enlace);
                                String enlaceHost = enlaceUri.getHost();
                                int enlacePort = enlaceUri.getPort();
                                
                                // Si no se especificó puerto, usamos el puerto estándar según el esquema
                                if (enlacePort == -1) {
                                    if ("https".equalsIgnoreCase(enlaceUri.getScheme())) {
                                        enlacePort = 443; // Puerto por defecto para HTTPS
                                    } else {
                                        enlacePort = 80; // Puerto por defecto para HTTP
                                    }
                                }
                                
                                String enlacePath = enlaceUri.getPath().isEmpty() ? "/" : enlaceUri.getPath();
                                
                                // Construir URL base respetando el puerto original
                                String enlaceBaseUrl;
                                if (enlaceUri.getPort() == -1) {
                                    // Si no hay puerto especificado, no lo incluimos
                                    enlaceBaseUrl = enlaceUri.getScheme() + "://" + enlaceHost;
                                } else {
                                    // Si hay puerto especificado, lo incluimos
                                    enlaceBaseUrl = enlaceUri.getScheme() + "://" + enlaceHost + ":" + enlacePort;
                                }
                                
                                // Solo procesamos enlaces del mismo dominio
                                if (enlaceHost.equals(host)) {
                                    // Si estamos en nivel 1 solo procesamos archivos del directorio actual o subdirectorios
                                    // para nivel 2+ procesamos todo lo referenciado
                                    int siguienteNivel = (nivelActual == 0) ? 1 : nivelActual + 1;
                                    descargarRecursivamente(enlaceBaseUrl, enlacePath, outputDir, 
                                                           siguienteNivel, nivelMax, childLatch);
                                } else {
                                    childLatch.countDown(); // No procesamos dominios externos
                                }
                            } catch (URISyntaxException e) {
                                childLatch.countDown(); // Ignorar enlaces malformados
                            }
                        } else {
                            // URL relativa
                            String enlacePath;
                            if (enlace.startsWith("/")) {
                                // Ruta absoluta desde la raíz
                                enlacePath = enlace;
                            } else {
                                // Ruta relativa, la combinamos con la ruta actual
                                String currentPath = path;
                                if (currentPath.endsWith("/")) {
                                    enlacePath = currentPath + enlace;
                                } else {
                                    // Si la ruta actual no termina con /, subimos un nivel
                                    if (currentPath.contains("/")) {
                                        currentPath = currentPath.substring(0, currentPath.lastIndexOf('/') + 1);
                                    } else {
                                        currentPath = "/";
                                    }
                                    enlacePath = currentPath + enlace;
                                }
                            }
                            
                            // Normalizar la ruta (eliminar ../, ./, etc)
                            enlacePath = normalizarRuta(enlacePath);
                            
                            // Si estamos en nivel 1 solo procesamos archivos del directorio actual o subdirectorios
                            // para nivel 2+ procesamos todo lo referenciado
                            int siguienteNivel = (nivelActual == 0) ? 1 : nivelActual + 1;
                            descargarRecursivamente(baseUrl, enlacePath, outputDir, 
                                                   siguienteNivel, nivelMax, childLatch);
                        }
                    }
                    
                    // Esperar a que terminen las descargas hijas
                    try {
                        childLatch.await();
                    } catch (InterruptedException e) {
                        System.err.println("Proceso interrumpido: " + e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error procesando " + fullUrl + ": " + e.getMessage());
            } finally {
                // Indicar que esta tarea ha terminado
                parentLatch.countDown();
            }
        });
    }
    
    private static byte[] descargarContenido(String host, int port, String path, String outputPath) 
            throws IOException {
        ByteArrayOutputStream contenidoBytes = new ByteArrayOutputStream();
        
        try (Socket socket = new Socket(host, port)) {
            // Configurar timeout
            socket.setSoTimeout(10000);
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
              // Enviar solicitud HTTP
            String request = "GET " + path + " HTTP/1.1\r\n" +
                           "Host: " + host + "\r\n" +
                           "User-Agent: ClienteHTTP-ESCOM/1.0\r\n" +
                           "Accept: */*\r\n" +
                           "Connection: close\r\n" +
                           "\r\n";
            out.writeBytes(request);
            out.flush();
              // Leer cabeceras
            ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
            int consecutiveCRLF = 0;
            int b;
            
            while ((b = in.read()) != -1) {
                headerBytes.write(b);
                
                // Detectar fin de cabeceras
                if (b == '\r') {
                    consecutiveCRLF++;
                } else if (b == '\n' && consecutiveCRLF == 1) {
                    consecutiveCRLF++;
                } else if (b == '\r' && consecutiveCRLF == 2) {
                    consecutiveCRLF++;
                } else if (b == '\n' && consecutiveCRLF == 3) {
                    break;
                } else {
                    consecutiveCRLF = 0;
                }
            }
            
            // Verificar respuesta HTTP
            String headers = headerBytes.toString("UTF-8");
            String[] headerLines = headers.split("\r\n");
            
            if (headerLines.length == 0 || !headerLines[0].startsWith("HTTP/1.")) {
                throw new IOException("Respuesta HTTP inválida");
            }
            
            String[] statusParts = headerLines[0].split(" ", 3);
            if (statusParts.length < 2) {
                throw new IOException("Formato de respuesta HTTP inválido");
            }
            
            int statusCode = Integer.parseInt(statusParts[1]);
            if (statusCode != 200) {
                throw new IOException("Error en la respuesta HTTP: " + headerLines[0]);
            }
              // Procesar cabeceras
            Map<String, String> headersMap = new HashMap<>();
            for (int i = 1; i < headerLines.length; i++) {
                String line = headerLines[i].trim();
                if (!line.isEmpty()) {
                    int colonPos = line.indexOf(':');
                    if (colonPos > 0) {
                        String headerName = line.substring(0, colonPos).trim().toLowerCase();
                        String headerValue = colonPos < line.length() - 1 ? 
                                            line.substring(colonPos + 1).trim() : "";
                        headersMap.put(headerName, headerValue);
                    }
                }
            }
              // Obtener content-length si está disponible
            long contentLength = -1;
            if (headersMap.containsKey("content-length")) {
                try {
                    contentLength = Long.parseLong(headersMap.get("content-length").trim());
                } catch (NumberFormatException e) {
                    contentLength = -1;
                }
            }
            
            // Leer el cuerpo y guardar el archivo
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    contenidoBytes.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (contentLength > 0 && totalBytesRead >= contentLength) {
                        break;
                    }
                }
            }
        }
        
        return contenidoBytes.toByteArray();
    }
    
    private static boolean esArchivoHtml(String fileName, byte[] contenido) {
        // Verificar por extensión
        if (fileName.toLowerCase().endsWith(".html") || fileName.toLowerCase().endsWith(".htm")) {
            return true;
        }
        
        // Verificar por contenido (buscar tags HTML)
        try {
            String inicio = new String(contenido, 0, Math.min(contenido.length, 1000), "UTF-8").toLowerCase();
            return inicio.contains("<html") || inicio.contains("<!doctype html") || 
                   inicio.contains("<head") || inicio.contains("<body");
        } catch (Exception e) {
            return false;
        }
    }
    
    private static List<String> extraerEnlaces(String html) {
        List<String> enlaces = new ArrayList<>();
        
        // Extraer enlaces de etiquetas <a href="...">
        Pattern patternA = Pattern.compile("<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>", 
                                          Pattern.CASE_INSENSITIVE);
        Matcher matcherA = patternA.matcher(html);
        while (matcherA.find()) {
            String enlace = matcherA.group(1);
            if (!enlace.startsWith("mailto:") && !enlace.startsWith("javascript:") && 
                !enlace.startsWith("#")) {
                enlaces.add(enlace);
            }
        }
        
        // Extraer enlaces de etiquetas <img src="...">
        Pattern patternImg = Pattern.compile("<img\\s+[^>]*src=[\"']([^\"']+)[\"'][^>]*>", 
                                            Pattern.CASE_INSENSITIVE);
        Matcher matcherImg = patternImg.matcher(html);
        while (matcherImg.find()) {
            enlaces.add(matcherImg.group(1));
        }
        
        // Extraer enlaces de etiquetas <link href="...">
        Pattern patternLink = Pattern.compile("<link\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>", 
                                             Pattern.CASE_INSENSITIVE);
        Matcher matcherLink = patternLink.matcher(html);
        while (matcherLink.find()) {
            enlaces.add(matcherLink.group(1));
        }
        
        // Extraer enlaces de etiquetas <script src="...">
        Pattern patternScript = Pattern.compile("<script\\s+[^>]*src=[\"']([^\"']+)[\"'][^>]*>", 
                                               Pattern.CASE_INSENSITIVE);
        Matcher matcherScript = patternScript.matcher(html);
        while (matcherScript.find()) {
            enlaces.add(matcherScript.group(1));
        }
        
        return enlaces;
    }
    
    private static String normalizarRuta(String path) {
        // Implementación simple para normalizar rutas con ../ y ./
        if (!path.contains("./")) {
            return path;
        }
        
        // Dividir la ruta en segmentos
        String[] segmentos = path.split("/");
        List<String> segmentosNormalizados = new ArrayList<>();
        
        for (String segmento : segmentos) {
            if (segmento.equals("..")) {
                if (!segmentosNormalizados.isEmpty()) {
                    segmentosNormalizados.remove(segmentosNormalizados.size() - 1);
                }
            } else if (!segmento.equals(".") && !segmento.isEmpty()) {
                segmentosNormalizados.add(segmento);
            }
        }
        
        // Reconstruir la ruta
        StringBuilder rutaNormalizada = new StringBuilder();
        if (path.startsWith("/")) {
            rutaNormalizada.append("/");
        }
        
        for (int i = 0; i < segmentosNormalizados.size(); i++) {
            rutaNormalizada.append(segmentosNormalizados.get(i));
            if (i < segmentosNormalizados.size() - 1) {
                rutaNormalizada.append("/");
            }
        }
        
        return rutaNormalizada.toString();
    }
}