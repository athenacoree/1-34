package com.aichat.imessage.tools

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ExternalIntegrationsHelper {

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android AIChatApp)")
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw Exception("HTTP ${conn.responseCode}: $err")
            }
        } catch (e: java.net.UnknownHostException) {
            throw Exception("⚠️ Sin conexión a internet. Por favor verifica tu red Wi-Fi o datos móviles.")
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("⚠️ Tiempo de espera agotado (sin respuesta del servidor).")
        } finally {
            conn.disconnect()
        }
    }

    suspend fun fetchElToqueRates(): String = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://api.eltoque.com/v1/rates")
            val json = JSONObject(raw)
            val tas = json.optJSONObject("tasas")
            val usd = tas?.optString("USD", "320") ?: "320"
            val eur = tas?.optString("EUR", "330") ?: "330"
            val mlc = tas?.optString("MLC", "270") ?: "270"
            "🇨🇺 **Tasa de Cambio Informal en Cuba (El Toque)**:\n• 🇺🇸 **USD**: $usd CUP\n• 🇪🇺 **EUR**: $eur CUP\n• 💳 **MLC**: $mlc CUP"
        } catch (e: Exception) {
            "🇨🇺 **Tasa de Cambio Informal en Cuba (Estimado El Toque)**:\n• 🇺🇸 **USD**: ~320 CUP\n• 🇪🇺 **EUR**: ~330 CUP\n• 💳 **MLC**: ~270 CUP\n*(Valores del mercado informal)*"
        }
    }

    fun executeEtecsaUssd(context: Context, code: String): String {
        val cleanCode = if (code.contains("*")) code.trim() else "*222#"
        val encodedCode = Uri.encode(cleanCode)
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$encodedCode")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "🇨🇺 **ETECSA Cuba**: Ejecutando código USSD `$cleanCode` en tu teléfono..."
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encodedCode")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(dialIntent) }
            "🇨🇺 **ETECSA Cuba**: Abriendo marcador con código `$cleanCode`."
        }
    }

    suspend fun generatePollinationsImage(context: Context, prompt: String): Pair<String, File?> = withContext(Dispatchers.IO) {
        try {
            val encodedPrompt = URLEncoder.encode(prompt.trim(), "UTF-8")
            val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=800&height=800&nologo=true"

            val url = URL(imageUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android AIChatApp)")

            if (conn.responseCode in 200..299) {
                val destDir = File(context.filesDir, "attachments").apply { mkdirs() }
                val imageFile = File(destDir, "imagen_${System.currentTimeMillis()}.png")
                conn.inputStream.use { input ->
                    imageFile.outputStream().use { output -> input.copyTo(output) }
                }
                Pair("🎨 **Imagen generada para**: \"$prompt\"", imageFile)
            } else {
                Pair("🎨 Error al generar la imagen en Pollinations.ai (HTTP ${conn.responseCode}).", null)
            }
        } catch (e: Exception) {
            Pair("🎨 Error al generar la imagen: ${e.message}", null)
        }
    }

    suspend fun fetchNagerHolidays(countryCode: String, year: Int = 2026): String = withContext(Dispatchers.IO) {
        try {
            val code = countryCode.trim().uppercase().ifBlank { "CU" }
            val raw = httpGet("https://date.nager.at/api/v3/PublicHolidays/$year/$code")
            val arr = JSONArray(raw)
            if (arr.length() == 0) return@withContext "📅 No se encontraron feriados para $code en $year."
            val items = mutableListOf<String>()
            for (i in 0 until minOf(8, arr.length())) {
                val o = arr.getJSONObject(i)
                items.add("• ${o.optString("date")}: **${o.optString("localName")}** (${o.optString("name")})")
            }
            "📅 **Feriados Públicos en $code ($year)**:\n" + items.joinToString("\n")
        } catch (e: Exception) {
            "📅 Error al obtener feriados: ${e.message}"
        }
    }

    suspend fun fetchAirQuality(lat: Double = 23.1136, lon: Double = -82.3666, locationName: String = "La Habana"): String = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi,pm10,pm2_5")
            val json = JSONObject(raw)
            val current = json.optJSONObject("current")
            val aqi = current?.optInt("us_aqi", -1) ?: -1
            val pm10 = current?.optDouble("pm10", 0.0) ?: 0.0
            val pm25 = current?.optDouble("pm2_5", 0.0) ?: 0.0
            "🍃 **Calidad del Aire en $locationName**:\n• Índice US AQI: **$aqi**\n• PM2.5: $pm25 µg/m³\n• PM10: $pm10 µg/m³"
        } catch (e: Exception) {
            "🍃 Error al obtener calidad del aire: ${e.message}"
        }
    }

    suspend fun fetchSunriseSunset(lat: Double = 23.1136, lon: Double = -82.3666, locationName: String = "La Habana"): String = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://api.sunrise-sunset.org/json?lat=$lat&lng=$lon&formatted=0")
            val json = JSONObject(raw)
            val results = json.optJSONObject("results")
            val sunrise = results?.optString("sunrise", "") ?: ""
            val sunset = results?.optString("sunset", "") ?: ""
            "🌅 **Salida y Puesta del Sol en $locationName**:\n• 🌅 Salida del sol: $sunrise (UTC)\n• 🌇 Puesta del sol: $sunset (UTC)"
        } catch (e: Exception) {
            "🌅 Error al consultar salida/puesta del sol: ${e.message}"
        }
    }

    suspend fun fetchCoinGeckoCrypto(crypto: String = "bitcoin"): String = withContext(Dispatchers.IO) {
        try {
            val id = crypto.lowercase().trim().ifBlank { "bitcoin" }
            val raw = httpGet("https://api.coingecko.com/api/v3/simple/price?ids=$id&vs_currencies=usd,eur")
            val json = JSONObject(raw)
            val data = json.optJSONObject(id)
            if (data == null) return@withContext "🪙 No se encontró información para crypto: \"$crypto\"."
            val usd = data.optDouble("usd", 0.0)
            val eur = data.optDouble("eur", 0.0)
            "🪙 **Precio Crypto (${id.uppercase()})**:\n• 🇺🇸 USD: **$$usd**\n• 🇪🇺 EUR: **€$eur**"
        } catch (e: Exception) {
            "🪙 Error al consultar CoinGecko: ${e.message}"
        }
    }

    suspend fun fetchUsgsEarthquakes(): String = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&limit=5&minmagnitude=4.5")
            val json = JSONObject(raw)
            val features = json.optJSONArray("features") ?: JSONArray()
            if (features.length() == 0) return@withContext "🌍 No hay sismos recientes de magnitud >= 4.5."
            val list = mutableListOf<String>()
            for (i in 0 until features.length()) {
                val prop = features.getJSONObject(i).optJSONObject("properties")
                val mag = prop?.optDouble("mag", 0.0) ?: 0.0
                val place = prop?.optString("place", "Desconocido") ?: "Desconocido"
                list.add("• Mag $mag - $place")
            }
            "🌍 **Sismos Recientes en el Mundo (USGS)**:\n" + list.joinToString("\n")
        } catch (e: Exception) {
            "🌍 Error al consultar sismos USGS: ${e.message}"
        }
    }

    suspend fun fetchRestCountry(country: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(country.trim(), "UTF-8")
            val raw = httpGet("https://restcountries.com/v3.1/name/$encoded")
            val arr = JSONArray(raw)
            val first = arr.getJSONObject(0)
            val nameObj = first.optJSONObject("name")
            val commonName = nameObj?.optString("common", country) ?: country
            val capital = first.optJSONArray("capital")?.optString(0, "N/A") ?: "N/A"
            val pop = first.optLong("population", 0)
            val region = first.optString("region", "")
            val flag = first.optString("flag", "🏳️")
            "🗺️ **País $flag ($commonName)**:\n• Capital: $capital\n• Región: $region\n• Población: ${String.format("%,d", pop)}"
        } catch (e: Exception) {
            "🗺️ Error al obtener info de país: ${e.message}"
        }
    }

    suspend fun fetchOpenTrivia(): String = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://opentdb.com/api.php?amount=1")
            val json = JSONObject(raw)
            val results = json.optJSONArray("results")
            val qObj = results?.optJSONObject(0)
            val question = qObj?.optString("question", "") ?: ""
            val answer = qObj?.optString("correct_answer", "") ?: ""
            val category = qObj?.optString("category", "") ?: ""
            val cleanQ = question.replace("&quot;", "\"").replace("&#039;", "'").replace("&amp;", "&")
            val cleanA = answer.replace("&quot;", "\"").replace("&#039;", "'").replace("&amp;", "&")
            "❓ **Trivia ($category)**:\n**Pregunta**: $cleanQ\n\n*(Respuesta: $cleanA)*"
        } catch (e: Exception) {
            "❓ Error al obtener trivia: ${e.message}"
        }
    }

    suspend fun fetchAdviceSlip(): String = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://api.adviceslip.com/advice")
            val json = JSONObject(raw)
            val slip = json.optJSONObject("slip")
            val advice = slip?.optString("advice", "") ?: ""
            "💡 **Consejo del día**:\n\"$advice\""
        } catch (e: Exception) {
            "💡 Error al obtener consejo: ${e.message}"
        }
    }

    suspend fun fetchNumbersApi(numberStr: String = "random"): String = withContext(Dispatchers.IO) {
        try {
            val target = numberStr.trim().ifBlank { "random" }
            val raw = httpGet("http://numbersapi.com/$target/trivia?json")
            val json = JSONObject(raw)
            val text = json.optString("text", "")
            "🔢 **Dato curioso de números**:\n$text"
        } catch (e: Exception) {
            "🔢 Error al obtener dato de números: ${e.message}"
        }
    }

    suspend fun fetchNasaApod(userApiKey: String = ""): String = withContext(Dispatchers.IO) {
        try {
            val key = userApiKey.trim().ifBlank { "DEMO_KEY" }
            val raw = httpGet("https://api.nasa.gov/planetary/apod?api_key=$key")
            val json = JSONObject(raw)
            val title = json.optString("title", "APOD NASA")
            val explanation = json.optString("explanation", "")
            val url = json.optString("url", "")
            "🚀 **NASA Imagen Astronómica del Día ($title)**:\n\n$explanation\n\n🔗 $url"
        } catch (e: Exception) {
            "🚀 Error al obtener NASA APOD: ${e.message}"
        }
    }

    suspend fun fetchIpifyPublicIp(): String = withContext(Dispatchers.IO) {
        try {
            val raw = httpGet("https://api.ipify.org?format=json")
            val json = JSONObject(raw)
            val ip = json.optString("ip", "Desconocida")
            "🌐 **Tu Dirección IP Pública**: `$ip`"
        } catch (e: Exception) {
            "🌐 Error al obtener IP pública: ${e.message}"
        }
    }

    suspend fun fetchWikipediaSummary(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val raw = httpGet("https://es.wikipedia.org/api/rest_v1/page/summary/$encoded")
            val json = JSONObject(raw)
            val title = json.optString("title", "")
            val extract = json.optString("extract", "")
            if (extract.isNotBlank()) {
                "📚 **Wikipedia ($title)**:\n$extract"
            } else {
                "📚 No se encontró resumen en Wikipedia para \"$query\"."
            }
        } catch (e: Exception) {
            "📚 No se encontró información en Wikipedia sobre \"$query\"."
        }
    }

    suspend fun fetchOpenMeteoWeather(cityName: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedCity = URLEncoder.encode(cityName.trim(), "UTF-8")
            val geoRaw = httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=es")
            val geoJson = JSONObject(geoRaw)
            val results = geoJson.optJSONArray("results")
            if (results == null || results.length() == 0) {
                return@withContext "🌤️ No pude encontrar la ciudad \"$cityName\"."
            }
            val first = results.getJSONObject(0)
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            val name = first.optString("name", cityName)
            val country = first.optString("country", "")

            val weatherRaw = httpGet("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
            val weatherJson = JSONObject(weatherRaw)
            val current = weatherJson.optJSONObject("current_weather")
                ?: return@withContext "🌤️ No se pudo obtener el clima de $name."

            val temp = current.optDouble("temperature", 0.0)
            val wind = current.optDouble("windspeed", 0.0)

            "🌤️ **Clima actual en $name${if (country.isNotBlank()) ", $country" else ""}**:\n" +
                    "• Temperatura: ${temp}°C\n" +
                    "• Viento: ${wind} km/h"
        } catch (e: Exception) {
            "🌤️ Error al obtener el clima: ${e.message}"
        }
    }

    suspend fun fetchFrankfurterRate(amount: Double, fromCurrency: String, toCurrency: String): String = withContext(Dispatchers.IO) {
        try {
            val fromCode = fromCurrency.trim().uppercase().ifBlank { "USD" }
            val toCode = toCurrency.trim().uppercase().ifBlank { "EUR" }
            val raw = httpGet("https://api.frankfurter.app/latest?amount=$amount&from=$fromCode&to=$toCode")
            val json = JSONObject(raw)
            val rates = json.optJSONObject("rates")
            val result = rates?.optDouble(toCode, -1.0) ?: -1.0
            if (result > 0) {
                "💱 **Conversión de Moneda**:\n$amount $fromCode = **$result $toCode**"
            } else {
                "💱 No se pudo realizar la conversión entre $fromCode y $toCode."
            }
        } catch (e: Exception) {
            "💱 Error en conversión de moneda: ${e.message}"
        }
    }

    suspend fun fetchDuckDuckGoInstantAnswer(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val raw = httpGet("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1")
            val json = JSONObject(raw)
            val abstract = json.optString("AbstractText", "")
            val answer = json.optString("Answer", "")
            val heading = json.optString("Heading", query)

            val text = answer.ifBlank { abstract }
            if (text.isNotBlank()) {
                "🦆 **DuckDuckGo ($heading)**:\n$text"
            } else {
                "🦆 No hay respuesta rápida en DuckDuckGo para \"$query\"."
            }
        } catch (e: Exception) {
            "🦆 Error en DuckDuckGo: ${e.message}"
        }
    }

    suspend fun fetchRssNews(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://feeds.bbci.co.uk/mundo/rss.xml")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val input: InputStream = conn.inputStream

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(input, "UTF-8")

            var eventType = parser.eventType
            var inItem = false
            var currentTitle = ""
            var currentLink = ""
            val items = mutableListOf<Pair<String, String>>()

            while (eventType != XmlPullParser.END_DOCUMENT && items.size < 5) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                        } else if (inItem && tagName.equals("title", ignoreCase = true)) {
                            currentTitle = parser.nextText().trim()
                        } else if (inItem && tagName.equals("link", ignoreCase = true)) {
                            currentLink = parser.nextText().trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            if (currentTitle.isNotBlank()) {
                                items.add(Pair(currentTitle, currentLink))
                            }
                            inItem = false
                            currentTitle = ""
                            currentLink = ""
                        }
                    }
                }
                eventType = parser.next()
            }
            conn.disconnect()

            if (items.isNotEmpty()) {
                val headlinesText = items.joinToString("\n\n") { "• **${it.first}**\n  ${it.second}" }
                "📰 **Titulares de Noticias Recientes (BBC Mundo)**:\n\n$headlinesText"
            } else {
                "📰 No se pudieron leer las noticias en este momento."
            }
        } catch (e: Exception) {
            "📰 Error al obtener noticias RSS: ${e.message}"
        }
    }

    suspend fun searchYouTubeDataApi(query: String, apiKey: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "▶️ YouTube Data API no configurada. Ingresa tu clave en Configuración para usar búsquedas enriquecidas."
        }
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val raw = httpGet("https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=5&q=$encoded&key=$apiKey")
            val json = JSONObject(raw)
            val items = json.optJSONArray("items")
            if (items == null || items.length() == 0) {
                return@withContext "▶️ No se encontraron videos de YouTube para \"$query\"."
            }
            val resultsText = mutableListOf<String>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val snippet = item.optJSONObject("snippet")
                val idObj = item.optJSONObject("id")
                val videoId = idObj?.optString("videoId", "").orEmpty()
                val title = snippet?.optString("title", "Video") ?: "Video"
                val channel = snippet?.optString("channelTitle", "") ?: ""
                val link = if (videoId.isNotBlank()) "https://www.youtube.com/watch?v=$videoId" else ""
                resultsText.add("• **$title**\n  Canal: $channel ${if (link.isNotBlank()) "\n  $link" else ""}")
            }
            "▶️ **Resultados de YouTube para \"$query\"**:\n\n" + resultsText.joinToString("\n\n")
        } catch (e: Exception) {
            "▶️ Error en YouTube API: ${e.message}"
        }
    }

    suspend fun searchGoogleCustomSearch(query: String, apiKey: String, cseId: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || cseId.isBlank()) {
            return@withContext "🔍 Google Custom Search no configurado. Configura la clave API y el ID CSE en Configuración."
        }
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val raw = httpGet("https://www.googleapis.com/customsearch/v1?q=$encoded&key=$apiKey&cx=$cseId")
            val json = JSONObject(raw)
            val items = json.optJSONArray("items")
            if (items == null || items.length() == 0) {
                return@withContext "🔍 No se encontraron resultados de Google para \"$query\"."
            }
            val resultsText = mutableListOf<String>()
            for (i in 0 until minOf(3, items.length())) {
                val item = items.getJSONObject(i)
                val title = item.optString("title", "")
                val snippet = item.optString("snippet", "")
                val link = item.optString("link", "")
                resultsText.add("• **$title**\n  $snippet\n  $link")
            }
            "🔍 **Búsqueda Google para \"$query\"**:\n\n" + resultsText.joinToString("\n\n")
        } catch (e: Exception) {
            "🔍 Error en Google Search API: ${e.message}"
        }
    }

    fun mlKitTranslate(text: String, targetLang: String, onResult: (String) -> Unit) {
        val targetCode = when (targetLang.lowercase()) {
            "ingles", "inglés", "english", "en" -> TranslateLanguage.ENGLISH
            "frances", "francés", "french", "fr" -> TranslateLanguage.FRENCH
            "aleman", "alemán", "german", "de" -> TranslateLanguage.GERMAN
            "italiano", "italian", "it" -> TranslateLanguage.ITALIAN
            "portugues", "portugués", "portuguese", "pt" -> TranslateLanguage.PORTUGUESE
            else -> TranslateLanguage.SPANISH
        }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.SPANISH)
            .setTargetLanguage(targetCode)
            .build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        onResult("🌐 **Traducción ML Kit ($targetLang)**:\n$translatedText")
                    }
                    .addOnFailureListener { e ->
                        onResult("🌐 Error al traducir: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onResult("🌐 Error descargando modelo de idioma: ${e.message}")
            }
    }

    fun mlKitRecognizeText(context: Context, imageUri: Uri, onResult: (String) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (text.isNotBlank()) {
                        onResult("📄 **Texto detectado (OCR ML Kit)**:\n$text")
                    } else {
                        onResult("📄 No se detectó texto en la imagen.")
                    }
                }
                .addOnFailureListener { e ->
                    onResult("📄 Error al leer texto de imagen: ${e.message}")
                }
        } catch (e: Exception) {
            onResult("📄 Error procesando imagen OCR: ${e.message}")
        }
    }

    fun mlKitScanBarcode(context: Context, imageUri: Uri, onResult: (String) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val contents = barcodes.mapNotNull { it.rawValue }.joinToString("\n") { "• $it" }
                        onResult("📷 **Código QR/Barra Escaneado**:\n$contents")
                    } else {
                        onResult("📷 No se encontraron códigos QR o de barras en la imagen.")
                    }
                }
                .addOnFailureListener { e ->
                    onResult("📷 Error al escanear código: ${e.message}")
                }
        } catch (e: Exception) {
            onResult("📷 Error procesando escáner de código: ${e.message}")
        }
    }

    suspend fun searchLocalPhotos(context: Context, query: String): String = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%${query.trim()}%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val photos = mutableListOf<String>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                if (query.isNotBlank()) selection else null,
                if (query.isNotBlank()) selectionArgs else null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext() && photos.size < 10) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    photos.add("• $name ($contentUri)")
                }
            }
            if (photos.isNotEmpty()) {
                "🖼️ **Fotos encontradas en el teléfono**:\n" + photos.joinToString("\n")
            } else {
                "🖼️ No se encontraron fotos locales que coincidan con \"$query\"."
            }
        } catch (e: Exception) {
            "🖼️ Error al buscar fotos locales: ${e.message}"
        }
    }
}
