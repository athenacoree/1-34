# AI Chat (Android nativo — Kotlin + Jetpack Compose)

Aplicación Android nativa de mensajería con asistente de Inteligencia Artificial (OpenRouter API) e integraciones del sistema local.

---

## 🛠️ Características Principales

### 🔮 Motor de Inteligencia Artificial y Herramientas Nativas
- **OpenRouter API**: Conexión con modelos como `openai/gpt-4o-mini`, Gemini, Claude, Llama, etc.
- **Acciones nativas en segundo plano**: La IA puede ejecutar comandos `[[ACCION:...]]` para controlar el teléfono:
  - ⏰ Crear alarmas y temporizadores.
  - 🔦 Encender/apagar la linterna.
  - 🔇 Activar/desactivar modo silencio.
  - 📱 Abrir e interactuar con aplicaciones instaladas.
  - 📞 Realizar llamadas y enviar SMS.
  - 📦 Exportar y comprimir conversaciones y adjuntos en formato `.zip`.

### ⚡ Motor Local de Comandos (Sin consumo de tokens)
Para ahorrar tokens de IA y ofrecer respuestas instantáneas (0 ms de latencia, 100% offline):
- La app incluye un **intérprete de comandos en Kotlin puro** basado en expresiones regulares (`Regex`) y coincidencia de patrones en español.
- **Ejemplos de comandos locales**:
  - *"Pon una alarma a las 7:30"*
  - *"Temporizador de 5 minutos"*
  - *"Enciende la linterna"*
  - *"Modo silencio"*
  - *"Abre WhatsApp"*
  - *"Llama a [contacto]"*
  - *"Manda un sms a [número] diciendo [mensaje]"*
  - *"Clima en [ciudad]"*
  - *"Qué es [concepto]" / "Quién es [persona]"*
  - *"Convertir 100 USD a EUR"*
  - *"Noticias"*
  - *"Busca fotos de [nombre]"*
  - *"Traduce [texto] al [idioma]"*
- **Evaluación de alternativas**: Se utilizó el enfoque de **Regex en Kotlin puro** por su ligereza (0 MB adicionales de almacenamiento), rapidez instantánea e independencia de internet. Para coincidencias conversacionales más complejas en el futuro, se puede evaluar incluir Google TensorFlow Lite Task Library (`NLClassifier`), que añade entre 1 y 20 MB pero permite clasificar intenciones con modelos de Machine Learning en el teléfono.

### 🌐 Integraciones Nativas y APIs Gratuitas
1. **Wikipedia API**: Resúmenes automáticos e instantáneos de cualquier concepto.
2. **Open-Meteo API**: Clima actual y pronóstico por ciudad (sin necesidad de API key).
3. **Frankfurter.app**: Conversor de divisas en tiempo real.
4. **DuckDuckGo Instant Answers**: Búsqueda rápida de datos directos.
5. **Noticias RSS**: Lectura de titulares actualizados (BBC Mundo).
6. **YouTube Data API v3**: Búsqueda detallada de videos (título, canal, enlaces).
7. **Google Custom Search API**: Búsqueda web real de Google.
8. **ML Kit (On-Device ML)**:
   - **Traducción sin conexión**: Traducción entre múltiples idiomas.
   - **OCR (Reconocimiento de Texto)**: Extracción de texto desde imágenes tomadas con la cámara.
   - **Escáner QR / Código de Barras**: Lectura de códigos QR.
9. **Búsqueda de Fotos Locales (MediaStore)**: Búsqueda en la galería real del dispositivo por nombre o fecha.

---

## 🗣️ Dictado por Voz y Síntesis de Voz (TTS)

- **Dictado por Voz (STT)**: Utiliza el reconocimiento de voz del sistema Android (`SpeechRecognizer`). *Nota: Requiere conexión a internet o el motor de voz del sistema instalado.*
- **Modo Manos Libres y Voz Femenina Siri/Alexa**:
  - La IA **solo lee sus respuestas en voz alta cuando el Modo Manos Libres está activo** (ícono de audífonos en la barra superior).
  - Incluye un botón para **silenciar/detener la voz en cualquier momento** (ícono de altavoz tachado).
  - El tono y velocidad de la voz se configuran y **se guardan permanentemente** en la app.

---

## 💾 Persistencia y Almacenamiento Local

- **Historial de Chats y Mensajes**: Se guarda en una base de datos local **SQLite** (vía `ChatRepository`), garantizando privacidad y acceso offline a tus mensajes.
- **Configuración y Ajustes**: Claves de API (OpenRouter, YouTube, Google), modelo seleccionado, tono/velocidad de voz y tema visual se guardan en **SharedPreferences**.

---

## 🔒 Permisos Just-In-Time (En tiempo de ejecución)

Los permisos (micrófono, cámara, notificaciones, archivos/fotos) **NO se solicitan de golpe al abrir la app**. La app solicitará individualmente cada permiso únicamente cuando el usuario active por primera vez la función correspondiente (por ejemplo, micrófono al tocar el botón de dictado).

---

## 🤖 Asistente del Sistema (VoiceInteractionService)

La app puede configurarse como el asistente digital predeterminado de Android (reemplazando a Google / Gemini):

1. Ve a **Ajustes del teléfono > Aplicaciones > Aplicaciones predeterminadas > App de asistente digital**.
2. Selecciona **AI Chat**.

> ⚠️ **Nota sobre fabricantes (Samsung, Xiaomi, Huawei, etc.)**: En algunas capas de personalización (One UI, MIUI/HyperOS, EMUI), los fabricantes ocultan o deshabilitan la opción de cambiar el asistente digital predeterminado por restricciones del sistema operativo. Esto **no es un fallo de la app**, sino una limitación impuesta por el fabricante del dispositivo.

---

## 🚀 Compilar y Firmar el Proyecto (GitHub Actions)

El proyecto incluye un workflow de GitHub Actions en `.github/workflows/build.yml`:
1. Sube el código a GitHub (rama `main`).
2. Configura los siguientes secretos en **Settings → Secrets and variables → Actions**:
   - `KEYSTORE_BASE64`: El archivo `.jks` codificado en Base64.
   - `KEYSTORE_PASSWORD`: Contraseña del keystore (`debugging`).
   - `KEY_ALIAS`: Alias de la clave (`debugging`).
   - `KEY_PASSWORD`: Contraseña de la clave (`debugging`).
3. La pestaña **Actions** compilará automáticamente tanto el APK de depuración (`app-debug-apk`) como el APK de versión firmado (`app-release-apk`).
4. Puedes descargar los instalables desde los artefactos del flujo de trabajo.
