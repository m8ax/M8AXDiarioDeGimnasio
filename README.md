\# 💪 M8AX - Diario De Gimnasio 💪



!\[Kotlin](https://img.shields.io/badge/Kotlin-88%25-blue?style=for-the-badge\&logo=kotlin) !\[HTML](https://img.shields.io/badge/HTML-12%25-orange?style=for-the-badge\&logo=html5) !\[Status](https://img.shields.io/badge/Status-Estable-green?style=for-the-badge) !\[Android](https://img.shields.io/badge/Android-SDK%2024+-brightgreen?style=for-the-badge\&logo=android)



Este repositorio alberga un proyecto de ingeniería de software para Android de gran envergadura, desarrollado íntegramente en Kotlin en un 88% y Html en un 12%. Lo que comenzó como un diario de entrenamiento físico ha evolucionado hasta convertirse en una plataforma multidisciplinar que integra módulos de salud, motores de cálculo matemático de alto rendimiento, herramientas de seguridad biométrica y sistemas de análisis multimedia. Con más de 25,800 líneas de código y aproximadamente 115 horas de desarrollo, este ecosistema destaca por su robustez y versatilidad técnica.



\## 🛠️ Tecnologías Principales



\* \*\*Lenguaje:\*\* Kotlin.

\* \*\*Base de Datos:\*\* Room para persistencia local de entrenamientos \*\*\*\*.

\* \*\*Seguridad:\*\* Cifrado AES-256 (GCM) y EncryptedSharedPreferences para la gestión de contraseñas \*\*\*\*.

\* \*\*Multimedia:\*\* MediaPlayer, AudioRecord, Visualizer API y Jsoup para RSS \*\*\*\*.

\* \*\*Cálculo:\*\* Algoritmos avanzados como Pollard's Rho para factorización y Criba de Segmentos para números primos \*\*\*\*.



\## ◼ Arquitectura de Datos y Persistencia



El sistema utiliza una infraestructura de datos dual basada en Room, garantizando la integridad y el aislamiento de la información sensible.



\* \*\*AppDatabase.kt:\*\* Gestiona la base de datos principal (M8AX-Gimnasio\_DB) para el seguimiento de la actividad física.

\* \*\*AppDatabase2.kt:\*\* Administra la base de datos independiente (M8AX-Glucosa\_DB) destinada a registros médicos.

\* \*\*Gimnasio.kt:\*\* Clase de datos (Entity) que define el esquema de los registros de entrenamiento, incluyendo marcas de tiempo y diarios personales.

\* \*\*Glucosa.kt:\*\* Entidad diseñada para la persistencia de niveles de azúcar en sangre.

\* \*\*GimnasioDao.kt:\*\* Interfaz de acceso a datos que implementa consultas complejas, como la suma de minutos activos y la obtención de tendencias históricas.

\* \*\*GlucosaDao.kt:\*\* Define operaciones de limpieza y recuperación de registros de salud.

\* \*\*GimnasioAdapter.kt:\*\* Controlador de interfaz para listas de entrenamiento que implementa lógica de formateo en notación romana y alertas por voz.

\* \*\*GlucosaAdapter.kt:\*\* Gestiona la visualización dinámica de datos médicos, aplicando códigos de colores según el rango de los valores registrados.

\* \*\*BackupUtils.kt:\*\* Proporciona funciones críticas para la exportación y restauración de las bases de datos .db mediante el sistema de archivos del dispositivo.



\## ◼ Núcleo Operativo y Gestión de Salud



\* \*\*MainActivity.kt:\*\* El centro neurálgico de la aplicación. Integra el análisis de constancia de entrenamiento, el precio del Bitcoin en tiempo real y servicios meteorológicos basados en la ubicación IP del usuario.

\* \*\*AppGlucosa.kt:\*\* Módulo de seguimiento médico que permite la generación de informes en PDF profesional con análisis de tendencias integrados.

\* \*\*FumarActivity.kt:\*\* Herramienta avanzada para la cesación tabáquica que calcula estadísticas de salud precisas, incluyendo el ahorro económico, la longitud de cigarrillos evitados y el volumen de aire recuperado.

\* \*\*FlexionesActivity.kt:\*\* Implementa el uso del sensor de proximidad para la contabilización automática de repeticiones de ejercicio sin necesidad de contacto físico con el terminal.

\* \*\*RutinasGim.kt:\*\* Actividad que carga interfaces web dinámicas para la planificación de entrenamientos específicos.

\* \*\*M8axGimActivity.kt:\*\* Acceso a recursos web externos sobre salud y bienestar. (Añadido desde la primera versión)



\## ◼ Motores de Cálculo de Alto Rendimiento



\* \*\*PrimosActivity.kt:\*\* Implementa una criba segmentada de Eratóstenes para la búsqueda masiva de números primos en rangos extensos. Genera informes estadísticos detallados sobre primos gemelos, capicúas y reversibles.

\* \*\*FactorizacionActivity.kt:\*\* Utiliza el algoritmo Pollard's Rho para descomponer números de gran magnitud (hasta 50 dígitos) en sus factores primos, empleando hilos de ejecución paralelos para optimizar el tiempo de respuesta.

\* \*\*CifrasActivity.kt:\*\* Motor de resolución del juego de cifras que aplica una búsqueda en anchura (BFS) para encontrar combinaciones aritméticas exactas en milisegundos.



\## ◼ Seguridad, Herramientas y Utilidades



\* \*\*PasswordsActivity.kt:\*\* Gestor de credenciales que utiliza cifrado AES-GCM-256 para el almacenamiento seguro.

\* \*\*PasswordManager.kt:\*\* Encargado de la gestión de la llave maestra mediante EncryptedSharedPreferences y la autenticación biométrica.

\* \*\*PasswordsAdapter.kt:\*\* Administra la visualización y copia segura de las contraseñas cifradas al portapapeles.

\* \*\*AstronomiaActivity.kt:\*\* Centro de datos astronómicos que calcula en tiempo real la iluminación lunar, posiciones solares y distancias espaciales según la geolocalización.

\* \*\*ActivityCalendarioAnual.kt:\*\* Generador masivo de calendarios PDF que soporta cálculos hasta el año 1,000,000.

\* \*\*CalendarioActivity.kt:\*\* Interfaz visual para la navegación por meses con indicadores de actividad física y fases lunares.

\* \*\*CrearQrActivity.kt:\*\* Generador de códigos QR de alta definición (2048px) que permite la inserción de logotipos personalizados.

\* \*\*VerticalCaptureActivity.kt:\*\* Actividad especializada en el escaneo vertical de códigos de barras y QR.

\* \*\*ListaCompraActivity.kt y NotasActivity.kt:\*\* Gestores de listas con soporte para notación romana, serialización JSON y exportación a documentos PDF.

\* \*\*ListaCompraAdapter.kt y NotasAdapter.kt:\*\* Controladores de vista que permiten el tachado dinámico de ítems y la confirmación por voz.

\* \*\*RelojGrandeActivity.kt:\*\* El reloj grande con noticias y voz.

\* \*\*RelojActivity.kt:\*\* Cronómetro y reloj ajustable.

\* \*\*M8AXRelojes.kt:\*\* Relojes internacionales y zonas horarias.

\* \*\*CriptoPrecios.kt:\*\* Monitor de criptomonedas.

\* \*\*ElTiempoFullComunidad.kt y ElTiempoActivity.kt:\*\* Previsiones meteorológicas.

\* \*\*SalvapantallasActivity.kt:\*\* Modo estético, animaciones.

\* \*\*TickerActivity.kt:\*\* Ticker de mensajes estilo LED.

\* \*\*M8AXMapas.kt:\*\* Integración GPS/mapas.

\* \*\*NavidadActivity.kt:\*\* Módulo temático navideño.



\## ◼ Multimedia, Inteligencia Artificial y Ocio



\* \*\*AudioEventosActivity.kt:\*\* Monitor de sonido ambiental que integra un osciloscopio, visualizadores de ondas y medidores VU en tiempo real.

\* \*\*ReproductorActivity.kt:\*\* Reproductor de música avanzado que escanea directorios de forma recursiva y gestiona metadatos e imágenes de carátula.

\* \*\*RadiosOnlineActivity.kt:\*\* Acceso a un catálogo de 42 emisoras de radio en streaming.

\* \*\*RadioService.kt:\*\* Servicio de primer plano que garantiza la reproducción ininterrumpida de audio y el control desde la barra de notificaciones.

\* \*\*CelebresVozActivity.kt:\*\* Sistema que recupera y narra frases célebres mediante síntesis de voz (TTS).

\* \*\*WikiInfinityActivity.kt y WikiHowActivity.kt:\*\* Lectores automáticos de artículos de Wikipedia y fuentes RSS para el aprendizaje continuo durante el entrenamiento.

\* \*\*ChatGPTActivity.kt:\*\* Interfaz integrada para la interacción con múltiples modelos de Inteligencia Artificial.

\* \*\*TetrisActivity.kt:\*\* Versión nativa del clásico juego con lógica de niveles y velocidad progresiva.

\* \*\*ChessActivity.kt, DamasActivity.kt, Conecta4.kt y ThePong.kt:\*\* Implementaciones híbridas de juegos de mesa y clásicos mediante WebViews con soporte para modo offline.



\## ◼ Componentes Visuales y Gráficos Personalizados



\* \*\*GraficaSimple.kt y GraficaSimple2.kt:\*\* Motores de renderizado para polígonos de tendencia, líneas de media y ejes estadísticos.

\* \*\*OndaView.kt:\*\* Visualizador dinámico que transforma la FFT del audio en barras animadas de colores.

\* \*\*CustomAnalogClock.kt:\*\* Reloj analógico táctil con soporte para zoom gestual y segundero continuo.

\* \*\*TickerView.kt:\*\* Implementación de un ticker LED deslizante para mensajes de gran longitud.

\* \*\*LunaView.kt:\*\* Vista especializada en el dibujo dinámico de las fases lunares.



\## ◼ Automatización y Seguridad del Sistema



\* \*\*SplashActivity.kt:\*\* Pantalla de inicio con lógica estacional y motivacional.

\* \*\*LoginActivity.kt:\*\* Sistema de acceso con protección biométrica y gestión de contraseñas de seguridad.

\* \*\*BootReceiver.kt:\*\* Asegura que los recordatorios de entrenamiento se reprogramen automáticamente tras un reinicio del terminal.

\* \*\*GimnasioReminderReceiver.kt:\*\* Controlador de notificaciones push con mensajes motivadores personalizados.



\## 📦 Stack Tecnológico y Dependencias (Gradle)



Para alcanzar este nivel de robustez y versatilidad, \*\*M8AX\*\* exprime el ecosistema de Android mediante la integración de las siguientes librerías:



\### 📱 Core y UI Framework

\* \*\*`androidx.core:core-ktx`\*\*: Extensiones de Kotlin para las APIs nativas de Android, permitiendo un código más conciso y eficiente.

\* \*\*`androidx.appcompat:appcompat`\*\*: Garantiza la compatibilidad de componentes modernos en versiones anteriores del sistema.

\* \*\*`com.google.android.material:material`\*\*: Implementación de \*\*Material Design 3\*\* para una interfaz profesional (botones, diálogos, menús).

\* \*\*`androidx.constraintlayout:constraintlayout`\*\*: Motor de maquetación avanzada para interfaces dinámicas y complejas sin pérdida de rendimiento.

\* \*\*`androidx.recyclerview:recyclerview`\*\*: Gestión eficiente de listas masivas de datos con reciclaje de vistas.

\* \*\*`me.zhanghai.android.fastscroll`\*\*: Implementación de scroll rápido para navegar instantáneamente entre miles de registros.



\### 🗄️ Persistencia y Datos

\* \*\*`androidx.room:room-runtime` \& `room-compiler`\*\*: Capa de abstracción sobre SQLite para la gestión segura de las bases de datos de entrenamientos y salud.

\* \*\*`com.google.code.gson:gson`\*\*: Serialización y deserialización de objetos Java/Kotlin a JSON para el manejo de notas y listas.



\### 🛡️ Seguridad y Criptografía

\* \*\*`androidx.security:security-crypto`\*\*: Gestión de claves maestras y almacenamiento cifrado mediante `EncryptedSharedPreferences`.

\* \*\*`androidx.biometric:biometric`\*\*: Integración de seguridad biométrica (huella dactilar/rostro) para el acceso a datos sensibles.

\* \*\*`com.madgag.spongycastle:core` \& `prov`\*\*: Adaptación de Bouncy Castle para Android, proporcionando algoritmos de \*\*criptografía avanzada\*\*.



\### 📄 Generación de Documentos

\* \*\*`com.itextpdf:itextg`\*\*: Motor especializado en la creación de \*\*informes médicos y calendarios PDF\*\* de alta complejidad directamente desde el terminal.



\### 🌐 Conectividad y Web Scraping

\* \*\*`com.squareup.okhttp3:okhttp`\*\*: Cliente HTTP de alto rendimiento para la sincronización de precios de Bitcoin, Criptomonedas y Clima.

\* \*\*`org.jsoup:jsoup`\*\*: Parser de HTML avanzado para la extracción de datos de Wikipedia y procesamiento de fuentes RSS.



\### 🔭 Astronomía y Cálculo Científico

\* \*\*`org.shredzone.commons:commons-suncalc`\*\*: Motor de cálculo astronómico para determinar fases lunares y posiciones solares con precisión milimétrica.



\### 📸 Multimedia y Visión Artificial

\* \*\*`com.journeyapps:zxing-android-embedded` \& `zxing:core`\*\*: Suite completa para la generación y escaneo de códigos \*\*QR y códigos de barras\*\*.

\* \*\*`com.github.bumptech.glide:glide` \& `compiler`\*\*: Sistema de carga y almacenamiento en caché de imágenes para un rendimiento visual fluido.

\* \*\*`com.davemorrissey.labs:subsampling-scale-image-view`\*\*: Visor de imágenes de alta resolución con soporte para zoom gestual masivo.



\### 📧 Comunicación

\* \*\*`com.sun.mail:android-mail` \& `android-activation`\*\*: Soporte nativo para el envío de correos electrónicos y exportación de reportes desde la aplicación.



---



\### ⚙️ Configuración del Entorno

\* \*\*Compile/Target SDK:\*\* 34 (Android 14)

\* \*\*Min SDK:\*\* 26 (Android 8.0 Oreo)

\* \*\*Java Version:\*\* 17 (JVM Target 17)

\* \*\*ViewBinding:\*\* Activado para seguridad de tipos en la UI.



\## 🔐 Permisos y Control del Sistema (AndroidManifest)



El ecosistema \*\*M8AX\*\* requiere un control avanzado del hardware para garantizar su funcionalidad multidisciplinar. A continuación se detallan los permisos y componentes clave declarados:



\### 🛠️ Permisos de Hardware y Sensores

\* \*\*Cámara (`CAMERA`)\*\*: Utilizada para el escaneo de códigos QR y de barras en `VerticalCaptureActivity`.

\* \*\*Audio (`RECORD\_AUDIO`)\*\*: Necesario para el monitor de sonido ambiental y el osciloscopio en tiempo real de `AudioEventosActivity`.

\* \*\*Ubicación (`ACCESS\_FINE\_LOCATION`, `ACCESS\_COARSE\_LOCATION`)\*\*: Crucial para el cálculo de efemérides en `AstronomiaActivity` y la precisión meteorológica en `ElTiempoActivity`.

\* \*\*Biometría (`USE\_BIOMETRIC`)\*\*: Seguridad de grado militar para el acceso a `PasswordsActivity` y el inicio de sesión.

\* \*\*Estado del Teléfono (`READ\_PHONE\_STATE`)\*\*: Gestiona la pausa automática de la radio o multimedia ante llamadas entrantes.



\### 📂 Gestión de Archivos y Almacenamiento

\* \*\*Almacenamiento Multimedia (`READ\_MEDIA\_IMAGES`, `VIDEO`, `AUDIO`)\*\*: Acceso optimizado para Android 13+ para la gestión del reproductor de música y exportación de informes.

\* \*\*Legado (`WRITE\_EXTERNAL\_STORAGE`)\*\*: Mantenido hasta SDK 28 para asegurar la compatibilidad de backups de la base de datos en dispositivos antiguos.



\### 📡 Red y Segundo Plano

\* \*\*Servicios en Primer Plano (`FOREGROUND\_SERVICE`, `MEDIA\_PLAYBACK`)\*\*: Garantiza que la radio online y el reproductor no se detengan aunque el usuario esté entrenando con la pantalla apagada.

\* \*\*Alarmas Exactas (`SCHEDULE\_EXACT\_ALARM`)\*\*: Para que los recordatorios de entrenamiento y salud (`GimnasioReminderReceiver`) se disparen en el segundo exacto.

\* \*\*Autoinicio (`RECEIVE\_BOOT\_COMPLETED`)\*\*: El `BootReceiver` reprograma todas las alertas automáticamente al reiniciar el terminal.



---



\## 🏛️ Estructura de Actividades y Servicios



La aplicación se organiza en un robusto sistema de componentes especializados:



\* \*\*`RadioService`\*\*: Servicio dedicado con declaración de tipo `mediaPlayback` para evitar interrupciones del sistema.

\* \*\*`BootReceiver`\*\*: Receptor de difusión para garantizar la persistencia de tareas tras el reinicio.

\* \*\*Configuración de Pantalla\*\*: 

&nbsp;   \* Actividades como `RelojGrandeActivity` y `SalvapantallasActivity` fuerzan el modo \*\*Landscape\*\* y mantienen la pantalla encendida (`keepScreenOn`).

&nbsp;   \* `MainActivity` y `AppGlucosa` utilizan `adjustResize` y `adjustPan` para una gestión inteligente del teclado virtual.

\* \*\*Seguridad de Red\*\*: Se habilita `usesCleartextTraffic="true"` para permitir la conexión con servidores de streaming de radio específicos que no utilizan protocolos cifrados.



\## ◼ Notas finales



Este resumen detalla los componentes principales; no obstante, el proyecto contiene numerosos ficheros de soporte adicionales, clases de datos y archivos de configuración que completan la robusta infraestructura de este sistema integral.



> \[!IMPORTANT]

> \*\*Estabilidad:\*\* La estabilidad de las bases de datos ha sido verificada mediante pruebas de estrés con un volumen de \*\*150,000 registros\*\*, lo que equivale aproximadamente a \*\*410 años de datos\*\* continuos sin presentar degradación relevante en el rendimiento.

>

> \*\*Rendimiento:\*\* Este software utiliza una arquitectura de hilos avanzada para garantizar que los cálculos pesados (como la factorización) no bloqueen la interfaz de usuario, manteniendo siempre la fluidez del sistema.



---



\# 🇺🇸 English Version



This repository houses a large-scale Android software engineering project, developed entirely with 88% Kotlin and 12% HTML. What began as a physical training diary has evolved into a multidisciplinary platform integrating health modules, high-performance mathematical calculation engines, biometric security tools, and multimedia analysis systems. With over 25,800 lines of code and approximately 115 development hours, this ecosystem stands out for its technical robustness and versatility.



\## 🛠️ Main Technologies



\* \*\*Language:\*\* Kotlin.

\* \*\*Database:\*\* Room for local training persistence \*\*\*\*.

\* \*\*Security:\*\* AES-256 (GCM) encryption and EncryptedSharedPreferences for password management \*\*\*\*.

\* \*\*Multimedia:\*\* MediaPlayer, AudioRecord, Visualizer API, and Jsoup for RSS \*\*\*\*.

\* \*\*Calculation:\*\* Advanced algorithms such as Pollard's Rho for factorization and Segmented Sieve for prime numbers \*\*\*\*.



\## ◼ Data Architecture and Persistence



The system utilizes a dual Room-based data infrastructure, guaranteeing integrity and isolation of sensitive information.



\* \*\*AppDatabase.kt:\*\* Manages the main database (M8AX-Gimnasio\_DB) for physical activity tracking.

\* \*\*AppDatabase2.kt:\*\* Administers the independent database (M8AX-Glucosa\_DB) destined for medical records.

\* \*\*Gimnasio.kt:\*\* Data class (Entity) defining the schema for training records, including timestamps and personal diaries.

\* \*\*Glucosa.kt:\*\* Entity designed for blood sugar level persistence.

\* \*\*GimnasioDao.kt:\*\* Data access interface implementing complex queries, such as the summation of active minutes and retrieval of historical trends.

\* \*\*GlucosaDao.kt:\*\* Defines cleanup operations and health record recovery.

\* \*\*GimnasioAdapter.kt:\*\* Interface controller for training lists implementing Roman notation formatting logic and voice alerts.

\* \*\*GlucosaAdapter.kt:\*\* Manages dynamic visualization of medical data, applying color codes based on the range of recorded values.

\* \*\*BackupUtils.kt:\*\* Provides critical functions for exporting and restoring .db databases via the device's file system.



\## ◼ Operational Core and Health Management



\* \*\*MainActivity.kt:\*\* The application's neural center. Integrates training consistency analysis, real-time Bitcoin pricing, and weather services based on the user's IP location.

\* \*\*AppGlucosa.kt:\*\* Medical tracking module allowing for professional PDF report generation with integrated trend analysis.

\* \*\*FumarActivity.kt:\*\* Advanced smoking cessation tool calculating precise health statistics, including financial savings, length of cigarettes avoided, and recovered air volume.

\* \*\*FlexionesActivity.kt:\*\* Implements the use of the proximity sensor for automatic exercise repetition counting without the need for physical contact with the terminal.

\* \*\*RutinasGim.kt:\*\* Activity that loads dynamic web interfaces for specific training planning.

\* \*\*M8axGimActivity.kt:\*\* Access to external web resources on health and wellness. (Added since the first version)



\## ◼ High-Performance Calculation Engines



\* \*\*PrimosActivity.kt:\*\* Implements a segmented Sieve of Eratosthenes for massive prime number searches in extensive ranges. Generates detailed statistical reports on twin, palindromic, and reversible primes.

\* \*\*FactorizacionActivity.kt:\*\* Uses the Pollard's Rho algorithm to decompose large magnitude numbers (up to 50 digits) into their prime factors, employing parallel execution threads to optimize response time.

\* \*\*CifrasActivity.kt:\*\* "Cifras" game resolution engine applying a Breadth-First Search (BFS) to find exact arithmetic combinations in milliseconds.



\## ◼ Security, Tools, and Utilities



\* \*\*PasswordsActivity.kt:\*\* Credential manager using AES-GCM-256 encryption for secure storage.

\* \*\*PasswordManager.kt:\*\* Responsible for master key management via EncryptedSharedPreferences and biometric authentication.

\* \*\*PasswordsAdapter.kt:\*\* Manages the visualization and secure copying of encrypted passwords to the clipboard.

\* \*\*AstronomiaActivity.kt:\*\* Astronomical data center calculating real-time lunar illumination, solar positions, and space distances based on geolocation.

\* \*\*ActivityCalendarioAnual.kt:\*\* Massive PDF calendar generator supporting calculations up to the year 1,000,000.

\* \*\*CalendarioActivity.kt:\*\* Visual interface for month-by-month navigation with physical activity indicators and lunar phases.

\* \*\*CrearQrActivity.kt:\*\* High-definition QR code generator (2048px) allowing for custom logo insertion.

\* \*\*VerticalCaptureActivity.kt:\*\* Specialized activity for vertical barcode and QR scanning.

\* \*\*ListaCompraActivity.kt and NotasActivity.kt:\*\* List managers with Roman notation support, JSON serialization, and export to PDF documents.

\* \*\*ListaCompraAdapter.kt and NotasAdapter.kt:\*\* View controllers allowing for dynamic item crossing and voice confirmation.

\* \*\*RelojGrandeActivity.kt:\*\* The big clock with news and voice.

\* \*\*RelojActivity.kt:\*\* Stopwatch and adjustable clock.

\* \*\*M8AXRelojes.kt:\*\* International clocks and time zones.

\* \*\*CriptoPrecios.kt:\*\* Cryptocurrency monitor.

\* \*\*ElTiempoFullComunidad.kt and ElTiempoActivity.kt:\*\* Weather forecasts.

\* \*\*SalvapantallasActivity.kt:\*\* Aesthetic mode, animations.

\* \*\*TickerActivity.kt:\*\* LED-style message ticker.

\* \*\*M8AXMapas.kt:\*\* GPS/Maps integration.

\* \*\*NavidadActivity.kt:\*\* Themed Christmas module.



\## ◼ Multimedia, Artificial Intelligence, and Leisure



\* \*\*AudioEventosActivity.kt:\*\* Ambient sound monitor integrating an oscilloscope, wave visualizers, and real-time VU meters.

\* \*\*ReproductorActivity.kt:\*\* Advanced music player that scans directories recursively and manages metadata and cover art.

\* \*\*RadiosOnlineActivity.kt:\*\* Access to a catalog of 42 streaming radio stations.

\* \*\*RadioService.kt:\*\* Foreground service ensuring uninterrupted audio playback and control from the notification bar.

\* \*\*CelebresVozActivity.kt:\*\* System that retrieves and narrates famous quotes via Text-to-Speech (TTS).

\* \*\*WikiInfinityActivity.kt and WikiHowActivity.kt:\*\* Automatic readers for Wikipedia articles and RSS feeds for continuous learning during training.

\* \*\*ChatGPTActivity.kt:\*\* Integrated interface for interaction with multiple Artificial Intelligence models.

\* \*\*TetrisActivity.kt:\*\* Native version of the classic game with level logic and progressive speed.

\* \*\*ChessActivity.kt, DamasActivity.kt, Conecta4.kt and ThePong.kt:\*\* Hybrid implementations of board games and classics via WebViews with offline support.



\## ◼ Visual Components and Custom Graphics



\* \*\*GraficaSimple.kt and GraficaSimple2.kt:\*\* Rendering engines for trend polygons, average lines, and statistical axes.

\* \*\*OndaView.kt:\*\* Dynamic visualizer transforming audio FFT into animated color bars.

\* \*\*CustomAnalogClock.kt:\*\* Tactile analog clock with gestural zoom support and continuous second hand.

\* \*\*TickerView.kt:\*\* Implementation of a sliding LED ticker for long messages.

\* \*\*LunaView.kt:\*\* Specialized view for dynamic drawing of lunar phases.



\## ◼ System Automation and Security



\* \*\*SplashActivity.kt:\*\* Start screen with seasonal and motivational logic.

\* \*\*LoginActivity.kt:\*\* Access system with biometric protection and security password management.

\* \*\*BootReceiver.kt:\*\* Ensures that training reminders are automatically rescheduled after a terminal reboot.

\* \*\*GimnasioReminderReceiver.kt:\*\* Push notification controller with personalized motivational messages.



\## 📦 Tech Stack and Dependencies (Gradle)



To achieve this level of robustness and versatility, \*\*M8AX\*\* leverages the Android ecosystem through the integration of the following libraries:



\### 📱 Core and UI Framework

\* \*\*`androidx.core:core-ktx`\*\*: Kotlin extensions for native Android APIs, allowing for more concise and efficient code.

\* \*\*`androidx.appcompat:appcompat`\*\*: Guarantees compatibility of modern components on older system versions.

\* \*\*`com.google.android.material:material`\*\*: Implementation of \*\*Material Design 3\*\* for a professional interface (buttons, dialogs, menus).

\* \*\*`androidx.constraintlayout:constraintlayout`\*\*: Advanced layout engine for dynamic and complex interfaces without performance loss.

\* \*\*`androidx.recyclerview:recyclerview`\*\*: Efficient management of massive data lists with view recycling.

\* \*\*`me.zhanghai.android.fastscroll`\*\*: Fast scroll implementation to instantly navigate through thousands of records.



\### 🗄️ Persistence and Data

\* \*\*`androidx.room:room-runtime` \& `room-compiler`\*\*: Abstraction layer over SQLite for secure management of training and health databases.

\* \*\*`com.google.code.gson:gson`\*\*: Serialization and deserialization of Java/Kotlin objects to JSON for handling notes and lists.



\### 🛡️ Security and Cryptography

\* \*\*`androidx.security:security-crypto`\*\*: Master key management and encrypted storage via `EncryptedSharedPreferences`.

\* \*\*`androidx.biometric:biometric`\*\*: Integration of biometric security (fingerprint/face) for access to sensitive data.

\* \*\*`com.madgag.spongycastle:core` \& `prov`\*\*: Android adaptation of Bouncy Castle, providing \*\*advanced cryptography\*\* algorithms.



\### 📄 Document Generation

\* \*\*`com.itextpdf:itextg`\*\*: Engine specialized in creating \*\*medical reports and PDF calendars\*\* of high complexity directly from the terminal.



\### 🌐 Connectivity and Web Scraping

\* \*\*`com.squareup.okhttp3:okhttp`\*\*: High-performance HTTP client for synchronizing Bitcoin, Cryptocurrency, and Weather prices.

\* \*\*`org.jsoup:jsoup`\*\*: Advanced HTML parser for data extraction from Wikipedia and processing of RSS feeds.



\### 🔭 Astronomy and Scientific Calculation

\* \*\*`org.shredzone.commons:commons-suncalc`\*\*: Astronomical calculation engine to determine lunar phases and solar positions with millimeter precision.



\### 📸 Multimedia and Computer Vision

\* \*\*`com.journeyapps:zxing-android-embedded` \& `zxing:core`\*\*: Complete suite for \*\*QR and Barcode\*\* generation and scanning.

\* \*\*`com.github.bumptech.glide:glide` \& `compiler`\*\*: Image loading and caching system for fluid visual performance.

\* \*\*`com.davemorrissey.labs:subsampling-scale-image-view`\*\*: High-resolution image viewer with support for massive gestural zoom.



\### 📧 Communication

\* \*\*`com.sun.mail:android-mail` \& `android-activation`\*\*: Native support for sending emails and exporting reports from the application.



---



\### ⚙️ Environment Configuration

\* \*\*Compile/Target SDK:\*\* 34 (Android 14)

\* \*\*Min SDK:\*\* 26 (Android 8.0 Oreo)

\* \*\*Java Version:\*\* 17 (JVM Target 17)

\* \*\*ViewBinding:\*\* Activated for type safety in the UI.



\## 🔐 Permissions and System Control (AndroidManifest)



The \*\*M8AX\*\* ecosystem requires advanced hardware control to guarantee its multidisciplinary functionality. Key declared permissions and components are detailed below:



\### 🛠️ Hardware Permissions and Sensors

\* \*\*Camera (`CAMERA`)\*\*: Used for QR and barcode scanning in `VerticalCaptureActivity`.

\* \*\*Audio (`RECORD\_AUDIO`)\*\*: Necessary for the ambient sound monitor and real-time oscilloscope in `AudioEventosActivity`.

\* \*\*Location (`ACCESS\_FINE\_LOCATION`, `ACCESS\_COARSE\_LOCATION`)\*\*: Crucial for ephemeris calculation in `AstronomiaActivity` and weather precision in `ElTiempoActivity`.

\* \*\*Biometrics (`USE\_BIOMETRIC`)\*\*: Military-grade security for access to `PasswordsActivity` and login.

\* \*\*Phone State (`READ\_PHONE\_STATE`)\*\*: Manages automatic pausing of radio or multimedia upon incoming calls.



\### 📂 File and Storage Management

\* \*\*Multimedia Storage (`READ\_MEDIA\_IMAGES`, `VIDEO`, `AUDIO`)\*\*: Optimized access for Android 13+ for music player management and report export.

\* \*\*Legacy (`WRITE\_EXTERNAL\_STORAGE`)\*\*: Maintained up to SDK 28 to ensure compatibility of database backups on older devices.



\### 📡 Network and Background

\* \*\*Foreground Services (`FOREGROUND\_SERVICE`, `MEDIA\_PLAYBACK`)\*\*: Guarantees that the online radio and player do not stop even if the user is training with the screen off.

\* \*\*Exact Alarms (`SCHEDULE\_EXACT\_ALARM`)\*\*: So that training and health reminders (`GimnasioReminderReceiver`) trigger at the exact second.

\* \*\*Auto-start (`RECEIVE\_BOOT\_COMPLETED`)\*\*: The `BootReceiver` reschedules all alerts automatically upon terminal reboot.



---



\## 🏛️ Activity and Service Structure



The application is organized into a robust system of specialized components:



\* \*\*`RadioService`\*\*: Dedicated service with `mediaPlayback` type declaration to avoid system interruptions.

\* \*\*`BootReceiver`\*\*: Broadcast receiver to guarantee task persistence after reboot.

\* \*\*Screen Configuration\*\*: 

&nbsp;   \* Activities such as `RelojGrandeActivity` and `SalvapantallasActivity` force \*\*Landscape\*\* mode and keep the screen on (`keepScreenOn`).

&nbsp;   \* `MainActivity` and `AppGlucosa` use `adjustResize` and `adjustPan` for intelligent virtual keyboard management.

\* \*\*Network Security\*\*: `usesCleartextTraffic="true"` is enabled to allow connection with specific radio streaming servers that do not use encrypted protocols.



\## ◼ Final Notes



This summary details the main components; however, the project contains numerous additional support files, data classes, and configuration files that complete the robust infrastructure of this comprehensive system.



> \[!IMPORTANT]

> \*\*Stability:\*\* Database stability has been verified through stress tests with a volume of \*\*150,000 records\*\*, which is approximately equivalent to \*\*410 years of continuous data\*\* without presenting relevant performance degradation.

>

> \*\*Performance:\*\* This software utilizes an advanced threading architecture to ensure that heavy calculations (such as factorization) do not block the user interface, maintaining system fluidity at all times.

