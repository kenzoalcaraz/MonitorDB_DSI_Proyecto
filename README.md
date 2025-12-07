PROYECTO: SISTEMA DE MONITOREO

Introducción
En diversos entornos industriales, académicos y de investigación es necesario monitorear en tiempo real el comportamiento físico de un dispositivo o estructura, 
especialmente cuando intervienen variables dinámicas como el movimiento o la vibración.

El Sistema de Monitoreo es una aplicación desarrollada en Java que permite la captura, transmisión, 
almacenamiento y visualización de datos obtenidos desde un dispositivo Arduino. 

Capturas de la Aplicacion:

<img width="591" height="446" alt="Captura de pantalla 2025-12-03 141115" src="https://github.com/user-attachments/assets/9cc1d13e-409d-480a-8d36-d48ac72050ea" />
<img width="591" height="446" alt="Captura de pantalla 2025-12-03 141225" src="https://github.com/user-attachments/assets/b5f15131-6770-4f7b-89bc-5f6ade979696" />
<img width="591" height="446" alt="Captura de pantalla 2025-12-03 141249" src="https://github.com/user-attachments/assets/969bfdf5-cb66-4188-ada0-4eab70b9a1de" />

Video de funcionamiento:
https://drive.google.com/file/d/1S6lQVePvgGop4JvSPWSLfSYDM9n8dyLM/view?usp=sharing

DESAFIOS
- Desafío:Asegurar que los datos del Arduino tengan el formato correcto
- Solución: Se agregó validación para comprobar que cada lectura realmente contiene valores numéricos de X, Y y Z.

- Desafío:Prevenir que la base de datos crezca de forma descontrolada.
- Solución:Se estructuró el sistema para insertar solo datos válidos y filtrar correctamente las consultas por fecha y hora.

- Desafío:Proteger la información durante el envío.
- Solución:Se aplicó un método de encriptación (AES) que asegura que los datos viajen protegidos entre cliente y servidor.

- Desafío: Evitar bloqueos en la interfaz durante consultas.
- Solución: Uso de hilos secundarios para cargar datos mientras la interfaz permanece interactiva.

IMPLEMENTACION

El cliente incluye una Vista Monitor, donde se realiza la lectura y visualización en tiempo real, y una Vista Histórico, 
donde se consultan los datos almacenados y se muestran en una gráfica para su análisis. 
Todo el sistema cumple con una arquitectura clara y segura que separa las responsabilidades del cliente, el servidor y la base de datos.

El servidor, desarrollado como una aplicación de consola, recibe los datos del cliente, los descifra y los guarda en una base de datos SQLite. 
Además, responde solicitudes de consulta del cliente, permitiendo obtener todos los registros o filtrarlos por fecha y hora, también usando comunicación cifrada.

Conclusiones

El desarrollo de este Sistema de Monitoreo permitió aplicar los aprendizajes adquiridos en clases, algunas cosas nunca las había hecho antes, 
como la creación de gráficas, la utilización del Arduino para generar datos, la encriptación y la creación de archivos ".exe".
