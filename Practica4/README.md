# Servidor HTTP 1.1

Este proyecto implementa un servidor HTTP 1.1 en Java que soporta los métodos GET, POST, PUT y DELETE.

## Características

- Implementación de HTTP 1.1
- Soporte para los métodos: GET, POST, PUT y DELETE
- Procesamiento de parámetros GET en URLs
- Configuración automática de tipos MIME basado en extensiones de archivo
- Pool de hilos para limitar el número de clientes concurrentes
- Verificación de permisos de archivo para operaciones de escritura y eliminación
- Respuestas con Content-Length y Content-Type adecuados
- Página index.html por defecto cuando no se especifica un recurso

## Requisitos

- Java 8 o superior

## Instrucciones de uso

1. Compile el proyecto
2. Ejecute la clase ServidorWEB
3. El servidor estará escuchando en el puerto 8000

## Métodos HTTP soportados

### GET
- Para obtener recursos del servidor
- Ejemplo: `GET /index.html HTTP/1.1`
- Parámetros en la URL: `GET /?parametro=valor HTTP/1.1`

### POST
- Para enviar datos al servidor
- Ejemplo: `POST /recurso HTTP/1.1`

### PUT
- Para crear o actualizar un recurso
- Ejemplo: `PUT /archivo.txt HTTP/1.1`
- Verifica permisos de escritura antes de crear o modificar archivos

### DELETE
- Para eliminar un recurso
- Ejemplo: `DELETE /archivo.txt HTTP/1.1`
- Verifica permisos de escritura antes de eliminar archivos

## Probando el servidor

Puede usar un navegador web para probar solicitudes GET, o herramientas como Postman o curl para probar todos los métodos HTTP:

```
# GET
curl http://localhost:8000/index.html

# POST
curl -X POST -d "datos=ejemplo" http://localhost:8000/recurso

# PUT
curl -X PUT -d "contenido del archivo" http://localhost:8000/archivo.txt

# DELETE
curl -X DELETE http://localhost:8000/archivo.txt
```

## Configuración

- Puerto del servidor: 8000 (modificable en la constante PUERTO)
- Número máximo de clientes concurrentes: 10 (modificable en la constante MAX_CLIENTS)
