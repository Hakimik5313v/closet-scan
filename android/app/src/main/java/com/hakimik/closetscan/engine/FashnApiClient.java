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
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * FashnApiClient — Cliente para la API de Virtual Try-On generativo de FASHN.ai.
 * Recibe la foto de cuerpo completo de la persona + la foto de la prenda y devuelve
 * la imagen fotorrealista generada por difusión.
 */
public class FashnApiClient {

    private static final String BASE_URL = "https://api.fashn.ai/v1";

    public interface Callback {
        void onProgress(int percentage, String estado);
        void onSuccess(Bitmap imagenGenerada, String urlImagen);
        void onError(String mensaje);
    }

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public void ejecutarTryOn(String apiKey,
                             String rutaFotoUsuario,
                             String rutaFotoPrenda,
                             String categoria,
                             Callback callback) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Configura tu Token de FASHN.ai en ⚙️ Ajustes (10 créditos gratis).");
            return;
        }

        if (rutaFotoUsuario == null || rutaFotoUsuario.isEmpty()) {
            callback.onError("Primero sube tu foto en el Vestier para poder vestirte con la prenda.");
            return;
        }

        if (rutaFotoPrenda == null || rutaFotoPrenda.isEmpty()) {
            callback.onError("Selecciona una prenda con foto para generar el Try-On.");
            return;
        }

        AppLogger.i("VTON", "Iniciando generación fotorrealista con FASHN.ai...");

        executor.execute(() -> {
            try {
                postProgreso(callback, 10, "Optimizando imágenes para la IA...");

                String base64Persona = convertirADataUri(rutaFotoUsuario);
                String base64Prenda = convertirADataUri(rutaFotoPrenda);

                if (base64Persona == null || base64Prenda == null) {
                    uiHandler.post(() -> callback.onError("Error al procesar los archivos de imagen locales."));
                    return;
                }

                postProgreso(callback, 25, "Enviando solicitud al motor de difusión...");

                // Mapeo de categoría para FASHN (tops, bottoms, one-pieces)
                String catFashn = "tops";
                if (categoria != null) {
                    String c = categoria.toUpperCase();
                    if (c.contains("INFERIOR") || c.contains("PANTALON") || c.contains("SHORT")) {
                        catFashn = "bottoms";
                    }
                }

                JSONObject payload = new JSONObject();
                payload.put("model_image", base64Persona);
                payload.put("garment_image", base64Prenda);
                payload.put("category", catFashn);

                RequestBody reqBody = RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(BASE_URL + "/run")
                        .addHeader("Authorization", "Bearer " + apiKey.trim())
                        .post(reqBody)
                        .build();

                String predictionId = null;
                try (Response response = httpClient.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    AppLogger.i("VTON", "Respuesta inicial /run: HTTP " + response.code());

                    if (!response.isSuccessful()) {
                        AppLogger.e("VTON", "Fallo al iniciar predicción: " + respBody);
                        String err = parsearError(respBody, response.code());
                        uiHandler.post(() -> callback.onError(err));
                        return;
                    }

                    JSONObject jsonResp = new JSONObject(respBody);
                    predictionId = jsonResp.optString("id");
                }

                if (predictionId == null || predictionId.isEmpty()) {
                    uiHandler.post(() -> callback.onError("No se recibió identificador de tarea de FASHN."));
                    return;
                }

                AppLogger.i("VTON", "Tarea en proceso (ID: " + predictionId + "). Esperando render...");

                // Polling cada 3 segundos hasta que termine (máx 90 segundos)
                int intentos = 0;
                int maxIntentos = 30;
                String urlResultado = null;

                while (intentos < maxIntentos) {
                    Thread.sleep(3000);
                    intentos++;

                    int progresoEstimado = Math.min(90, 30 + (intentos * 2));
                    postProgreso(callback, progresoEstimado, "Generando prenda y adaptando pliegues (" + (intentos * 3) + "s)...");

                    Request pollRequest = new Request.Builder()
                            .url(BASE_URL + "/status/" + predictionId)
                            .addHeader("Authorization", "Bearer " + apiKey.trim())
                            .get()
                            .build();

                    try (Response pollResponse = httpClient.newCall(pollRequest).execute()) {
                        String statusBody = pollResponse.body() != null ? pollResponse.body().string() : "";

                        if (pollResponse.isSuccessful()) {
                            JSONObject statusJson = new JSONObject(statusBody);
                            String status = statusJson.optString("status");
                            AppLogger.i("VTON", "Estado predicción: " + status);

                            if ("completed".equalsIgnoreCase(status)) {
                                JSONArray outputArray = statusJson.optJSONArray("output");
                                if (outputArray != null && outputArray.length() > 0) {
                                    urlResultado = outputArray.getString(0);
                                }
                                break;
                            } else if ("failed".equalsIgnoreCase(status)) {
                                String errorMsg = statusJson.optString("error", "Error desconocido en el servidor de IA");
                                uiHandler.post(() -> callback.onError("Fallo en render: " + errorMsg));
                                return;
                            }
                        }
                    }
                }

                if (urlResultado == null) {
                    uiHandler.post(() -> callback.onError("El tiempo de render excedió el límite. Intenta de nuevo."));
                    return;
                }

                postProgreso(callback, 95, "Descargando imagen fotorrealista final...");
                AppLogger.i("VTON", "Descargando imagen renderizada desde: " + urlResultado);

                // Descargar el Bitmap del resultado
                Request dlRequest = new Request.Builder().url(urlResultado).get().build();
                Bitmap bitmapFinal = null;
                try (Response dlResponse = httpClient.newCall(dlRequest).execute()) {
                    if (dlResponse.isSuccessful() && dlResponse.body() != null) {
                        try (InputStream is = dlResponse.body().byteStream()) {
                            bitmapFinal = BitmapFactory.decodeStream(is);
                        }
                    }
                }

                if (bitmapFinal != null) {
                    final Bitmap bmpFinal = bitmapFinal;
                    final String urlFinal = urlResultado;
                    AppLogger.i("VTON", "Imagen fotorrealista generada y recibida con éxito (" + bmpFinal.getWidth() + "x" + bmpFinal.getHeight() + ").");
                    uiHandler.post(() -> {
                        callback.onProgress(100, "¡Outfit generado exitosamente!");
                        callback.onSuccess(bmpFinal, urlFinal);
                    });
                } else {
                    uiHandler.post(() -> callback.onError("No se pudo decodificar la imagen devuelta por FASHN."));
                }

            } catch (Exception e) {
                AppLogger.e("VTON", "Error de comunicación con FASHN.ai", e);
                uiHandler.post(() -> callback.onError("Error de conexión con FASHN: " + e.getMessage()));
            }
        });
    }

    private void postProgreso(Callback callback, int pct, String estado) {
        uiHandler.post(() -> callback.onProgress(pct, estado));
    }

    /**
     * Convierte una ruta de imagen local en un Data URI estándar data:image/jpeg;base64,...
     * redimensionando a máx 1024x1024 para optimizar la transferencia y el tiempo de render.
     */
    private String convertirADataUri(String ruta) {
        try {
            File f = new File(ruta);
            if (!f.exists()) return null;

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
            if (bmp == null) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            bmp.recycle();

            byte[] bytes = baos.toByteArray();
            String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + b64;
        } catch (Exception e) {
            AppLogger.e("VTON", "Error al codificar imagen a base64", e);
            return null;
        }
    }

    private String parsearError(String body, int code) {
        try {
            JSONObject json = new JSONObject(body);
            String msg = json.optString("error", json.optString("message", ""));
            if (code == 401 || msg.toLowerCase().contains("unauthorized") || msg.toLowerCase().contains("invalid key")) {
                return "Token de FASHN inválido. Revisa tu clave en ⚙️ Ajustes.";
            }
            if (code == 402 || msg.toLowerCase().contains("credits") || msg.toLowerCase().contains("balance")) {
                return "Te quedaste sin créditos en FASHN. Puedes recargar o usar otra cuenta.";
            }
            if (!msg.isEmpty()) return "FASHN (Error " + code + "): " + msg;
        } catch (Exception ignored) {}
        return "Error en servicio de IA FASHN (código HTTP " + code + ")";
    }
}
