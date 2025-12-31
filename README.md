# M8AX - Diario de Entrenamiento y Navaja Suiza Digital

Este proyecto es una aplicación integral para Android diseñada originalmente como un **diario de gimnasio**, pero que ha evolucionado hasta convertirse en un ecosistema completo de herramientas de productividad, salud, seguridad, multimedia y juegos ****. Casi todas las interacciones están narradas mediante **TTS (Text-To-Speech)** para una experiencia inmersiva ****.

## 🛠️ Tecnologías Principales
*   **Lenguaje:** Kotlin.
*   **Base de Datos:** Room para persistencia local de entrenamientos ****.
*   **Seguridad:** Cifrado **AES-256** (GCM) y **EncryptedSharedPreferences** para la gestión de contraseñas ****.
*   **Multimedia:** MediaPlayer, AudioRecord, Visualizer API y Jsoup para RSS ****.
*   **Cálculo:** Algoritmos avanzados como Pollard's Rho para factorización y Criba de Segmentos para números primos ****.

---

## 📂 Descripción Detallada de Archivos

A continuación, se detalla la función de cada fichero que compone el sistema:

### 🏋️ Gestión de Gimnasio
*   **AppDatabase.kt**: Configura la base de datos **Room** para almacenar los registros de ejercicio ****.
*   **Gimnasio.kt**: Define la entidad de la base de datos (id, fecha, minutos de ejercicio y diario personal) ****.
*   **GimnasioDao.kt**: Contiene las consultas SQL para insertar, borrar, actualizar y obtener estadísticas de entrenamiento ****.
*   **MainActivity.kt**: Es el núcleo del programa. Gestiona la interfaz principal, calcula **medias de ejercicio en tiempo real**, obtiene el clima por IP e integra el sistema de exportación a **PDF, JSON y TXT** ****.
*   **GimnasioAdapter.kt**: Gestiona la lista visual de entrenamientos, permitiendo editar minutos y leer el diario mediante voz ****.
*   **GimnasioReminderReceiver.kt**: Emite notificaciones programadas para recordar al usuario que debe entrenar ****.
*   **BootReceiver.kt**: Reprograma las alarmas de entrenamiento automáticamente cuando se reinicia el móvil ****.
*   **RutinasGim.kt**: Proporciona una guía visual de rutinas de ejercicios mediante una interfaz web integrada ****.
*   **M8axGimActivity.kt**: Acceso a recursos web externos sobre salud y bienestar ****.

### 🔐 Seguridad y Productividad
*   **PasswordsActivity.kt**: Gestor de contraseñas con **login biométrico** (huella). Permite generar claves basadas en **entropía** y exportar un PDF cifrado con contraseña maestra ****.
*   **PasswordManager.kt**: Gestiona el almacenamiento seguro de la clave maestra mediante el esquema **AES256_SIV** ****.
*   **PasswordsAdapter.kt**: Adaptador para visualizar y copiar contraseñas al portapapeles con un toque ****.
*   **ListaCompraActivity.kt**: Gestor de lista de la compra que permite marcar productos, usar **notación romana** y exportar la lista a PDF ****.
*   **CrearQrActivity.kt**: Generador y escáner de códigos QR. Permite personalizar el código con **logos del usuario** y guardarlos en alta resolución ****.
*   **ChatGPTActivity.kt**: Menú de acceso rápido a múltiples motores de **Inteligencia Artificial** para texto, imágenes y música ****.

### 🏥 Salud y Bienestar
*   **FumarActivity.kt**: Monitor detallado para dejar de fumar. Calcula dinero ahorrado, cigarrillos evitados y **mejoras en la salud** (alquitrán y nicotina no ingeridos) con animaciones dinámicas ****.
*   **FlexionesActivity.kt**: Contador automático que utiliza el **sensor de proximidad**. Incluye más de 40 frases de motivación por voz cada 10 repeticiones ****.

### 📊 Matemáticas y Cálculo Avanzado
*   **FactorizacionActivity.kt**: Factoriza números de hasta **50 dígitos** usando hilos paralelos y algoritmos complejos como **Pollard's rho**, mostrando estadísticas de operaciones por segundo ****.
*   **PrimosActivity.kt**: Generador avanzado de números primos en rangos masivos (Long), con estadísticas sobre terminaciones, primos gemelos y exportación a TXT ****.
*   **CifrasActivity.kt**: Resuelve el juego de las cifras mediante un motor de búsqueda por fuerza bruta (**BFS**), calculando la solución exacta o la más cercana ****.

### 🎵 Multimedia y Radio
*   **ReproductorActivity.kt**: Reproductor de música por carpetas con búsqueda de carátulas, temporizador de apagado y **pausa automática en llamadas** ****.
*   **OndaView.kt**: Visualizador de espectro de audio en tiempo real que reacciona a la música mediante procesamiento **FFT** ****.
*   **RadiosOnlineActivity.kt**: Sintonizador de más de **40 emisoras** nacionales e internacionales ****.
*   **RadioService.kt**: Servicio de fondo que permite seguir escuchando la radio mientras se usan otras aplicaciones o con la pantalla apagada ****.

### 📅 Calendarios y Relojes
*   **CalendarioActivity.kt**: Calendario mensual con **fases lunares reales** y marcas de días entrenados ****.
*   **ActivityCalendarioAnual.kt**: Generador de **PDFs de calendarios anuales** (hasta 20,000 años) con dibujos de la luna y estadísticas de gimnasio del año seleccionado ****.
*   **RelojGrandeActivity.kt**: Reloj de pantalla completa con **noticiero RSS dinámico** (marquesina) y anuncios de hora por voz ****.
*   **RelojActivity.kt**: Cronómetro y reloj con escala ajustable y notificaciones de tiempo transcurrido ****.
*   **CustomAnalogClock.kt**: Vista personalizada de un reloj analógico clásico escalable ****.
*   **M8AXRelojes.kt**: Visualización de relojes internacionales y zonas horarias ****.

### 🎮 Juegos
*   **TetrisActivity.kt**: El clásico juego con niveles de velocidad progresiva y comentarios jocosos por voz en cada pieza ****.
*   **ChessActivity.kt**: Juego de ajedrez con interfaz web y frases de Star Wars deslizantes ****.
*   **Conecta4Activity.kt**, **DamasActivity.kt** y **ThePong.kt**: Versiones adaptadas de estos clásicos para Android ****.

### 📡 Información y Utilidades
*   **AudioEventosActivity.kt**: Analizador de sonido ambiental con osciloscopio y disparador de **flash/vibración rítmica** basado en el nivel de decibelios ****.
*   **CriptoPrecios.kt**: Monitor de mercado para 50 criptomonedas con alertas de volatilidad por voz cada 5 minutos ****.
*   **ElTiempoFullComunidad.kt** y **ElTiempoActivity.kt**: Información meteorológica detallada y previsiones ****.
*   **WikiHowActivity.kt** y **WikiInfinityActivity.kt**: Lectores de noticias y artículos aleatorios de Wikipedia automatizados por voz para "aprender mientras entrenas" ****.
*   **CelebresVozActivity.kt**: Galería de frases célebres que se leen por voz mientras cambian fondos artísticos ****.
*   **SalvapantallasActivity.kt**: Modo de visualización estética que muestra los registros del diario y la hora con animaciones de partículas ****.
*   **TickerActivity.kt** y **TickerView.kt**: Permiten mostrar mensajes personalizados desplazándose por la pantalla (estilo cartelera LED) ****.
*   **TickerCriptoActivity.kt**: Variante del ticker para seguir precios de criptos y consumo de datos de la app ****.
*   **SplashActivity.kt**: Pantalla de bienvenida con videos y sonidos aleatorios que cambian según la hora del día o fechas festivas ****.
*   **M8AXMapas.kt**: Integración de mapas GPS para navegación ****.
*   **NavidadActivity.kt**: Módulo temático para fechas navideñas ****.

---

**Nota:** Este software utiliza una arquitectura de hilos avanzada para garantizar que los cálculos pesados (como la factorización) no bloqueen la interfaz de usuario, manteniendo siempre la fluidez del sistema ****.
