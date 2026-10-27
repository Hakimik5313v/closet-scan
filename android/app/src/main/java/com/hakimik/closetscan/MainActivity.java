package com.hakimik.closetscan;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.annotation.SuppressLint;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.hakimik.closetscan.data.entity.OutfitEntity;
import com.hakimik.closetscan.data.entity.PrendaEntity;
import com.hakimik.closetscan.engine.FashnApiClient;
import com.hakimik.closetscan.engine.GeminiApiClient;
import com.hakimik.closetscan.engine.RecomendadorEngine;
import com.hakimik.closetscan.util.AppLogger;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.os.Environment;
import android.os.Build;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import android.graphics.BitmapFactory;
import android.util.Base64;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * MainActivity — Sistema Completo Closet/Scan (Estilo Noir Monocromático):
 *  - INICIO: Dashboard de estilo con Gráfico de Dona nativo y Carrusel de Escaneos Recientes
 *  - ARMARIO: Galería completa con filtros por categoría y búsqueda en tiempo real
 *  - ESCÁNER: Captura fotográfica con CameraX (flash, cambio de lente, visor de encuadre)
 *  - FICHA DE PRENDA: Edición interactiva de nombre, categoría, ocasión y material
 *  - VESTIER DIGITAL (Opción 2): Comparativa Lado a Lado con Presencia Humana (Tu Foto o Silueta)
 *    + Capas de Outfit seleccionadas + Motor Virtual Try-On con IA y Modal de Resultado.
 *  - ASESOR: Pestaña funcional de recomendaciones y tendencias
 */
public class MainActivity extends AppCompatActivity {

    private PrendaAdapter adapter;
    private PrendaViewModel viewModel;
    private List<PrendaEntity> todasLasPrendas = new ArrayList<>();
    private String filtroActual = "TODOS";
    private String textoBusquedaActual = "";
    private PrendaEntity prendaEnEdicion = null;
    private String tabActual = "INICIO";
    private String rutaFotoTemporal = null;

    // Vistas contenedoras
    private View viewInicio, viewArmario, viewConfirmarPrenda, viewEscaner, viewVestier, viewAsesor;
    private View topBarGlobal, dividerTop, bottomNavigation, dividerBottom;

    // Tabs inferiores
    private TextView tabInicio, tabArmario, tabEscaner, tabVestier, tabAsesor;

    // Widgets de Inicio / Estadísticas y Dashboard
    private GraficoDonaView graficoDonaInicio;
    private TextView tvTotalPrendasStat;
    private TextView tvLeyendaItem1, tvLeyendaItem2, tvLeyendaItem3;
    private TextView btnVerTodoArmario;
    private LinearLayout llContenedorRecientes;

    // Widgets de Armario (Galería y Búsqueda)
    private EditText etBuscarPrenda;
    private TextView tvArmarioSubtitulo;
    private TextView btnFiltroTodos, btnFiltroSuperior, btnFiltroInferior, btnFiltroCalzado, btnFiltroFavoritos;
    private ExtendedFloatingActionButton fabAgregarPrenda;

    // Widgets de Ficha "CONFIRMAR PRENDA" (Figma)
    private TextView btnVolverDeConfirmar, tvTituloConfirmar, tvFotoFichaTexto, tvColorBadgeFicha;
    private ImageView imgFotoFichaReal;
    private EditText etNombreFicha;
    private TextView pillCategoria, pillMaterial, pillOcasión;
    private Spinner spinnerCategoriaFicha, spinnerEstiloFicha, spinnerMaterialFicha;
    private MaterialButton btnGuardarFicha, btnProbarVestierDesdeFicha, btnQuitarFondoPrenda;
    private TextView btnEliminarFicha;
    private String colorDetectadoHex = "#FFFFFF";
    private String colorDetectadoNombre = "Blanco";

    // Widgets de Escáner (CameraX)
    private CamaraManager camaraManager;
    private PreviewView previewViewEscaner;
    private TextView btnFlashEscaner, btnGaleriaEscaner, btnGirarCamaraEscaner;
    private View btnDisparadorEscaner;
    private ActivityResultLauncher<String> permisoCamaraLauncher;
    private ActivityResultLauncher<String> selectorGaleriaLauncher;
    private ActivityResultLauncher<String> selectorFotoUsuarioLauncher;

    // Widgets del Vestier Digital (Opción 2 — Humano + Outfit + IA)
    private TextView tvVestierAfinidad, btnVestierLimpiar, btnVestierHistorial;
    private ImageView imgFotoUsuarioVestier;
    private TextView tvBadgeUsuario, btnCambiarFotoUsuario, btnQuitarFotoUsuario;
    private FrameLayout frameCapaSuperior, frameCapaInferior, frameCapaCalzado;
    private ImageView imgVestierSuperior, imgVestierInferior, imgVestierCalzado;
    private TextView tvVestierPlaceholderSuperior, tvVestierPlaceholderInferior, tvVestierPlaceholderCalzado;
    private TextView btnQuitarSuperior, btnQuitarInferior, btnQuitarCalzado;
    private TextView btnTabVestierSuperior, btnTabVestierInferior, btnTabVestierCalzado, btnTabVestierAbrigo;
    private LinearLayout llContenedorPrendasVestier;
    private MaterialButton btnVestierAleatorio, btnProbarConIA, btnGuardarOutfit;

    // Modal Virtual Try-On (IA)
    private FrameLayout modalVirtualTryOn;
    private TextView btnCerrarModalTryOn;
    private ImageView imgResultadoTryOn;
    private TextView tvDetallePrendasTryOn, tvScoreTryOn;
    private MaterialButton btnGuardarLookTryOn, btnCompartirLookTryOn, btnVerHistorialTryOn, btnExportarGaleriaTryOn;
    private TextView btnCompararAntesDespues, tvBadgeEstadoComparacion;
    private Bitmap currentTryOnBitmap = null;
    // Variables para Zoom táctil (Pinch-to-zoom) y Comparador Antes/Después
    private final Matrix matrixTryOn = new Matrix();
    private final Matrix savedMatrixTryOn = new Matrix();
    private final PointF startPointTryOn = new PointF();
    private final PointF midPointTryOn = new PointF();
    private float oldDistTryOn = 1f;
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_DRAG = 1;
    private static final int TOUCH_ZOOM = 2;
    private int modeTryOn = TOUCH_NONE;
    // Vistas de progreso IA dentro del modal
    private android.widget.LinearLayout llProgresoIA, llResultadoIA;
    private android.widget.ProgressBar progressBarIA;
    private TextView tvProgresoTexto, tvPorcentajeIA;

    // Modal Ajustar Foto Usuario
    private FrameLayout modalAjustarFotoUsuario;
    private TextView btnCerrarModalAjustarFoto;
    private ImageView imgAjustarFotoPreview;
    private TextView tvZoomNivel, btnZoomMenos, btnZoomMas, btnMoverArriba, btnMoverAbajo, btnCentrarReset;
    private MaterialButton btnConfirmarFotoAjustada, btnQuitarFondoFoto;
    private Bitmap bitmapOriginalFotoUsuario = null;
    private float zoomFotoUsuario = 1.0f;
    private float offsetYFotoUsuario = 0f;

    // Modal Historial Looks
    private FrameLayout modalHistorialLooks;
    private TextView btnCerrarModalHistorial;
    private LinearLayout llListaHistorialLooks;


    // INICIO — sección outfits guardados
    private TextView btnVerOutfitsVestier;
    private TextView tvOutfitsVacioInicio;
    private android.widget.HorizontalScrollView hsvOutfitsGuardados;
    private android.widget.LinearLayout llContenedorOutfitsInicio;

    // Motor Gemini
    private GeminiApiClient geminiClient;
    private static final String PREF_GEMINI_API_KEY = "gemini_api_key";

    // Motor FASHN.ai (Virtual Try-On Generativo Fotorrealista)
    private FashnApiClient fashnClient;
    private static final String PREF_FASHN_API_KEY = "fashn_api_key";

    // Selector de Motor de Try-On (Local, Replicate, Gemini)
    private static final String PREF_MOTOR_TRYON = "motor_tryon";
    private static final String PREF_REPLICATE_API_KEY = "replicate_api_key";
    private static final String PREF_GEMINI_IMG_PROMPT = "gemini_img_prompt";
    private static final String MOTOR_LOCAL = "LOCAL";
    private static final String MOTOR_REPLICATE = "REPLICATE";
    private static final String MOTOR_GEMINI = "GEMINI";
    private static final String DEFAULT_GEMINI_PROMPT = "Viste a esta persona con la prenda seleccionada, manteniendo su rostro, cuerpo y pose fotorrealistas.";


    // Estado del Vestier
    private PrendaEntity prendaSuperiorSeleccionada = null;
    private PrendaEntity prendaInferiorSeleccionada = null;
    private PrendaEntity prendaCalzadoSeleccionada = null;
    private String categoriaVestierActiva = "SUPERIOR";
    private String rutaFotoUsuarioPersonal = null;

    // ASESOR & RECOMENDACIONES (HakiForge Match)
    private TextView btnRefrescarAsesor;
    private TextView tvBadgePorcentajeAsesor;
    private TextView tvSubtituloClimaAsesor;
    private ImageView imgPreviewOutfitSugerido;
    private TextView tvPlaceholderOutfitSugerido;
    private TextView tvSugeridoSupNombre, tvSugeridoInfNombre, tvSugeridoCalNombre;
    private MaterialButton btnProbarLookEnVestier;
    private LinearLayout llContenedorOfertasTiendas;
    private PrendaEntity sugerenciaSupActual = null;
    private PrendaEntity sugerenciaInfActual = null;
    private PrendaEntity sugerenciaCalActual = null;

    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREF_FOTO_USUARIO = "foto_usuario_personal";

    // TopBar
    private TextView btnConfiguracion;

    private final String[] categoriasOpciones = {"Superior", "Inferior", "Calzado", "Abrigo"};
    private final String[] estilosOpciones = {"Casual", "Formal", "Elegante", "Deportivo", "Clásico"};
    private final String[] materialesOpciones = {"Algodón", "Lino", "Cuero", "Denim / Jean", "Poliéster", "Lana", "Seda"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar sistema de auditoría y diagnóstico
        AppLogger.init(this);
        AppLogger.i("INICIO", "Closet/Scan iniciado.");

        prefs = getSharedPreferences("closet_scan_prefs", MODE_PRIVATE);

        vincularVistas();
        configurarLaunchers();
        configurarNavegacionInferior();
        configurarDashboardInicio();
        configurarArmario();
        configurarFichaConfirmarPrenda();
        configurarEscaner();
        configurarVestier();
        configurarModalVirtualTryOn();
        configurarModalAjustarFoto();
        configurarModalHistorial();
        configurarAsesor();
        configurarTopBar();
        configurarBotonAtras();

        viewModel = new ViewModelProvider(this).get(PrendaViewModel.class);
        viewModel.getPrendas().observe(this, prendas -> {
            todasLasPrendas = (prendas != null) ? prendas : new ArrayList<>();
            actualizarEstadisticas();
            aplicarFiltrosYBusqueda();
            actualizarCarruselVestier();
        });

        // Observar outfits guardados para mostrarlos en INICIO
        viewModel.getOutfits().observe(this, this::actualizarOutfitsEnInicio);

        viewModel.sembrarPrendasSiVacio();

        // Inicializar clientes de IA
        geminiClient = new GeminiApiClient();
        fashnClient = new FashnApiClient();

        // Iniciar en el Hub Principal
        cambiarTab("INICIO");
    }

    private void configurarLaunchers() {
        permisoCamaraLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        iniciarCamara();
                    } else {
                        Toast.makeText(this, "Se requiere permiso de cámara para el Escáner", Toast.LENGTH_LONG).show();
                        cambiarTab("INICIO");
                    }
                }
        );

        selectorGaleriaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String rutaLocal = copiarUriAArchivoLocal(uri, "prenda_galeria");
                        if (rutaLocal != null) {
                            abrirFichaPrenda(null, rutaLocal);
                            tvFotoFichaTexto.setText("🖼️ [ FOTO DE GALERÍA LISTA ]");
                        }
                    }
                }
        );

        selectorFotoUsuarioLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        abrirEditorAjustarFoto(uri);
                    }
                }
        );
    }

    private void configurarBotonAtras() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (modalAjustarFotoUsuario != null && modalAjustarFotoUsuario.getVisibility() == View.VISIBLE) {
                    modalAjustarFotoUsuario.setVisibility(View.GONE);
                } else if (modalHistorialLooks != null && modalHistorialLooks.getVisibility() == View.VISIBLE) {
                    modalHistorialLooks.setVisibility(View.GONE);
                } else if (modalVirtualTryOn != null && modalVirtualTryOn.getVisibility() == View.VISIBLE) {
                    modalVirtualTryOn.setVisibility(View.GONE);
                } else if (viewConfirmarPrenda.getVisibility() == View.VISIBLE) {
                    cerrarFicha();
                } else if (!"INICIO".equals(tabActual)) {
                    cambiarTab("INICIO");
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void vincularVistas() {
        topBarGlobal = findViewById(R.id.topBarGlobal);
        dividerTop = findViewById(R.id.dividerTop);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        dividerBottom = findViewById(R.id.dividerBottom);

        viewInicio = findViewById(R.id.viewInicio);
        viewArmario = findViewById(R.id.viewArmario);
        viewConfirmarPrenda = findViewById(R.id.viewConfirmarPrenda);
        viewEscaner = findViewById(R.id.viewEscaner);
        viewVestier = findViewById(R.id.viewVestier);
        viewAsesor = findViewById(R.id.viewAsesor);

        tabInicio = findViewById(R.id.tabInicio);
        tabArmario = findViewById(R.id.tabArmario);
        tabEscaner = findViewById(R.id.tabEscaner);
        tabVestier = findViewById(R.id.tabVestier);
        tabAsesor = findViewById(R.id.tabAsesor);

        // Dashboard Inicio
        graficoDonaInicio = findViewById(R.id.graficoDonaInicio);
        tvTotalPrendasStat = findViewById(R.id.tvTotalPrendasStat);
        tvLeyendaItem1 = findViewById(R.id.tvLeyendaItem1);
        tvLeyendaItem2 = findViewById(R.id.tvLeyendaItem2);
        tvLeyendaItem3 = findViewById(R.id.tvLeyendaItem3);
        btnVerTodoArmario = findViewById(R.id.btnVerTodoArmario);
        llContenedorRecientes = findViewById(R.id.llContenedorRecientes);

        // Armario
        etBuscarPrenda = findViewById(R.id.etBuscarPrenda);
        tvArmarioSubtitulo = findViewById(R.id.tvArmarioSubtitulo);
        btnFiltroTodos = findViewById(R.id.btnFiltroTodos);
        btnFiltroSuperior = findViewById(R.id.btnFiltroSuperior);
        btnFiltroInferior = findViewById(R.id.btnFiltroInferior);
        btnFiltroCalzado = findViewById(R.id.btnFiltroCalzado);
        btnFiltroFavoritos = findViewById(R.id.btnFiltroFavoritos);
        fabAgregarPrenda = findViewById(R.id.fabAgregarPrenda);

        // Ficha Confirmar Prenda
        btnVolverDeConfirmar = findViewById(R.id.btnVolverDeConfirmar);
        tvTituloConfirmar = findViewById(R.id.tvTituloConfirmar);
        imgFotoFichaReal = findViewById(R.id.imgFotoFichaReal);
        tvFotoFichaTexto = findViewById(R.id.tvFotoFichaTexto);
        tvColorBadgeFicha = findViewById(R.id.tvColorBadgeFicha);
        etNombreFicha = findViewById(R.id.etNombreFicha);
        pillCategoria = findViewById(R.id.pillCategoria);
        pillMaterial = findViewById(R.id.pillMaterial);
        pillOcasión = findViewById(R.id.pillOcasión);
        spinnerCategoriaFicha = findViewById(R.id.spinnerCategoriaFicha);
        spinnerEstiloFicha = findViewById(R.id.spinnerEstiloFicha);
        spinnerMaterialFicha = findViewById(R.id.spinnerMaterialFicha);
        btnGuardarFicha = findViewById(R.id.btnGuardarFicha);
        btnProbarVestierDesdeFicha = findViewById(R.id.btnProbarVestierDesdeFicha);
        btnQuitarFondoPrenda = findViewById(R.id.btnQuitarFondoPrenda);
        btnEliminarFicha = findViewById(R.id.btnEliminarFicha);

        // Escáner CameraX
        previewViewEscaner = findViewById(R.id.previewViewEscaner);
        btnFlashEscaner = findViewById(R.id.btnFlashEscaner);
        btnGaleriaEscaner = findViewById(R.id.btnGaleriaEscaner);
        btnDisparadorEscaner = findViewById(R.id.btnDisparadorEscaner);
        btnGirarCamaraEscaner = findViewById(R.id.btnGirarCamaraEscaner);

        // Vestier Digital (Opción 2)
        tvVestierAfinidad = findViewById(R.id.tvVestierAfinidad);
        btnVestierLimpiar = findViewById(R.id.btnVestierLimpiar);
        btnVestierHistorial = findViewById(R.id.btnVestierHistorial);
        imgFotoUsuarioVestier = findViewById(R.id.imgFotoUsuarioVestier);
        tvBadgeUsuario = findViewById(R.id.tvBadgeUsuario);
        btnCambiarFotoUsuario = findViewById(R.id.btnCambiarFotoUsuario);
        btnQuitarFotoUsuario = findViewById(R.id.btnQuitarFotoUsuario);
        frameCapaSuperior = findViewById(R.id.frameCapaSuperior);
        frameCapaInferior = findViewById(R.id.frameCapaInferior);
        frameCapaCalzado = findViewById(R.id.frameCapaCalzado);
        imgVestierSuperior = findViewById(R.id.imgVestierSuperior);
        imgVestierInferior = findViewById(R.id.imgVestierInferior);
        imgVestierCalzado = findViewById(R.id.imgVestierCalzado);
        tvVestierPlaceholderSuperior = findViewById(R.id.tvVestierPlaceholderSuperior);
        tvVestierPlaceholderInferior = findViewById(R.id.tvVestierPlaceholderInferior);
        tvVestierPlaceholderCalzado = findViewById(R.id.tvVestierPlaceholderCalzado);
        btnQuitarSuperior = findViewById(R.id.btnQuitarSuperior);
        btnQuitarInferior = findViewById(R.id.btnQuitarInferior);
        btnQuitarCalzado = findViewById(R.id.btnQuitarCalzado);
        btnTabVestierSuperior = findViewById(R.id.btnTabVestierSuperior);
        btnTabVestierInferior = findViewById(R.id.btnTabVestierInferior);
        btnTabVestierCalzado = findViewById(R.id.btnTabVestierCalzado);
        btnTabVestierAbrigo = findViewById(R.id.btnTabVestierAbrigo);
        llContenedorPrendasVestier = findViewById(R.id.llContenedorPrendasVestier);
        btnVestierAleatorio = findViewById(R.id.btnVestierAleatorio);
        btnProbarConIA = findViewById(R.id.btnProbarConIA);
        btnGuardarOutfit = findViewById(R.id.btnGuardarOutfit);

        // Modal Virtual Try-On
        modalVirtualTryOn = findViewById(R.id.modalVirtualTryOn);
        btnCerrarModalTryOn = findViewById(R.id.btnCerrarModalTryOn);
        imgResultadoTryOn = findViewById(R.id.imgResultadoTryOn);
        tvDetallePrendasTryOn = findViewById(R.id.tvDetallePrendasTryOn);
        tvScoreTryOn = findViewById(R.id.tvScoreTryOn);
        btnGuardarLookTryOn = findViewById(R.id.btnGuardarLookTryOn);
        btnVerHistorialTryOn = findViewById(R.id.btnVerHistorialTryOn);
        btnCompartirLookTryOn = findViewById(R.id.btnCompartirLookTryOn);
        btnExportarGaleriaTryOn = findViewById(R.id.btnExportarGaleriaTryOn);
        llProgresoIA = findViewById(R.id.llProgresoIA);
        llResultadoIA = findViewById(R.id.llResultadoIA);
        progressBarIA = findViewById(R.id.progressBarIA);
        tvProgresoTexto = findViewById(R.id.tvProgresoTexto);
        tvPorcentajeIA = findViewById(R.id.tvPorcentajeIA);
        btnCompararAntesDespues = findViewById(R.id.btnCompararAntesDespues);
        tvBadgeEstadoComparacion = findViewById(R.id.tvBadgeEstadoComparacion);
        configurarVisorTryOnInteractivo();

        // Modal Ajustar Foto Usuario
        modalAjustarFotoUsuario = findViewById(R.id.modalAjustarFotoUsuario);
        btnCerrarModalAjustarFoto = findViewById(R.id.btnCerrarModalAjustarFoto);
        imgAjustarFotoPreview = findViewById(R.id.imgAjustarFotoPreview);
        tvZoomNivel = findViewById(R.id.tvZoomNivel);
        btnZoomMenos = findViewById(R.id.btnZoomMenos);
        btnZoomMas = findViewById(R.id.btnZoomMas);
        btnMoverArriba = findViewById(R.id.btnMoverArriba);
        btnMoverAbajo = findViewById(R.id.btnMoverAbajo);
        btnCentrarReset = findViewById(R.id.btnCentrarReset);
        btnQuitarFondoFoto = findViewById(R.id.btnQuitarFondoFoto);
        btnConfirmarFotoAjustada = findViewById(R.id.btnConfirmarFotoAjustada);

        // Modal Historial Looks
        modalHistorialLooks = findViewById(R.id.modalHistorialLooks);
        btnCerrarModalHistorial = findViewById(R.id.btnCerrarModalHistorial);
        llListaHistorialLooks = findViewById(R.id.llListaHistorialLooks);

        // Asesor & Match
        btnRefrescarAsesor = findViewById(R.id.btnRefrescarAsesor);
        tvBadgePorcentajeAsesor = findViewById(R.id.tvBadgePorcentajeAsesor);
        tvSubtituloClimaAsesor = findViewById(R.id.tvSubtituloClimaAsesor);
        imgPreviewOutfitSugerido = findViewById(R.id.imgPreviewOutfitSugerido);
        tvPlaceholderOutfitSugerido = findViewById(R.id.tvPlaceholderOutfitSugerido);
        tvSugeridoSupNombre = findViewById(R.id.tvSugeridoSupNombre);
        tvSugeridoInfNombre = findViewById(R.id.tvSugeridoInfNombre);
        tvSugeridoCalNombre = findViewById(R.id.tvSugeridoCalNombre);
        btnProbarLookEnVestier = findViewById(R.id.btnProbarLookEnVestier);
        llContenedorOfertasTiendas = findViewById(R.id.llContenedorOfertasTiendas);

        // INICIO outfits guardados
        btnVerOutfitsVestier = findViewById(R.id.btnVerOutfitsVestier);
        tvOutfitsVacioInicio = findViewById(R.id.tvOutfitsVacioInicio);
        hsvOutfitsGuardados = findViewById(R.id.hsvOutfitsGuardados);
        llContenedorOutfitsInicio = findViewById(R.id.llContenedorOutfitsInicio);

        btnConfiguracion = findViewById(R.id.btnConfiguracion);
    }

    private void configurarTopBar() {
        btnConfiguracion.setOnClickListener(v -> mostrarDialogoAjustes());
    }

    /** Menú de Ajustes Generales: Hub modular con secciones de IA, Logs, Cuenta y Datos. */
    private void mostrarDialogoAjustes() {
        View hubView = getLayoutInflater().inflate(R.layout.dialog_ajustes_hub, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(hubView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView btnCerrarHub = hubView.findViewById(R.id.btnCerrarHub);
        View btnMenuApiKey = hubView.findViewById(R.id.btnMenuApiKey);
        View btnMenuMotor = hubView.findViewById(R.id.btnMenuMotor);
        View btnMenuLogs = hubView.findViewById(R.id.btnMenuLogs);
        View btnMenuCuenta = hubView.findViewById(R.id.btnMenuCuenta);
        View btnMenuDatosArmario = hubView.findViewById(R.id.btnMenuDatosArmario);
        TextView tvEstadoApiKeySubtext = hubView.findViewById(R.id.tvEstadoApiKeySubtext);
        TextView tvEstadoMotorSubtext = hubView.findViewById(R.id.tvEstadoMotorSubtext);

        // Estado actual de la API Key Gemini (Asesor de Estilo)
        String key = prefs.getString(PREF_GEMINI_API_KEY, "");
        if (key.isEmpty()) {
            tvEstadoApiKeySubtext.setText("Sin configurar (toca para ingresar clave)");
            tvEstadoApiKeySubtext.setTextColor(Color.parseColor("#FF9999"));
        } else {
            tvEstadoApiKeySubtext.setText("● Clave Gemini guardada y lista");
            tvEstadoApiKeySubtext.setTextColor(Color.parseColor("#80FF80"));
        }

        // Estado actual del Motor de Probador Virtual (Try-On)
        String motorActual = prefs.getString(PREF_MOTOR_TRYON, MOTOR_LOCAL);
        if (MOTOR_REPLICATE.equals(motorActual)) {
            String keyRep = prefs.getString(PREF_REPLICATE_API_KEY, "");
            if (keyRep.isEmpty()) {
                tvEstadoMotorSubtext.setText("☁️ Replicate IDM-VTON • Sin API Key");
                tvEstadoMotorSubtext.setTextColor(Color.parseColor("#FF9999"));
            } else {
                tvEstadoMotorSubtext.setText("☁️ Replicate IDM-VTON ($0.024/img)");
                tvEstadoMotorSubtext.setTextColor(Color.parseColor("#80FF80"));
            }
        } else if (MOTOR_GEMINI.equals(motorActual)) {
            String keyGem = prefs.getString(PREF_GEMINI_API_KEY, "");
            if (keyGem.isEmpty()) {
                tvEstadoMotorSubtext.setText("✨ Gemini Imagen • Sin API Key");
                tvEstadoMotorSubtext.setTextColor(Color.parseColor("#FF9999"));
            } else {
                tvEstadoMotorSubtext.setText("✨ Gemini Imagen ($0.04/img)");
                tvEstadoMotorSubtext.setTextColor(Color.parseColor("#80FF80"));
            }
        } else {
            tvEstadoMotorSubtext.setText("🖥️ Servidor Local • Sin costo");
            tvEstadoMotorSubtext.setTextColor(Color.parseColor("#80FF80"));
        }

        btnCerrarHub.setOnClickListener(v -> dialog.dismiss());

        btnMenuApiKey.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoConfigurarApiKey();
        });

        if (btnMenuMotor != null) {
            btnMenuMotor.setOnClickListener(v -> {
                dialog.dismiss();
                mostrarDialogoConfigurarMotor();
            });
        }

        btnMenuLogs.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoConsolaLogs();
        });

        btnMenuCuenta.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("👤 Perfil & Cuenta")
                    .setMessage("Closet/Scan opera bajo arquitectura Local & Offline-First.\n\n" +
                            "• Tus fotos y prendas se almacenan 100% en la memoria interna de tu dispositivo.\n" +
                            "• Las llamadas de análisis de estilo van directo a Google AI Studio con tu propia clave, sin intermediarios ni recopilación externa de datos.\n" +
                            "• No requiere registro en servidores de terceros.")
                    .setPositiveButton("Entendido", null)
                    .show();
        });

        btnMenuDatosArmario.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("👕 Gestión de Armario")
                    .setMessage("¿Deseas restablecer las prendas de demostración en tu armario?")
                    .setPositiveButton("Restablecer Prendas Demo", (d, w) -> {
                        viewModel.sembrarPrendasSiVacio();
                        Toast.makeText(this, "Prendas de demostración verificadas", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        dialog.show();
    }

    /** Modal dedicado exclusivamente a la configuración y prueba de la API Key. */
    private void mostrarDialogoConfigurarApiKey() {
        View view = getLayoutInflater().inflate(R.layout.dialog_ajustes_apikey, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText etApiKey = view.findViewById(R.id.etApiKeyInput);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarApiKeyModal);
        MaterialButton btnProbar = view.findViewById(R.id.btnProbarApiKeyModal);
        TextView tvEstadoTest = view.findViewById(R.id.tvEstadoTestApiKey);
        TextView btnCerrar = view.findViewById(R.id.btnCerrarApiKeyDialog);

        String guardada = prefs.getString(PREF_GEMINI_API_KEY, "");
        etApiKey.setText(guardada);

        btnCerrar.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoAjustes();
        });

        btnGuardar.setOnClickListener(v -> {
            String nueva = etApiKey.getText().toString().trim();
            prefs.edit().putString(PREF_GEMINI_API_KEY, nueva).apply();
            AppLogger.i("AJUSTES", "Clave Gemini actualizada por el usuario.");
            Toast.makeText(this, "✅ Clave de API guardada", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            mostrarDialogoAjustes();
        });

        btnProbar.setOnClickListener(v -> {
            String clave = etApiKey.getText().toString().trim();
            if (clave.isEmpty()) {
                tvEstadoTest.setVisibility(View.VISIBLE);
                tvEstadoTest.setTextColor(Color.parseColor("#FF6B6B"));
                tvEstadoTest.setText("❌ Ingresa una clave primero");
                return;
            }
            tvEstadoTest.setVisibility(View.VISIBLE);
            tvEstadoTest.setTextColor(Color.parseColor("#FFFF80"));
            tvEstadoTest.setText("⏳ Conectando con Gemini 3.5 Flash-Lite...");

            geminiClient.probarConexion(clave, new GeminiApiClient.TestCallback() {
                @Override
                public void onSuccess(String mensaje) {
                    tvEstadoTest.setTextColor(Color.parseColor("#80FF80"));
                    tvEstadoTest.setText(mensaje);
                }

                @Override
                public void onError(String error) {
                    tvEstadoTest.setTextColor(Color.parseColor("#FF6B6B"));
                    tvEstadoTest.setText(error);
                }
            });
        });

        dialog.show();
    }

    /** Modal dedicado exclusivamente a la configuración de la clave de FASHN.ai (Try-On Generativo). */
    private void mostrarDialogoConfigurarFashn() {
        View view = getLayoutInflater().inflate(R.layout.dialog_ajustes_fashn, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText etApiKey = view.findViewById(R.id.etFashnApiKeyInput);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarFashnKey);
        TextView btnCerrar = view.findViewById(R.id.btnCerrarFashnDialog);

        String guardada = prefs.getString(PREF_FASHN_API_KEY, "");
        etApiKey.setText(guardada);

        btnCerrar.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoAjustes();
        });

        btnGuardar.setOnClickListener(v -> {
            String nueva = etApiKey.getText().toString().trim();
            prefs.edit().putString(PREF_FASHN_API_KEY, nueva).apply();
            AppLogger.i("AJUSTES", "Clave FASHN actualizada por el usuario.");
            Toast.makeText(this, "✅ Clave de FASHN.ai guardada", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            mostrarDialogoAjustes();
        });

        dialog.show();
    }

    /** Modal dedicado a la selección y configuración del motor de Try-On (Local, Replicate, Gemini). */
    private void mostrarDialogoConfigurarMotor() {
        View view = getLayoutInflater().inflate(R.layout.dialog_ajustes_motor, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        RadioButton rbLocal = view.findViewById(R.id.rbMotorLocal);
        RadioButton rbReplicate = view.findViewById(R.id.rbMotorReplicate);
        RadioButton rbGemini = view.findViewById(R.id.rbMotorGemini);

        View cardLocal = view.findViewById(R.id.cardMotorLocal);
        View cardReplicate = view.findViewById(R.id.cardMotorReplicate);
        View cardGemini = view.findViewById(R.id.cardMotorGemini);

        View layoutUrlServidor = view.findViewById(R.id.layoutUrlServidor);
        View layoutApiKeyReplicate = view.findViewById(R.id.layoutApiKeyReplicate);
        View layoutApiKeyGemini = view.findViewById(R.id.layoutApiKeyGemini);

        EditText etUrlServidor = view.findViewById(R.id.etUrlServidor);
        EditText etApiKeyReplicate = view.findViewById(R.id.etApiKeyReplicate);
        EditText etApiKeyGemini = view.findViewById(R.id.etApiKeyGemini);
        EditText etPromptGemini = view.findViewById(R.id.etPromptGemini);

        TextView tvLinkReplicate = view.findViewById(R.id.tvLinkReplicate);
        TextView tvLinkGemini = view.findViewById(R.id.tvLinkGemini);
        android.widget.Button btnGuardar = view.findViewById(R.id.btnMotorGuardar);
        android.widget.Button btnCancelar = view.findViewById(R.id.btnMotorCancelar);

        final String[] motorSeleccionado = { prefs.getString(PREF_MOTOR_TRYON, MOTOR_LOCAL) };
        etUrlServidor.setText(prefs.getString("servidor_url", BRIDGE_URL));
        etApiKeyReplicate.setText(prefs.getString(PREF_REPLICATE_API_KEY, ""));
        etApiKeyGemini.setText(prefs.getString(PREF_GEMINI_API_KEY, ""));
        etPromptGemini.setText(prefs.getString(PREF_GEMINI_IMG_PROMPT, DEFAULT_GEMINI_PROMPT));

        Runnable actualizarUI = () -> {
            boolean isLocal = MOTOR_LOCAL.equals(motorSeleccionado[0]);
            boolean isReplicate = MOTOR_REPLICATE.equals(motorSeleccionado[0]);
            boolean isGemini = MOTOR_GEMINI.equals(motorSeleccionado[0]);

            rbLocal.setChecked(isLocal);
            rbReplicate.setChecked(isReplicate);
            rbGemini.setChecked(isGemini);

            layoutUrlServidor.setVisibility(isLocal ? View.VISIBLE : View.GONE);
            layoutApiKeyReplicate.setVisibility(isReplicate ? View.VISIBLE : View.GONE);
            layoutApiKeyGemini.setVisibility(isGemini ? View.VISIBLE : View.GONE);
        };

        actualizarUI.run();

        View.OnClickListener clickLocal = v -> { motorSeleccionado[0] = MOTOR_LOCAL; actualizarUI.run(); };
        View.OnClickListener clickReplicate = v -> { motorSeleccionado[0] = MOTOR_REPLICATE; actualizarUI.run(); };
        View.OnClickListener clickGemini = v -> { motorSeleccionado[0] = MOTOR_GEMINI; actualizarUI.run(); };

        rbLocal.setOnClickListener(clickLocal);
        cardLocal.setOnClickListener(clickLocal);
        rbReplicate.setOnClickListener(clickReplicate);
        cardReplicate.setOnClickListener(clickReplicate);
        rbGemini.setOnClickListener(clickGemini);
        cardGemini.setOnClickListener(clickGemini);

        tvLinkReplicate.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://replicate.com")));
            } catch (Exception ignored) {}
        });

        tvLinkGemini.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")));
            } catch (Exception ignored) {}
        });

        btnCancelar.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoAjustes();
        });

        btnGuardar.setOnClickListener(v -> {
            String nuevoMotor = motorSeleccionado[0];
            String url = etUrlServidor.getText().toString().trim();
            String keyRep = etApiKeyReplicate.getText().toString().trim();
            String keyGem = etApiKeyGemini.getText().toString().trim();
            String promptGem = etPromptGemini.getText().toString().trim();

            if (url.isEmpty()) url = BRIDGE_URL;
            if (promptGem.isEmpty()) promptGem = DEFAULT_GEMINI_PROMPT;

            prefs.edit()
                    .putString(PREF_MOTOR_TRYON, nuevoMotor)
                    .putString("servidor_url", url)
                    .putString(PREF_REPLICATE_API_KEY, keyRep)
                    .putString(PREF_GEMINI_API_KEY, keyGem)
                    .putString(PREF_GEMINI_IMG_PROMPT, promptGem)
                    .apply();

            AppLogger.i("AJUSTES", "Motor Try-On guardado: " + nuevoMotor);
            Toast.makeText(this, "✅ Motor de Try-On guardado: " + nuevoMotor, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            mostrarDialogoAjustes();
        });

        dialog.show();
    }

    /** Modal dedicado a la consola de terminal y logs de IA en vivo. */
    private void mostrarDialogoConsolaLogs() {
        View view = getLayoutInflater().inflate(R.layout.dialog_ajustes_logs, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView btnCerrar = view.findViewById(R.id.btnCerrarLogsDialog);
        TextView btnCopiar = view.findViewById(R.id.btnCopiarLogsModal);
        TextView btnLimpiar = view.findViewById(R.id.btnLimpiarLogsModal);
        TextView tvLogs = view.findViewById(R.id.tvTerminalLogsModal);
        ScrollView scroll = view.findViewById(R.id.scrollLogsModal);

        tvLogs.setText(AppLogger.getLogsFormatted());
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        AppLogger.LogListener logListener = entrada -> runOnUiThread(() -> {
            tvLogs.setText(AppLogger.getLogsFormatted());
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        });
        AppLogger.addListener(logListener);

        dialog.setOnDismissListener(d -> AppLogger.removeListener(logListener));

        btnCerrar.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoAjustes();
        });

        btnCopiar.setOnClickListener(v -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData cd = android.content.ClipData.newPlainText("ClosetScan Logs", AppLogger.getLogsFormatted());
            if (cm != null) {
                cm.setPrimaryClip(cd);
                Toast.makeText(this, "📋 Registros copiados al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });

        btnLimpiar.setOnClickListener(v -> {
            AppLogger.clear();
            tvLogs.setText(AppLogger.getLogsFormatted());
        });

        dialog.show();
    }

    private void configurarDashboardInicio() {
        if (btnVerTodoArmario != null) {
            btnVerTodoArmario.setOnClickListener(v -> cambiarTab("ARMARIO"));
        }
        if (btnVerOutfitsVestier != null) {
            btnVerOutfitsVestier.setOnClickListener(v -> cambiarTab("VESTIER"));
        }
    }

    private void configurarNavegacionInferior() {
        tabInicio.setOnClickListener(v -> cambiarTab("INICIO"));
        tabArmario.setOnClickListener(v -> cambiarTab("ARMARIO"));
        tabEscaner.setOnClickListener(v -> cambiarTab("ESCANER"));
        tabVestier.setOnClickListener(v -> cambiarTab("VESTIER"));
        tabAsesor.setOnClickListener(v -> cambiarTab("ASESOR"));
    }

    private void cambiarTab(String tab) {
        tabActual = tab;
        viewInicio.setVisibility("INICIO".equals(tab) ? View.VISIBLE : View.GONE);
        viewArmario.setVisibility("ARMARIO".equals(tab) ? View.VISIBLE : View.GONE);
        viewConfirmarPrenda.setVisibility(View.GONE);
        viewEscaner.setVisibility("ESCANER".equals(tab) ? View.VISIBLE : View.GONE);
        viewVestier.setVisibility("VESTIER".equals(tab) ? View.VISIBLE : View.GONE);
        viewAsesor.setVisibility("ASESOR".equals(tab) ? View.VISIBLE : View.GONE);

        boolean esEscaner = "ESCANER".equals(tab);
        topBarGlobal.setVisibility(esEscaner ? View.GONE : View.VISIBLE);
        dividerTop.setVisibility(esEscaner ? View.GONE : View.VISIBLE);

        actualizarEstiloTab(tabInicio, "INICIO".equals(tab));
        actualizarEstiloTab(tabArmario, "ARMARIO".equals(tab));
        actualizarEstiloTab(tabEscaner, "ESCANER".equals(tab));
        actualizarEstiloTab(tabVestier, "VESTIER".equals(tab));
        actualizarEstiloTab(tabAsesor, "ASESOR".equals(tab));

        if (esEscaner) {
            verificarPermisoEIniciarCamara();
        } else if (camaraManager != null && camaraManager.isTorchOn()) {
            camaraManager.setFlash(false);
        }

        if ("VESTIER".equals(tab)) {
            cargarFotoUsuario();
            actualizarCarruselVestier();
            actualizarLienzoVestier();
        } else if ("ASESOR".equals(tab)) {
            actualizarAsesorUI();
        }
    }

    private void actualizarEstiloTab(TextView tabView, boolean activo) {
        if (tabView == null) return;
        if (activo) {
            tabView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            tabView.setTextColor(Color.parseColor("#000000"));
            tabView.setTypeface(null, Typeface.BOLD);
        } else {
            tabView.setBackgroundColor(Color.parseColor("#0D0D10"));
            tabView.setTextColor(Color.parseColor("#888899"));
            tabView.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void configurarArmario() {
        RecyclerView recycler = findViewById(R.id.recyclerPrendas);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new PrendaAdapter();
        recycler.setAdapter(adapter);

        adapter.setOnPrendaClickListener((prenda, posicion) -> abrirFichaPrenda(prenda, prenda.rutaFotoOriginal));

        adapter.setOnFavoritoClickListener((prenda, posicion) -> {
            prenda.esFavorita = !prenda.esFavorita;
            viewModel.actualizar(prenda);
            adapter.notifyItemChanged(posicion);
            Toast.makeText(this, prenda.esFavorita ? "❤️ Agregada a favoritos" : "Quitada de favoritos", Toast.LENGTH_SHORT).show();
            if ("FAVORITOS".equalsIgnoreCase(filtroActual)) {
                aplicarFiltrosYBusqueda();
            }
        });

        etBuscarPrenda.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusquedaActual = s.toString().trim();
                aplicarFiltrosYBusqueda();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFiltroTodos.setOnClickListener(v -> seleccionarFiltro("TODOS"));
        btnFiltroSuperior.setOnClickListener(v -> seleccionarFiltro("SUPERIOR"));
        btnFiltroInferior.setOnClickListener(v -> seleccionarFiltro("INFERIOR"));
        btnFiltroCalzado.setOnClickListener(v -> seleccionarFiltro("CALZADO"));
        if (btnFiltroFavoritos != null) {
            btnFiltroFavoritos.setOnClickListener(v -> seleccionarFiltro("FAVORITOS"));
        }

        fabAgregarPrenda.setOnClickListener(v -> cambiarTab("ESCANER"));
    }

    private void seleccionarFiltro(String categoria) {
        filtroActual = categoria;
        resaltarFiltro(btnFiltroTodos, "TODOS".equals(categoria));
        resaltarFiltro(btnFiltroSuperior, "SUPERIOR".equals(categoria));
        resaltarFiltro(btnFiltroInferior, "INFERIOR".equals(categoria));
        resaltarFiltro(btnFiltroCalzado, "CALZADO".equals(categoria));
        if (btnFiltroFavoritos != null) {
            resaltarFiltro(btnFiltroFavoritos, "FAVORITOS".equals(categoria));
        }
        aplicarFiltrosYBusqueda();
    }

    private void resaltarFiltro(TextView btn, boolean activo) {
        if (activo) {
            btn.setBackgroundColor(Color.parseColor("#FFFFFF"));
            btn.setTextColor(Color.parseColor("#000000"));
        } else {
            btn.setBackgroundColor(Color.parseColor("#1B1B22"));
            btn.setTextColor(Color.parseColor("#CCCCCC"));
        }
    }

    private void aplicarFiltrosYBusqueda() {
        List<PrendaEntity> filtradas = new ArrayList<>();
        for (PrendaEntity p : todasLasPrendas) {
            boolean coincideCategoria = "TODOS".equalsIgnoreCase(filtroActual) ||
                    ("FAVORITOS".equalsIgnoreCase(filtroActual) && p.esFavorita) ||
                    (p.categoria != null && p.categoria.equalsIgnoreCase(filtroActual));

            boolean coincideBusqueda = textoBusquedaActual.isEmpty() ||
                    (p.nombre != null && p.nombre.toLowerCase().contains(textoBusquedaActual.toLowerCase())) ||
                    (p.categoria != null && p.categoria.toLowerCase().contains(textoBusquedaActual.toLowerCase())) ||
                    (p.estilo != null && p.estilo.toLowerCase().contains(textoBusquedaActual.toLowerCase())) ||
                    (p.colorNombre != null && p.colorNombre.toLowerCase().contains(textoBusquedaActual.toLowerCase()));

            if (coincideCategoria && coincideBusqueda) {
                filtradas.add(p);
            }
        }
        adapter.setLista(filtradas);
        tvArmarioSubtitulo.setText("Mostrando " + filtradas.size() + " prendas • Toca una para abrir su ficha");
    }

    // ─── CONFIGURACIÓN DEL ESCÁNER (CAMERAX) ──────────────────────────────────
    private void configurarEscaner() {
        btnFlashEscaner.setOnClickListener(v -> {
            if (camaraManager != null) {
                boolean encendido = camaraManager.toggleFlash();
                btnFlashEscaner.setTextColor(encendido ? Color.parseColor("#FFFF00") : Color.parseColor("#FFFFFF"));
            }
        });

        btnGirarCamaraEscaner.setOnClickListener(v -> {
            if (camaraManager != null) {
                camaraManager.toggleLens();
            }
        });

        btnGaleriaEscaner.setOnClickListener(v -> {
            selectorGaleriaLauncher.launch("image/*");
        });

        btnDisparadorEscaner.setOnClickListener(v -> {
            if (camaraManager != null && camaraManager.isReady()) {
                Toast.makeText(this, "📸 Capturando prenda...", Toast.LENGTH_SHORT).show();
                camaraManager.takePhoto(new CamaraManager.PhotoCallback() {
                    @Override
                    public void onPhotoSaved(@NonNull File photoFile) {
                        Toast.makeText(MainActivity.this, "✅ Prenda capturada", Toast.LENGTH_SHORT).show();
                        abrirFichaPrenda(null, photoFile.getAbsolutePath());
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        Toast.makeText(MainActivity.this, "Error al capturar: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Cámara preparándose...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verificarPermisoEIniciarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciarCamara() {
        if (camaraManager == null) {
            camaraManager = new CamaraManager(this, this, previewViewEscaner);
        }
        camaraManager.initialize();
    }

    // ─── FICHA "CONFIRMAR PRENDA" ─────────────────────────────────────────────
    private void configurarFichaConfirmarPrenda() {
        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categoriasOpciones);
        spinnerCategoriaFicha.setAdapter(adapterCat);

        ArrayAdapter<String> adapterEst = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, estilosOpciones);
        spinnerEstiloFicha.setAdapter(adapterEst);

        ArrayAdapter<String> adapterMat = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, materialesOpciones);
        spinnerMaterialFicha.setAdapter(adapterMat);

        spinnerCategoriaFicha.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pillCategoria.setText(categoriasOpciones[position].toUpperCase());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerEstiloFicha.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pillOcasión.setText(estilosOpciones[position].toUpperCase());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMaterialFicha.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pillMaterial.setText(materialesOpciones[position].toUpperCase());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnVolverDeConfirmar.setOnClickListener(v -> cerrarFicha());

        if (btnQuitarFondoPrenda != null) {
            btnQuitarFondoPrenda.setOnClickListener(v -> ejecutarQuitarFondoPrenda());
        }

        btnGuardarFicha.setOnClickListener(v -> {
            String nuevoNombre = etNombreFicha.getText().toString().trim();
            if (nuevoNombre.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre para la prenda", Toast.LENGTH_SHORT).show();
                return;
            }

            String nuevaCat = spinnerCategoriaFicha.getSelectedItem().toString();
            String nuevoEst = spinnerEstiloFicha.getSelectedItem().toString();
            String nuevoMat = spinnerMaterialFicha.getSelectedItem().toString();

            if (prendaEnEdicion == null) {
                PrendaEntity nueva = new PrendaEntity(nuevoNombre, nuevaCat, nuevoEst,
                        colorDetectadoHex, colorDetectadoNombre, rutaFotoTemporal, null);
                nueva.material = nuevoMat;
                viewModel.insertar(nueva);
                Toast.makeText(this, "✅ Prenda guardada en tu armario", Toast.LENGTH_SHORT).show();
            } else {
                prendaEnEdicion.nombre = nuevoNombre;
                prendaEnEdicion.categoria = nuevaCat;
                prendaEnEdicion.estilo = nuevoEst;
                prendaEnEdicion.material = nuevoMat;
                prendaEnEdicion.colorHex = colorDetectadoHex;
                prendaEnEdicion.colorNombre = colorDetectadoNombre;
                if (rutaFotoTemporal != null && !rutaFotoTemporal.isEmpty()) {
                    prendaEnEdicion.rutaFotoOriginal = rutaFotoTemporal;
                }
                viewModel.actualizar(prendaEnEdicion);
                Toast.makeText(this, "✅ Prenda actualizada en el armario", Toast.LENGTH_SHORT).show();
            }

            rutaFotoTemporal = null;
            cerrarFicha();
            cambiarTab("ARMARIO");
        });

        btnEliminarFicha.setOnClickListener(v -> {
            if (prendaEnEdicion != null) {
                viewModel.eliminar(prendaEnEdicion);
                Toast.makeText(this, "🗑️ Prenda eliminada del armario", Toast.LENGTH_SHORT).show();
            }
            rutaFotoTemporal = null;
            cerrarFicha();
            cambiarTab("ARMARIO");
        });

        btnProbarVestierDesdeFicha.setOnClickListener(v -> {
            if (prendaEnEdicion != null) {
                asignarPrendaAVestier(prendaEnEdicion);
            }
            rutaFotoTemporal = null;
            cerrarFicha();
            cambiarTab("VESTIER");
            Toast.makeText(this, "🎭 Llevada al Vestier para probar", Toast.LENGTH_SHORT).show();
        });
    }

    private void abrirFichaPrenda(PrendaEntity prenda, String rutaFoto) {
        prendaEnEdicion = prenda;
        this.rutaFotoTemporal = (prenda != null) ? prenda.rutaFotoOriginal : rutaFoto;

        viewArmario.setVisibility(View.GONE);
        viewEscaner.setVisibility(View.GONE);
        viewInicio.setVisibility(View.GONE);
        viewVestier.setVisibility(View.GONE);
        viewConfirmarPrenda.setVisibility(View.VISIBLE);

        topBarGlobal.setVisibility(View.GONE);
        dividerTop.setVisibility(View.GONE);

        String fotoAMostrar = this.rutaFotoTemporal;
        boolean fotoCargada = false;
        if (fotoAMostrar != null && !fotoAMostrar.isEmpty()) {
            try {
                File f = new File(fotoAMostrar);
                if (f.exists()) {
                    imgFotoFichaReal.setImageURI(Uri.fromFile(f));
                    fotoCargada = true;
                } else if (fotoAMostrar.startsWith("content://") || fotoAMostrar.startsWith("file://")) {
                    imgFotoFichaReal.setImageURI(Uri.parse(fotoAMostrar));
                    fotoCargada = true;
                }
            } catch (Exception ignored) {}
        }

        if (fotoCargada) {
            imgFotoFichaReal.setVisibility(View.VISIBLE);
            tvFotoFichaTexto.setVisibility(View.GONE);
        } else {
            imgFotoFichaReal.setImageDrawable(null);
            imgFotoFichaReal.setVisibility(View.GONE);
            tvFotoFichaTexto.setVisibility(View.VISIBLE);
        }

        if (prenda == null) {
            tvTituloConfirmar.setText("CONFIRMAR PRENDA");
            etNombreFicha.setText(rutaFoto != null ? "Prenda Escaneada" : "");
            etNombreFicha.setHint("Ej: Camiseta Oversize Negra");
            tvFotoFichaTexto.setText(rutaFoto != null ? "📸 [ FOTO CAPTURADA LISTA ]" : "[ NUEVA PRENDA ]");

            if (this.rutaFotoTemporal != null && !this.rutaFotoTemporal.isEmpty()) {
                ColorInfo ci = detectarColorDominante(this.rutaFotoTemporal);
                colorDetectadoHex = ci.hex;
                colorDetectadoNombre = ci.nombre;
            } else {
                colorDetectadoHex = "#FFFFFF";
                colorDetectadoNombre = "Blanco";
            }
            tvColorBadgeFicha.setText("Color: " + colorDetectadoNombre);
            pillCategoria.setText("SUPERIOR");
            pillMaterial.setText("ALGODÓN");
            pillOcasión.setText("CASUAL");

            seleccionarEnSpinner(spinnerCategoriaFicha, "Superior", categoriasOpciones);
            seleccionarEnSpinner(spinnerEstiloFicha, "Casual", estilosOpciones);
            seleccionarEnSpinner(spinnerMaterialFicha, "Algodón", materialesOpciones);

            btnGuardarFicha.setText("GUARDAR EN MI ARMARIO");
            btnEliminarFicha.setVisibility(View.GONE);
            btnProbarVestierDesdeFicha.setVisibility(View.GONE);
            if (btnQuitarFondoPrenda != null) btnQuitarFondoPrenda.setVisibility(View.VISIBLE);
        } else {
            tvTituloConfirmar.setText("FICHA DE PRENDA");
            etNombreFicha.setText(prenda.nombre);
            tvFotoFichaTexto.setText("[ " + (prenda.nombre != null ? prenda.nombre.toUpperCase() : "PRENDA") + " ]");
            colorDetectadoHex = prenda.colorHex != null ? prenda.colorHex : "#FFFFFF";
            colorDetectadoNombre = prenda.colorNombre != null ? prenda.colorNombre : "Estándar";
            tvColorBadgeFicha.setText("Color: " + colorDetectadoNombre);
            pillCategoria.setText(prenda.categoria != null ? prenda.categoria.toUpperCase() : "GENERAL");
            pillMaterial.setText(prenda.material != null ? prenda.material.toUpperCase() : "ALGODÓN");
            pillOcasión.setText(prenda.estilo != null ? prenda.estilo.toUpperCase() : "CASUAL");

            seleccionarEnSpinner(spinnerCategoriaFicha, prenda.categoria, categoriasOpciones);
            seleccionarEnSpinner(spinnerEstiloFicha, prenda.estilo, estilosOpciones);
            seleccionarEnSpinner(spinnerMaterialFicha, prenda.material != null ? prenda.material : "Algodón", materialesOpciones);

            btnGuardarFicha.setText("GUARDAR CAMBIOS");
            btnEliminarFicha.setVisibility(View.VISIBLE);
            btnProbarVestierDesdeFicha.setVisibility(View.VISIBLE);
            if (btnQuitarFondoPrenda != null) btnQuitarFondoPrenda.setVisibility(View.VISIBLE);
        }
    }

    private void abrirFichaPrendaDesdeUri(Uri uri) {
        abrirFichaPrenda(null, uri.toString());
        tvFotoFichaTexto.setText("🖼️ [ FOTO DE GALERÍA LISTA ]");
    }

    private void seleccionarEnSpinner(Spinner spinner, String valor, String[] lista) {
        if (valor == null || spinner == null) return;
        for (int i = 0; i < lista.length; i++) {
            if (lista[i].equalsIgnoreCase(valor)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void cerrarFicha() {
        viewConfirmarPrenda.setVisibility(View.GONE);
        if ("ESCANER".equals(tabActual)) {
            viewEscaner.setVisibility(View.VISIBLE);
        } else if ("INICIO".equals(tabActual)) {
            viewInicio.setVisibility(View.VISIBLE);
            topBarGlobal.setVisibility(View.VISIBLE);
            dividerTop.setVisibility(View.VISIBLE);
        } else if ("VESTIER".equals(tabActual)) {
            viewVestier.setVisibility(View.VISIBLE);
            topBarGlobal.setVisibility(View.VISIBLE);
            dividerTop.setVisibility(View.VISIBLE);
        } else {
            cambiarTab("ARMARIO");
        }
    }

    // ─── DASHBOARD ESTADÍSTICAS Y GRÁFICO DE DONA ─────────────────────────────
    private void actualizarEstadisticas() {
        int total = todasLasPrendas.size();
        Map<String, Integer> estilosMap = new LinkedHashMap<>();

        for (PrendaEntity p : todasLasPrendas) {
            String estilo = (p.estilo != null && !p.estilo.trim().isEmpty()) ? p.estilo : "Casual";
            estilosMap.put(estilo, estilosMap.getOrDefault(estilo, 0) + 1);
        }

        if (tvTotalPrendasStat != null) {
            tvTotalPrendasStat.setText(String.valueOf(total));
        }

        if (graficoDonaInicio != null) {
            graficoDonaInicio.setData(estilosMap);
        }

        actualizarLeyendaEstilos(estilosMap, total);
        actualizarEscaneosRecientes();
    }

    private void actualizarLeyendaEstilos(Map<String, Integer> estilosMap, int total) {
        if (tvLeyendaItem1 == null || tvLeyendaItem2 == null || tvLeyendaItem3 == null) return;

        List<Map.Entry<String, Integer>> lista = new ArrayList<>(estilosMap.entrySet());
        lista.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        if (total == 0 || lista.isEmpty()) {
            tvLeyendaItem1.setText("● Sin prendas");
            tvLeyendaItem1.setVisibility(View.VISIBLE);
            tvLeyendaItem2.setVisibility(View.GONE);
            tvLeyendaItem3.setVisibility(View.GONE);
            return;
        }

        if (lista.size() > 0) {
            Map.Entry<String, Integer> e1 = lista.get(0);
            int pct1 = Math.round((e1.getValue() * 100f) / total);
            tvLeyendaItem1.setText("● " + e1.getKey() + " " + pct1 + "%");
            tvLeyendaItem1.setTextColor(0xFFFFFFFF);
            tvLeyendaItem1.setVisibility(View.VISIBLE);
        }

        if (lista.size() > 1) {
            Map.Entry<String, Integer> e2 = lista.get(1);
            int pct2 = Math.round((e2.getValue() * 100f) / total);
            tvLeyendaItem2.setText("● " + e2.getKey() + " " + pct2 + "%");
            tvLeyendaItem2.setTextColor(0xFFCCCCCC);
            tvLeyendaItem2.setVisibility(View.VISIBLE);
        } else {
            tvLeyendaItem2.setVisibility(View.GONE);
        }

        if (lista.size() > 2) {
            Map.Entry<String, Integer> e3 = lista.get(2);
            int pct3 = Math.round((e3.getValue() * 100f) / total);
            tvLeyendaItem3.setText("● " + e3.getKey() + " " + pct3 + "%");
            tvLeyendaItem3.setTextColor(0xFF888888);
            tvLeyendaItem3.setVisibility(View.VISIBLE);
        } else {
            tvLeyendaItem3.setVisibility(View.GONE);
        }
    }

    private void actualizarEscaneosRecientes() {
        if (llContenedorRecientes == null) return;
        llContenedorRecientes.removeAllViews();

        if (todasLasPrendas.isEmpty()) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("Aún no tienes prendas registradas.\nToca '+' o Escáner para agregar.");
            tvVacio.setTextColor(0xFF666675);
            tvVacio.setTextSize(12);
            tvVacio.setPadding(8, 16, 8, 16);
            llContenedorRecientes.addView(tvVacio);
            return;
        }

        int limite = Math.min(todasLasPrendas.size(), 6);
        for (int i = todasLasPrendas.size() - 1; i >= todasLasPrendas.size() - limite; i--) {
            PrendaEntity prenda = todasLasPrendas.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) (130 * getResources().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
            card.setLayoutParams(lp);
            card.setBackgroundColor(0xFF14141B);
            card.setPadding(
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density)
            );

            FrameLayout frame = new FrameLayout(this);
            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    (int) (110 * getResources().getDisplayMetrics().density)
            );
            frame.setLayoutParams(flp);
            frame.setBackgroundColor(0xFF0D0D12);

            boolean fotoCargada = false;
            if (prenda.rutaFotoOriginal != null && !prenda.rutaFotoOriginal.isEmpty()) {
                try {
                    File f = new File(prenda.rutaFotoOriginal);
                    ImageView img = new ImageView(this);
                    img.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    if (f.exists()) {
                        img.setImageURI(Uri.fromFile(f));
                        frame.addView(img);
                        fotoCargada = true;
                    } else if (prenda.rutaFotoOriginal.startsWith("content://") || prenda.rutaFotoOriginal.startsWith("file://")) {
                        img.setImageURI(Uri.parse(prenda.rutaFotoOriginal));
                        frame.addView(img);
                        fotoCargada = true;
                    }
                } catch (Exception ignored) {}
            }

            if (!fotoCargada) {
                TextView tv = new TextView(this);
                tv.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                ));
                String txt = (prenda.nombre != null && !prenda.nombre.isEmpty())
                        ? prenda.nombre.substring(0, 1).toUpperCase()
                        : "✦";
                tv.setText(txt);
                tv.setTextColor(0xFF444455);
                tv.setTextSize(26);
                tv.setTypeface(null, Typeface.BOLD);
                frame.addView(tv);
            }

            card.addView(frame);

            TextView tvNombre = new TextView(this);
            tvNombre.setText(prenda.nombre != null ? prenda.nombre : "Sin nombre");
            tvNombre.setTextColor(0xFFFFFFFF);
            tvNombre.setTextSize(12);
            tvNombre.setMaxLines(1);
            tvNombre.setEllipsize(TextUtils.TruncateAt.END);
            tvNombre.setPadding(0, (int) (6 * getResources().getDisplayMetrics().density), 0, 0);
            card.addView(tvNombre);

            TextView tvSub = new TextView(this);
            tvSub.setText(prenda.categoria != null ? prenda.categoria.toUpperCase() : "GENERAL");
            tvSub.setTextColor(0xFF888899);
            tvSub.setTextSize(10);
            card.addView(tvSub);

            card.setOnClickListener(v -> abrirFichaPrenda(prenda, prenda.rutaFotoOriginal));

            llContenedorRecientes.addView(card);
        }
    }

    /**
     * Renderiza el carrusel de outfits guardados en INICIO.
     * Llamado reactivamente cuando cambia la lista de outfits en Room.
     */
    /**
     * Renderiza las tarjetas de outfits guardados en INICIO.
     * Muestra:
     *  - Portada con foto Try-On fotorrealista (o prenda principal) + badge de afinidad real
     *  - Trío de prendas (miniatura de Top + Bottom + Calzado)
     *  - Botón "PROBAR EN VESTIER" para cargarlo en 1 toque
     */
    private void actualizarOutfitsEnInicio(List<OutfitEntity> outfits) {
        if (llContenedorOutfitsInicio == null || hsvOutfitsGuardados == null) return;
        llContenedorOutfitsInicio.removeAllViews();

        if (outfits == null || outfits.isEmpty()) {
            hsvOutfitsGuardados.setVisibility(View.GONE);
            if (tvOutfitsVacioInicio != null) tvOutfitsVacioInicio.setVisibility(View.VISIBLE);
            return;
        }

        hsvOutfitsGuardados.setVisibility(View.VISIBLE);
        if (tvOutfitsVacioInicio != null) tvOutfitsVacioInicio.setVisibility(View.GONE);

        int dp6 = (int) (6 * getResources().getDisplayMetrics().density);
        int dp8 = (int) (8 * getResources().getDisplayMetrics().density);
        int dp10 = (int) (10 * getResources().getDisplayMetrics().density);
        int dp12 = (int) (12 * getResources().getDisplayMetrics().density);
        int cardWidth = (int) (175 * getResources().getDisplayMetrics().density);
        int coverHeight = (int) (95 * getResources().getDisplayMetrics().density);

        // Mostrar últimos 6 outfits, más reciente primero
        int limite = Math.min(outfits.size(), 6);
        for (int i = outfits.size() - 1; i >= outfits.size() - limite; i--) {
            OutfitEntity outfit = outfits.get(i);

            // Identificar prendas que componen el trío
            PrendaEntity sup = null;
            PrendaEntity inf = null;
            PrendaEntity cal = null;
            for (PrendaEntity p : todasLasPrendas) {
                if (p.id == outfit.idPrendaSuperior) sup = p;
                if (p.id == outfit.idPrendaInferior) inf = p;
                if (outfit.idCalzado > 0 && p.id == outfit.idCalzado) cal = p;
            }

            // Buscar si existe foto de Try-On generada para este look
            File fotoTryOn = null;
            File dirLooks = new File(getFilesDir(), "looks_historial");
            if (dirLooks.exists()) {
                File[] files = dirLooks.listFiles((d, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
                if (files != null && files.length > 0 && sup != null && sup.nombre != null) {
                    String clean = sup.nombre.replaceAll("[^a-zA-Z0-9]", "_");
                    for (File f : files) {
                        if (f.getName().contains(clean)) {
                            fotoTryOn = f;
                            break;
                        }
                    }
                }
            }

            // Tarjeta contenedora Noir
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpCard.setMargins(0, 0, dp12, 0);
            card.setLayoutParams(lpCard);
            card.setBackgroundColor(0xFF13131B);
            card.setPadding(dp8, dp8, dp8, dp8);

            // 1. Portada del Outfit (Foto Try-On o Prenda Superior) + Badge Afinidad
            FrameLayout framePortada = new FrameLayout(this);
            framePortada.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, coverHeight));
            framePortada.setBackgroundColor(0xFF0C0C12);

            ImageView imgPortada = new ImageView(this);
            imgPortada.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            imgPortada.setScaleType(ImageView.ScaleType.CENTER_CROP);

            boolean tieneFotoPortada = false;
            if (fotoTryOn != null && fotoTryOn.exists()) {
                imgPortada.setImageURI(Uri.fromFile(fotoTryOn));
                tieneFotoPortada = true;
            } else if (sup != null && sup.rutaFotoOriginal != null && !sup.rutaFotoOriginal.isEmpty()) {
                try {
                    File fSup = new File(sup.rutaFotoOriginal);
                    if (fSup.exists()) {
                        imgPortada.setImageURI(Uri.fromFile(fSup));
                        tieneFotoPortada = true;
                    }
                } catch (Exception ignored) {}
            }

            if (tieneFotoPortada) {
                framePortada.addView(imgPortada);
            } else {
                TextView tvCoverEmoji = new TextView(this);
                tvCoverEmoji.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
                tvCoverEmoji.setText("✨👔");
                tvCoverEmoji.setTextSize(24);
                framePortada.addView(tvCoverEmoji);
            }

            // Badge de Afinidad Cromática Real en la esquina
            TextView badgeAfinidad = new TextView(this);
            FrameLayout.LayoutParams lpBadge = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            lpBadge.setMargins(0, dp6, dp6, 0);
            badgeAfinidad.setLayoutParams(lpBadge);
            badgeAfinidad.setText(outfit.porcentajeAfinidad + "% MATCH");
            badgeAfinidad.setTextColor(0xFF000000);
            badgeAfinidad.setTextSize(8.5f);
            badgeAfinidad.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            badgeAfinidad.setBackgroundColor(0xFFFFFFFF);
            badgeAfinidad.setPadding(dp6, 2, dp6, 2);
            framePortada.addView(badgeAfinidad);

            if (fotoTryOn != null) {
                TextView badgeTryOn = new TextView(this);
                FrameLayout.LayoutParams lpBadgeTry = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
                lpBadgeTry.setMargins(dp6, 0, 0, dp6);
                badgeTryOn.setLayoutParams(lpBadgeTry);
                badgeTryOn.setText("✨ TRY-ON IA");
                badgeTryOn.setTextColor(0xFFFFFFFF);
                badgeTryOn.setTextSize(8f);
                badgeTryOn.setBackgroundColor(0xCC000000);
                badgeTryOn.setPadding(dp6, 2, dp6, 2);
                framePortada.addView(badgeTryOn);
            }

            card.addView(framePortada);

            // 2. Nombre y Ocasión
            TextView tvNombre = new TextView(this);
            String titulo = (outfit.ocasion != null && !outfit.ocasion.isEmpty())
                    ? outfit.ocasion : "Look #" + (outfits.size() - i);
            tvNombre.setText(titulo);
            tvNombre.setTextColor(0xFFFFFFFF);
            tvNombre.setTextSize(11.5f);
            tvNombre.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvNombre.setMaxLines(1);
            tvNombre.setEllipsize(TextUtils.TruncateAt.END);
            tvNombre.setPadding(0, dp6, 0, 2);
            card.addView(tvNombre);

            // 3. Trío de Prendas (Miniaturas lado a lado)
            LinearLayout llTrio = new LinearLayout(this);
            llTrio.setOrientation(LinearLayout.HORIZONTAL);
            llTrio.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            llTrio.setPadding(0, 2, 0, dp6);

            llTrio.addView(crearMiniaturaPrenda(sup, "👔", "Top"));
            llTrio.addView(crearMiniaturaPrenda(inf, "👖", "Bottom"));
            llTrio.addView(crearMiniaturaPrenda(cal, "👟", "Calzado"));
            card.addView(llTrio);

            // 4. Botón Directo "PROBAR EN VESTIER" (1 toque)
            com.google.android.material.button.MaterialButton btnProbar = new com.google.android.material.button.MaterialButton(this);
            LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (32 * getResources().getDisplayMetrics().density));
            btnProbar.setLayoutParams(lpBtn);
            btnProbar.setText("PONER EN VESTIER");
            btnProbar.setTextSize(9f);
            btnProbar.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            btnProbar.setTextColor(0xFFFFFFFF);
            btnProbar.setBackgroundColor(0xFF222230);
            btnProbar.setPadding(0, 0, 0, 0);
            btnProbar.setCornerRadius(4);
            btnProbar.setOnClickListener(v -> cargarOutfitEnVestier(outfit));
            card.addView(btnProbar);

            // Tocar toda la tarjeta también lo carga
            card.setOnClickListener(v -> cargarOutfitEnVestier(outfit));

            llContenedorOutfitsInicio.addView(card);
        }
    }

    private View crearMiniaturaPrenda(PrendaEntity prenda, String fallbackIcono, String labelTipo) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(2, 0, 2, 0);
        col.setLayoutParams(lp);

        FrameLayout frame = new FrameLayout(this);
        int dp36 = (int) (36 * getResources().getDisplayMetrics().density);
        frame.setLayoutParams(new LinearLayout.LayoutParams(dp36, dp36));
        frame.setBackgroundColor(0xFF1B1B25);

        if (prenda != null && prenda.rutaFotoOriginal != null && !prenda.rutaFotoOriginal.isEmpty()) {
            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cargarMiniaturaEnImageView(iv, prenda.rutaFotoOriginal);
            frame.addView(iv);
        } else {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            tv.setGravity(Gravity.CENTER);
            tv.setText(fallbackIcono);
            tv.setTextSize(14);
            frame.addView(tv);
        }
        col.addView(frame);

        TextView tvTipo = new TextView(this);
        tvTipo.setText(labelTipo);
        tvTipo.setTextColor(0xFF888899);
        tvTipo.setTextSize(8.5f);
        tvTipo.setGravity(Gravity.CENTER);
        tvTipo.setPadding(0, 1, 0, 0);
        col.addView(tvTipo);

        return col;
    }

    private void cargarOutfitEnVestier(OutfitEntity outfit) {
        if (outfit == null) return;
        prendaSuperiorSeleccionada = null;
        prendaInferiorSeleccionada = null;
        prendaCalzadoSeleccionada = null;

        for (PrendaEntity p : todasLasPrendas) {
            if (p.id == outfit.idPrendaSuperior) prendaSuperiorSeleccionada = p;
            if (p.id == outfit.idPrendaInferior) prendaInferiorSeleccionada = p;
            if (outfit.idCalzado > 0 && p.id == outfit.idCalzado) prendaCalzadoSeleccionada = p;
        }

        actualizarLienzoVestier();
        cambiarTab("VESTIER");
        String nombre = (outfit.ocasion != null && !outfit.ocasion.isEmpty()) ? outfit.ocasion : "Outfit";
        Toast.makeText(this, "👔 Cargado en Vestier: " + nombre, Toast.LENGTH_SHORT).show();
    }


    private void configurarVestier() {
        // Pestañas de categorías
        btnTabVestierSuperior.setOnClickListener(v -> cambiarCategoriaVestier("SUPERIOR"));
        btnTabVestierInferior.setOnClickListener(v -> cambiarCategoriaVestier("INFERIOR"));
        btnTabVestierCalzado.setOnClickListener(v -> cambiarCategoriaVestier("CALZADO"));

        // Foto del Usuario / Modelo
        btnCambiarFotoUsuario.setOnClickListener(v -> selectorFotoUsuarioLauncher.launch("image/*"));
        btnQuitarFotoUsuario.setOnClickListener(v -> resetearFotoUsuario());

        // Botones de desmonte individual en cada capa
        btnQuitarSuperior.setOnClickListener(v -> {
            prendaSuperiorSeleccionada = null;
            actualizarLienzoVestier();
        });
        btnQuitarInferior.setOnClickListener(v -> {
            prendaInferiorSeleccionada = null;
            actualizarLienzoVestier();
        });
        btnQuitarCalzado.setOnClickListener(v -> {
            prendaCalzadoSeleccionada = null;
            actualizarLienzoVestier();
        });

        // Toques en las capas activan la pestaña correspondiente
        frameCapaSuperior.setOnClickListener(v -> cambiarCategoriaVestier("SUPERIOR"));
        frameCapaInferior.setOnClickListener(v -> cambiarCategoriaVestier("INFERIOR"));
        frameCapaCalzado.setOnClickListener(v -> cambiarCategoriaVestier("CALZADO"));

        btnTabVestierSuperior.setOnClickListener(v -> cambiarCategoriaVestier("SUPERIOR"));
        btnTabVestierInferior.setOnClickListener(v -> cambiarCategoriaVestier("INFERIOR"));
        btnTabVestierCalzado.setOnClickListener(v -> cambiarCategoriaVestier("CALZADO"));
        btnTabVestierAbrigo.setOnClickListener(v -> cambiarCategoriaVestier("ABRIGO"));

        // Botones de acción
        btnVestierLimpiar.setOnClickListener(v -> {
            prendaSuperiorSeleccionada = null;
            prendaInferiorSeleccionada = null;
            prendaCalzadoSeleccionada = null;
            actualizarLienzoVestier();
            Toast.makeText(this, "Vestier reiniciado", Toast.LENGTH_SHORT).show();
        });

        btnVestierAleatorio.setOnClickListener(v -> generarOutfitAleatorio());
        btnGuardarOutfit.setOnClickListener(v -> guardarOutfitActual());
        btnProbarConIA.setOnClickListener(v -> ejecutarVirtualTryOnIA());

        cargarFotoUsuario();
        actualizarLienzoVestier();
        actualizarCarruselVestier();
    }

    private void guardarFotoUsuario(String uriString) {
        rutaFotoUsuarioPersonal = uriString;
        prefs.edit().putString(PREF_FOTO_USUARIO, uriString).apply();
        cargarFotoUsuario();
        AppLogger.foto("Foto de usuario guardada en Vestier: " + uriString);
        Toast.makeText(this, "👤 Foto de perfil actualizada en el Vestier", Toast.LENGTH_SHORT).show();
    }

    private void resetearFotoUsuario() {
        rutaFotoUsuarioPersonal = null;
        prefs.edit().remove(PREF_FOTO_USUARIO).apply();
        cargarFotoUsuario();
        AppLogger.foto("Foto de perfil reseteada a silueta.");
        Toast.makeText(this, "Silueta anatómica restaurada", Toast.LENGTH_SHORT).show();
    }

    private void cargarFotoUsuario() {
        if (rutaFotoUsuarioPersonal == null) {
            rutaFotoUsuarioPersonal = prefs.getString(PREF_FOTO_USUARIO, null);
        }

        if (rutaFotoUsuarioPersonal != null && !rutaFotoUsuarioPersonal.isEmpty()) {
            try {
                File f = new File(rutaFotoUsuarioPersonal);
                if (f.exists()) {
                    imgFotoUsuarioVestier.setImageURI(Uri.fromFile(f));
                    imgFotoUsuarioVestier.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    tvBadgeUsuario.setText("TU FOTO");
                    btnQuitarFotoUsuario.setVisibility(View.VISIBLE);
                    AppLogger.foto("Foto de perfil cargada correctamente.");
                    return;
                } else {
                    AppLogger.w("FOTO", "Archivo de foto de usuario no existe: " + rutaFotoUsuarioPersonal);
                }
            } catch (Exception e) {
                AppLogger.e("FOTO", "Error al cargar foto de usuario", e);
            }
        }

        // Si no hay foto personal cargada, usar silueta humana Noir
        imgFotoUsuarioVestier.setImageResource(R.drawable.ic_silueta_humana);
        imgFotoUsuarioVestier.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        tvBadgeUsuario.setText("TU SILUETA");
        btnQuitarFotoUsuario.setVisibility(View.GONE);
    }

    private void cambiarCategoriaVestier(String cat) {
        categoriaVestierActiva = cat;
        resaltarTabVestier(btnTabVestierSuperior, "SUPERIOR".equals(cat));
        resaltarTabVestier(btnTabVestierInferior, "INFERIOR".equals(cat));
        resaltarTabVestier(btnTabVestierCalzado, "CALZADO".equals(cat));
        resaltarTabVestier(btnTabVestierAbrigo, "ABRIGO".equals(cat));
        actualizarCarruselVestier();
    }

    private void resaltarTabVestier(TextView btn, boolean activo) {
        if (btn == null) return;
        if (activo) {
            btn.setBackgroundColor(Color.parseColor("#FFFFFF"));
            btn.setTextColor(Color.parseColor("#000000"));
            btn.setTypeface(null, Typeface.BOLD);
        } else {
            btn.setBackgroundColor(Color.parseColor("#1B1B22"));
            btn.setTextColor(Color.parseColor("#CCCCCC"));
            btn.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void actualizarLienzoVestier() {
        mostrarPrendaEnCapa(prendaSuperiorSeleccionada, imgVestierSuperior, tvVestierPlaceholderSuperior, btnQuitarSuperior, "[ + SUPERIOR ]");
        mostrarPrendaEnCapa(prendaInferiorSeleccionada, imgVestierInferior, tvVestierPlaceholderInferior, btnQuitarInferior, "[ + INFERIOR ]");
        mostrarPrendaEnCapa(prendaCalzadoSeleccionada, imgVestierCalzado, tvVestierPlaceholderCalzado, btnQuitarCalzado, "[ + CALZADO ]");

        calcularAfinidadOutfit();
    }

    private void mostrarPrendaEnCapa(PrendaEntity prenda, ImageView img, TextView tvPlaceholder, TextView btnQuitar, String textoDefecto) {
        if (prenda != null) {
            boolean fotoCargada = false;
            if (prenda.rutaFotoOriginal != null && !prenda.rutaFotoOriginal.isEmpty()) {
                try {
                    File f = new File(prenda.rutaFotoOriginal);
                    if (f.exists()) {
                        img.setImageURI(Uri.fromFile(f));
                        fotoCargada = true;
                    } else if (prenda.rutaFotoOriginal.startsWith("content://") || prenda.rutaFotoOriginal.startsWith("file://")) {
                        img.setImageURI(Uri.parse(prenda.rutaFotoOriginal));
                        fotoCargada = true;
                    }
                } catch (Exception ignored) {}
            }

            if (fotoCargada) {
                img.setVisibility(View.VISIBLE);
                tvPlaceholder.setVisibility(View.GONE);
            } else {
                img.setImageDrawable(null);
                img.setVisibility(View.GONE);
                tvPlaceholder.setText(prenda.nombre != null ? prenda.nombre.toUpperCase() : "PRENDA");
                tvPlaceholder.setVisibility(View.VISIBLE);
            }
            btnQuitar.setVisibility(View.VISIBLE);
        } else {
            img.setImageDrawable(null);
            img.setVisibility(View.GONE);
            tvPlaceholder.setText(textoDefecto);
            tvPlaceholder.setVisibility(View.VISIBLE);
            btnQuitar.setVisibility(View.GONE);
        }
    }

    private void actualizarCarruselVestier() {
        if (llContenedorPrendasVestier == null) return;
        llContenedorPrendasVestier.removeAllViews();

        List<PrendaEntity> filtradas = new ArrayList<>();
        for (PrendaEntity p : todasLasPrendas) {
            if (p.categoria != null && p.categoria.toUpperCase().contains(categoriaVestierActiva)) {
                filtradas.add(p);
            }
        }

        if (filtradas.isEmpty()) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("No hay prendas en '" + categoriaVestierActiva.toLowerCase() + "'.");
            tvVacio.setTextColor(0xFF666675);
            tvVacio.setTextSize(11);
            tvVacio.setPadding(10, 16, 10, 16);
            llContenedorPrendasVestier.addView(tvVacio);
            return;
        }

        for (PrendaEntity prenda : filtradas) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) (90 * getResources().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            item.setLayoutParams(lp);
            item.setBackgroundColor(0xFF14141B);
            item.setPadding(
                    (int) (5 * getResources().getDisplayMetrics().density),
                    (int) (5 * getResources().getDisplayMetrics().density),
                    (int) (5 * getResources().getDisplayMetrics().density),
                    (int) (5 * getResources().getDisplayMetrics().density)
            );

            FrameLayout frame = new FrameLayout(this);
            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    (int) (75 * getResources().getDisplayMetrics().density)
            );
            frame.setLayoutParams(flp);
            frame.setBackgroundColor(0xFF0D0D12);

            boolean cargada = false;
            if (prenda.rutaFotoOriginal != null && !prenda.rutaFotoOriginal.isEmpty()) {
                ImageView img = new ImageView(this);
                img.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                cargarMiniaturaEnImageView(img, prenda.rutaFotoOriginal);
                frame.addView(img);
                cargada = true;
            }

            if (!cargada) {
                TextView tv = new TextView(this);
                tv.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                ));
                String initial = (prenda.nombre != null && !prenda.nombre.isEmpty()) ? prenda.nombre.substring(0, 1).toUpperCase() : "✦";
                tv.setText(initial);
                tv.setTextColor(0xFF555566);
                tv.setTextSize(18);
                tv.setTypeface(null, Typeface.BOLD);
                frame.addView(tv);
            }

            item.addView(frame);

            TextView tvNombre = new TextView(this);
            tvNombre.setText(prenda.nombre != null ? prenda.nombre : "Sin nombre");
            tvNombre.setTextColor(0xFFFFFFFF);
            tvNombre.setTextSize(10);
            tvNombre.setMaxLines(1);
            tvNombre.setEllipsize(TextUtils.TruncateAt.END);
            tvNombre.setPadding(0, (int) (3 * getResources().getDisplayMetrics().density), 0, 0);
            item.addView(tvNombre);

            item.setOnClickListener(v -> asignarPrendaAVestier(prenda));

            llContenedorPrendasVestier.addView(item);
        }
    }

    private void asignarPrendaAVestier(PrendaEntity prenda) {
        if (prenda == null || prenda.categoria == null) return;
        String cat = prenda.categoria.toUpperCase();
        if (cat.contains("SUPERIOR")) {
            prendaSuperiorSeleccionada = prenda;
        } else if (cat.contains("INFERIOR")) {
            prendaInferiorSeleccionada = prenda;
        } else if (cat.contains("CALZADO")) {
            prendaCalzadoSeleccionada = prenda;
        } else if (cat.contains("ABRIGO")) {
            prendaSuperiorSeleccionada = prenda;
        } else {
            if ("SUPERIOR".equals(categoriaVestierActiva)) prendaSuperiorSeleccionada = prenda;
            else if ("INFERIOR".equals(categoriaVestierActiva)) prendaInferiorSeleccionada = prenda;
            else if ("CALZADO".equals(categoriaVestierActiva)) prendaCalzadoSeleccionada = prenda;
            else prendaSuperiorSeleccionada = prenda;
        }
        AppLogger.i("VESTIER", "Prenda asignada: " + prenda.nombre + " (" + prenda.categoria + ")");
        actualizarLienzoVestier();
    }

    /** Cálculo de afinidad cromática y de estilo en tiempo real usando HakiForge Engine (0 tokens, offline). */
    private void calcularAfinidadOutfit() {
        int prendasActivas = 0;
        if (prendaSuperiorSeleccionada != null) prendasActivas++;
        if (prendaInferiorSeleccionada != null) prendasActivas++;
        if (prendaCalzadoSeleccionada != null) prendasActivas++;

        if (prendasActivas == 0) {
            tvVestierAfinidad.setText("AFINIDAD: --");
            return;
        }

        String colSup = prendaSuperiorSeleccionada != null && prendaSuperiorSeleccionada.colorNombre != null ? prendaSuperiorSeleccionada.colorNombre : null;
        String colInf = prendaInferiorSeleccionada != null && prendaInferiorSeleccionada.colorNombre != null ? prendaInferiorSeleccionada.colorNombre : null;
        String colCal = prendaCalzadoSeleccionada != null && prendaCalzadoSeleccionada.colorNombre != null ? prendaCalzadoSeleccionada.colorNombre : null;

        int afinidad = 70;
        if (colSup != null && colInf != null) {
            afinidad = RecomendadorEngine.calcularAfinidadCompleta(colSup, colInf, colCal);
        } else if (colSup != null && colCal != null) {
            afinidad = RecomendadorEngine.calcularAfinidad(colSup, colCal);
        } else if (colInf != null && colCal != null) {
            afinidad = RecomendadorEngine.calcularAfinidad(colInf, colCal);
        }

        // Coherencia de estilo: +5% si ambas prendas comparten estilo
        if (prendaSuperiorSeleccionada != null && prendaInferiorSeleccionada != null) {
            String estSup = prendaSuperiorSeleccionada.estilo;
            String estInf = prendaInferiorSeleccionada.estilo;
            if (estSup != null && estSup.equalsIgnoreCase(estInf)) {
                afinidad = Math.min(100, afinidad + 5);
            }
        }

        tvVestierAfinidad.setText("AFINIDAD HAKIFORGE: " + afinidad + "%");
    }

    private void generarOutfitAleatorio() {
        List<PrendaEntity> superiores = new ArrayList<>();
        List<PrendaEntity> inferiores = new ArrayList<>();
        List<PrendaEntity> calzado = new ArrayList<>();

        for (PrendaEntity p : todasLasPrendas) {
            if (p.categoria == null) continue;
            String cat = p.categoria.toUpperCase();
            if (cat.contains("SUPERIOR") || cat.contains("ABRIGO")) superiores.add(p);
            else if (cat.contains("INFERIOR")) inferiores.add(p);
            else if (cat.contains("CALZADO")) calzado.add(p);
        }

        Random rnd = new Random();
        if (!superiores.isEmpty()) {
            prendaSuperiorSeleccionada = superiores.get(rnd.nextInt(superiores.size()));
        }
        if (!inferiores.isEmpty()) {
            prendaInferiorSeleccionada = inferiores.get(rnd.nextInt(inferiores.size()));
        }
        if (!calzado.isEmpty()) {
            prendaCalzadoSeleccionada = calzado.get(rnd.nextInt(calzado.size()));
        }

        actualizarLienzoVestier();
        Toast.makeText(this, "🎲 Outfit generado al azar", Toast.LENGTH_SHORT).show();
    }

    private void guardarOutfitActual() {
        if (prendaSuperiorSeleccionada == null && prendaInferiorSeleccionada == null && prendaCalzadoSeleccionada == null) {
            Toast.makeText(this, "Selecciona al menos una prenda para guardar el outfit", Toast.LENGTH_SHORT).show();
            return;
        }

        int idSup = (prendaSuperiorSeleccionada != null) ? prendaSuperiorSeleccionada.id : 0;
        int idInf = (prendaInferiorSeleccionada != null) ? prendaInferiorSeleccionada.id : 0;
        int idCal = (prendaCalzadoSeleccionada != null) ? prendaCalzadoSeleccionada.id : 0;

        String ocasion = "Casual";
        if (prendaSuperiorSeleccionada != null && prendaSuperiorSeleccionada.estilo != null) {
            ocasion = prendaSuperiorSeleccionada.estilo;
        }

        // Cálculo real de afinidad con HakiForge Engine
        int afinidadReal = 70;
        String colSup = prendaSuperiorSeleccionada != null && prendaSuperiorSeleccionada.colorNombre != null ? prendaSuperiorSeleccionada.colorNombre : "Negro";
        String colInf = prendaInferiorSeleccionada != null && prendaInferiorSeleccionada.colorNombre != null ? prendaInferiorSeleccionada.colorNombre : "Azul marino";
        String colCal = prendaCalzadoSeleccionada != null && prendaCalzadoSeleccionada.colorNombre != null ? prendaCalzadoSeleccionada.colorNombre : null;

        if (prendaSuperiorSeleccionada != null && prendaInferiorSeleccionada != null) {
            afinidadReal = RecomendadorEngine.calcularAfinidadCompleta(colSup, colInf, colCal);
        } else if (prendaSuperiorSeleccionada != null && prendaCalzadoSeleccionada != null) {
            afinidadReal = RecomendadorEngine.calcularAfinidad(colSup, colCal);
        } else if (prendaInferiorSeleccionada != null && prendaCalzadoSeleccionada != null) {
            afinidadReal = RecomendadorEngine.calcularAfinidad(colInf, colCal);
        }

        OutfitEntity outfit = new OutfitEntity(idSup, idInf, idCal, ocasion, afinidadReal);
        viewModel.insertarOutfit(outfit);
        AppLogger.i("VESTIER", "Outfit guardado en Room con afinidad HakiForge: " + afinidadReal + "%");
        Toast.makeText(this, "✅ Outfit guardado (" + afinidadReal + "% afinidad)", Toast.LENGTH_SHORT).show();
    }

    // ─── MOTOR DE EXPERIENCIA IA ───────────────────────────────────────────────
    private static final String BRIDGE_URL = "http://192.168.1.66:8000";

    private String getBridgeUrl() {
        return prefs != null ? prefs.getString("servidor_url", BRIDGE_URL) : BRIDGE_URL;
    }

    private void ejecutarVirtualTryOnIA() {
        if (prendaSuperiorSeleccionada == null && prendaInferiorSeleccionada == null && prendaCalzadoSeleccionada == null) {
            Toast.makeText(this, "Selecciona al menos una prenda en el Vestier para probar con IA", Toast.LENGTH_SHORT).show();
            return;
        }

        // Diálogo de opciones del probador
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Generar Look");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        // Opción 1: Probador según motor configurado
        String motorConfigurado = prefs != null ? prefs.getString(PREF_MOTOR_TRYON, MOTOR_LOCAL) : MOTOR_LOCAL;
        android.widget.Button btnMotor = new android.widget.Button(this);
        if (MOTOR_REPLICATE.equals(motorConfigurado)) {
            btnMotor.setText("Probador Virtual (Replicate IDM-VTON)\nNube de alta fidelidad • $0.024/img");
        } else if (MOTOR_GEMINI.equals(motorConfigurado)) {
            btnMotor.setText("Probador Virtual (Gemini Imagen)\nEdición generativa fotorrealista • $0.04/img");
        } else {
            btnMotor.setText("Probador Virtual (Servidor Local)\nIDM-VTON en tu PC • Sin costo");
        }
        btnMotor.setTextSize(13f);
        btnMotor.setAllCaps(false);
        btnMotor.setBackgroundColor(0xFF14141B);
        btnMotor.setTextColor(0xFFFFFFFF);
        btnMotor.setPadding(32, 28, 32, 28);
        android.widget.LinearLayout.LayoutParams lp1 = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.setMargins(0, 0, 0, 16);
        btnMotor.setLayoutParams(lp1);

        // Opción 2: Asesor Gemini
        android.widget.Button btnGemini = new android.widget.Button(this);
        btnGemini.setText("Asesor de Estilo (Gemini)\nEvaluación de combinación y ocasión");
        btnGemini.setTextSize(13f);
        btnGemini.setAllCaps(false);
        btnGemini.setBackgroundColor(0xFF14141B);
        btnGemini.setTextColor(0xFFCCCCCC);
        btnGemini.setPadding(32, 28, 32, 28);
        android.widget.LinearLayout.LayoutParams lp2 = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(0, 0, 0, 16);
        btnGemini.setLayoutParams(lp2);

        // Opción 3: Local Offline
        android.widget.Button btnOffline = new android.widget.Button(this);
        btnOffline.setText("Compatibilidad Cromática (Offline)\nCálculo de colores instantáneo sin red");
        btnOffline.setTextSize(13f);
        btnOffline.setAllCaps(false);
        btnOffline.setBackgroundColor(0xFF14141B);
        btnOffline.setTextColor(0xFFAAAAAA);
        btnOffline.setPadding(32, 28, 32, 28);
        btnOffline.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        layout.addView(btnMotor);
        layout.addView(btnGemini);
        layout.addView(btnOffline);
        builder.setView(layout);
        builder.setNegativeButton("Cancelar", null);

        android.app.AlertDialog dlg = builder.create();
        dlg.show();

        btnMotor.setOnClickListener(v -> { dlg.dismiss(); ejecutarTryOnSegunMotor(); });
        btnGemini.setOnClickListener(v -> { dlg.dismiss(); ejecutarAnalisisEstiloGemini(); });
        btnOffline.setOnClickListener(v -> { dlg.dismiss(); ejecutarAnalisisHakiForgeOffline(); });
    }

    /** Despacha la ejecución del probador virtual según el motor seleccionado en Ajustes. */
    private void ejecutarTryOnSegunMotor() {
        String motor = prefs != null ? prefs.getString(PREF_MOTOR_TRYON, MOTOR_LOCAL) : MOTOR_LOCAL;
        if (MOTOR_REPLICATE.equals(motor)) {
            ejecutarTryOnReplicate();
        } else if (MOTOR_GEMINI.equals(motor)) {
            ejecutarTryOnGemini();
        } else {
            ejecutarTryOnLocalBridge();
        }
    }

    /** MODO 1: Try-On fotorrealista gratis usando el servidor local (server_bridge.py + IDM-VTON). */
    private void ejecutarTryOnLocalBridge() {
        PrendaEntity prendaAVestir = prendaSuperiorSeleccionada;
        if (prendaAVestir == null) prendaAVestir = prendaInferiorSeleccionada;
        if (prendaAVestir == null) prendaAVestir = prendaCalzadoSeleccionada;

        if (prendaAVestir == null || prendaAVestir.rutaFotoOriginal == null || prendaAVestir.rutaFotoOriginal.isEmpty()) {
            Toast.makeText(this, "La prenda necesita tener foto para el Try-On. Escanéala primero.", Toast.LENGTH_LONG).show();
            return;
        }

        if (rutaFotoUsuarioPersonal == null || rutaFotoUsuarioPersonal.isEmpty()) {
            Toast.makeText(this, "📷 Agrega tu foto personal en el Vestier primero (botón 'Mi Foto')", Toast.LENGTH_LONG).show();
            return;
        }

        mostrarModalCargando("Conectando con HakiForge Bridge (IDM-VTON)...", "5%");
        AppLogger.i("BRIDGE", "Iniciando try-on local. Prenda: " + prendaAVestir.nombre);

        final PrendaEntity prendaFinal = prendaAVestir;
        final String fotoUsuario = rutaFotoUsuarioPersonal;

        new Thread(() -> {
            try {
                // 1. Convertir imágenes a base64
                runOnUiThread(() -> { tvProgresoTexto.setText("Preparando imágenes..."); progressBarIA.setProgress(15); tvPorcentajeIA.setText("15%"); });

                String modelB64 = imagenFileABase64(fotoUsuario);
                String garmentB64 = imagenFileABase64(prendaFinal.rutaFotoOriginal);

                if (modelB64 == null || garmentB64 == null) {
                    runOnUiThread(() -> {
                        ocultarProgreso();
                        Toast.makeText(this, "No se pudieron leer las imágenes. Verifica que existan.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                runOnUiThread(() -> { tvProgresoTexto.setText("Enviando al motor IDM-VTON en tu PC..."); progressBarIA.setProgress(30); tvPorcentajeIA.setText("30%"); });

                // 2. Construir JSON
                String categoria = "upper_body";
                if (prendaFinal.categoria != null && prendaFinal.categoria.toLowerCase().contains("inferior")) categoria = "lower_body";

                org.json.JSONObject jsonBody = new org.json.JSONObject();
                jsonBody.put("model_image", "data:image/jpeg;base64," + modelB64);
                jsonBody.put("garment_image", "data:image/jpeg;base64," + garmentB64);
                jsonBody.put("category", categoria);

                // 3. POST al server_bridge.py
                java.net.URL url = new java.net.URL(getBridgeUrl() + "/api/tryon");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(120000); // IDM-VTON puede tardar hasta 2 min

                byte[] bodyBytes = jsonBody.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                conn.getOutputStream().write(bodyBytes);

                runOnUiThread(() -> { tvProgresoTexto.setText("Motor de difusión procesando (puede tardar ~20-30 seg)..."); progressBarIA.setProgress(50); tvPorcentajeIA.setText("50%"); });

                int responseCode = conn.getResponseCode();
                java.io.InputStream stream = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
                String respuesta = scanner.hasNext() ? scanner.next() : "";
                conn.disconnect();

                runOnUiThread(() -> { tvProgresoTexto.setText("Descargando resultado..."); progressBarIA.setProgress(90); tvPorcentajeIA.setText("90%"); });

                org.json.JSONObject respJson = new org.json.JSONObject(respuesta);
                if ("success".equals(respJson.optString("status"))) {
                    String imgDataB64 = respJson.getString("image_base64");
                    String b64pure = imgDataB64.contains(",") ? imgDataB64.split(",")[1] : imgDataB64;
                    byte[] imgBytes = android.util.Base64.decode(b64pure, android.util.Base64.DEFAULT);
                    Bitmap resultBitmap = android.graphics.BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);

                    runOnUiThread(() -> {
                        ocultarProgreso();
                        if (resultBitmap != null) {
                            mostrarBitmapEnVisorTryOn(resultBitmap);
                            guardarLookEnHistorial(resultBitmap, prendaFinal.nombre);
                        }
                        llResultadoIA.setVisibility(View.GONE);
                        modalVirtualTryOn.setVisibility(View.VISIBLE);
                        AppLogger.i("BRIDGE", "Try-On fotorrealista completado con éxito.");
                        Toast.makeText(this, "✨ Look generado y guardado en tu historial", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    String err = respJson.optString("message", "Error desconocido del servidor");
                    runOnUiThread(() -> {
                        ocultarProgreso();
                        new android.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("Error en Try-On Local")
                                .setMessage("El servidor respondió: " + err + "\n\n" +
                                        "Asegúrate de que 'python3 server_bridge.py' esté corriendo en tu PC y que el teléfono esté en la misma red Wi-Fi.")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }

            } catch (java.net.ConnectException e) {
                runOnUiThread(() -> {
                    ocultarProgreso();
                    new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("⚠️ Servidor no encontrado")
                            .setMessage("No se pudo conectar a " + getBridgeUrl() + ".\n\n" +
                                    "Para usar el Try-On con IA local:\n" +
                                    "1. Abre una terminal en tu PC\n" +
                                    "2. Ejecuta: python3 server_bridge.py\n" +
                                    "3. Asegúrate de que el teléfono esté en la misma Wi-Fi\n" +
                                    "4. Vuelve a intentarlo.")
                            .setPositiveButton("Entendido", null)
                            .show();
                });
            } catch (Exception e) {
                AppLogger.e("BRIDGE", "Error en try-on local: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    ocultarProgreso();
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /** MODO REPLICATE: Virtual Try-On con IDM-VTON en la nube ($0.024/img). */
    private void ejecutarTryOnReplicate() {
        PrendaEntity prendaAVestir = prendaSuperiorSeleccionada;
        if (prendaAVestir == null) prendaAVestir = prendaInferiorSeleccionada;
        if (prendaAVestir == null) prendaAVestir = prendaCalzadoSeleccionada;

        if (prendaAVestir == null || prendaAVestir.rutaFotoOriginal == null || prendaAVestir.rutaFotoOriginal.isEmpty()) {
            Toast.makeText(this, "La prenda necesita tener foto para el Try-On. Escanéala primero.", Toast.LENGTH_LONG).show();
            return;
        }

        if (rutaFotoUsuarioPersonal == null || rutaFotoUsuarioPersonal.isEmpty()) {
            Toast.makeText(this, "📷 Agrega tu foto personal en el Vestier primero (botón 'Mi Foto')", Toast.LENGTH_LONG).show();
            return;
        }

        String apiKey = prefs != null ? prefs.getString(PREF_REPLICATE_API_KEY, "") : "";
        if (apiKey.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("API Key de Replicate requerida")
                    .setMessage("Para usar el motor de Replicate, necesitas ingresar tu API Key en ⚙️ Ajustes -> Motor de Probador Virtual.")
                    .setPositiveButton("Configurar", (d, w) -> mostrarDialogoConfigurarMotor())
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        mostrarModalCargando("Conectando con Replicate (IDM-VTON Cloud)...", "10%");
        AppLogger.i("REPLICATE", "Iniciando Try-On en la nube con Replicate. Prenda: " + prendaAVestir.nombre);

        final PrendaEntity prendaFinal = prendaAVestir;
        final String fotoUsuario = rutaFotoUsuarioPersonal;
        final String tokenReplicate = apiKey.trim();

        new Thread(() -> {
            try {
                runOnUiThread(() -> { tvProgresoTexto.setText("Optimizando imágenes para la nube..."); progressBarIA.setProgress(20); tvPorcentajeIA.setText("20%"); });

                String modelB64 = imagenFileABase64(fotoUsuario);
                String garmentB64 = imagenFileABase64(prendaFinal.rutaFotoOriginal);

                if (modelB64 == null || garmentB64 == null) {
                    runOnUiThread(() -> {
                        ocultarProgreso();
                        Toast.makeText(this, "No se pudieron leer las imágenes locales.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                runOnUiThread(() -> { tvProgresoTexto.setText("Iniciando GPU en Replicate..."); progressBarIA.setProgress(35); tvPorcentajeIA.setText("35%"); });

                org.json.JSONObject inputJson = new org.json.JSONObject();
                inputJson.put("human_img", "data:image/jpeg;base64," + modelB64);
                inputJson.put("garm_img", "data:image/jpeg;base64," + garmentB64);
                inputJson.put("garment_des", prendaFinal.nombre != null ? prendaFinal.nombre : "garment");
                inputJson.put("is_checked", true);
                inputJson.put("is_checked_crop", false);
                inputJson.put("denoise_steps", 30);
                inputJson.put("seed", 42);

                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("version", "c871bb9b046607b680449ecbae55fd8c6d945e0a1948644bf2361b3d021d3ff4");
                payload.put("input", inputJson);

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                RequestBody reqBody = RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request postReq = new Request.Builder()
                        .url("https://api.replicate.com/v1/predictions")
                        .addHeader("Authorization", "Bearer " + tokenReplicate)
                        .post(reqBody)
                        .build();

                String predictionId = null;
                try (Response response = client.newCall(postReq).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    AppLogger.i("REPLICATE", "Respuesta POST /predictions: HTTP " + response.code());

                    if (!response.isSuccessful()) {
                        AppLogger.e("REPLICATE", "Error inicial: " + respBody);
                        runOnUiThread(() -> {
                            ocultarProgreso();
                            new android.app.AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Error en Replicate")
                                    .setMessage("Error HTTP " + response.code() + ": " + respBody)
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                        return;
                    }

                    org.json.JSONObject respObj = new org.json.JSONObject(respBody);
                    predictionId = respObj.optString("id");
                }

                if (predictionId == null || predictionId.isEmpty()) {
                    runOnUiThread(() -> {
                        ocultarProgreso();
                        Toast.makeText(this, "No se recibió ID de predicción de Replicate.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                AppLogger.i("REPLICATE", "Predicción creada (ID: " + predictionId + "). Esperando GPU...");
                int intentos = 0;
                int maxIntentos = 45;
                String urlResultado = null;

                while (intentos < maxIntentos) {
                    Thread.sleep(2500);
                    intentos++;

                    int prog = Math.min(90, 35 + (intentos * 2));
                    int segs = intentos * 2;
                    runOnUiThread(() -> {
                        tvProgresoTexto.setText("Generando con IDM-VTON en la nube (" + segs + "s)...");
                        progressBarIA.setProgress(prog);
                        tvPorcentajeIA.setText(prog + "%");
                    });

                    Request pollReq = new Request.Builder()
                            .url("https://api.replicate.com/v1/predictions/" + predictionId)
                            .addHeader("Authorization", "Bearer " + tokenReplicate)
                            .get()
                            .build();

                    try (Response pollResp = client.newCall(pollReq).execute()) {
                        if (pollResp.isSuccessful() && pollResp.body() != null) {
                            String pollBody = pollResp.body().string();
                            org.json.JSONObject statusObj = new org.json.JSONObject(pollBody);
                            String status = statusObj.optString("status");
                            AppLogger.i("REPLICATE", "Estado Replicate (" + segs + "s): " + status);

                            if ("succeeded".equalsIgnoreCase(status)) {
                                org.json.JSONArray outArr = statusObj.optJSONArray("output");
                                if (outArr != null && outArr.length() > 0) {
                                    urlResultado = outArr.getString(0);
                                } else if (statusObj.has("output") && !statusObj.isNull("output")) {
                                    urlResultado = statusObj.getString("output");
                                }
                                break;
                            } else if ("failed".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)) {
                                String err = statusObj.optString("error", "Fallo al procesar en Replicate");
                                runOnUiThread(() -> {
                                    ocultarProgreso();
                                    new android.app.AlertDialog.Builder(MainActivity.this)
                                            .setTitle("Error en Replicate")
                                            .setMessage("La GPU reportó un error: " + err)
                                            .setPositiveButton("OK", null)
                                            .show();
                                });
                                return;
                            }
                        }
                    }
                }

                if (urlResultado == null) {
                    runOnUiThread(() -> {
                        ocultarProgreso();
                        Toast.makeText(this, "Tiempo de espera agotado al renderizar en Replicate.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                runOnUiThread(() -> { tvProgresoTexto.setText("Descargando imagen final..."); progressBarIA.setProgress(95); tvPorcentajeIA.setText("95%"); });

                Request dlReq = new Request.Builder().url(urlResultado).get().build();
                Bitmap resultBitmap = null;
                try (Response dlResp = client.newCall(dlReq).execute()) {
                    if (dlResp.isSuccessful() && dlResp.body() != null) {
                        try (java.io.InputStream is = dlResp.body().byteStream()) {
                            resultBitmap = BitmapFactory.decodeStream(is);
                        }
                    }
                }

                if (resultBitmap != null) {
                    final Bitmap finalBmp = resultBitmap;
                    runOnUiThread(() -> {
                        ocultarProgreso();
                        mostrarBitmapEnVisorTryOn(finalBmp);
                        guardarLookEnHistorial(finalBmp, prendaFinal.nombre);
                        llResultadoIA.setVisibility(View.GONE);
                        modalVirtualTryOn.setVisibility(View.VISIBLE);
                        AppLogger.i("REPLICATE", "Try-On completado exitosamente via Replicate.");
                        Toast.makeText(this, "✨ Look generado con Replicate IDM-VTON", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        ocultarProgreso();
                        Toast.makeText(this, "No se pudo decodificar la imagen devuelta por Replicate.", Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                AppLogger.e("REPLICATE", "Excepción durante Try-On Replicate", e);
                runOnUiThread(() -> {
                    ocultarProgreso();
                    Toast.makeText(this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /** MODO GEMINI: Virtual Try-On con Imagen 3 / Gemini multimodal ($0.04/img). */
    private void ejecutarTryOnGemini() {
        PrendaEntity prendaAVestir = prendaSuperiorSeleccionada;
        if (prendaAVestir == null) prendaAVestir = prendaInferiorSeleccionada;
        if (prendaAVestir == null) prendaAVestir = prendaCalzadoSeleccionada;

        if (prendaAVestir == null || prendaAVestir.rutaFotoOriginal == null || prendaAVestir.rutaFotoOriginal.isEmpty()) {
            Toast.makeText(this, "La prenda necesita tener foto para el Try-On. Escanéala primero.", Toast.LENGTH_LONG).show();
            return;
        }

        String apiKey = prefs != null ? prefs.getString(PREF_GEMINI_API_KEY, "") : "";
        if (apiKey.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("API Key de Google AI Studio requerida")
                    .setMessage("Para usar el motor de Gemini Imagen, necesitas ingresar tu API Key en ⚙️ Ajustes -> Motor de Probador Virtual.")
                    .setPositiveButton("Configurar", (d, w) -> mostrarDialogoConfigurarMotor())
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        mostrarModalCargando("Generando con Google Gemini Imagen...", "15%");
        AppLogger.i("GEMINI_IMG", "Iniciando generación de imagen con Gemini. Prenda: " + prendaAVestir.nombre);

        final PrendaEntity prendaFinal = prendaAVestir;
        final String tokenGemini = apiKey.trim();
        final String customPrompt = prefs != null ? prefs.getString(PREF_GEMINI_IMG_PROMPT, DEFAULT_GEMINI_PROMPT) : DEFAULT_GEMINI_PROMPT;

        new Thread(() -> {
            try {
                runOnUiThread(() -> { tvProgresoTexto.setText("Construyendo prompt de estilo fotorrealista..."); progressBarIA.setProgress(30); tvPorcentajeIA.setText("30%"); });

                String promptFinal = customPrompt + ". Prenda de vestir: " + prendaFinal.nombre;
                if (prendaFinal.categoria != null && !prendaFinal.categoria.isEmpty()) {
                    promptFinal += " (categoría: " + prendaFinal.categoria + ")";
                }
                if (prendaFinal.colorNombre != null && !prendaFinal.colorNombre.isEmpty()) {
                    promptFinal += ", color " + prendaFinal.colorNombre;
                }
                promptFinal += ". Fotografía de moda profesional, iluminación de estudio, figura completa fotorrealista, alta definición.";

                org.json.JSONObject instance = new org.json.JSONObject();
                instance.put("prompt", promptFinal);

                org.json.JSONArray instances = new org.json.JSONArray();
                instances.put(instance);

                org.json.JSONObject parameters = new org.json.JSONObject();
                parameters.put("sampleCount", 1);
                parameters.put("aspectRatio", "3:4");

                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("instances", instances);
                payload.put("parameters", parameters);

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                RequestBody reqBody = RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                String url = "https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:predict?key=" + tokenGemini;

                Request request = new Request.Builder()
                        .url(url)
                        .post(reqBody)
                        .build();

                runOnUiThread(() -> { tvProgresoTexto.setText("Sintetizando imagen con Imagen 3..."); progressBarIA.setProgress(60); tvPorcentajeIA.setText("60%"); });

                try (Response response = client.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    AppLogger.i("GEMINI_IMG", "Respuesta HTTP " + response.code());

                    if (!response.isSuccessful()) {
                        AppLogger.e("GEMINI_IMG", "Fallo en Imagen 3: " + respBody);
                        runOnUiThread(() -> {
                            ocultarProgreso();
                            new android.app.AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Error en Gemini Imagen")
                                    .setMessage("Google AI Studio respondió (HTTP " + response.code() + "):\n\n" + respBody + "\n\nVerifica que tu API Key tenga permisos para Imagen 3.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                        return;
                    }

                    org.json.JSONObject respObj = new org.json.JSONObject(respBody);
                    org.json.JSONArray predictions = respObj.optJSONArray("predictions");

                    if (predictions != null && predictions.length() > 0) {
                        org.json.JSONObject firstPred = predictions.getJSONObject(0);
                        String b64Image = firstPred.optString("bytesBase64Encoded");

                        if (b64Image != null && !b64Image.isEmpty()) {
                            byte[] imgBytes = android.util.Base64.decode(b64Image, android.util.Base64.DEFAULT);
                            Bitmap resultBitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);

                            if (resultBitmap != null) {
                                final Bitmap finalBmp = resultBitmap;
                                runOnUiThread(() -> {
                                    ocultarProgreso();
                                    mostrarBitmapEnVisorTryOn(finalBmp);
                                    guardarLookEnHistorial(finalBmp, prendaFinal.nombre);
                                    llResultadoIA.setVisibility(View.GONE);
                                    modalVirtualTryOn.setVisibility(View.VISIBLE);
                                    AppLogger.i("GEMINI_IMG", "Imagen generada con éxito con Imagen 3.");
                                    Toast.makeText(this, "✨ Look generado con Gemini Imagen", Toast.LENGTH_SHORT).show();
                                });
                                return;
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        ocultarProgreso();
                        Toast.makeText(this, "Respuesta vacía o formato desconocido de Gemini.", Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                AppLogger.e("GEMINI_IMG", "Excepción al invocar Gemini Imagen", e);
                runOnUiThread(() -> {
                    ocultarProgreso();
                    Toast.makeText(this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /** Convierte una imagen (ruta o URI string) a base64. Retorna null si falla. */
    private String imagenFileABase64(String rutaOUri) {
        try {
            byte[] bytes;
            if (rutaOUri.startsWith("content://")) {
                try (java.io.InputStream in = getContentResolver().openInputStream(android.net.Uri.parse(rutaOUri))) {
                    if (in == null) return null;
                    bytes = leerTodosLosBytes(in);
                }
            } else {
                java.io.File f = new java.io.File(rutaOUri);
                if (!f.exists()) return null;
                try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
                    bytes = leerTodosLosBytes(fis);
                }
            }
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            AppLogger.e("BRIDGE", "Error leyendo imagen: " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] leerTodosLosBytes(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) buffer.write(chunk, 0, n);
        return buffer.toByteArray();
    }

    /** MODO 3: Análisis offline usando RecomendadorEngine (HakiForge, sin internet). */
    private void ejecutarAnalisisHakiForgeOffline() {
        if (prendaSuperiorSeleccionada == null && prendaInferiorSeleccionada == null) {
            Toast.makeText(this, "Selecciona al menos una prenda Superior e Inferior para analizar la combinación", Toast.LENGTH_LONG).show();
            return;
        }

        mostrarModalCargando("Calculando afinidad cromática HakiForge...", "50%");

        new Thread(() -> {
            try {
                Thread.sleep(600); // Pequeña pausa para mostrar el loader
            } catch (InterruptedException ignored) {}

            String colorSup = (prendaSuperiorSeleccionada != null && prendaSuperiorSeleccionada.colorNombre != null)
                    ? prendaSuperiorSeleccionada.colorNombre : "Negro";
            String colorInf = (prendaInferiorSeleccionada != null && prendaInferiorSeleccionada.colorNombre != null)
                    ? prendaInferiorSeleccionada.colorNombre : "Negro";

            int afinidad = com.hakimik.closetscan.engine.RecomendadorEngine.calcularAfinidad(colorSup, colorInf);

            String nomSup = (prendaSuperiorSeleccionada != null) ? prendaSuperiorSeleccionada.nombre : "—";
            String nomInf = (prendaInferiorSeleccionada != null) ? prendaInferiorSeleccionada.nombre : "—";
            String nomCal = (prendaCalzadoSeleccionada != null) ? prendaCalzadoSeleccionada.nombre : "—";

            String valoracion;
            String consejo;
            if (afinidad >= 85) {
                valoracion = "✨ Excelente combinación";
                consejo = "Los colores se complementan perfectamente. Esta combinación funciona para cualquier ocasión.";
            } else if (afinidad >= 65) {
                valoracion = "👍 Buena combinación";
                consejo = "Buena armonía cromática y equilibrio visual.";
            } else if (afinidad >= 45) {
                valoracion = "🤔 Combinación aceptable";
                consejo = "Los colores no chocan pero tampoco resaltan.";
            } else {
                valoracion = "⚠️ Contraste fuerte";
                consejo = "Esta combinación puede funcionar como look atrevido o urbano.";
            }

            final String scoreText = afinidad + "% — " + valoracion;
            final String detalleText = "⚡ Análisis HakiForge Offline (0 Tokens)\n" +
                    "─────────────────\n" +
                    "Superior: " + nomSup + " (" + colorSup + ")\n" +
                    "Inferior: " + nomInf + " (" + colorInf + ")\n" +
                    "Calzado: " + nomCal + "\n\n" +
                    "📊 Afinidad Cromática: " + afinidad + "%\n\n" +
                    "💡 " + consejo;

            runOnUiThread(() -> mostrarResultadoTryOn(detalleText, scoreText));
        }).start();
    }

    /** MODO 2: Análisis estilístico, armonía de color y puntaje con Gemini 3.5 Flash-Lite. */
    private void ejecutarAnalisisEstiloGemini() {
        String apiKey = prefs.getString(PREF_GEMINI_API_KEY, "");
        if (apiKey.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("🔑 API Key requerida")
                    .setMessage("Para analizar la combinación con IA necesitas tu clave de Google AI Studio (es gratis).\n\n" +
                            "Configúrala en ⚙️ Ajustes en 1 toque.")
                    .setPositiveButton("Ir a Ajustes", (d, w) -> mostrarDialogoAjustes())
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        mostrarModalCargando("Conectando con Gemini 3.5 Flash-Lite...", "10%");

        String nomSup = (prendaSuperiorSeleccionada != null) ? prendaSuperiorSeleccionada.nombre : null;
        String nomInf = (prendaInferiorSeleccionada != null) ? prendaInferiorSeleccionada.nombre : null;
        String nomCal = (prendaCalzadoSeleccionada != null) ? prendaCalzadoSeleccionada.nombre : null;

        geminiClient.analizarOutfit(
                apiKey,
                rutaFotoUsuarioPersonal,
                nomSup, nomInf, nomCal,
                new GeminiApiClient.Callback() {
                    @Override
                    public void onProgress(int percentage) {
                        actualizarProgreso(percentage);
                    }
                    @Override
                    public void onSuccess(String textoCompleto, String puntuacion) {
                        mostrarResultadoTryOn(textoCompleto, puntuacion);
                    }
                    @Override
                    public void onError(String mensaje) {
                        ocultarProgreso();
                        modalVirtualTryOn.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "❌ " + mensaje, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void mostrarModalCargando(String textoInicial, String porcentajeInicial) {
        // Mostrar foto del usuario o silueta
        boolean imagenPuesta = false;
        if (rutaFotoUsuarioPersonal != null) {
            try {
                File f = new File(rutaFotoUsuarioPersonal);
                if (f.exists()) {
                    imgResultadoTryOn.setImageURI(android.net.Uri.fromFile(f));
                    imagenPuesta = true;
                } else {
                    imgResultadoTryOn.setImageURI(android.net.Uri.parse(rutaFotoUsuarioPersonal));
                    imagenPuesta = true;
                }
            } catch (Exception ignored) {}
        }
        if (!imagenPuesta && prendaSuperiorSeleccionada != null && prendaSuperiorSeleccionada.rutaFotoOriginal != null) {
            try {
                File f = new File(prendaSuperiorSeleccionada.rutaFotoOriginal);
                if (f.exists()) imgResultadoTryOn.setImageURI(android.net.Uri.fromFile(f));
            } catch (Exception ignored) {}
        }
        if (!imagenPuesta) {
            imgResultadoTryOn.setImageResource(R.drawable.ic_silueta_humana);
        }

        // Mostrar overlay de progreso
        llProgresoIA.setVisibility(View.VISIBLE);
        llResultadoIA.setVisibility(View.GONE);
        progressBarIA.setProgress(0);
        tvProgresoTexto.setText(textoInicial);
        tvPorcentajeIA.setText(porcentajeInicial);

        modalVirtualTryOn.setVisibility(View.VISIBLE);
    }

    private void actualizarProgresoGenerativo(int pct, String estado) {
        if (progressBarIA != null) {
            progressBarIA.setProgress(pct);
            tvPorcentajeIA.setText(pct + "%");
            if (pct < 30) {
                tvProgresoTexto.setText("Preparando imágenes y enviando a GPU (" + estado + ")...");
            } else if (pct < 85) {
                tvProgresoTexto.setText("Sintetizando pliegues y cuerpo con difusión neuronal (" + estado + ")...");
            } else if (pct < 98) {
                tvProgresoTexto.setText("Descargando imagen en alta resolución...");
            } else {
                tvProgresoTexto.setText("¡Imagen completada!");
            }
        }
    }

    private void actualizarProgreso(int pct) {
        if (progressBarIA != null) {
            progressBarIA.setProgress(pct);
            tvPorcentajeIA.setText(pct + "%");
            if (pct < 40) tvProgresoTexto.setText("Conectando con Gemini IA...");
            else if (pct < 70) tvProgresoTexto.setText("Analizando combinación de colores y estilo...");
            else if (pct < 90) tvProgresoTexto.setText("Generando recomendaciones personalizadas...");
            else tvProgresoTexto.setText("Finalizando análisis...");
        }
    }

    private void ocultarProgreso() {
        if (llProgresoIA != null) llProgresoIA.setVisibility(View.GONE);
    }

    private void mostrarResultadoTryOn(String textoGemini, String puntuacion) {
        // Actualizar textos del resultado
        tvDetallePrendasTryOn.setText(textoGemini);
        tvScoreTryOn.setText(puntuacion);

        // Ocultar la barra de progreso, mostrar resultado
        llProgresoIA.setVisibility(View.GONE);
        llResultadoIA.setVisibility(View.VISIBLE);

        // Asegurar que el modal está visible
        modalVirtualTryOn.setVisibility(View.VISIBLE);
    }

    private void configurarModalVirtualTryOn() {
        btnCerrarModalTryOn.setOnClickListener(v -> modalVirtualTryOn.setVisibility(View.GONE));

        btnGuardarLookTryOn.setOnClickListener(v -> {
            if (currentTryOnBitmap != null) {
                String prendaNom = (prendaSuperiorSeleccionada != null) ? prendaSuperiorSeleccionada.nombre : "Outfit";
                guardarLookEnHistorial(currentTryOnBitmap, prendaNom);
            }
            guardarOutfitActual();
            Toast.makeText(this, "💾 Look guardado en tu historial", Toast.LENGTH_SHORT).show();
        });

        if (btnVerHistorialTryOn != null) {
            btnVerHistorialTryOn.setOnClickListener(v -> mostrarModalHistorial());
        }

        if (btnVestierHistorial != null) {
            btnVestierHistorial.setOnClickListener(v -> mostrarModalHistorial());
        }

        if (btnExportarGaleriaTryOn != null) {
            btnExportarGaleriaTryOn.setOnClickListener(v -> exportarLookAGaleria());
        }

        btnCompartirLookTryOn.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mi Look en Closet/Scan");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "✨ ¡Mira este look que me probé con Inteligencia Artificial en Closet/Scan!");
            startActivity(Intent.createChooser(shareIntent, "Compartir Outfit"));
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configurarVisorTryOnInteractivo() {
        if (imgResultadoTryOn == null) return;

        // 1. Zoom Táctil (Pinch-to-zoom) y Paneo (Drag)
        imgResultadoTryOn.setOnTouchListener((v, event) -> {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrixTryOn.set(matrixTryOn);
                    startPointTryOn.set(event.getX(), event.getY());
                    modeTryOn = TOUCH_DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDistTryOn = calcularDistancia(event);
                    if (oldDistTryOn > 10f) {
                        savedMatrixTryOn.set(matrixTryOn);
                        calcularPuntoMedio(midPointTryOn, event);
                        modeTryOn = TOUCH_ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    modeTryOn = TOUCH_NONE;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (modeTryOn == TOUCH_DRAG) {
                        matrixTryOn.set(savedMatrixTryOn);
                        float dx = event.getX() - startPointTryOn.x;
                        float dy = event.getY() - startPointTryOn.y;
                        matrixTryOn.postTranslate(dx, dy);
                    } else if (modeTryOn == TOUCH_ZOOM) {
                        float newDist = calcularDistancia(event);
                        if (newDist > 10f) {
                            matrixTryOn.set(savedMatrixTryOn);
                            float scale = newDist / oldDistTryOn;
                            matrixTryOn.postScale(scale, scale, midPointTryOn.x, midPointTryOn.y);
                        }
                    }
                    break;
            }
            imgResultadoTryOn.setImageMatrix(matrixTryOn);
            return true;
        });

        // 2. Comparador Antes / Después (Presionar = Foto Original, Soltar = Try-On IA)
        if (btnCompararAntesDespues != null) {
            btnCompararAntesDespues.setOnTouchListener((v, event) -> {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    cargarFotoOriginalEnVisor();
                    if (tvBadgeEstadoComparacion != null) {
                        tvBadgeEstadoComparacion.setText("📷 ANTES (FOTO ORIGINAL)");
                        tvBadgeEstadoComparacion.setTextColor(Color.parseColor("#FFAA00"));
                    }
                    btnCompararAntesDespues.setBackgroundColor(Color.parseColor("#994400"));
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (currentTryOnBitmap != null) {
                        imgResultadoTryOn.setImageBitmap(currentTryOnBitmap);
                        resetearZoomTryOn();
                    }
                    if (tvBadgeEstadoComparacion != null) {
                        tvBadgeEstadoComparacion.setText("✨ DESPUÉS (LOOK IA)");
                        tvBadgeEstadoComparacion.setTextColor(Color.parseColor("#80FF80"));
                    }
                    btnCompararAntesDespues.setBackgroundColor(Color.parseColor("#E6151522"));
                    return true;
                }
                return false;
            });
        }
    }

    private void mostrarBitmapEnVisorTryOn(Bitmap bmp) {
        if (bmp == null || imgResultadoTryOn == null) return;
        currentTryOnBitmap = bmp;
        imgResultadoTryOn.setImageBitmap(bmp);
        resetearZoomTryOn();
    }

    private void resetearZoomTryOn() {
        if (imgResultadoTryOn == null) return;
        imgResultadoTryOn.post(() -> {
            int viewW = imgResultadoTryOn.getWidth();
            int viewH = imgResultadoTryOn.getHeight();
            android.graphics.drawable.Drawable d = imgResultadoTryOn.getDrawable();
            if (viewW <= 0 || viewH <= 0 || d == null) return;
            int imgW = d.getIntrinsicWidth();
            int imgH = d.getIntrinsicHeight();
            if (imgW <= 0 || imgH <= 0) return;

            matrixTryOn.reset();
            float scale = Math.min((float) viewW / imgW, (float) viewH / imgH);
            float dx = (viewW - imgW * scale) / 2f;
            float dy = (viewH - imgH * scale) / 2f;
            matrixTryOn.postScale(scale, scale);
            matrixTryOn.postTranslate(dx, dy);
            imgResultadoTryOn.setImageMatrix(matrixTryOn);
        });
    }

    private void cargarFotoOriginalEnVisor() {
        if (rutaFotoUsuarioPersonal != null && !rutaFotoUsuarioPersonal.isEmpty()) {
            try {
                Bitmap bmpUser = null;
                if (rutaFotoUsuarioPersonal.startsWith("content://") || rutaFotoUsuarioPersonal.startsWith("file://")) {
                    java.io.InputStream is = getContentResolver().openInputStream(Uri.parse(rutaFotoUsuarioPersonal));
                    bmpUser = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                } else {
                    File f = new File(rutaFotoUsuarioPersonal);
                    if (f.exists()) bmpUser = BitmapFactory.decodeFile(f.getAbsolutePath());
                }
                if (bmpUser != null) {
                    imgResultadoTryOn.setImageBitmap(bmpUser);
                    resetearZoomTryOn();
                }
            } catch (Exception ignored) {}
        }
    }

    private float calcularDistancia(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void calcularPuntoMedio(PointF point, MotionEvent event) {
        if (event.getPointerCount() < 2) return;
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private void configurarModalAjustarFoto() {
        if (btnCerrarModalAjustarFoto != null) {
            btnCerrarModalAjustarFoto.setOnClickListener(v -> modalAjustarFotoUsuario.setVisibility(View.GONE));
        }

        if (btnZoomMas != null) {
            btnZoomMas.setOnClickListener(v -> {
                zoomFotoUsuario = Math.min(3.0f, zoomFotoUsuario + 0.2f);
                actualizarPreviewAjustarFoto();
            });
        }

        if (btnZoomMenos != null) {
            btnZoomMenos.setOnClickListener(v -> {
                zoomFotoUsuario = Math.max(1.0f, zoomFotoUsuario - 0.2f);
                actualizarPreviewAjustarFoto();
            });
        }

        if (btnMoverArriba != null) {
            btnMoverArriba.setOnClickListener(v -> {
                offsetYFotoUsuario -= 40f;
                actualizarPreviewAjustarFoto();
            });
        }

        if (btnMoverAbajo != null) {
            btnMoverAbajo.setOnClickListener(v -> {
                offsetYFotoUsuario += 40f;
                actualizarPreviewAjustarFoto();
            });
        }

        if (btnCentrarReset != null) {
            btnCentrarReset.setOnClickListener(v -> {
                zoomFotoUsuario = 1.0f;
                offsetYFotoUsuario = 0f;
                actualizarPreviewAjustarFoto();
            });
        }

        if (btnQuitarFondoFoto != null) {
            btnQuitarFondoFoto.setOnClickListener(v -> ejecutarQuitarFondoFoto());
        }

        if (btnConfirmarFotoAjustada != null) {
            btnConfirmarFotoAjustada.setOnClickListener(v -> confirmarYGuardarFotoAjustada());
        }
    }

    private void ejecutarQuitarFondoFoto() {
        if (bitmapOriginalFotoUsuario == null) return;

        btnQuitarFondoFoto.setEnabled(false);
        btnQuitarFondoFoto.setText("⏳ Quitando fondo en tu PC...");

        new Thread(() -> {
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bitmapOriginalFotoUsuario.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                String b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);

                org.json.JSONObject req = new org.json.JSONObject();
                req.put("image", "data:image/jpeg;base64," + b64);

                java.net.URL url = new java.net.URL(getBridgeUrl() + "/api/remove_bg");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(45000);

                conn.getOutputStream().write(req.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

                int code = conn.getResponseCode();
                java.io.InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String resp = s.hasNext() ? s.next() : "";
                conn.disconnect();

                org.json.JSONObject respJson = new org.json.JSONObject(resp);
                if ("success".equals(respJson.optString("status"))) {
                    String imgB64 = respJson.getString("image_base64");
                    String pure = imgB64.contains(",") ? imgB64.split(",")[1] : imgB64;
                    byte[] bytes = android.util.Base64.decode(pure, android.util.Base64.DEFAULT);
                    Bitmap sinFondo = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    runOnUiThread(() -> {
                        btnQuitarFondoFoto.setEnabled(true);
                        btnQuitarFondoFoto.setText("🪄 FONDO DE HABITACIÓN ELIMINADO");
                        if (sinFondo != null) {
                            bitmapOriginalFotoUsuario = sinFondo;
                            actualizarPreviewAjustarFoto();
                            Toast.makeText(this, "🪄 ¡Fondo eliminado! Ahora saldrás nítido sin paredes", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    String err = respJson.optString("message", "Error del servidor");
                    runOnUiThread(() -> {
                        btnQuitarFondoFoto.setEnabled(true);
                        btnQuitarFondoFoto.setText("🪄 QUITAR FONDO DEL CUARTO (IA)");
                        Toast.makeText(this, "Error: " + err, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnQuitarFondoFoto.setEnabled(true);
                    btnQuitarFondoFoto.setText("🪄 QUITAR FONDO DEL CUARTO (IA)");
                    Toast.makeText(this, "Asegúrate de que 'server_bridge.py' esté activo en tu PC: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void configurarModalHistorial() {
        if (btnCerrarModalHistorial != null) {
            btnCerrarModalHistorial.setOnClickListener(v -> modalHistorialLooks.setVisibility(View.GONE));
        }
    }

    private void abrirEditorAjustarFoto(Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            bitmapOriginalFotoUsuario = android.graphics.BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmapOriginalFotoUsuario == null) {
                Toast.makeText(this, "No se pudo leer la imagen seleccionada", Toast.LENGTH_SHORT).show();
                return;
            }

            zoomFotoUsuario = 1.0f;
            offsetYFotoUsuario = 0f;
            modalAjustarFotoUsuario.setVisibility(View.VISIBLE);
            imgAjustarFotoPreview.post(this::actualizarPreviewAjustarFoto);
        } catch (Exception e) {
            AppLogger.e("FOTO", "Error al abrir editor de foto: " + e.getMessage());
            Toast.makeText(this, "Error al cargar la foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarPreviewAjustarFoto() {
        if (bitmapOriginalFotoUsuario == null || imgAjustarFotoPreview == null) return;

        if (tvZoomNivel != null) {
            tvZoomNivel.setText(String.format(java.util.Locale.US, "Zoom: %.1fx", zoomFotoUsuario));
        }

        int viewW = imgAjustarFotoPreview.getWidth();
        int viewH = imgAjustarFotoPreview.getHeight();
        if (viewW <= 0) viewW = 750;
        if (viewH <= 0) viewH = 1000;

        float bmpW = bitmapOriginalFotoUsuario.getWidth();
        float bmpH = bitmapOriginalFotoUsuario.getHeight();

        float scaleBase = Math.max((float) viewW / bmpW, (float) viewH / bmpH);
        float scaleTotal = scaleBase * zoomFotoUsuario;

        android.graphics.Matrix matrix = new android.graphics.Matrix();
        float tx = (viewW - bmpW * scaleTotal) / 2f;
        float ty = (viewH - bmpH * scaleTotal) / 2f + offsetYFotoUsuario;

        matrix.postScale(scaleTotal, scaleTotal);
        matrix.postTranslate(tx, ty);

        imgAjustarFotoPreview.setImageMatrix(matrix);
        imgAjustarFotoPreview.setImageBitmap(bitmapOriginalFotoUsuario);
    }

    private void confirmarYGuardarFotoAjustada() {
        if (bitmapOriginalFotoUsuario == null) return;

        try {
            int targetW = 768;
            int targetH = 1024;
            Bitmap croppedBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(croppedBitmap);

            float bmpW = bitmapOriginalFotoUsuario.getWidth();
            float bmpH = bitmapOriginalFotoUsuario.getHeight();

            float scaleBase = Math.max((float) targetW / bmpW, (float) targetH / bmpH);
            float scaleTotal = scaleBase * zoomFotoUsuario;

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            float tx = (targetW - bmpW * scaleTotal) / 2f;
            float ty = (targetH - bmpH * scaleTotal) / 2f + (offsetYFotoUsuario * (targetH / 1000f));
            matrix.postScale(scaleTotal, scaleTotal);
            matrix.postTranslate(tx, ty);

            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(bitmapOriginalFotoUsuario, matrix, paint);

            File dir = new File(getFilesDir(), "perfil");
            if (!dir.exists()) dir.mkdirs();
            File archivoFinal = new File(dir, "perfil_usuario_" + System.currentTimeMillis() + ".jpg");

            java.io.FileOutputStream fos = new java.io.FileOutputStream(archivoFinal);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
            fos.flush();
            fos.close();

            guardarFotoUsuario(archivoFinal.getAbsolutePath());
            modalAjustarFotoUsuario.setVisibility(View.GONE);
            Toast.makeText(this, "✨ Foto encuadrada y guardada en tu Vestier", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            AppLogger.e("FOTO", "Error al recortar y guardar foto: " + e.getMessage());
            Toast.makeText(this, "Error al procesar el recorte: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarLookEnHistorial(Bitmap bitmap, String nombrePrenda) {
        if (bitmap == null) return;
        new Thread(() -> {
            try {
                File dir = new File(getFilesDir(), "looks_historial");
                if (!dir.exists()) dir.mkdirs();
                String cleanName = (nombrePrenda != null) ? nombrePrenda.replaceAll("[^a-zA-Z0-9]", "_") : "Outfit";
                String fileName = "look_" + System.currentTimeMillis() + "_" + cleanName + ".jpg";
                File file = new File(dir, fileName);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                fos.flush();
                fos.close();
                AppLogger.i("HISTORIAL", "Look guardado en historial: " + file.getAbsolutePath());
            } catch (Exception e) {
                AppLogger.e("HISTORIAL", "Error al guardar look: " + e.getMessage());
            }
        }).start();
    }

    private void mostrarModalHistorial() {
        if (llListaHistorialLooks == null || modalHistorialLooks == null) return;
        llListaHistorialLooks.removeAllViews();

        File dir = new File(getFilesDir(), "looks_historial");
        File[] files = (dir.exists()) ? dir.listFiles((d, name) -> name.endsWith(".jpg") || name.endsWith(".png")) : null;

        if (files == null || files.length == 0) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("Aún no tienes looks en tu historial.\n¡Prueba una prenda con IA para verla aquí!");
            tvVacio.setTextColor(android.graphics.Color.parseColor("#888899"));
            tvVacio.setTextSize(13);
            tvVacio.setGravity(android.view.Gravity.CENTER);
            tvVacio.setPadding(24, 80, 24, 80);
            llListaHistorialLooks.addView(tvVacio);
        } else {
            java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

            for (File file : files) {
                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 16);
                card.setLayoutParams(cardParams);
                card.setCardBackgroundColor(android.graphics.Color.parseColor("#171724"));
                card.setRadius(24);
                card.setStrokeColor(android.graphics.Color.parseColor("#2A2A3E"));
                card.setStrokeWidth(2);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(14, 14, 14, 14);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                ImageView imgThumb = new ImageView(this);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(160, 210);
                imgParams.setMargins(0, 0, 16, 0);
                imgThumb.setLayoutParams(imgParams);
                imgThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgThumb.setImageURI(Uri.fromFile(file));

                LinearLayout infoCol = new LinearLayout(this);
                infoCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                infoCol.setLayoutParams(infoParams);

                TextView tvTitulo = new TextView(this);
                tvTitulo.setText("✨ Look Fotorrealista");
                tvTitulo.setTextColor(android.graphics.Color.WHITE);
                tvTitulo.setTextSize(13);
                tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
                String fechaStr = sdf.format(new java.util.Date(file.lastModified()));

                TextView tvFecha = new TextView(this);
                tvFecha.setText(fechaStr);
                tvFecha.setTextColor(android.graphics.Color.parseColor("#9999AA"));
                tvFecha.setTextSize(11);
                tvFecha.setPadding(0, 4, 0, 8);

                com.google.android.material.button.MaterialButton btnAbrir = new com.google.android.material.button.MaterialButton(this);
                btnAbrir.setText("VER EN GRANDE");
                btnAbrir.setTextSize(10);
                btnAbrir.setTextColor(android.graphics.Color.BLACK);
                btnAbrir.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                btnAbrir.setCornerRadius(18);
                btnAbrir.setOnClickListener(v -> {
                    modalHistorialLooks.setVisibility(View.GONE);
                    Bitmap bmp = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bmp != null) {
                        mostrarBitmapEnVisorTryOn(bmp);
                        llResultadoIA.setVisibility(View.GONE);
                        modalVirtualTryOn.setVisibility(View.VISIBLE);
                    }
                });

                infoCol.addView(tvTitulo);
                infoCol.addView(tvFecha);
                infoCol.addView(btnAbrir);

                row.addView(imgThumb);
                row.addView(infoCol);
                card.addView(row);
                llListaHistorialLooks.addView(card);
            }
        }

        modalHistorialLooks.setVisibility(View.VISIBLE);
    }

    // ─── ASESOR & RECOMENDACIONES (HakiForge Match) ───────────────────────────
    private void configurarAsesor() {
        if (btnRefrescarAsesor != null) {
            btnRefrescarAsesor.setOnClickListener(v -> generarNuevoMatchAsesor());
        }
        if (btnProbarLookEnVestier != null) {
            btnProbarLookEnVestier.setOnClickListener(v -> aplicarSugerenciaEnVestier());
        }
        cargarOfertasTiendas();
    }

    private void actualizarAsesorUI() {
        if (sugerenciaSupActual == null) {
            generarNuevoMatchAsesor();
        }
    }

    private void generarNuevoMatchAsesor() {
        List<PrendaEntity> superiores = new ArrayList<>();
        List<PrendaEntity> inferiores = new ArrayList<>();
        List<PrendaEntity> calzadoOAcc = new ArrayList<>();

        for (PrendaEntity p : todasLasPrendas) {
            if (p.categoria == null) continue;
            String cat = p.categoria.toUpperCase();
            if (cat.contains("SUPERIOR") || cat.contains("ABRIGO")) superiores.add(p);
            else if (cat.contains("INFERIOR")) inferiores.add(p);
            else if (cat.contains("CALZADO")) calzadoOAcc.add(p);
        }

        Random rnd = new Random();
        if (!superiores.isEmpty()) {
            sugerenciaSupActual = superiores.get(rnd.nextInt(superiores.size()));
        } else {
            sugerenciaSupActual = new PrendaEntity("Polo Marino", "Superior", "Casual", "#000080", "Azul marino", null, null);
        }

        String colSup = (sugerenciaSupActual.colorNombre != null) ? sugerenciaSupActual.colorNombre : "Azul marino";

        // Búsqueda inteligente: encontrar la prenda inferior del armario con mejor armonía cromática
        if (!inferiores.isEmpty()) {
            PrendaEntity mejorInf = inferiores.get(0);
            int mejorAfinidad = -1;
            for (PrendaEntity inf : inferiores) {
                String cInf = (inf.colorNombre != null) ? inf.colorNombre : "Negro";
                int score = RecomendadorEngine.calcularAfinidad(colSup, cInf);
                if (score > mejorAfinidad) {
                    mejorAfinidad = score;
                    mejorInf = inf;
                }
            }
            sugerenciaInfActual = mejorInf;
        } else {
            sugerenciaInfActual = new PrendaEntity("Pantalón Caqui", "Inferior", "Casual", "#C3B091", "Beige", null, null);
        }

        // Búsqueda de calzado que complemente el conjunto
        String colInf = (sugerenciaInfActual.colorNombre != null) ? sugerenciaInfActual.colorNombre : "Beige";
        if (!calzadoOAcc.isEmpty()) {
            PrendaEntity mejorCal = calzadoOAcc.get(0);
            int mejorScore = -1;
            for (PrendaEntity cal : calzadoOAcc) {
                String cCal = (cal.colorNombre != null) ? cal.colorNombre : "Blanco";
                int score = RecomendadorEngine.calcularAfinidadCompleta(colSup, colInf, cCal);
                if (score > mejorScore) {
                    mejorScore = score;
                    mejorCal = cal;
                }
            }
            sugerenciaCalActual = mejorCal;
        } else {
            sugerenciaCalActual = new PrendaEntity("Tenis Blancos", "Calzado", "Deportivo", "#FFFFFF", "Blanco", null, null);
        }

        String colCal = (sugerenciaCalActual.colorNombre != null) ? sugerenciaCalActual.colorNombre : "Blanco";
        // Afinidad 100% real calculada matemáticamente sin números inventados ni hardcodeados
        int afinidad = RecomendadorEngine.calcularAfinidadCompleta(colSup, colInf, colCal);

        if (tvBadgePorcentajeAsesor != null) {
            tvBadgePorcentajeAsesor.setText(afinidad + "%");
        }

        if (tvSugeridoSupNombre != null) {
            tvSugeridoSupNombre.setText(sugerenciaSupActual.nombre != null ? sugerenciaSupActual.nombre : "Polo Marino");
        }
        if (tvSugeridoInfNombre != null) {
            tvSugeridoInfNombre.setText(sugerenciaInfActual.nombre != null ? sugerenciaInfActual.nombre : "Pantalón Caqui");
        }
        if (tvSugeridoCalNombre != null) {
            tvSugeridoCalNombre.setText(sugerenciaCalActual.nombre != null ? sugerenciaCalActual.nombre : "Tenis Blancos");
        }

        // Cargar preview fotográfico si existe foto
        boolean fotoCargada = false;
        if (sugerenciaSupActual.rutaFotoOriginal != null && !sugerenciaSupActual.rutaFotoOriginal.isEmpty()) {
            try {
                File f = new File(sugerenciaSupActual.rutaFotoOriginal);
                if (f.exists()) {
                    imgPreviewOutfitSugerido.setImageURI(Uri.fromFile(f));
                    fotoCargada = true;
                } else if (sugerenciaSupActual.rutaFotoOriginal.startsWith("content://") || sugerenciaSupActual.rutaFotoOriginal.startsWith("file://")) {
                    imgPreviewOutfitSugerido.setImageURI(Uri.parse(sugerenciaSupActual.rutaFotoOriginal));
                    fotoCargada = true;
                }
            } catch (Exception ignored) {}
        }
        if (fotoCargada) {
            imgPreviewOutfitSugerido.setVisibility(View.VISIBLE);
            if (tvPlaceholderOutfitSugerido != null) tvPlaceholderOutfitSugerido.setVisibility(View.GONE);
        } else {
            imgPreviewOutfitSugerido.setVisibility(View.GONE);
            if (tvPlaceholderOutfitSugerido != null) tvPlaceholderOutfitSugerido.setVisibility(View.VISIBLE);
        }

        AppLogger.i("ASESOR", "Nuevo outfit sugerido generado: " + sugerenciaSupActual.nombre + " + " + sugerenciaInfActual.nombre + " (" + afinidad + "%)");
    }

    private void aplicarSugerenciaEnVestier() {
        if (sugerenciaSupActual != null) prendaSuperiorSeleccionada = sugerenciaSupActual;
        if (sugerenciaInfActual != null) prendaInferiorSeleccionada = sugerenciaInfActual;
        if (sugerenciaCalActual != null) prendaCalzadoSeleccionada = sugerenciaCalActual;
        cambiarTab("VESTIER");
        Toast.makeText(this, "✨ Look sugerido cargado en tu Vestier", Toast.LENGTH_SHORT).show();
    }

    private static class OfertaTienda {
        String tienda;
        String prenda;
        String categoria;
        String precioOriginal;
        String precioOferta;
        String descuento;
        String icono;
        String urlTienda;

        OfertaTienda(String tienda, String prenda, String categoria, String precioOriginal, String precioOferta, String descuento, String icono, String urlTienda) {
            this.tienda = tienda;
            this.prenda = prenda;
            this.categoria = categoria;
            this.precioOriginal = precioOriginal;
            this.precioOferta = precioOferta;
            this.descuento = descuento;
            this.icono = icono;
            this.urlTienda = urlTienda;
        }
    }

    private void cargarOfertasTiendas() {
        if (llContenedorOfertasTiendas == null) return;
        llContenedorOfertasTiendas.removeAllViews();

        List<OfertaTienda> ofertas = new ArrayList<>();
        ofertas.add(new OfertaTienda(
                "Zara",
                "Chaqueta bomber",
                "Abrigo",
                "$129.900",
                "$89.900",
                "-31%",
                "🧥",
                "https://www.zara.com/co/es/search?searchTerm=chaqueta%20bomber"
        ));
        ofertas.add(new OfertaTienda(
                "Pull & Bear",
                "Mocasines cuero",
                "Calzado",
                "$149.900",
                "$104.900",
                "-30%",
                "👞",
                "https://www.pullandbear.com/co/hombre/calzado/mocasines-n6464"
        ));
        ofertas.add(new OfertaTienda(
                "Bershka",
                "Sobrecamisa Pana",
                "Superior",
                "$119.900",
                "$79.900",
                "-33%",
                "👔",
                "https://www.bershka.com/co/hombre/ropa/camisas-c1010193239.html"
        ));
        ofertas.add(new OfertaTienda(
                "H&M",
                "Chaqueta Denim Oversize",
                "Superior",
                "$149.900",
                "$99.900",
                "-33%",
                "🧥",
                "https://co.hm.com/hombre/ropa/chaquetas"
        ));

        for (OfertaTienda of : ofertas) {
            View card = getLayoutInflater().inflate(R.layout.item_oferta_tienda, llContenedorOfertasTiendas, false);

            TextView tvIcono = card.findViewById(R.id.tvIconoOfertaTienda);
            TextView tvTienda = card.findViewById(R.id.tvNombreTienda);
            TextView tvPrenda = card.findViewById(R.id.tvPrendaOfertaTienda);
            TextView tvPrecioTachado = card.findViewById(R.id.tvPrecioTachado);
            TextView tvPrecioOferta = card.findViewById(R.id.tvPrecioOferta);
            TextView tvBadge = card.findViewById(R.id.tvBadgeDescuento);
            MaterialButton btnVerWeb = card.findViewById(R.id.btnVerEnTiendaWeb);
            MaterialButton btnProbar = card.findViewById(R.id.btnProbarOfertaEnVestier);

            tvIcono.setText(of.icono);
            tvTienda.setText(of.tienda);
            tvPrenda.setText(of.prenda);
            tvPrecioTachado.setText(of.precioOriginal);
            tvPrecioTachado.setPaintFlags(tvPrecioTachado.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvPrecioOferta.setText(of.precioOferta);
            tvBadge.setText(of.descuento);

            // Botón 1: Abrir la tienda oficial real en navegador o app externa
            btnVerWeb.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(of.urlTienda));
                    startActivity(intent);
                    AppLogger.i("OFERTAS", "Abriendo tienda oficial: " + of.tienda + " (" + of.urlTienda + ")");
                } catch (Exception e) {
                    Toast.makeText(this, "No se pudo abrir la página de la tienda", Toast.LENGTH_SHORT).show();
                }
            });

            // Botón 2: Probar la prenda de la tienda en el Vestier Digital propio
            btnProbar.setOnClickListener(v -> {
                PrendaEntity prendaTienda = new PrendaEntity(
                        of.prenda + " (" + of.tienda + ")",
                        of.categoria,
                        "Casual",
                        "#111111",
                        "Negro",
                        null,
                        null
                );

                String cat = of.categoria.toUpperCase();
                if (cat.contains("ABRIGO") || cat.contains("SUPERIOR")) {
                    prendaSuperiorSeleccionada = prendaTienda;
                } else if (cat.contains("CALZADO")) {
                    prendaCalzadoSeleccionada = prendaTienda;
                } else {
                    prendaInferiorSeleccionada = prendaTienda;
                }

                cambiarTab("VESTIER");
                AppLogger.i("OFERTAS", "Prenda de tienda probada en Vestier: " + of.tienda + " " + of.prenda);
                Toast.makeText(this, "🛍️ Probando " + of.prenda + " de " + of.tienda + " en tu Vestier", Toast.LENGTH_SHORT).show();
            });

            llContenedorOfertasTiendas.addView(card);
        }
    }

    /**
     * Copia un URI de galería/almacenamiento a un archivo privado permanente en getFilesDir().
     * Esto soluciona de raíz los problemas de SecurityException y permisos caducados en Android 10+.
     */
    private String copiarUriAArchivoLocal(Uri uri, String prefijo) {
        if (uri == null) return null;
        try {
            File dir = new File(getFilesDir(), "imagenes");
            if (!dir.exists()) dir.mkdirs();
            File destino = new File(dir, prefijo + "_" + System.currentTimeMillis() + ".jpg");

            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(destino)) {
                if (is == null) {
                    AppLogger.e("IMG", "No se pudo abrir InputStream para URI: " + uri);
                    return null;
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
            }
            AppLogger.foto("Foto copiada exitosamente a archivo local: " + destino.getName() + " (" + (destino.length() / 1024) + " KB)");
            return destino.getAbsolutePath();
        } catch (Exception e) {
            AppLogger.e("IMG", "Fallo al copiar imagen seleccionada", e);
            Toast.makeText(this, "No se pudo procesar la foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ─── HELPERS DE EXTRACCIÓN DE COLOR, REcorte DE PRENDA Y EXPORTACIÓN ───
    private static class ColorInfo {
        String hex;
        String nombre;
        ColorInfo(String hex, String nombre) {
            this.hex = hex;
            this.nombre = nombre;
        }
    }

    private ColorInfo detectarColorDominante(String rutaFoto) {
        if (rutaFoto == null || rutaFoto.isEmpty()) {
            return new ColorInfo("#FFFFFF", "Blanco");
        }
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 8;
            Bitmap bmp = null;
            if (rutaFoto.startsWith("content://") || rutaFoto.startsWith("file://")) {
                InputStream is = getContentResolver().openInputStream(Uri.parse(rutaFoto));
                bmp = BitmapFactory.decodeStream(is, null, opts);
                if (is != null) is.close();
            } else {
                bmp = BitmapFactory.decodeFile(rutaFoto, opts);
            }

            if (bmp == null) return new ColorInfo("#FFFFFF", "Blanco");

            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int startX = (int) (width * 0.2);
            int endX = (int) (width * 0.8);
            int startY = (int) (height * 0.2);
            int endY = (int) (height * 0.8);

            long totalR = 0, totalG = 0, totalB = 0;
            int count = 0;

            for (int y = startY; y < endY; y += 3) {
                for (int x = startX; x < endX; x += 3) {
                    int pixel = bmp.getPixel(x, y);
                    int a = Color.alpha(pixel);
                    if (a > 120) {
                        totalR += Color.red(pixel);
                        totalG += Color.green(pixel);
                        totalB += Color.blue(pixel);
                        count++;
                    }
                }
            }
            bmp.recycle();

            if (count == 0) return new ColorInfo("#FFFFFF", "Blanco");

            int avgR = (int) (totalR / count);
            int avgG = (int) (totalG / count);
            int avgB = (int) (totalB / count);

            String hex = String.format("#%02X%02X%02X", avgR, avgG, avgB);
            String nombre = clasificarColorNombre(avgR, avgG, avgB);
            AppLogger.i("COLOR", "Dominante detectado: " + nombre + " (" + hex + ") en " + count + " muestras");
            return new ColorInfo(hex, nombre);
        } catch (Exception e) {
            AppLogger.e("COLOR", "Error extrayendo color dominante", e);
            return new ColorInfo("#FFFFFF", "Blanco");
        }
    }

    private String clasificarColorNombre(int r, int g, int b) {
        float[] hsv = new float[3];
        Color.RGBToHSV(r, g, b, hsv);
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];

        if (v < 0.18f) return "Negro";
        if (s < 0.12f && v > 0.82f) return "Blanco";
        if (s < 0.15f) return "Gris";

        if (h >= 15 && h <= 45 && s < 0.5f && v > 0.65f) return "Beige";
        if (h >= 15 && h <= 45 && v < 0.5f) return "Marrón";

        if (h < 15 || h >= 345) {
            return (v < 0.5f) ? "Borgoña" : "Rojo";
        } else if (h < 45) {
            return "Naranja";
        } else if (h < 70) {
            return "Amarillo";
        } else if (h < 160) {
            return (v < 0.45f) ? "Verde Militar" : "Verde";
        } else if (h < 260) {
            return (v < 0.45f) ? "Azul Marino" : "Azul";
        } else if (h < 300) {
            return "Morado";
        } else {
            return "Rosado";
        }
    }

    private void ejecutarQuitarFondoPrenda() {
        if (rutaFotoTemporal == null || rutaFotoTemporal.isEmpty()) {
            Toast.makeText(this, "No hay foto de prenda para recortar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnQuitarFondoPrenda != null) {
            btnQuitarFondoPrenda.setEnabled(false);
            btnQuitarFondoPrenda.setText("⏳ Quitando fondo en tu PC...");
        }

        new Thread(() -> {
            try {
                Bitmap bmpOriginal = null;
                if (rutaFotoTemporal.startsWith("content://") || rutaFotoTemporal.startsWith("file://")) {
                    InputStream is = getContentResolver().openInputStream(Uri.parse(rutaFotoTemporal));
                    bmpOriginal = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                } else {
                    bmpOriginal = BitmapFactory.decodeFile(rutaFotoTemporal);
                }

                if (bmpOriginal == null) {
                    runOnUiThread(() -> {
                        if (btnQuitarFondoPrenda != null) {
                            btnQuitarFondoPrenda.setEnabled(true);
                            btnQuitarFondoPrenda.setText("🪄 QUITAR FONDO A LA PRENDA (IA)");
                        }
                        Toast.makeText(this, "No se pudo leer la foto de la prenda", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                int maxDim = 1024;
                int origW = bmpOriginal.getWidth();
                int origH = bmpOriginal.getHeight();
                float scale = Math.min(1.0f, (float) maxDim / Math.max(origW, origH));
                Bitmap bmpScaled = Bitmap.createScaledBitmap(bmpOriginal, Math.round(origW * scale), Math.round(origH * scale), true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmpScaled.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                byte[] bytes = baos.toByteArray();
                String base64Img = Base64.encodeToString(bytes, Base64.NO_WRAP);

                JSONObject req = new JSONObject();
                req.put("image", "data:image/jpeg;base64," + base64Img);

                java.net.URL url = new java.net.URL(getBridgeUrl() + "/api/remove_bg");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(45000);

                conn.getOutputStream().write(req.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String resp = s.hasNext() ? s.next() : "";
                conn.disconnect();

                JSONObject respJson = new JSONObject(resp);
                if ("success".equals(respJson.optString("status"))) {
                    String imgB64 = respJson.getString("image_base64");
                    String pure = imgB64.contains(",") ? imgB64.split(",")[1] : imgB64;
                    byte[] resultBytes = Base64.decode(pure, Base64.DEFAULT);
                    Bitmap bmpClean = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);

                    File dirPrendas = new File(getFilesDir(), "prendas_sin_fondo");
                    if (!dirPrendas.exists()) dirPrendas.mkdirs();
                    File cleanFile = new File(dirPrendas, "prenda_clean_" + System.currentTimeMillis() + ".png");
                    FileOutputStream fos = new FileOutputStream(cleanFile);
                    bmpClean.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();

                    rutaFotoTemporal = cleanFile.getAbsolutePath();
                    ColorInfo ci = detectarColorDominante(rutaFotoTemporal);
                    colorDetectadoHex = ci.hex;
                    colorDetectadoNombre = ci.nombre;

                    runOnUiThread(() -> {
                        if (btnQuitarFondoPrenda != null) {
                            btnQuitarFondoPrenda.setEnabled(true);
                            btnQuitarFondoPrenda.setText("🪄 FONDO DE PRENDA ELIMINADO");
                        }
                        imgFotoFichaReal.setImageBitmap(bmpClean);
                        imgFotoFichaReal.setVisibility(View.VISIBLE);
                        tvFotoFichaTexto.setVisibility(View.GONE);
                        tvColorBadgeFicha.setText("Color: " + colorDetectadoNombre);
                        Toast.makeText(this, "🪄 ¡Fondo de prenda eliminado con éxito!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    String err = respJson.optString("message", "Error del servidor");
                    runOnUiThread(() -> {
                        if (btnQuitarFondoPrenda != null) {
                            btnQuitarFondoPrenda.setEnabled(true);
                            btnQuitarFondoPrenda.setText("🪄 QUITAR FONDO A LA PRENDA (IA)");
                        }
                        Toast.makeText(this, "Error: " + err, Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                AppLogger.e("REMOVE_BG", "Error al quitar fondo a prenda", e);
                runOnUiThread(() -> {
                    if (btnQuitarFondoPrenda != null) {
                        btnQuitarFondoPrenda.setEnabled(true);
                        btnQuitarFondoPrenda.setText("🪄 QUITAR FONDO A LA PRENDA (IA)");
                    }
                    Toast.makeText(this, "No se pudo conectar al Bridge local (asegúrate que server_bridge.py esté corriendo en tu PC)", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void exportarLookAGaleria() {
        if (currentTryOnBitmap == null) {
            Toast.makeText(this, "No hay imagen de Try-On para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String filename = "ClosetScan_Look_" + System.currentTimeMillis() + ".jpg";
            OutputStream fos;
            Uri imageUri = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ClosetScan");

                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri);
                } else {
                    throw new IOException("Error creando registro en MediaStore");
                }
            } else {
                File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File csDir = new File(imagesDir, "ClosetScan");
                if (!csDir.exists()) csDir.mkdirs();
                File imageFile = new File(csDir, filename);
                fos = new FileOutputStream(imageFile);
                imageUri = Uri.fromFile(imageFile);
            }

            if (fos != null) {
                currentTryOnBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.flush();
                fos.close();

                if (imageUri != null) {
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(imageUri);
                    sendBroadcast(scanIntent);
                }

                Toast.makeText(this, "📸 ¡Look guardado en tu Galería de Fotos!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            AppLogger.e("GALLERY", "Error exportando a galería", e);
            Toast.makeText(this, "Error guardando en galería: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarMiniaturaEnImageView(ImageView img, String ruta) {
        if (ruta == null || ruta.isEmpty()) return;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 8;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bmp = null;
            if (ruta.startsWith("content://") || ruta.startsWith("file://")) {
                InputStream is = getContentResolver().openInputStream(Uri.parse(ruta));
                bmp = BitmapFactory.decodeStream(is, null, opts);
                if (is != null) is.close();
            } else {
                bmp = BitmapFactory.decodeFile(ruta, opts);
            }
            if (bmp != null) {
                img.setImageBitmap(bmp);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camaraManager != null) {
            camaraManager.release();
        }
    }
}