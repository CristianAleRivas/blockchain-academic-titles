# API REST de Blockchain Academic Titles

API REST independiente, implementada con Quarkus. Expone consultas HTTP para un frontend y se comunica con el nodo existente usando su protocolo JSON sobre TLS. No mantiene una blockchain paralela ni devuelve datos de identidad cifrados.

## Requisitos

- Java 21
- Maven 3.9+
- Nodo blockchain TLS activo

## Preparar y ejecutar

Desde la raíz del repositorio, instalar primero el proyecto principal para que Maven pueda resolverlo como dependencia local:

```powershell
mvn install -DskipTests
```

Configurar la confianza TLS usada por el nodo. Si se creó el truststore siguiendo el README principal:

```powershell
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStore=$PWD\truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit"
```

Iniciar la API en modo desarrollo:

```powershell
Set-Location api
mvn quarkus:dev
```

La API queda disponible en `http://localhost:8080`. El nodo principal se configura con `BLOCKCHAIN_NODE_HOST` y `BLOCKCHAIN_NODE_PORT` (por defecto `localhost:8443`); el segundo peer se configura con `BLOCKCHAIN_PEER_HOST` y `BLOCKCHAIN_PEER_PORT` (por defecto `localhost:8444`). Ambos nodos deben estar ejecutándose y compartir la confianza TLS del servidor.

Los orígenes de desarrollo permitidos para CORS son `http://localhost:5173` y `http://127.0.0.1:5173`; ajustar `quarkus.http.cors.origins` antes de usar otro origen frontend.

## Endpoints

| Método | Endpoint | Respuesta |
|---|---|---|
| `GET` | `/api/blocks` | Resumen de todos los bloques |
| `GET` | `/api/blocks/{hash}` | Detalle de un bloque y sus títulos públicos |
| `GET` | `/api/titles` | Todos los títulos confirmados |
| `GET` | `/api/titles/{transactionId}` | Título por identificador y resumen del bloque |
| `POST` | `/api/titles` | Agrega un título a la cola pendiente |
| `GET` | `/api/titles/pending` | Consulta los títulos pendientes de minado en esta instancia de API |
| `POST` | `/api/chain/mine` | Mina y publica en el nodo un bloque con todos los títulos pendientes |
| `GET` | `/api/chain/validation` | Resultado de validar la cadena recibida del nodo |
| `GET` | `/api/chain/status` | Estado de conexión con el nodo y cantidad de bloques |
| `GET` | `/api/chain/nodes` | Conexión, validez, altura y último hash de ambos nodos; informa si están sincronizados |

Las respuestas de títulos solo incluyen identificador de transacción, programa, grado, institución, fechas y firma/identificador del emisor. No exponen el nombre ni el DUI cifrados.

Las consultas de bloques, títulos, validación y estado aceptan `?node=node1` o `?node=node2` para elegir el nodo consultado. Si se omite, se usa `node1`. La API confirma la sincronización únicamente cuando ambos nodos están conectados, las dos cadenas son válidas y coinciden en altura y hash final. El navegador consulta únicamente la API REST; nunca abre conexiones directas a los sockets TLS.

Ejemplo de `POST /api/titles`:

```json
{
  "transactionId": "tx-2026-001",
  "fullName": "Ana Lopez",
  "dui": "01234567-1",
  "academicProgram": "Ingenieria",
  "degree": "Licenciatura",
  "institutionName": "Universidad de El Salvador",
  "graduationDate": "2025-06-15",
  "emissionDate": "2026-10-20",
  "issuerSignature": "UES-REG-001"
}
```

La creación agrega el título a una cola en memoria de esta instancia de API, sin confirmar todavía la transacción. Se pueden enviar varios títulos y `POST /api/chain/mine` los incluye en un solo bloque. Si el nodo rechaza el bloque, la API devuelve `409` y conserva los títulos pendientes para permitir resolver el conflicto y volver a intentar. Los pendientes se pierden si se reinicia la API antes de minarlos.

El bloque minado se envía al nodo configurado. Los demás nodos conectados lo adoptan mediante el protocolo de sincronización P2P existente, que consulta peers cada cinco segundos; la propagación es eventual, no una escritura atómica simultánea. Los recursos inexistentes responden `404`; si el nodo TLS no está disponible, la API responde `503`.

Esta API de demostración no tiene autenticación ni autorización. Manténgala en entorno local/confiable y no exponga el endpoint de creación públicamente sin agregar controles de acceso y protección de transporte adecuados.

## Compilar

```powershell
mvn package
```
