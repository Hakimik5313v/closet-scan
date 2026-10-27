// [FILENAME: CamaraManager.java]
package com.hakimik.closetscan;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CamaraManager
 * ------------------------------------------------------------------
 * Encapsula la inicialización y el control de CameraX para el módulo
 * de escaneo del proyecto Closet/Scan.
 *
 * Responsabilidades:
 *  - Inicializar {@link ProcessCameraProvider} y enlazar los casos de uso
 *    {@link Preview} y {@link ImageCapture} al ciclo de vida.
 *  - Capturar fotografías en alta resolución (4:3, máxima calidad) y
 *    guardarlas en el almacenamiento privado de la app
 *    ({@code context.getFilesDir()/prendas}) — nunca en almacenamiento
 *    compartido, cumpliendo el requisito de privacidad del MVP.
 *  - Alternar el flash en modo TORCH / OFF mediante
 *    {@link CameraControl#enableTorch(boolean)}.
 *  - Alternar la lente trasera / frontal
 *    ({@link CameraSelector#LENS_FACING_BACK} /
 *    {@link CameraSelector#LENS_FACING_FRONT}) re-enlazando los casos de uso.
 *
 * Uso típico desde un Fragment:
 * <pre>
 *   camaraManager = new CamaraManager(requireContext(), getViewLifecycleOwner(), previewView);
 *   camaraManager.initialize();
 *   ...
 *   camaraManager.takePhoto(new CamaraManager.PhotoCallback() {
 *       public void onPhotoSaved(@NonNull File file) { ... }
 *       public void onError(@NonNull Exception e) { ... }
 *   });
 * </pre>
 * Todos los métodos públicos deben invocarse desde el hilo principal.
 */
public final class CamaraManager {

    private static final String TAG = "CamaraManager";

    /** Subcarpeta dentro de filesDir() donde se persisten las fotos de prendas. */
    private static final String PHOTOS_DIR = "prendas";
    private static final String FILE_PREFIX = "prenda_";
    private static final String FILE_EXT = ".jpg";

    private final Context appContext;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;
    private final Executor mainExecutor;
    private final ExecutorService ioExecutor;

    private ListenableFuture<ProcessCameraProvider> providerFuture;
    private ProcessCameraProvider cameraProvider;

    @Nullable private Preview preview;
    @Nullable private ImageCapture imageCapture;
    @Nullable private Camera camera;

    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean torchOn = false;
    private boolean initialized = false;
    private boolean initializing = false;

    /**
     * Callback de captura. Los métodos se invocan siempre en el hilo principal.
     */
    public interface PhotoCallback {
        void onPhotoSaved(@NonNull File file);
        void onError(@NonNull Exception exception);
    }

    /**
     * @param context        contexto (se usa el applicationContext internamente)
     * @param lifecycleOwner dueño del ciclo de vida para el enlace de CameraX
     * @param previewView    PreviewView donde se renderiza el preview
     */
    public CamaraManager(@NonNull Context context,
                         @NonNull LifecycleOwner lifecycleOwner,
                         @NonNull PreviewView previewView) {
        this.appContext = context.getApplicationContext();
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        this.mainExecutor = ContextCompat.getMainExecutor(appContext);
        this.ioExecutor = Executors.newSingleThreadExecutor();
    }

    // ------------------------------------------------------------------
    //  Inicialización
    // ------------------------------------------------------------------

    /**
     * Inicializa CameraX de forma asíncrona y enlaza los casos de uso.
     * Seguro invocarlo varias veces: sólo realiza el trabajo una vez.
     */
    @MainThread
    public void initialize() {
        if (initialized || initializing) return;
        initializing = true;

        providerFuture = ProcessCameraProvider.getInstance(appContext);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                bindUseCases();
                initialized = true;
                Log.d(TAG, "CameraX inicializado (lensFacing=" + lensFacing + ")");
            } catch (Exception e) {
                Log.e(TAG, "Error inicializando CameraX", e);
            } finally {
                initializing = false;
            }
        }, mainExecutor);
    }

    /**
     * Enlaza Preview + ImageCapture al ciclo de vida de la pantalla.
     * Se reutiliza en el cambio de lente.
     */
    private void bindUseCases() {
        if (cameraProvider == null) return;

        try {
            cameraProvider.unbindAll();

            preview = new Preview.Builder()
                    .setTargetRotation(currentRotation())
                    .build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            imageCapture = new ImageCapture.Builder()
                    // Calidad máxima: desactiva la reducción de resolución automática.
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    // Alta resolución: 4:3 nativo del sensor, máxima disponible.
                    .setResolutionSelector(buildHighResSelector())
                    // El flash de captura lo gestiona la antorcha (TORCH).
                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    .setTargetRotation(currentRotation())
                    .build();

            CameraSelector selector = new CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build();

            camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, selector, preview, imageCapture);

            // Restaura el estado de la antorcha tras un cambio de lente.
            applyTorch(torchOn);

        } catch (Exception e) {
            Log.e(TAG, "Error enlazando casos de uso de CameraX", e);
        }
    }

    /**
     * Estrategia de resolución: relación de aspecto 4:3 (proporción nativa
     * del sensor fotográfico) eligiendo la resolución más alta disponible.
     */
    @NonNull
    private ResolutionSelector buildHighResSelector() {
        return new ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build();
    }

    // ------------------------------------------------------------------
    //  Captura de foto
    // ------------------------------------------------------------------

    /**
     * Toma una fotografía y la guarda en el almacenamiento privado
     * ({@code filesDir()/prendas}). El callback se invoca en el hilo principal.
     *
     * @param callback listener opcional con la ruta del archivo o el error.
     */
    @MainThread
    public void takePhoto(@Nullable final PhotoCallback callback) {
        if (imageCapture == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException(
                        "CameraX no está inicializado. Invoca initialize() primero."));
            }
            return;
        }

        final File dir = getPhotosDir();
        if (!dir.exists() && !dir.mkdirs()) {
            if (callback != null) {
                callback.onError(new IOException(
                        "No se pudo crear el directorio privado: " + dir.getAbsolutePath()));
            }
            return;
        }

        final File targetFile = new File(dir, buildFileName());

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(targetFile).build();

        imageCapture.takePicture(outputOptions, ioExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        Log.d(TAG, "Foto guardada: " + targetFile.getAbsolutePath());
                        mainExecutor.execute(() -> {
                            if (callback != null) callback.onPhotoSaved(targetFile);
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Error al capturar la foto", exception);
                        mainExecutor.execute(() -> {
                            if (callback != null) callback.onError(exception);
                        });
                    }
                });
    }

    // ------------------------------------------------------------------
    //  Flash (TORCH / OFF)
    // ------------------------------------------------------------------

    /**
     * Alterna la antorcha (modo linterna continua).
     *
     * @return {@code true} si la antorcha quedó encendida, {@code false} si
     *         quedó apagada o si la cámara actual no dispone de unidad de flash.
     */
    @MainThread
    public boolean toggleFlash() {
        if (camera == null) {
            Log.w(TAG, "toggleFlash() invocado antes de enlazar la cámara");
            return torchOn;
        }

        CameraInfo info = camera.getCameraInfo();
        if (!info.hasFlashUnit()) {
            Log.w(TAG, "La cámara actual no dispone de flash");
            torchOn = false;
            return false;
        }

        torchOn = !torchOn;
        applyTorch(torchOn);
        Log.d(TAG, "Flash -> " + (torchOn ? "TORCH" : "OFF"));
        return torchOn;
    }

    /** Fuerza el estado de la antorcha. Útil para restaurar estado en UI. */
    @MainThread
    public void setFlash(boolean on) {
        if (torchOn == on) return;
        toggleFlash();
    }

    private void applyTorch(boolean on) {
        if (camera == null) return;
        CameraControl control = camera.getCameraControl();
        control.enableTorch(on);
    }

    // ------------------------------------------------------------------
    //  Cambio de lente (trasera / frontal)
    // ------------------------------------------------------------------

    /**
     * Alterna entre lente trasera y frontal y re-enlaza los casos de uso.
     * El flash siempre se apaga al cambiar de lente (la frontal no suele
     * disponer de antorcha).
     */
    @MainThread
    public void toggleLens() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;

        torchOn = false;

        if (cameraProvider != null) {
            bindUseCases();
        }
        Log.d(TAG, "Lente cambiada -> " + describeLensFacing());
    }

    // ------------------------------------------------------------------
    //  Ciclo de vida / liberación
    // ------------------------------------------------------------------

    /**
     * Libera la cámara y los recursos del executor. Llamar en onDestroy().
     */
    @MainThread
    public void release() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        camera = null;
        imageCapture = null;
        preview = null;
        initialized = false;
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
        }
        Log.d(TAG, "CamaraManager liberado");
    }

    // ------------------------------------------------------------------
    //  Getters / utilidades
    // ------------------------------------------------------------------

    /** @return la lente actualmente activa (LENS_FACING_BACK / LENS_FACING_FRONT). */
    @NonNull
    public int getLensFacing() {
        return lensFacing;
    }

    /** @return {@code true} si la antorcha está encendida. */
    public boolean isTorchOn() {
        return torchOn;
    }

    /** @return {@code true} si CameraX está listo para capturar. */
    public boolean isReady() {
        return initialized && imageCapture != null && camera != null;
    }

    /** @return el directorio privado donde se guardan las fotos de prendas. */
    @NonNull
    public File getPhotosDir() {
        return new File(appContext.getFilesDir(), PHOTOS_DIR);
    }

    @Nullable
    public ImageCapture getImageCapture() {
        return imageCapture;
    }

    @Nullable
    public Camera getCamera() {
        return camera;
    }

    @NonNull
    private String describeLensFacing() {
        return (lensFacing == CameraSelector.LENS_FACING_BACK) ? "BACK" : "FRONT";
    }

    @NonNull
    private String buildFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
                .format(new Date());
        String lens = (lensFacing == CameraSelector.LENS_FACING_BACK) ? "back" : "front";
        return FILE_PREFIX + timestamp + "_" + lens + FILE_EXT;
    }

    private int currentRotation() {
        if (previewView.getDisplay() != null) {
            return previewView.getDisplay().getRotation();
        }
        return Surface.ROTATION_0;
    }
}