# 💪 M8AX - Diario De Gimnasio 💪

![Kotlin](https://img.shields.io/badge/Kotlin-88%25-blue?style=for-the-badge&logo=kotlin)
![HTML](https://img.shields.io/badge/HTML-12%25-orange?style=for-the-badge&logo=html5)
![Status](https://img.shields.io/badge/Status-Estable-green?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-SDK%2024+-brightgreen?style=for-the-badge&logo=android)

---

Este repositorio alberga un proyecto de ingeniería de software para Android de gran envergadura, desarrollado íntegramente en **Kotlin (88%)** y **HTML (12%)**. Lo que comenzó como un diario de entrenamiento físico ha evolucionado hasta convertirse en una **plataforma multidisciplinar** que integra módulos de salud, motores de cálculo matemático de alto rendimiento, herramientas de seguridad biométrica y sistemas de análisis multimedia.

Con **más de 25.800 líneas de código** y aproximadamente **115 horas de desarrollo**, este ecosistema destaca por su **robustez**, **escalabilidad** y **versatilidad técnica**.

---

## 🛠️ Tecnologías Principales

- **Lenguaje:** Kotlin
- **Base de datos:** Room para persistencia local de entrenamientos
- **Seguridad:** Cifrado **AES-256 (GCM)** y `EncryptedSharedPreferences` para la gestión de contraseñas
- **Multimedia:** `MediaPlayer`, `AudioRecord`, `Visualizer API` y `Jsoup` para RSS
- **Cálculo:** Algoritmos avanzados como **Pollard’s Rho** (factorización) y **Criba Segmentada** (números primos)

---

## ◼ Arquitectura de Datos y Persistencia

El sistema utiliza una **infraestructura de datos dual** basada en Room, garantizando la integridad y el aislamiento de la información sensible.

- **AppDatabase.kt:** Base de datos principal `M8AX-Gimnasio_DB` para actividad física
- **AppDatabase2.kt:** Base de datos independiente `M8AX-Glucosa_DB` para registros médicos
- **Gimnasio.kt:** Entidad de entrenamientos con marcas temporales y diarios personales
- **Glucosa.kt:** Entidad para persistencia de niveles de glucosa
- **GimnasioDao.kt:** Consultas complejas (suma de minutos, tendencias históricas)
- **GlucosaDao.kt:** Limpieza y recuperación de registros médicos
- **GimnasioAdapter.kt:** Formateo en notación romana y alertas por voz
- **GlucosaAdapter.kt:** Visualización dinámica con códigos de color por rangos
- **BackupUtils.kt:** Exportación y restauración de bases de datos `.db`

---

## ◼ Núcleo Operativo y Gestión de Salud

- **MainActivity.kt:** Centro neurálgico. Analiza constancia de entrenamiento, precio de Bitcoin en tiempo real y meteorología por IP
- **AppGlucosa.kt:** Seguimiento médico con generación de informes PDF profesionales
- **FumarActivity.kt:** Cesación tabáquica con métricas avanzadas (ahorro, aire recuperado, cigarrillos evitados)
- **FlexionesActivity.kt:** Conteo automático de repeticiones mediante sensor de proximidad
- **RutinasGim.kt:** Interfaces web dinámicas para planificación de entrenamientos
- **M8axGimActivity.kt:** Recursos web externos de salud y bienestar (desde la primera versión)

---

## ◼ Motores de Cálculo de Alto Rendimiento

- **PrimosActivity.kt:** Criba segmentada de Eratóstenes con estadísticas de primos gemelos, capicúas y reversibles
- **FactorizacionActivity.kt:** Pollard’s Rho con hilos paralelos (hasta 50 dígitos)
- **CifrasActivity.kt:** Resolución del juego de cifras mediante BFS en milisegundos

---

## ◼ Seguridad, Herramientas y Utilidades

- **PasswordsActivity.kt:** Gestor de credenciales con AES‑GCM‑256
- **PasswordManager.kt:** Llave maestra y biometría con `EncryptedSharedPreferences`
- **PasswordsAdapter.kt:** Copia segura al portapapeles
- **AstronomiaActivity.kt:** Cálculo de iluminación lunar, posiciones solares y distancias espaciales
- **ActivityCalendarioAnual.kt:** Generador de calendarios PDF hasta el año 1.000.000
- **CalendarioActivity.kt:** Navegación mensual con indicadores de actividad y fases lunares
- **CrearQrActivity.kt:** Generador QR HD (2048px) con logotipos
- **VerticalCaptureActivity.kt:** Escaneo vertical de códigos de barras y QR
- **ListaCompraActivity.kt / NotasActivity.kt:** Listas con notación romana, JSON y exportación PDF
- **ListaCompraAdapter.kt / NotasAdapter.kt:** Tachado dinámico y confirmación por voz
- **RelojGrandeActivity.kt:** Reloj grande con noticias y voz
- **RelojActivity.kt:** Cronómetro y reloj ajustable
- **M8AXRelojes.kt:** Relojes internacionales y zonas horarias
- **CriptoPrecios.kt:** Monitor de criptomonedas
- **ElTiempoFullComunidad.kt / ElTiempoActivity.kt:** Previsión meteorológica
- **SalvapantallasActivity.kt:** Modo estético con animaciones
- **TickerActivity.kt:** Ticker LED de mensajes
- **M8AXMapas.kt:** Integración GPS / mapas
- **NavidadActivity.kt:** Módulo temático navideño

---

## ◼ Multimedia, Inteligencia Artificial y Ocio

- **AudioEventosActivity.kt:** Monitor ambiental con osciloscopio, FFT y VU meters
- **ReproductorActivity.kt:** Reproductor musical con escaneo recursivo y carátulas
- **RadiosOnlineActivity.kt:** Catálogo de 42 emisoras online
- **RadioService.kt:** Servicio en primer plano con control por notificaciones
- **CelebresVozActivity.kt:** Frases célebres con TTS
- **WikiInfinityActivity.kt / WikiHowActivity.kt:** Lectores automáticos de Wikipedia y RSS
- **ChatGPTActivity.kt:** Interfaz integrada con múltiples modelos de IA
- **TetrisActivity.kt:** Juego clásico nativo con dificultad progresiva
- **ChessActivity.kt / DamasActivity.kt / Conecta4.kt / ThePong.kt:** Juegos híbridos vía WebView con modo offline

---

## ◼ Componentes Visuales y Gráficos Personalizados

- **GraficaSimple.kt / GraficaSimple2.kt:** Renderizado de tendencias y medias
- **OndaView.kt:** Visualizador FFT animado
- **CustomAnalogClock.kt:** Reloj analógico táctil con zoom
- **TickerView.kt:** Ticker LED deslizante
- **LunaView.kt:** Dibujo dinámico de fases lunares

---

## ◼ Automatización y Seguridad del Sistema

- **SplashActivity.kt:** Inicio estacional y motivacional
- **LoginActivity.kt:** Acceso biométrico y contraseñas
- **BootReceiver.kt:** Reprogramación tras reinicio
- **GimnasioReminderReceiver.kt:** Notificaciones motivacionales

---

## 📦 Stack Tecnológico y Dependencias (Gradle)

### 📱 Core y UI

- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `androidx.constraintlayout:constraintlayout`
- `androidx.recyclerview:recyclerview`
- `me.zhanghai.android.fastscroll`

### 🗄️ Persistencia

- `androidx.room:room-runtime` / `room-compiler`
- `com.google.code.gson:gson`

### 🛡️ Seguridad

- `androidx.security:security-crypto`
- `androidx.biometric:biometric`
- `com.madgag.spongycastle:core` / `prov`

### 📄 Documentos

- `com.itextpdf:itextg`

### 🌐 Red y Scraping

- `com.squareup.okhttp3:okhttp`
- `org.jsoup:jsoup`

### 🔭 Astronomía

- `org.shredzone.commons:commons-suncalc`

### 📸 Multimedia

- `com.journeyapps:zxing-android-embedded` / `zxing:core`
- `com.github.bumptech.glide:glide` / `compiler`
- `com.davemorrissey.labs:subsampling-scale-image-view`

### 📧 Comunicación

- `com.sun.mail:android-mail` / `android-activation`

---

### ⚙️ Configuración del Entorno

- **Compile / Target SDK:** 34 (Android 14)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Java:** 17 (JVM Target 17)
- **ViewBinding:** Activado

---

## 🔐 Permisos y Control del Sistema

### 🛠️ Hardware y Sensores

- **CAMERA:** Escaneo QR / barras
- **RECORD_AUDIO:** Monitor ambiental
- **ACCESS_FINE_LOCATION / COARSE:** Astronomía y clima
- **USE_BIOMETRIC:** Seguridad biométrica
- **READ_PHONE_STATE:** Gestión multimedia ante llamadas

### 📂 Almacenamiento

- **READ_MEDIA_IMAGES / VIDEO / AUDIO:** Android 13+
- **WRITE_EXTERNAL_STORAGE:** Compatibilidad hasta SDK 28

### 📡 Red y Segundo Plano

- **FOREGROUND_SERVICE / MEDIA_PLAYBACK**
- **SCHEDULE_EXACT_ALARM**
- **RECEIVE_BOOT_COMPLETED**

---

## 🏛️ Estructura de Actividades y Servicios

- **RadioService:** Servicio `mediaPlayback`
- **BootReceiver:** Persistencia tras reinicio
- **Pantalla:** Landscape forzado y `keepScreenOn` en reloj y salvapantallas
- **Teclado:** `adjustResize` / `adjustPan`
- **Red:** `usesCleartextTraffic="true"` para radios legacy

---

## ◼ Notas Finales

> **Estabilidad:** Bases de datos validadas con **150.000 registros** (~410 años de datos).
>
> **Rendimiento:** Arquitectura multihilo para evitar bloqueos de UI incluso en cálculos pesados.

---

# 🇺🇸 English Version

*(Contenido íntegro en inglés conservado exactamente como en la versión original, con el mismo orden y estructura, listo para GitHub)*

---

# 🇺🇸 English Version

# 💪 M8AX - Gym Training Log 💪

![Kotlin](https://img.shields.io/badge/Kotlin-88%25-blue?style=for-the-badge&logo=kotlin)
![HTML](https://img.shields.io/badge/HTML-12%25-orange?style=for-the-badge&logo=html5)
![Status](https://img.shields.io/badge/Status-Stable-green?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-SDK%2024+-brightgreen?style=for-the-badge&logo=android)

---

This repository hosts a **large-scale Android software engineering project**, developed entirely with **88% Kotlin** and **12% HTML**. What originally began as a simple physical training diary has evolved into a **multidisciplinary platform** integrating health modules, high-performance mathematical computation engines, biometric security tools, and advanced multimedia analysis systems.

With **over 25,800 lines of code** and approximately **115 hours of development**, this ecosystem stands out for its **technical robustness, scalability, and architectural versatility**.

---

## 🛠️ Main Technologies

- **Language:** Kotlin  
- **Database:** Room for local training persistence  
- **Security:** AES-256 (GCM) encryption and `EncryptedSharedPreferences` for credential management  
- **Multimedia:** MediaPlayer, AudioRecord, Visualizer API, and Jsoup for RSS processing  
- **Computation:** Advanced algorithms such as **Pollard’s Rho** (factorization) and **Segmented Sieve** for prime number generation  

---

## ◼ Data Architecture and Persistence

The system relies on a **dual Room-based data architecture**, ensuring strong isolation and integrity of sensitive information.

- **AppDatabase.kt:** Manages the main database (`M8AX-Gimnasio_DB`) for physical activity tracking  
- **AppDatabase2.kt:** Handles the independent medical database (`M8AX-Glucosa_DB`)  
- **Gimnasio.kt:** Entity defining the training record schema, including timestamps and personal notes  
- **Glucosa.kt:** Entity designed for blood glucose level persistence  
- **GimnasioDao.kt:** Data access interface implementing complex queries such as total active minutes and historical trends  
- **GlucosaDao.kt:** Defines cleanup and recovery operations for medical records  
- **GimnasioAdapter.kt:** UI controller applying Roman numeral formatting and voice alerts  
- **GlucosaAdapter.kt:** Dynamic visualization of medical data using range-based color coding  
- **BackupUtils.kt:** Critical utilities for exporting and restoring `.db` files via the device filesystem  

---

## ◼ Operational Core and Health Management

- **MainActivity.kt:** The application’s neural center. Integrates training consistency analysis, real-time Bitcoin pricing, and IP-based weather services  
- **AppGlucosa.kt:** Medical tracking module capable of generating professional PDF reports with trend analysis  
- **FumarActivity.kt:** Advanced smoking cessation tool computing precise health statistics (money saved, cigarettes avoided, recovered air volume)  
- **FlexionesActivity.kt:** Uses the proximity sensor for automatic exercise repetition counting without physical contact  
- **RutinasGim.kt:** Loads dynamic web interfaces for structured workout planning  
- **M8axGimActivity.kt:** Access point to external health and wellness web resources (present since the first release)  

---

## ◼ High-Performance Calculation Engines

- **PrimosActivity.kt:** Implements a segmented Sieve of Eratosthenes for large-scale prime number searches, generating statistics on twin, palindromic, and reversible primes  
- **FactorizacionActivity.kt:** Uses Pollard’s Rho to factor large integers (up to 50 digits) with parallel execution threads  
- **CifrasActivity.kt:** “Numbers game” solver applying Breadth-First Search (BFS) to compute exact arithmetic solutions in milliseconds  

---

## ◼ Security, Tools, and Utilities

- **PasswordsActivity.kt:** Credential manager using AES-GCM-256 encryption  
- **PasswordManager.kt:** Master key management via `EncryptedSharedPreferences` and biometric authentication  
- **PasswordsAdapter.kt:** Secure visualization and clipboard copying of encrypted credentials  
- **AstronomiaActivity.kt:** Astronomical data hub calculating lunar illumination, solar positions, and spatial distances in real time  
- **ActivityCalendarioAnual.kt:** Massive PDF calendar generator supporting calculations up to year **1,000,000**  
- **CalendarioActivity.kt:** Monthly navigation interface with activity indicators and lunar phases  
- **CrearQrActivity.kt:** High-definition QR code generator (2048px) with logo embedding  
- **VerticalCaptureActivity.kt:** Specialized vertical barcode and QR scanner  
- **ListaCompraActivity.kt / NotasActivity.kt:** List managers supporting Roman numerals, JSON serialization, and PDF export  
- **ListaCompraAdapter.kt / NotasAdapter.kt:** Dynamic item strike-through with voice confirmation  
- **RelojGrandeActivity.kt:** Large-format clock with news and voice output  
- **RelojActivity.kt:** Stopwatch and configurable clock  
- **M8AXRelojes.kt:** International clocks and time zones  
- **CriptoPrecios.kt:** Cryptocurrency price monitor  
- **ElTiempoFullComunidad.kt / ElTiempoActivity.kt:** Weather forecasting modules  
- **SalvapantallasActivity.kt:** Aesthetic screensaver mode with animations  
- **TickerActivity.kt:** LED-style scrolling message ticker  
- **M8AXMapas.kt:** GPS and map integration  
- **NavidadActivity.kt:** Themed Christmas module  

---

## ◼ Multimedia, Artificial Intelligence, and Entertainment

- **AudioEventosActivity.kt:** Ambient sound monitor with oscilloscope, waveform visualizers, and real-time VU meters  
- **ReproductorActivity.kt:** Advanced music player with recursive directory scanning and metadata handling  
- **RadiosOnlineActivity.kt:** Catalog of 42 online radio stations  
- **RadioService.kt:** Foreground service ensuring uninterrupted playback and notification control  
- **CelebresVozActivity.kt:** Famous quotes narration via Text-to-Speech (TTS)  
- **WikiInfinityActivity.kt / WikiHowActivity.kt:** Automatic readers for Wikipedia articles and RSS feeds  
- **ChatGPTActivity.kt:** Integrated interface for interaction with multiple AI models  
- **TetrisActivity.kt:** Native implementation of the classic game with progressive difficulty  
- **ChessActivity.kt / DamasActivity.kt / Conecta4.kt / ThePong.kt:** Hybrid board and classic games via WebView with offline support  

---

## ◼ Custom Visual Components and Graphics

- **GraficaSimple.kt / GraficaSimple2.kt:** Rendering engines for trend lines, averages, and statistical axes  
- **OndaView.kt:** Dynamic FFT-based audio visualizer with animated bars  
- **CustomAnalogClock.kt:** Interactive analog clock with gesture zoom and continuous second hand  
- **TickerView.kt:** Sliding LED ticker for long messages  
- **LunaView.kt:** Dynamic lunar phase rendering view  

---

## ◼ System Automation and Security

- **SplashActivity.kt:** Startup screen with seasonal and motivational logic  
- **LoginActivity.kt:** Biometric-secured login and password management  
- **BootReceiver.kt:** Automatically reschedules reminders after device reboot  
- **GimnasioReminderReceiver.kt:** Push notification controller with personalized motivational messages  

---

## 📦 Tech Stack and Dependencies (Gradle)

### 📱 Core & UI
- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `androidx.constraintlayout:constraintlayout`
- `androidx.recyclerview:recyclerview`
- `me.zhanghai.android.fastscroll`

### 🗄️ Persistence
- `androidx.room:room-runtime` / `room-compiler`
- `com.google.code.gson:gson`

### 🛡️ Security
- `androidx.security:security-crypto`
- `androidx.biometric:biometric`
- `com.madgag.spongycastle:core` / `prov`

### 📄 Documents
- `com.itextpdf:itextg`

### 🌐 Networking & Scraping
- `com.squareup.okhttp3:okhttp`
- `org.jsoup:jsoup`

### 🔭 Astronomy
- `org.shredzone.commons:commons-suncalc`

### 📸 Multimedia
- `com.journeyapps:zxing-android-embedded` / `zxing:core`
- `com.github.bumptech.glide:glide` / `compiler`
- `com.davemorrissey.labs:subsampling-scale-image-view`

### 📧 Communication
- `com.sun.mail:android-mail` / `android-activation`

---

## ◼ Final Notes

> **Stability:** Database stability has been validated through stress testing with **150,000 records**, equivalent to approximately **410 years of continuous data** without performance degradation.  
>
> **Performance:** The system employs a multi-threaded architecture to ensure heavy computations never block the UI, maintaining smooth and responsive operation at all times.

---