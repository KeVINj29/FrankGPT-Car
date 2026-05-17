# FrankGPT-Car - El Chispas ⚡

**FrankGPT-Car** es un proyecto de robótica basado en un chasis de ruedas Mecanum, profundamente modificado y mejorado para ofrecer una experiencia de control total. Este repositorio contiene tanto el firmware (Arduino) del vehículo como el código fuente de **El Chispas**, nuestra aplicación de Android personalizada con estética cyberpunk.

El proyecto original fue reconstruido para integrar transmisión de video en tiempo real, control total mediante gamepads de consola (Bluetooth) y rutinas de movimiento autónomo.

## 🏗️ Arquitectura del Proyecto

El repositorio está dividido en dos componentes principales:

* 📁 **[`/Arduino`](./Arduino)**: Contiene el firmware `El_Chispas.ino` modificado para gestionar la tracción Mecanum, la lógica de los sensores (ultrasónicos e infrarrojos) y la comunicación serial.
* 📁 **[`/AndroidApp`](./AndroidApp)**: Contiene el código fuente completo de la aplicación móvil de control. *(Consulta el [README específico de la App](./AndroidApp/README.md) para más detalles sobre sus funciones).*

## ⚙️ Hardware Modificado y Componentes

A diferencia de un carrito estándar, **FrankGPT-Car** integra las siguientes mejoras de hardware:

* **Chasis Mecanum:** Para movimiento omnidireccional (desplazamientos laterales y diagonales).
* **Módulo Bluetooth (HC-05/HC-06):** Para recepción de comandos desde la app móvil.
* **ESP32-CAM:** Añadida para habilitar el streaming de video en tiempo real (Modo FPV).
* **Sensores Infrarrojos:** Para el modo autónomo de Seguidor de Línea.
* **Sensor Ultrasónico (HC-SR04):** Para la detección de objetos en los modos Evitador de Obstáculos (Avoid) y Seguimiento (Follow).
* **Driver de Motores L293D / Shield:** Para la gestión de potencia de los 4 motores de DC.

## 🚀 Instalación y Despliegue

### 1. Firmware del Vehículo (Arduino)
1. Abre la carpeta `/Arduino/El_Chispas/`.
2. Carga el archivo `El_Chispas.ino` en tu IDE de Arduino.
3. Asegúrate de desconectar los pines RX/TX del módulo Bluetooth antes de flashear el código a la placa.
4. Sube el código y vuelve a conectar los pines.

### 2. Aplicación de Control (Android)
Si solo quieres instalar la aplicación móvil en tu teléfono para probar el carrito:
* Ve a la sección de [**Releases**](https://github.com/TuUsuario/FrankGPT-Car/releases) de este repositorio.
* Descarga el archivo `El_Chispas.apk` de la última versión.
* Instálalo en tu dispositivo Android (requiere Android 8.0+ y conceder permisos de Bluetooth).

Si deseas modificar la aplicación:
* Clona este repositorio y abre la carpeta `/AndroidApp` en **Android Studio**.

## 🧠 Modos de Operación

El sistema cuenta con un protocolo de comunicación serial de un solo carácter que permite cambiar dinámicamente entre distintos comportamientos:

1.  **Manual (Gamepad):** Soporte nativo para controles de PS4/PS5/Xbox vía Bluetooth para manejo omnidireccional.
2.  **Modo Avoid (`T`):** El algoritmo en Arduino escanea el entorno; si detecta peligro inminente (<18cm) retrocede y esquiva, si está a media distancia (<45cm) hace un esquive suave.
3.  **Modo Follow (`W`):** El carrito mantiene una "zona de confort" (15-25cm). Si el objeto se aleja, lo persigue; si se acerca demasiado, retrocede; si lo pierde, inicia un escaneo giratorio.
4.  **Modo Line Follower (`X`):** Utiliza los sensores IR para centrarse automáticamente y seguir una línea de alto contraste.

---
*Transformando un chasis estándar en un vehículo de exploración omnidireccional.*
