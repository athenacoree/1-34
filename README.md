# AI Chat (Android nativo — Kotlin + Jetpack Compose)

Reescritura 100% nativa de tu app "AI Chat estilo iMessage". Ya **no** usa
WebView ni HTML/CSS/JS: toda la interfaz está dibujada con Jetpack Compose y
toda la lógica vive en Kotlin. Es la misma app, misma paleta de colores
(clara/oscura), mismas funciones — solo que ahora corre como una app Android
nativa de verdad.

## ¿Qué cambia respecto a la versión anterior (WebView)?
- Interfaz más fluida (sin el overhead de renderizar HTML/CSS dentro de un navegador embebido).
- Menor consumo de memoria y batería.
- El código es 100% Kotlin: para agregar funciones nuevas ya no se toca `app.js`, se edita directamente en `app/src/main/java/...`.

## Lo que NO cambia
- **Sigue necesitando internet para hablar con la IA.** Esto es así en cualquier lenguaje (Kotlin, Swift, JS, lo que sea) porque el modelo de IA corre en los servidores de OpenRouter, no en el teléfono. Abrir la app, ver tus chats guardados y navegar la interfaz sí funciona sin conexión.
- Tu clave de API y tus conversaciones se guardan solo en el teléfono (ahora en `SharedPreferences` en vez de `localStorage`, el equivalente nativo).

## Estructura
```
AIChatNative/
├── app/src/main/java/com/aichat/imessage/
│   ├── MainActivity.kt          ← punto de entrada, monta Compose
│   ├── data/
│   │   ├── Models.kt             ← Conversation, ChatMessage, AppSettings
│   │   ├── Storage.kt             ← persistencia (SharedPreferences)
│   │   └── OpenRouterApi.kt       ← llamada HTTP a OpenRouter
│   ├── viewmodel/AppViewModel.kt  ← toda la lógica (equivalente a app.js)
│   └── ui/
│       ├── theme/Theme.kt          ← paleta de colores clara/oscura
│       ├── components/             ← Avatar, burbuja de mensaje, "escribiendo…"
│       ├── AppRoot.kt               ← navegación lista ↔ chat
│       ├── ChatListScreen.kt        ← lista de chats + swipe para borrar
│       ├── ChatScreen.kt            ← conversación abierta
│       ├── NewChatDialog.kt
│       ├── SettingsDialog.kt
│       └── ToastHost.kt
├── build.gradle.kts / settings.gradle.kts / gradle.properties
└── .github/workflows/build.yml   ← compila el APK automáticamente en GitHub
```

## Compilar en GitHub (sin instalar nada)
1. Sube la carpeta `AIChatNative` a un repositorio de GitHub.
2. Ve a la pestaña **Actions**. El workflow "Build APK" corre solo con cada
   push a `main`, o dispáralo manualmente con **Run workflow**.
3. Descarga el APK generado desde el artefacto **app-debug-apk**.

## Compilar en tu computadora (opcional)
1. Abre la carpeta con **Android Studio** (versión reciente, con soporte Compose).
2. Deja que sincronice Gradle.
3. Run ▶ para probarla, o **Build > Build APK(s)** para generar el instalable.

## Agregar nuevas funciones
Cada una de las 30 funciones que hablamos antes (streaming, voz, imágenes,
múltiples modelos, etc.) se puede ir agregando aquí mismo, directo en Kotlin.
Si quieres, dime cuáles y las implemento.
