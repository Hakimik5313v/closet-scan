package com.hakimik.closetscan.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AppLogger — Sistema de auditoría y diagnóstico centralizado para Closet/Scan.
 * Registra eventos del ciclo de vida, llamadas a Gemini IA, operaciones de imágenes y errores.
 * Los registros están disponibles en memoria y en almacenamiento local para inspección desde ⚙️ Ajustes.
 */
public class AppLogger {

    public interface LogListener {
        void onNuevoLog(String entrada);
    }

    private static final String TAG = "ClosetScanApp";
    private static final int MAX_ENTRIES = 200;
    private static final List<String> logsEnMemoria = new ArrayList<>();
    private static final List<LogListener> listeners = new ArrayList<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private static File archivoLog = null;

    public static synchronized void init(Context context) {
        if (archivoLog == null && context != null) {
            try {
                File dir = new File(context.getFilesDir(), "logs");
                if (!dir.exists()) dir.mkdirs();
                archivoLog = new File(dir, "closet_scan.log");
                i("LOGGER", "Sistema de logs inicializado. Archivo: " + archivoLog.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "No se pudo crear archivo de logs", e);
            }
        }
    }

    public static synchronized void i(String tag, String mensaje) {
        agregarEntrada("INFO", tag, mensaje);
    }

    public static synchronized void w(String tag, String mensaje) {
        agregarEntrada("WARN", tag, mensaje);
    }

    public static synchronized void e(String tag, String mensaje) {
        agregarEntrada("ERROR", tag, mensaje);
    }

    public static synchronized void e(String tag, String mensaje, Throwable t) {
        String stack = "";
        if (t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            stack = " | " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
        agregarEntrada("ERROR", tag, mensaje + stack);
    }

    public static synchronized void gemini(String mensaje) {
        agregarEntrada("GEMINI", "IA", mensaje);
    }

    public static synchronized void foto(String mensaje) {
        agregarEntrada("FOTO", "MEDIA", mensaje);
    }

    private static synchronized void agregarEntrada(String nivel, String tag, String mensaje) {
        String timestamp = dateFormat.format(new Date());
        String entrada = String.format(Locale.getDefault(), "[%s] [%s] [%s] %s", timestamp, nivel, tag, mensaje);

        // Imprimir también a Logcat de Android
        switch (nivel) {
            case "ERROR":
                Log.e(TAG, entrada);
                break;
            case "WARN":
                Log.w(TAG, entrada);
                break;
            default:
                Log.i(TAG, entrada);
                break;
        }

        logsEnMemoria.add(entrada);
        if (logsEnMemoria.size() > MAX_ENTRIES) {
            logsEnMemoria.remove(0);
        }

        // Escribir a archivo si existe
        if (archivoLog != null) {
            try (FileWriter fw = new FileWriter(archivoLog, true)) {
                fw.write(entrada + "\n");
            } catch (Exception ignored) {}
        }

        // Notificar a listeners activos (UI de diagnóstico)
        for (LogListener l : new ArrayList<>(listeners)) {
            try {
                l.onNuevoLog(entrada);
            } catch (Exception ignored) {}
        }
    }

    public static synchronized String getLogsFormatted() {
        if (logsEnMemoria.isEmpty()) {
            return "[Sin registros aún. Realiza acciones o pruebas de IA para ver actividad]";
        }
        StringBuilder sb = new StringBuilder();
        for (String l : logsEnMemoria) {
            sb.append(l).append("\n");
        }
        return sb.toString();
    }

    public static synchronized void clear() {
        logsEnMemoria.clear();
        if (archivoLog != null && archivoLog.exists()) {
            archivoLog.delete();
        }
        i("LOGGER", "Registros de diagnóstico limpiados por el usuario.");
    }

    public static synchronized void addListener(LogListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static synchronized void removeListener(LogListener listener) {
        listeners.remove(listener);
    }
}
