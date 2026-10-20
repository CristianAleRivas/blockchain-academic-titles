# Blockchain para registro y verificación de títulos académicos salvadoreños

## Descripción del proyecto

Este proyecto implementa una aplicación educativa de blockchain en Java 21, ejecutable desde consola, con el objetivo de simular el lado servidor de una cadena de bloques para registrar y verificar títulos académicos emitidos en El Salvador.

La aplicación está pensada para su uso académico dentro de una clase de ingeniería de software o seguridad informática. No sustituye trámites oficiales del Ministerio de Educación, ni certificaciones legales, ni apostillas. La blockchain aquí demostrada funciona como registro inmutable de integridad y autenticidad del contenido publicado.

## Objetivo del parcial

El objetivo del parcial es demostrar, en una sola instancia de servidor, el ciclo completo de una blockchain:

1. Crear el bloque Genesis.
2. Preparar un bloque propuesto.
3. Registrar transacciones académicas.
4. Minar el bloque con prueba de trabajo.
5. Agregarlo a la cadena y validar su integridad.

## Cinco etapas del parcial

### 1. Bloque Génesis

El bloque génesis es el primer bloque de la cadena. Se mina de forma especial y no contiene transacciones académicas. Su hash previo es una secuencia de ceros y su finalidad es iniciar la cadena.

### 2. Bloque propuesto

Un bloque propuesto se prepara con un identificador, timestamp, hash previo y una lista de transacciones aún no confirmadas. Permite acumular varias transacciones antes del minado.

### 3. Transacciones

Cada transacción representa la emisión de un título académico. Se registran los datos académicos visibles y se cifran los datos sensibles del ciudadano, como nombre y DUI, para evitar exposición en la cadena pública.

### 4. Minado

Durante el minado se prueba diferentes valores de nonce hasta encontrar un hash que cumpla la dificultad establecida. Esto valida la prueba de trabajo.

### 5. Adición a la cadena

Una vez minado, el bloque se agrega a la blockchain si cumple todas las reglas de validación: hash correcto, previousHash válido, ID consecutivo, sin duplicados y transacciones válidas.

## Diez reglas de negocio

1. Todos los nodos deben llegar al mismo consenso de información.
2. Todo bloque debe tener un hash.
3. Todo bloque debe cumplir prueba de trabajo.
4. La información almacenada debe ser consultable públicamente con resumen del bloque sin revelar identidades en texto plano.
5. La identidad debe estar cifrada.
6. Siempre se debe minar el último bloque propuesto.
7. Los bloques minados son inmutables.
8. Un bloque puede contener N transacciones.
9. El bloque Génesis se mina para abrir la cadena y no contiene títulos.
10. La modificación manual de un bloque debe ser detectada por la validación.

## Estructura de carpetas

```text
blockchain-academic-titles/
├── pom.xml
├── README.md
├── src/
│   ├── main/java/sv/edu/blockchain/
│   │   ├── Main.java
│   │   ├── BlockChain.java
│   │   ├── console/
│   │   ├── crypto/
│   │   ├── model/
│   │   └── service/
│   └── test/java/sv/edu/blockchain/
│       └── BlockchainTest.java
└── target/
```

## Requisitos de instalación

- Java 21
- Maven 3.9+
- VS Code con la extensión Extension Pack for Java

## Comandos para compilar y ejecutar desde VS Code

```bash
mvn clean compile
mvn clean compile exec:java
```

También se puede ejecutar directamente con Java:

```bash
java -cp target/classes sv.edu.blockchain.Main
```

## Servidores de red y Wallet TLS

El servidor de red acepta conexiones TCP protegidas con TLS, intercambia solicitudes/respuestas JSON y ofrece `get-chain`, `get-block` y `submit-block`. Los bloques recibidos se validan con la prueba de trabajo y las reglas existentes antes de agregarse. Los nodos consultan a sus peers cada cinco segundos y adoptan la cadena válida más larga; cadenas de igual longitud se resuelven determinísticamente usando el hash final.

Cada bloque se guarda como `<hash>.json`. El JSON contiene un payload cifrado con AES-GCM y su hash visible para verificar el nombre. La clave de almacenamiento se proporciona mediante `BLOCKCHAIN_STORAGE_KEY`; no se guarda en el repositorio. Perder o cambiar esta clave impide leer los bloques ya almacenados.

### Crear certificados de desarrollo

Se necesita un certificado de servidor y un truststore que confíe en él. Para pruebas locales se puede generar un certificado autofirmado; en producción se debe usar una CA confiable y proteger los secretos por separado.

```powershell
keytool -genkeypair -alias blockchain -keyalg RSA -keysize 3072 -validity 365 -storetype PKCS12 -keystore server.p12 -storepass changeit -keypass changeit -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
keytool -exportcert -alias blockchain -keystore server.p12 -storepass changeit -rfc -file server.crt
keytool -importcert -noprompt -alias blockchain -file server.crt -keystore truststore.p12 -storetype PKCS12 -storepass changeit
```

Los comandos usan contraseñas de ejemplo para desarrollo; no deben reutilizarse en despliegues. Configure la variable de entorno antes de iniciar los procesos:

```powershell
$env:BLOCKCHAIN_STORAGE_KEY = "use-a-long-random-secret-for-local-development"
```

Inicie dos terminales en el directorio del proyecto. Ambos servidores deben confiar en el certificado presentado por sus peers:

```powershell
mvn exec:java "-Dexec.mainClass=sv.edu.blockchain.server.BlockchainServer" "-Dexec.args=8443 data\node1 localhost:8444" "-Djavax.net.ssl.keyStore=server.p12" "-Djavax.net.ssl.keyStorePassword=changeit" "-Djavax.net.ssl.trustStore=truststore.p12" "-Djavax.net.ssl.trustStorePassword=changeit"
mvn exec:java "-Dexec.mainClass=sv.edu.blockchain.server.BlockchainServer" "-Dexec.args=8444 data\node2 localhost:8443" "-Djavax.net.ssl.keyStore=server.p12" "-Djavax.net.ssl.keyStorePassword=changeit" "-Djavax.net.ssl.trustStore=truststore.p12" "-Djavax.net.ssl.trustStorePassword=changeit"
```

Ejecute la Wallet en una tercera terminal, con la misma confianza TLS:

```powershell
mvn exec:java "-Dexec.mainClass=sv.edu.blockchain.wallet.WalletMain" "-Dexec.args=localhost 8443" "-Djavax.net.ssl.trustStore=truststore.p12" "-Djavax.net.ssl.trustStorePassword=changeit"
```

La Wallet de demostración permite consultar la cadena y los detalles públicos de cada título; no requiere login. Los datos de identidad continúan cifrados y no se transmiten en texto plano. El listener actual cifra el canal con TLS y valida certificados del servidor, pero no habilita certificados de cliente ni autenticación de usuarios.

Al iniciar un nodo nuevo, la cadena contiene solamente el bloque Génesis, por lo que la Wallet mostrará cero títulos. La Wallet actual es de consulta; `submit-block` requiere enviar un bloque ya minado y la aplicación de consola `Main` trabaja con una cadena independiente en memoria, no con el servidor de red. Por lo tanto, registrar un título desde `Main` no lo publicará en estos nodos.

El consenso implementado es educativo: prueba de trabajo y selección de la cadena válida más larga, con desempate determinista. No es un protocolo BFT ni garantiza seguridad frente a nodos maliciosos o una mayoría de potencia de minado.

## Ejemplo de ejecución en consola

```text
=== BLOCKCHAIN DE TÍTULOS ACADÉMICOS ===

Configuración actual: dificultad 3, prefijo 000

1. Crear y minar bloque Génesis
2. Crear nuevo bloque propuesto
3. Registrar título académico
4. Mostrar bloque propuesto
5. Minar último bloque
6. Mostrar resumen de la blockchain
7. Validar blockchain
8. Buscar título por hash o identificador
9. Demostrar alteración y detectar fraude
0. Salir
```

## Cómo se cifra la identidad

La identidad del ciudadano se protege con AES/GCM/NoPadding. Se genera un IV aleatorio por operación y se almacena junto al texto cifrado en Base64. El nombre y el DUI no se muestran en texto plano al imprimir la blockchain.

Esto es una simulación académica; en un sistema real se usarían claves protegidas, almacenamiento seguro, firmas digitales, billeteras digitales y credenciales verificables administradas con control de acceso adecuado.

## Cómo se valida la cadena

La validación recorre todos los bloques y comprueba:

- hash correcto,
- prueba de trabajo válida,
- previousHash correcto,
- IDs consecutivos,
- ausencia de duplicados,
- integridad de las transacciones,
- consistencia entre bloques.

Cuando se altera un bloque o una transacción, la validación detecta ese cambio y marca la cadena como comprometida.

## Aclaración importante

Esta aplicación es una simulación académica para demostrar integridad, no un registro oficial del MINED ni de otra autoridad estatal. La blockchain demuestra la integridad del registro y la autenticidad de la emisión registrada, pero la validez legal depende de las instituciones correspondientes, notarios, ministerios o autoridades competentes.

No se debe asumir que la blockchain sustituye automáticamente apostillas, notarizaciones o validaciones legales.
