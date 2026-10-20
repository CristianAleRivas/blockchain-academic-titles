# Frontend de la Wallet académica

Interfaz en español para consultar títulos académicos, registrar títulos pendientes, revisar bloques y solicitar su minado. La aplicación consume exclusivamente la API REST de Quarkus; no se conecta directamente a los nodos blockchain.

## Requisitos

- Node.js 20 o posterior
- npm
- API Quarkus disponible en `http://localhost:8080`

## Instalación y ejecución

Desde esta carpeta:

```powershell
npm install
npm run dev
```

Abre la dirección que indique Vite (normalmente `http://localhost:5173`). Durante el desarrollo, Vite reenvía las solicitudes `/api/...` a `http://localhost:8080`.

La Wallet consulta el estado de ambos peers a través de Quarkus. El selector permite elegir cuál nodo usar para consultar títulos, bloques y validación; el panel compara la altura, el hash final y la validez de ambas cadenas. La cola de pendientes y el minado siguen perteneciendo a la instancia de API y al nodo principal configurado.

Para comprobar tipos y generar la compilación de producción:

```powershell
npm run build
```

La cola de títulos pendientes pertenece a la instancia de API que los recibió y se guarda en memoria. Al minar, la API incluye todos sus títulos pendientes en un único bloque. Nombre y DUI solo se envían al registrar el título; no se guardan en almacenamiento del navegador ni se muestran en las consultas públicas.
