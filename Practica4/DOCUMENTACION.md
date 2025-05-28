# Documentación del Servidor HTTP Bloqueante

## Descripción General
Este proyecto implementa un servidor HTTP 1.1 bloqueante en Java que es capaz de atender múltiples peticiones concurrentes mediante un pool de hilos. El servidor soporta los métodos HTTP principales (GET, POST, PUT, DELETE) y puede servir diferentes tipos de archivos basados en sus extensiones.

## Características Técnicas

- **Protocolo:** HTTP 1.1
- **Puerto por defecto:** 8000
- **Concurrencia:** Hasta 10 clientes simultáneos (configurable mediante `MAX_CLIENTS`)
- **Métodos HTTP soportados:** GET, POST, PUT, DELETE
- **Tipos MIME soportados:** HTML, CSS, JavaScript, imágenes (JPG, PNG, GIF), PDF, XML, JSON, y más

## Arquitectura del Sistema

La aplicación se estructura en una clase principal `ServidorWEB` que inicializa el servidor y maneja las conexiones entrantes, y una clase interna `Manejador` que procesa las peticiones HTTP individuales.

### Diagrama de Componentes

```
ServidorWEB
  |
  ├── Socket de Servidor (Puerto 8000)
  ├── Thread Pool (10 hilos máximo)
  |
  └── Manejador (Clase interna)
       |
       ├── Procesamiento de Peticiones HTTP
       ├── Gestión de Métodos (GET, POST, PUT, DELETE)
       ├── Manejo de Respuestas HTTP
       └── Gestión de Errores
```

## Flujo de Ejecución

1. El servidor se inicializa y comienza a escuchar en el puerto 8000
2. Por cada conexión entrante:
   - Se acepta la conexión
   - Se asigna a un hilo del pool de hilos
   - Se crea una instancia de `Manejador` para procesar la petición
3. El `Manejador`:
   - Lee la petición HTTP
   - Extrae método, URI, versión HTTP, cabeceras y cuerpo
   - Procesa la petición según el método HTTP
   - Genera y envía la respuesta adecuada
   - Cierra la conexión

## Implementación Detallada

### Clase `ServidorWEB`

La clase principal que:
- Inicializa el socket del servidor en el puerto 8000
- Configura un pool de hilos limitado a 10 hilos concurrentes
- Acepta conexiones entrantes en un bucle infinito
- Asigna cada conexión a un hilo del pool

```java
public ServidorWEB() throws Exception {
    this.ss = new ServerSocket(PUERTO);
    this.threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
    
    for (;;) {
        Socket clientSocket = ss.accept();
        threadPool.submit(() -> {
            try {
                new Manejador(clientSocket).run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

### Clase interna `Manejador`

Esta clase extiende `Thread` y procesa las peticiones HTTP:

1. **Constructor**: Recibe y almacena el socket del cliente
2. **Método run()**: Implementa la lógica principal de procesamiento
   - Configura streams de entrada/salida
   - Lee y analiza la petición HTTP
   - Distribuye a los métodos específicos según el tipo de petición
   - Maneja errores y excepciones
   - Cierra el socket al finalizar

### Procesamiento de Métodos HTTP

#### GET
- Soporta dos modos:
  - Con parámetros de consulta: Muestra los parámetros en una página HTML
  - Sin parámetros: Sirve archivos estáticos
- Si no se especifica un recurso, sirve `index.html` por defecto
- Verifica existencia y permisos de lectura

```java
private void handleGetRequest(String uri, Map<String, String> headers) throws IOException {
    if (uri.contains("?")) {
        // Manejo de parámetros
        String[] parts = uri.split("\\?", 2);
        String query = parts[1];
        // ...mostrar parámetros en HTML
    } else {
        // Servir archivo
        String filePath = uri.substring(1);
        if (filePath.isEmpty() || filePath.equals("/")) {
            filePath = "index.html";
        }
        serveFile(filePath);
    }
}
```

#### POST
- Recibe datos enviados por el cliente
- Muestra una página de confirmación con los datos recibidos

```java
private void handlePostRequest(String uri, Map<String, String> headers, String requestBody) throws IOException {
    String path = uri.substring(1);
    // ...generar respuesta HTML con los datos recibidos
}
```

#### PUT
- Crea o actualiza un archivo en el servidor
- Verifica permisos de escritura
- Crea directorios intermedios si es necesario

```java
private void handlePutRequest(String uri, Map<String, String> headers, String requestBody) throws IOException {
    String path = uri.substring(1);
    File file = new File(path);
    // ...verificar permisos y crear/actualizar archivo
    Files.write(file.toPath(), requestBody.getBytes());
    // ...generar respuesta de éxito
}
```

#### DELETE
- Elimina un archivo del servidor
- Verifica existencia y permisos antes de eliminar

```java
private void handleDeleteRequest(String uri) throws IOException {
    String path = uri.substring(1);
    File file = new File(path);
    // ...verificar existencia y permisos
    file.delete();
    // ...generar respuesta de éxito
}
```

## Gestión de Tipos MIME

El servidor mantiene un mapa de extensiones de archivo a tipos MIME:

```java
private static final Map<String, String> MIME_TYPES = new HashMap<>();
static {
    MIME_TYPES.put("html", "text/html");
    MIME_TYPES.put("htm", "text/html");
    MIME_TYPES.put("txt", "text/plain");
    MIME_TYPES.put("css", "text/css");
    MIME_TYPES.put("js", "application/javascript");
    MIME_TYPES.put("jpg", "image/jpeg");
    // ...otros tipos MIME
}
```

El método `getMimeType()` determina el tipo MIME apropiado para cada archivo:

```java
private String getMimeType(String fileName) {
    String extension = "";
    int i = fileName.lastIndexOf('.');
    if (i > 0) {
        extension = fileName.substring(i + 1).toLowerCase();
    }
    
    String mimeType = MIME_TYPES.get(extension);
    if (mimeType == null) {
        // Default para tipos desconocidos
        mimeType = "application/octet-stream";
    }
    
    return mimeType;
}
```

## Gestión de Errores

El servidor maneja diversos códigos de estado HTTP:

| Código | Descripción | Situación |
|--------|-------------|-----------|
| 400 | Bad Request | Petición malformada o incompleta |
| 403 | Forbidden | Permisos insuficientes |
| 404 | Not Found | Recurso no encontrado |
| 500 | Internal Server Error | Error interno del servidor |
| 501 | Not Implemented | Método HTTP no soportado |
| 505 | HTTP Version Not Supported | Versión HTTP no compatible |

Método para enviar respuestas de error:

```java
private void sendErrorResponse(int statusCode, String statusMessage) throws IOException {
    pw.println("HTTP/1.1 " + statusCode + " " + statusMessage);
    // ...cabeceras
    pw.println();
    // ...HTML con mensaje de error
    pw.flush();
}
```

## Concurrencia y Rendimiento

- El servidor utiliza un `ExecutorService` para gestionar un pool de hilos
- El número máximo de hilos está configurado en `MAX_CLIENTS` (10 por defecto)
- Esta implementación es más eficiente que crear un hilo por cada conexión:
  - Reduce la sobrecarga de creación/destrucción de hilos
  - Limita el uso de recursos del sistema
  - Previene ataques de denegación de servicio (DoS) básicos

## Casos de Uso

### Servir archivos estáticos
1. Cliente solicita: `GET /index.html HTTP/1.1`
2. Servidor verifica existencia y permisos
3. Servidor determina tipo MIME: `text/html`
4. Servidor lee y envía el archivo con cabeceras adecuadas

### Procesar parámetros GET
1. Cliente solicita: `GET /?nombre=Juan&edad=25 HTTP/1.1`
2. Servidor extrae parámetros: `nombre=Juan&edad=25`
3. Servidor genera una página HTML mostrando los parámetros

### Actualizar un archivo (PUT)
1. Cliente envía: `PUT /archivo.txt HTTP/1.1` con datos
2. Servidor verifica permisos de escritura
3. Servidor crea o actualiza el archivo
4. Servidor envía respuesta 201 Created

### Eliminar un archivo (DELETE)
1. Cliente envía: `DELETE /archivo.txt HTTP/1.1`
2. Servidor verifica existencia y permisos
3. Servidor elimina el archivo
4. Servidor envía respuesta 200 OK

## Pruebas y Uso

### Desde un navegador web
- Acceso básico: `http://localhost:8000/`
- Parámetros GET: `http://localhost:8000/?param1=valor1&param2=valor2`

### Usando curl
```bash
# GET
curl http://localhost:8000/index.html

# POST
curl -X POST -d "datos=ejemplo" http://localhost:8000/recurso

# PUT
curl -X PUT -d "contenido=nuevo" http://localhost:8000/archivo.txt

# DELETE
curl -X DELETE http://localhost:8000/archivo.txt
```

## Limitaciones y Posibles Mejoras

### Limitaciones actuales
- No implementa todas las características de HTTP 1.1 (conexiones persistentes)
- No soporta HTTPS
- Manejo básico de algunos tipos de contenido específicos
- Sin autenticación ni autorización

### Mejoras potenciales
- Implementar conexiones persistentes (keep-alive)
- Añadir soporte para HTTPS mediante SSL/TLS
- Ampliar el conjunto de tipos MIME soportados
- Implementar compresión de contenido (gzip, deflate)
- Añadir caché de archivos estáticos
- Implementar logging detallado
- Configuración mediante archivo externo

## Conclusiones

Este servidor HTTP bloqueante implementa las funcionalidades básicas del protocolo HTTP 1.1 con un enfoque en la concurrencia limitada mediante pool de hilos. Aunque tiene limitaciones en comparación con servidores de producción, es una buena base para entender los fundamentos de HTTP y el desarrollo de servidores web en Java.

La arquitectura basada en hilos demuestra cómo manejar múltiples conexiones simultáneas mientras se controla el uso de recursos del sistema, y la implementación de los métodos HTTP principales muestra los diferentes patrones de interacción cliente-servidor en aplicaciones web.
