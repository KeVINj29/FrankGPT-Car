# El Chispas ⚡ - Mecanum Robot Controller

**El Chispas** es una aplicación de Android avanzada diseñada para el control remoto de robots con ruedas Mecanum. Ofrece una interfaz intuitiva con estética "cyberpunk", soporte para streaming de video en tiempo real (ESP32-CAM) y compatibilidad total con mandos (gamepads) Bluetooth.

---

## 🚀 Funciones Principales

### 1. Modos de Control
*   **Control Manual Omnidireccional:** Movimiento en 8 direcciones (incluyendo diagonales) y rotación sobre su propio eje.
*   **Modo FPV (First Person View):** Visualización en tiempo real de la cámara del robot con opción de pantalla completa.
*   **Modos Autónomos:**
    *   **Seguidor de Línea (LINE):** El robot sigue una trayectoria marcada en el suelo.
    *   **Evitador de Obstáculos (AVOID):** Navegación automática esquivando colisiones.
    *   **Modo Seguimiento (FOLLOW):** El robot mantiene una distancia constante con un objeto/objetivo.

### 2. Conectividad
*   **Bluetooth (BT):** Conexión serie (RFCOMM) para el envío de comandos al microcontrolador (Arduino/ESP32).
*   **Cámara (CAM):** Receptor de streaming HTTP (típicamente desde una ESP32-CAM).

---

## 🎮 Uso con Mando (Gamepad)

La app está optimizada para ser usada con un control de consola (Xbox, PS4/5, etc.). 

| Botón Mando | Función en "El Chispas" | Comando Serial |
| :--- | :--- | :---: |
| **Joystick Izquierdo / D-Pad** | Movimiento Omnidireccional | A, B, C, D, G, H, I, J |
| **L1 / R1** | Rotación Izquierda / Derecha | E / F |
| **Triángulo / Y** | **Modo Seguidor de Línea** | `X` |
| **Cuadrado / X** | **Modo Follow (Seguimiento)** | `W` |
| **Equis / A** | **Modo Avoid (Esquivador)** | `T` |
| **Círculo / B** | **Parada de Emergencia / Salir de Modo** | `S` |
| **Gatillo Izquierdo (L2)** | **Mostrar/Ocultar botón de Reconexión BT** | - |
| **Gatillo Derecho (R2)** | **Salir de FPV / Resetear Interfaz** | - |

---

## 🛠 Configuración y Conexión

### Paso 1: Conexión Bluetooth
1. Enciende el Bluetooth de tu robot.
2. Abre la app y presiona el botón **"BT"** (arriba a la izquierda).
3. Selecciona tu dispositivo de la lista de vinculados.
4. El estado cambiará a `SYSTEM_ONLINE` en color verde.

### Paso 2: Conexión de Cámara (Opcional)
1. Presiona el botón **"CAM"**.
2. Ingresa la dirección IP de tu ESP32-CAM (por defecto está en: `192.168.4.1`).
3. El video aparecerá de fondo. Si tienes un mando conectado, entrará automáticamente en **Modo FPV Total**.

### Paso 3: Reconexión Rápida
Si pierdes la conexión mientras usas el mando, presiona **L2**. Aparecerá un botón central de "RECONECTAR BT" para que no tengas que navegar por los menús pequeños.

---

## 📟 Protocolo de Comunicación (Serial)

La app envía tramas de texto cortas con el formato `%X#`, donde `X` es el comando:

*   **A**: Adelante | **B**: Atrás | **C**: Izquierda | **D**: Derecha
*   **G/H/I/J**: Diagonales
*   **E**: Rotar Izquierda | **F**: Rotar Derecha
*   **S**: STOP (Parar todo)
*   **X**: Activar Seguidor de Línea
*   **W**: Activar Modo Follow
*   **T**: Activar Modo Avoid

---

##  Requisitos
*   Android 8.0 o superior.
*   Permisos de Bluetooth y Ubicación concedidos.
*   Microcontrolador con módulo Bluetooth (HC-05, HC-06 o ESP32 integrado).

---
*Desarrollado para el control total de la potencia Chispas.* 
