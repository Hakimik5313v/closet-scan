package com.hakimik.closetscan.engine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.hakimik.closetscan.util.AppLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GeminiApiClient — Cliente para Google AI Studio (Gemini 1.5 Flash).
 * Integrado con AppLogger para trazabilidad de errores, cuota y respuestas en tiempo real.
 */
public class GeminiApiClient {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";

    public interface Callback {
        void onProgress(int percentage);
        void onSuccess(String textoCompleto, String puntuacion);
        void onError(String mensaje);
    }

    public interface TestCallback {
        void onSuccess(String mensaje);
        void onError(String error);
    }

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public void evaluarOutfit(String apiKey,
                               String rutaFotoUsuario,
                               String prendaSuperior,
                               String prendaInferior,
                               String prendaCalzado,
                               Callback callback) {
        analizarOutfit(apiKey, rutaFotoUsuario, prendaSuperior, prendaInferior, prendaCalzado, callback);
    }

    public void analizarOutfit(String apiKey,
                               String rutaFotoUsuario,
                               String prendaSuperior,
                               String prendaInferior,
                               String prendaCalzado,
                               Callback callback) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            AppLogger.w("IA", "Intento de análisis sin API Key configurada.");
            callback.onError("Configura tu API Key de Google AI Studio en ⚙️ Ajustes");
            return;
        }

        AppLogger.gemini("Iniciando análisis de outfit con Gemini 1.5 Flash...");
        AppLogger.i("IA", "Prendas -> Sup: " + prendaSuperior + " | Inf: " + prendaInferior + 
                " | Calzado: " + prendaCalzado + 
                " | Foto usuario: " + (rutaFotoUsuario != null ? rutaFotoUsuario : "Silueta"));

        executor.execute(() -> {
            postProgreso(callback, 10);
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
            postProgreso(callback, 25);

            long startTime = System.currentTimeMillis();

            try {
                // Construir prompt detallado
                StringBuilder prompt = new StringBuilder();
                prompt.append("Eres un asesor de moda de alto nivel y estilista visual. Analiza este outfit en español:\n\n");
                if (prendaSuperior != null) prompt.append("• Prenda Superior: ").append(prendaSuperior).append("\n");
                if (prendaInferior != null) prompt.append("• Prenda Inferior: ").append(prendaInferior).append("\n");
                if (prendaCalzado != null)  prompt.append("• Calzado: ").append(prendaCalzado).append("\n");
                prompt.append("\nResponde EXACTAMENTE en este formato (máx 90 palabras):\n");
                prompt.append("PUNTUACIÓN: X/10\n");
                prompt.append("LOOK: [Una frase describiendo el estilo y cohesión general]\n");
                prompt.append("CONSEJO: [Un consejo de mejora corto con emoji]");

                JSONObject jsonBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt.toString());
                parts.put(textPart);

                // Procesar e incluir imagen si está disponible
                if (rutaFotoUsuario != null && !rutaFotoUsuario.isEmpty()) {
                    byte[] bytesImagen = prepararBytesImagen(rutaFotoUsuario);
                    if (bytesImagen != null) {
                        String b64 = Base64.encodeToString(bytesImagen, Base64.NO_WRAP);
                        JSONObject imgPart = new JSONObject();
                        JSONObject inline = new JSONObject();
                        inline.put("mime_type", "image/jpeg");
                        inline.put("data", b64);
                        imgPart.put("inline_data", inline);
                        parts.put(imgPart);
                        AppLogger.gemini("Foto adjuntada al prompt (" + (bytesImagen.length / 1024) + " KB).");
                    } else {
                        AppLogger.w("IA", "No se pudo preparar la imagen en ruta: " + rutaFotoUsuario);
                    }
                }

                content.put("parts", parts);
                contents.put(content);
                jsonBody.put("contents", contents);

                JSONObject genConfig = new JSONObject();
                genConfig.put("maxOutputTokens", 250);
                genConfig.put("temperature", 0.7);
                jsonBody.put("generationConfig", genConfig);

                postProgreso(callback, 50);
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                postProgreso(callback, 70);

                String url = BASE_URL + "?key=" + apiKey.trim();
                RequestBody reqBody = RequestBody.create(
                        jsonBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );
                Request request = new Request.Builder().url(url).post(reqBody).build();

                postProgreso(callback, 85);

                AppLogger.gemini("Enviando petición HTTP POST a Gemini API...");

                try (Response response = httpClient.newCall(request).execute()) {
                    long latency = System.currentTimeMillis() - startTime;
                    String rbody = response.body() != null ? response.body().string() : "";

                    AppLogger.i("IA", "Respuesta HTTP " + response.code() + " en " + latency + "ms.");

                    if (!response.isSuccessful()) {
                        AppLogger.e("IA", "Error en llamada a Gemini: Código " + response.code() + " | Body: " + rbody);
                        final String err = parsearError(rbody, response.code());
                        uiHandler.post(() -> callback.onError(err));
                        return;
                    }

                    JSONObject resp = new JSONObject(rbody);
                    String texto = resp.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    String puntuacion = extraerPuntuacion(texto);
                    AppLogger.gemini("Éxito en respuesta (" + texto.length() + " caracteres, " + puntuacion + ").");

                    uiHandler.post(() -> {
                        callback.onProgress(100);
                        callback.onSuccess(texto.trim(), puntuacion);
                    });
                }

            } catch (Exception e) {
                AppLogger.e("IA", "Excepción de red/petición en GeminiApiClient", e);
                uiHandler.post(() -> callback.onError("Error de conexión: " + e.getMessage()));
            }
        });
    }

    /**
     * Prueba rápida de conexión para verificar que el API Key funciona correctamente.
     */
    public void probarConexion(String apiKey, TestCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("La clave está vacía.");
            return;
        }

        AppLogger.gemini("Iniciando test de conexión con Google AI Studio...");

        executor.execute(() -> {
            long t0 = System.currentTimeMillis();
            try {
                JSONObject jsonBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                JSONObject textPart = new JSONObject();
                textPart.put("text", "Responde exactamente la palabra: OK");
                parts.put(textPart);

                content.put("parts", parts);
                contents.put(content);
                jsonBody.put("contents", contents);

                JSONObject genConfig = new JSONObject();
                genConfig.put("maxOutputTokens", 10);
                jsonBody.put("generationConfig", genConfig);

                String url = BASE_URL + "?key=" + apiKey.trim();
                RequestBody reqBody = RequestBody.create(
                        jsonBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );
                Request request = new Request.Builder().url(url).post(reqBody).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    long latency = System.currentTimeMillis() - t0;
                    String body = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        AppLogger.gemini("Test de conexión EXITOSO (" + latency + "ms). Tu API Key está activa.");
                        uiHandler.post(() -> callback.onSuccess("✅ Conexión exitosa con Gemini en " + latency + "ms. Clave activa y lista."));
                    } else {
                        AppLogger.e("IA", "Test de conexión FALLIDO: HTTP " + response.code() + " | " + body);
                        String errMsg = parsearError(body, response.code());
                        uiHandler.post(() -> callback.onError("❌ " + errMsg));
                    }
                }
            } catch (Exception e) {
                AppLogger.e("IA", "Error en test de conexión", e);
                uiHandler.post(() -> callback.onError("❌ Error de red: " + e.getMessage()));
            }
        });
    }

    private void postProgreso(Callback callback, int valor) {
        uiHandler.post(() -> callback.onProgress(valor));
    }

    /**
     * Prepara los bytes de la imagen, redimensionando a máx 1024x1024 para evitar problemas de cuota o payload.
     */
    private byte[] prepararBytesImagen(String ruta) {
        try {
            File f = new File(ruta);
            if (!f.exists()) {
                AppLogger.w("IMG", "Archivo de imagen no encontrado en: " + ruta);
                return null;
            }

            // Si es menor a 1MB, leer directo
            if (f.length() < 1024 * 1024) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] data = new byte[(int) f.length()];
                    int read = fis.read(data);
                    if (read > 0) return data;
                }
            }

            // Si es más grande, decodificar con muestreo y recomprimir a JPEG
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(ruta, opts);

            int maxDim = Math.max(opts.outWidth, opts.outHeight);
            int inSampleSize = 1;
            while (maxDim / inSampleSize > 1024) {
                inSampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = inSampleSize;
            Bitmap bmp = BitmapFactory.decodeFile(ruta, opts);
            if (bmp != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                bmp.recycle();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            AppLogger.e("IMG", "Error al procesar imagen para Gemini: " + e.getMessage(), e);
        }
        return null;
    }

    private String parsearError(String body, int code) {
        try {
            JSONObject err = new JSONObject(body).getJSONObject("error");
            String msg = err.getString("message");
            if (code == 400 && msg.toLowerCase().contains("api_key")) {
                return "API Key inválida. Revisa tu clave en ⚙️";
            }
            if (code == 403) {
                return "Acceso denegado (403): Revisa permisos o región en Google AI Studio.";
            }
            if (code == 429) {
                return "Límite de cuota alcanzado (429). Espera un minuto o cambia de clave.";
            }
            return "Error " + code + ": " + msg;
        } catch (Exception e) {
            return "Error del servidor Gemini (código " + code + ")";
        }
    }

    private String extraerPuntuacion(String texto) {
        String[] lineas = texto.split("\n");
        for (String linea : lineas) {
            if (linea.startsWith("PUNTUACIÓN:") || linea.startsWith("PUNTUACION:")) {
                return linea.trim();
            }
        }
        return "";
    }
}
