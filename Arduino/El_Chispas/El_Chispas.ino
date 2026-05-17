#include <SoftwareSerial.h>

// --- CONFIGURACIÓN DE PINES Y VARIABLES ---
SoftwareSerial mySerial(A0, A1); // RX, TX
String BT_value = "";
String BT_value_temp = "";
int Speed = 255;
char currentMode = 'S'; // Almacena el modo actual (S = Stop)

// Sensores Ultrasónicos
const int Trig = A3;
const int Echo = A2; 

// Sensores Infrarrojos (Seguidor de Línea)
const int IR_LEFT = A4;  // Infrarrojo Izquierdo
const int IR_RIGHT = A5; // Infrarrojo Derecho

// Pines L293D
const int PWM2A = 11;      // M1 motor
const int PWM2B = 3;       // M2 motor  
const int PWM0A = 6;       // M3 motor 
const int PWM0B = 5;       // M4 motor
const int DIR_CLK = 4;     // Línea de reloj
const int DIR_EN = 7;      // Habilitación L293D
const int DATA = 8;        // Línea de datos
const int DIR_LATCH = 12;  // Latch de salida

// --- DEFINICIÓN DE MOVIMIENTOS (HEX/DEC) ---
const int Move_Forward      = 39;
const int Move_Backward     = 216;
const int Left_Move         = 116;
const int Right_Move        = 139;
const int Right_Rotate      = 149;
const int Left_Rotate       = 106;
const int Stop              = 0;
const int Upper_Left_Move   = 36;
const int Upper_Right_Move  = 3;
const int Lower_Left_Move   = 80;
const int Lower_Right_Move  = 136;

// --- FUNCIÓN MOTOR ---
void Motor(int Dir, int s1, int s2, int s3, int s4) {
    analogWrite(PWM2A, s1);
    analogWrite(PWM2B, s2);
    analogWrite(PWM0A, s3);
    analogWrite(PWM0B, s4);
    digitalWrite(DIR_LATCH, LOW);
    shiftOut(DATA, DIR_CLK, MSBFIRST, Dir);
    digitalWrite(DIR_LATCH, HIGH);
}

// --- FUNCIÓN ULTRASONIDO ---
float checkdistance() {
    digitalWrite(Trig, LOW);
    delayMicroseconds(2);
    digitalWrite(Trig, HIGH);
    delayMicroseconds(10);
    digitalWrite(Trig, LOW);
    
    // Agregamos un timeout de 30000 microsegundos (30ms) para evitar el lag
    float pulse = pulseIn(Echo, HIGH, 30000); 
    
    // Si pulse es 0, significa que no rebotó en nada (camino súper libre)
    if (pulse == 0) {
        return 100.0; // Le decimos al código que el objeto está lejos (100cm)
    }
    
    return pulse / 58.00;
}

// --- MODO SEGUIDOR DE LÍNEA ---
void Line_Follower_Mode() {
    int valLeft = digitalRead(IR_LEFT);
    int valRight = digitalRead(IR_RIGHT);

    // Velocidades
    int forwardSpeed = 110; 
    int turnSpeed = 90;     

    if (valLeft == 0 && valRight == 0) {
        // 1. LOS DOS PRENDIDOS (Piso blanco) -> Avanza hacia el frente
        Motor(Move_Forward, forwardSpeed, forwardSpeed, forwardSpeed, forwardSpeed);
    } 
    else if (valLeft == 1 && valRight == 0) {
        // 2. IZQUIERDO APAGADO (Tocó línea) -> Gira izquierda para centrarse
        Motor(Left_Rotate, turnSpeed, turnSpeed, turnSpeed, turnSpeed);
    } 
    else if (valLeft == 0 && valRight == 1) {
        // 3. DERECHO APAGADO (Tocó línea) -> Gira derecha para centrarse
        Motor(Right_Rotate, turnSpeed, turnSpeed, turnSpeed, turnSpeed);
    } 
    else if (valLeft == 1 && valRight == 1) {
        // 4. LOS DOS APAGADOS (Pisó la cinta con ambos) -> Avanza hacia el frente 
        Motor(Move_Forward, forwardSpeed, forwardSpeed, forwardSpeed, forwardSpeed);
    }
}

// --- MODO EVASIÓN (AVOID) ---
void Ultrasonic_Avoidance() {
    float dist = checkdistance();

    if (dist > 0 && dist < 18) {
        // NIVEL 1: Peligro Inminente
        Motor(Stop, 0, 0, 0, 0);
        delay(100); 
        Motor(Move_Backward, 160, 160, 160, 160);
        delay(250);
        Motor(Left_Rotate, 180, 180, 180, 180);
        delay(300);
    } 
    else if (dist >= 18 && dist < 45) {
        // NIVEL 2: Esquive suave
        Motor(Left_Rotate, 140, 140, 140, 140); 
    } 
    else {
        // Camino libre
        Motor(Move_Forward, 130, 130, 130, 130);
    }
}

// --- MODO SEGUIDOR (FOLLOW) ---
void Ultrasonic_Follow() {
    float dist = checkdistance();

    // 1. ZONA DE CONFORT (15cm a 25cm): Se queda quieto
    if (dist >= 15 && dist <= 25) {
        Motor(Stop, 0, 0, 0, 0);
    } 
    // 2. PELIGRO DE CHOQUE (< 15cm): Retrocede suavemente
    else if (dist < 15 && dist > 0) {
        Motor(Move_Backward, 140, 140, 140, 140);
    } 
    // 3. MODO PERSECUCIÓN (25cm a 50cm): Te sigue derecho
    else if (dist > 25 && dist <= 50) {
        Motor(Move_Forward, 130, 130, 130, 130);
    } 
    // 4. OBJETIVO PERDIDO (> 50cm): Modo Escáner
    else {
        // En vez de detenerse, gira suavemente para escanear el área
        // En cuanto vuelva a meter en su "cono", saltará al paso 3 y avanzará.
        Motor(Right_Rotate, 110, 110, 110, 110); 
    }
}

void setup() {
    mySerial.begin(9600);
    Serial.begin(9600);
    
    // Configuración de pines Shield
    pinMode(DIR_CLK, OUTPUT);
    pinMode(DATA, OUTPUT);
    pinMode(DIR_EN, OUTPUT);
    pinMode(DIR_LATCH, OUTPUT);
    
    // Configuración de sensores
    pinMode(Trig, OUTPUT);
    pinMode(Echo, INPUT);
    pinMode(IR_LEFT, INPUT);  // Declaramos pines analógicos como entradas digitales
    pinMode(IR_RIGHT, INPUT);

    Motor(Stop, 0, 0, 0, 0);
}

void loop() {
    // 1. LECTURA DE COMANDOS (Sin bloqueos)
    while (mySerial.available() > 0) {
        char c = (char)mySerial.read();
        if (c == '%') { 
            BT_value_temp = ""; 
        } else if (c == '#') { 
            BT_value = BT_value_temp; 
            BT_value_temp = "";
            if (BT_value.length() > 0) {
                currentMode = BT_value[0]; // Actualizar estado de la máquina
            }
        } else { 
            BT_value_temp += c; 
        }
    }

    // 2. EJECUCIÓN DEL MODO SELECCIONADO
    switch (currentMode) {
        case 'A': Motor(Move_Forward, Speed, Speed, Speed, Speed); break;
        case 'B': Motor(Move_Backward, Speed, Speed, Speed, Speed); break;
        case 'C': Motor(Left_Move, Speed, Speed, Speed, Speed); break;
        case 'D': Motor(Right_Move, Speed, Speed, Speed, Speed); break;
        case 'E': Motor(Left_Rotate, Speed, Speed, Speed, Speed); break;
        case 'F': Motor(Right_Rotate, Speed, Speed, Speed, Speed); break;
        case 'G': Motor(Upper_Left_Move, Speed, Speed, Speed, Speed); break;
        case 'H': Motor(Upper_Right_Move, Speed, Speed, Speed, Speed); break;
        case 'I': Motor(Lower_Left_Move, Speed, Speed, Speed, Speed); break;
        case 'J': Motor(Lower_Right_Move, Speed, Speed, Speed, Speed); break;
        case 'T': Ultrasonic_Avoidance(); break;
        case 'W': Ultrasonic_Follow(); break;
        case 'X': Line_Follower_Mode(); break; 
        case 'S': Motor(Stop, 0, 0, 0, 0); break;
        default: Motor(Stop, 0, 0, 0, 0); break;
    }
}