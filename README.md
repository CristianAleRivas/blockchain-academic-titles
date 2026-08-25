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
