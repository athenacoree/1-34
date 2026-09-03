package com.aichat.imessage.tools

import android.content.Context
import com.aichat.imessage.data.AppSettings

sealed class LocalCommandResult {
    data class Handled(val replyText: String, val actions: List<AiAction> = emptyList()) : LocalCommandResult()
    object NotHandled : LocalCommandResult()
}

object LocalCommandEngine {

    suspend fun process(text: String, context: Context, settings: AppSettings): LocalCommandResult {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()

        // 0. ipify.org (IP pública)
        if (lower == "mi ip" || lower == "cuál es mi ip" || lower == "cual es mi ip" || lower == "ip pública") {
            val reply = ExternalIntegrationsHelper.fetchIpifyPublicIp()
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.IPIFY, emptyList())))
        }

        // 0. Advice Slip (Consejo)
        if (lower.contains("dame un consejo") || lower == "un consejo" || lower == "consejo") {
            val reply = ExternalIntegrationsHelper.fetchAdviceSlip()
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.ADVICE_SLIP, emptyList())))
        }

        // 0. Open Trivia Database
        if (lower.contains("trivia") || lower.contains("pregunta de trivia")) {
            val reply = ExternalIntegrationsHelper.fetchOpenTrivia()
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.TRIVIA, emptyList())))
        }

        // 0. NASA APOD
        if (lower.contains("nasa") || lower.contains("foto del día") || lower.contains("imagen del día")) {
            val reply = ExternalIntegrationsHelper.fetchNasaApod(settings.nasaApiKey)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.NASA_APOD, emptyList())))
        }

        // 0. CoinGecko (Crypto)
        if (lower.contains("bitcoin") || lower.contains("ethereum") || lower.contains("crypto") || lower.contains("criptomonedas")) {
            val crypto = when {
                lower.contains("ethereum") -> "ethereum"
                lower.contains("solana") -> "solana"
                else -> "bitcoin"
            }
            val reply = ExternalIntegrationsHelper.fetchCoinGeckoCrypto(crypto)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.COINGECKO, listOf(crypto))))
        }

        // 0. USGS Earthquake
        if (lower.contains("sismo") || lower.contains("terremoto") || lower.contains("temblor")) {
            val reply = ExternalIntegrationsHelper.fetchUsgsEarthquakes()
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.USGS_EARTHQUAKE, emptyList())))
        }

        // 0. Sunrise/Sunset
        if (lower.contains("salida del sol") || lower.contains("puesta del sol") || lower.contains("sale el sol")) {
            val reply = ExternalIntegrationsHelper.fetchSunriseSunset()
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.SUNRISE_SUNSET, emptyList())))
        }

        // 0. Calidad del Aire
        if (lower.contains("calidad del aire") || lower.contains("contaminacion") || lower.contains("aqi")) {
            val reply = ExternalIntegrationsHelper.fetchAirQuality()
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.AIR_QUALITY, emptyList())))
        }

        // 0. Nager.Date (Feriados)
        if (lower.contains("feriado") || lower.contains("días festivos") || lower.contains("dias festivos")) {
            val reply = ExternalIntegrationsHelper.fetchNagerHolidays("CU")
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.NAGER_DATE, listOf("CU"))))
        }

        // 0. REST Countries
        val countryMatch = Regex("""^(?:info|informacion|capital|pais|país)\s+de\s+(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (countryMatch != null) {
            val country = countryMatch.groupValues[1].trim()
            val reply = ExternalIntegrationsHelper.fetchRestCountry(country)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.REST_COUNTRIES, listOf(country))))
        }

        // 0. Numbers API
        val numberMatch = Regex("""^(?:dato|curiosidad)\s+(?:del?|sobre)?\s*número\s*(\d+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (numberMatch != null) {
            val num = numberMatch.groupValues[1]
            val reply = ExternalIntegrationsHelper.fetchNumbersApi(num)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.NUMBERS_API, listOf(num))))
        }

        // 0. Generación de Imagen gratis (Pollinations.ai)
        val imageMatch = Regex("""^(?:dibújame|dibujame|dibuja|hazme\s+una\s+imagen\s+de|genera\s+una\s+imagen\s+de|crea\s+una\s+imagen\s+de)\s+(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (imageMatch != null) {
            val prompt = imageMatch.groupValues[1].trim()
            val (text, file) = ExternalIntegrationsHelper.generatePollinationsImage(context, prompt)
            return LocalCommandResult.Handled(text, listOf(AiAction(AiActionType.GENERAR_IMAGEN, listOf(prompt))))
        }

        // 0. Cuba: El Toque (Tasa de cambio)
        if (lower.contains("el toque") || lower.contains("tasa de cambio") || (lower.contains("dolar") && lower.contains("cuanto")) || (lower.contains("dólar") && lower.contains("cuanto")) || lower == "dolar" || lower == "dólar" || lower == "mlc") {
            val reply = ExternalIntegrationsHelper.fetchElToqueRates()
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.EL_TOQUE, emptyList())))
        }

        // 0. Cuba: ETECSA USSD Saldo / Megas / Bonos
        if (lower.contains("saldo") || lower.contains("cuanto me queda") || lower.contains("cuántos megas") || lower.contains("cuantos megas") || lower.contains("*222#") || lower.contains("*222*328#")) {
            val code = when {
                lower.contains("megas") || lower.contains("datos") || lower.contains("internet") -> "*222*328#"
                lower.contains("bono") -> "*222*266#"
                lower.contains("sms") -> "*222*767#"
                lower.contains("minuto") || lower.contains("voz") -> "*222*869#"
                else -> "*222#"
            }
            val reply = ExternalIntegrationsHelper.executeEtecsaUssd(context, code)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.ETECSA_USSD, listOf(code))))
        }

        // 1. Alarmas
        val alarmMatch = Regex("""^(?:pon|pone|poner|crea|crear|programa|programar)?\s*(?:una\s+)?alarma\s*(?:para\s+las?|a\s+las?|las?)?\s*(\d{1,2})(?::(\d{2}))?\s*(?:am|pm)?$""", RegexOption.IGNORE_CASE).find(trimmed)
            ?: Regex("""^alarma\s*(?:a\s+las?|para\s+las?)?\s*(\d{1,2})(?::(\d{2}))?$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (alarmMatch != null) {
            val hour = alarmMatch.groupValues[1].toIntOrNull() ?: 8
            val min = alarmMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            val success = SystemTaskHelper.createAlarm(context, hour, min, "Alarma")
            val reply = if (success) "⏰ Alarma programada localmente para las ${String.format("%02d:%02d", hour, min)}." else "⚠️ No se pudo programar la alarma."
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.CREAR_ALARMA, listOf(hour.toString(), min.toString()))))
        }

        // 2. Temporizador
        val timerMatch = Regex("""^(?:pone|pon|poner|crea|crear|inicia|iniciar)?\s*(?:un\s+)?temporizador\s+de\s+(\d+)\s+(minutos?|segundos?|horas?)""", RegexOption.IGNORE_CASE).find(trimmed)
        if (timerMatch != null) {
            val num = timerMatch.groupValues[1].toIntOrNull() ?: 1
            val unit = timerMatch.groupValues[2].lowercase()
            val seconds = when {
                unit.startsWith("hora") -> num * 3600
                unit.startsWith("minuto") -> num * 60
                else -> num
            }
            val success = SystemTaskHelper.createTimer(context, seconds, "Temporizador")
            val reply = if (success) "⏱️ Temporizador iniciado por $num $unit." else "⚠️ No se pudo iniciar el temporizador."
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.CREAR_TEMPORIZADOR, listOf(seconds.toString()))))
        }

        // 3. Linterna
        if (lower.contains("enciende la linterna") || lower.contains("encender linterna") || lower == "prender linterna" || lower == "linterna on") {
            return LocalCommandResult.Handled("🔦 Encendiendo linterna.", listOf(AiAction(AiActionType.FLASHLIGHT, listOf("on"))))
        }
        if (lower.contains("apaga la linterna") || lower.contains("apagar linterna") || lower == "linterna off") {
            return LocalCommandResult.Handled("🔦 Apagando linterna.", listOf(AiAction(AiActionType.FLASHLIGHT, listOf("off"))))
        }

        // 4. Silencio / Sonido
        if (lower.contains("activa el silencio") || lower.contains("pon en silencio") || lower == "modo silencio" || lower == "silenciar") {
            return LocalCommandResult.Handled("🔇 Activando silencio.", listOf(AiAction(AiActionType.MUTE, listOf("on"))))
        }
        if (lower.contains("desactiva el silencio") || lower.contains("quitar silencio") || lower == "modo sonido") {
            return LocalCommandResult.Handled("🔊 Modo sonido normal activado.", listOf(AiAction(AiActionType.MUTE, listOf("off"))))
        }

        // 5. Abrir App
        val openAppMatch = Regex("""^(?:abre|abrir|launch|open)\s+(?:la\s+app\s+)?(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (openAppMatch != null) {
            val appName = openAppMatch.groupValues[1].trim()
            val success = AppLauncherHelper.openAppByNameOrPackage(context, appName)
            val reply = if (success) "📱 Abriendo $appName..." else "⚠️ No pude encontrar la aplicación \"$appName\"."
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.ABRIR_APP, listOf(appName))))
        }

        // 6. Llamar
        val callMatch = Regex("""^(?:llama|llamar|marcar)\s+a?\s*(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (callMatch != null) {
            val target = callMatch.groupValues[1].trim()
            return LocalCommandResult.Handled("📞 Abriendo marcador para $target...", listOf(AiAction(AiActionType.LLAMAR, listOf(target))))
        }

        // 7. SMS
        val smsMatch = Regex("""^(?:manda|mandar|envia|enviar)\s+(?:un\s+)?sms\s+a\s+([^\s]+)\s+(?:diciendo|con el mensaje)?\s*(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (smsMatch != null) {
            val phone = smsMatch.groupValues[1].trim()
            val msg = smsMatch.groupValues[2].trim()
            val success = CommunicationHelper.sendSms(context, phone, msg)
            val reply = if (success) "💬 Abriendo app de SMS para enviar a $phone." else "⚠️ No se pudo abrir la app de SMS."
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.ENVIAR_SMS, listOf(phone, msg))))
        }

        // 8. Clima (Open-Meteo)
        val weatherMatch = Regex("""^(?:clima|tiempo|pronostico)\s+(?:en|de)?\s*(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (weatherMatch != null) {
            val city = weatherMatch.groupValues[1].trim()
            val reply = ExternalIntegrationsHelper.fetchOpenMeteoWeather(city)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.OPEN_METEO, listOf(city))))
        }

        // 9. Wikipedia ("qué es X", "quién es X")
        val wikiMatch = Regex("""^(?:que|qué|quien|quién)\s+es\s+(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
            ?: Regex("""^(?:definicion|concepto|resumen)\s+de\s+(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (wikiMatch != null) {
            val topic = wikiMatch.groupValues[1].trim()
            val reply = ExternalIntegrationsHelper.fetchWikipediaSummary(topic)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.WIKIPEDIA, listOf(topic))))
        }

        // 10. Moneda (Frankfurter)
        val currencyMatch = Regex("""^(?:convertir|convierte)\s+(\d+(?:\.\d+)?)\s+([a-zA-Z]{3})\s+a\s+([a-zA-Z]{3})$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (currencyMatch != null) {
            val amount = currencyMatch.groupValues[1].toDoubleOrNull() ?: 1.0
            val from = currencyMatch.groupValues[2]
            val to = currencyMatch.groupValues[3]
            val reply = ExternalIntegrationsHelper.fetchFrankfurterRate(amount, from, to)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.FRANKFURTER, listOf(amount.toString(), from, to))))
        }

        // 11. Noticias RSS
        if (lower == "noticias" || lower == "titulares" || lower == "ultimas noticias" || lower == "ver noticias") {
            val reply = ExternalIntegrationsHelper.fetchRssNews()
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.RSS_NEWS, emptyList())))
        }

        // 12. Buscar fotos locales
        val photoMatch = Regex("""^(?:busca|buscar|encuentra|ver)?\s*(?:mis\s+)?fotos\s+(?:de|en|con)?\s*(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (photoMatch != null) {
            val query = photoMatch.groupValues[1].trim()
            val reply = ExternalIntegrationsHelper.searchLocalPhotos(context, query)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.BUSCAR_FOTOS, listOf(query))))
        }

        // 13. Traducción (ML Kit)
        val translateMatch = Regex("""^(?:traduce|traducir)\s+(.+)\s+al\s+([a-zA-ZáéíóúñÁÉÍÓÚÑ]+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (translateMatch != null) {
            val textToTranslate = translateMatch.groupValues[1].trim()
            val targetLang = translateMatch.groupValues[2].trim()
            var translationResult = "🌐 Traduciendo..."
            ExternalIntegrationsHelper.mlKitTranslate(textToTranslate, targetLang) { res ->
                translationResult = res
            }
            return LocalCommandResult.Handled(translationResult, listOf(AiAction(AiActionType.MLKIT_TRANSLATE, listOf(textToTranslate, targetLang))))
        }

        // 14. Búsqueda YouTube API (si está configurada la clave)
        val ytMatch = Regex("""^(?:busca|buscar)\s+en\s+youtube\s+(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (ytMatch != null && settings.youtubeApiKey.isNotBlank()) {
            val query = ytMatch.groupValues[1].trim()
            val reply = ExternalIntegrationsHelper.searchYouTubeDataApi(query, settings.youtubeApiKey)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.YOUTUBE_API_SEARCH, listOf(query))))
        }

        // 15. Búsqueda Google Custom Search API (si está configurada la clave)
        val googleMatch = Regex("""^(?:busca|buscar)\s+en\s+google\s+(.+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        if (googleMatch != null && settings.googleApiKey.isNotBlank() && settings.googleCseId.isNotBlank()) {
            val query = googleMatch.groupValues[1].trim()
            val reply = ExternalIntegrationsHelper.searchGoogleCustomSearch(query, settings.googleApiKey, settings.googleCseId)
            return LocalCommandResult.Handled(reply, listOf(AiAction(AiActionType.GOOGLE_SEARCH, listOf(query))))
        }

        return LocalCommandResult.NotHandled
    }
}
