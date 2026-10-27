// [FILENAME: GraficoDonaView.java]
package com.hakimik.closetscan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GraficoDonaView
 * <p>
 * Gráfico circular de dona (Donut Chart) dibujado a mano sobre {@link Canvas#drawArc} con
 * {@link Paint.Style#STROKE}, en paleta Noir monocromática.
 * <p>
 * Uso típico (desde un Fragment que observa un LiveData<Map<String,Integer>>):
 * <pre>
 *   graficoDona.setData(distribucionEstilos);   // { "Casual": 12, "Formal": 5, ... }
 * </pre>
 * La vista NO conoce Room ni ViewModel: solo recibe el mapa ya agregado.
 * <p>
 * <b>Hilo:</b> {@link #setData(Map)} puede invocarse desde cualquier hilo; internamente decide
 * entre {@code invalidate()} (hilo UI) y {@code postInvalidate()} (cualquier otro hilo).
 */
public class GraficoDonaView extends View {

    // ------------------------------------------------------------------------------------
    // Paleta Noir
    // ------------------------------------------------------------------------------------

    /** Blanco puro. */
    @ColorInt public static final int NOIR_BLANCO = 0xFFFFFFFF;
    /** Gris claro. */
    @ColorInt public static final int NOIR_GRIS_CLARO = 0xFFCCCCCC;
    /** Gris medio. */
    @ColorInt public static final int NOIR_GRIS_MEDIO = 0xFF888888;
    /** Gris oscuro (también usado como pista del anillo vacío). */
    @ColorInt public static final int NOIR_GRIS_OSCURO = 0xFF444444;

    /** Orden de asignación de colores a los sectores. */
    private static final int[] PALETA_NOIR = {
            NOIR_BLANCO,
            NOIR_GRIS_CLARO,
            NOIR_GRIS_MEDIO,
            NOIR_GRIS_OSCURO
    };

    // ------------------------------------------------------------------------------------
    // Constantes de geometría
    // ------------------------------------------------------------------------------------

    private static final float ANGULO_INICIO = -90f;   // 12 en punto
    private static final float ANGULO_TOTAL = 360f;
    private static final float GROSOR_ANILLO_DP = 24f;
    private static final float TAMANO_POR_DEFECTO_DP = 200f;

    // ------------------------------------------------------------------------------------
    // Estado de dibujo
    // ------------------------------------------------------------------------------------

    private final Paint paintSector = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();               // reutilizado: cero allocations por frame

    private final List<Segmento> segmentos = new ArrayList<>();
    private int totalPrendas = 0;
    private boolean vacio = true;

    // ------------------------------------------------------------------------------------
    // Constructores
    // ------------------------------------------------------------------------------------

    public GraficoDonaView(Context context) {
        super(context);
        init();
    }

    public GraficoDonaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GraficoDonaView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float grosor = GROSOR_ANILLO_DP * getResources().getDisplayMetrics().density;

        paintSector.setStyle(Paint.Style.STROKE);
        paintSector.setStrokeCap(Paint.Cap.BUTT);
        paintSector.setStrokeWidth(grosor);
        paintSector.setAntiAlias(true);

        paintPista.setStyle(Paint.Style.STROKE);
        paintPista.setStrokeCap(Paint.Cap.BUTT);
        paintPista.setStrokeWidth(grosor);
        paintPista.setColor(NOIR_GRIS_OSCURO);
        paintPista.setAntiAlias(true);

        setWillNotDraw(false);
    }

    // ------------------------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------------------------

    /**
     * Carga la distribución de estilos del armario y redibuja el gráfico.
     * <p>
     * Calcula porcentajes y ángulos de barrido proporcionalmente al total. Las entradas
     * {@code null} o con valor {@code <= 0} se ignoran. El último sector se ajusta para que
     * la suma de barridos sea exactamente {@code 360°} (evita huecos por redondeo float).
     *
     * @param distribucionEstilos mapa {@code estilo -> cantidad de prendas}. Puede ser {@code null}.
     */
    public void setData(@Nullable Map<String, Integer> distribucionEstilos) {
        segmentos.clear();
        totalPrendas = 0;

        // 1) Copia defensiva, sin entradas sin ángulo posible, conservando el orden de inserción.
        Map<String, Integer> validos = new LinkedHashMap<>();
        if (distribucionEstilos != null) {
            for (Map.Entry<String, Integer> entrada : distribucionEstilos.entrySet()) {
                if (entrada == null) continue;
                String estilo = entrada.getKey();
                Integer cantidad = entrada.getValue();
                if (estilo == null || estilo.trim().isEmpty()) continue;
                if (cantidad == null || cantidad <= 0) continue;
                validos.put(estilo, cantidad);
            }
        }

        // 2) Total del armario.
        int total = 0;
        for (Integer cantidad : validos.values()) {
            total += cantidad;
        }
        totalPrendas = total;

        // 3) Cálculo dinámico de porcentajes y ángulos.
        if (total > 0) {
            final int numeroSectores = validos.size();
            int indice = 0;
            float acumulado = 0f;

            for (Map.Entry<String, Integer> entrada : validos.entrySet()) {
                indice++;
                int cantidad = entrada.getValue();
                float porcentaje = (cantidad * 100f) / total;

                float barrido;
                if (indice == numeroSectores) {
                    barrido = ANGULO_TOTAL - acumulado;   // cierre exacto del anillo
                } else {
                    barrido = ANGULO_TOTAL * (cantidad / (float) total);
                }

                float inicio = ANGULO_INICIO + acumulado;
                acumulado += barrido;

                int color = PALETA_NOIR[(indice - 1) % PALETA_NOIR.length];
                segmentos.add(new Segmento(entrada.getKey(), cantidad, porcentaje, inicio, barrido, color));
            }
        }

        vacio = segmentos.isEmpty();
        solicitarRedibujado();
    }

    /** @return total de prendas representadas (0 si no hay datos válidos). */
    public int getTotalPrendas() {
        return totalPrendas;
    }

    /** @return {@code true} si no hay datos que dibujar (se pinta solo la pista del anillo). */
    public boolean estaVacio() {
        return vacio;
    }

    /**
     * @param estilo nombre del estilo (comparación case-insensitive)
     * @return porcentaje 0..100 que representa ese estilo, o {@code 0f} si no existe.
     */
    public float getPorcentaje(@NonNull String estilo) {
        for (Segmento segmento : segmentos) {
            if (estilo.equalsIgnoreCase(segmento.getEtiqueta())) {
                return segmento.getPorcentaje();
            }
        }
        return 0f;
    }

    /** @return lista inmutable de sectores, útil para pintar la leyenda desde la UI. */
    @NonNull
    public List<Segmento> getSegmentos() {
        return Collections.unmodifiableList(segmentos);
    }

    // ------------------------------------------------------------------------------------
    // Medición y dibujo
    // ------------------------------------------------------------------------------------

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int tamanoDefecto = Math.round(TAMANO_POR_DEFECTO_DP * getResources().getDisplayMetrics().density);
        int ancho = resolverDimension(widthMeasureSpec, tamanoDefecto);
        int alto = resolverDimension(heightMeasureSpec, tamanoDefecto);
        setMeasuredDimension(ancho, alto);
    }

    private static int resolverDimension(int measureSpec, int tamanoPorDefecto) {
        int modo = MeasureSpec.getMode(measureSpec);
        int tamano = MeasureSpec.getSize(measureSpec);
        switch (modo) {
            case MeasureSpec.EXACTLY:
                return tamano;
            case MeasureSpec.AT_MOST:
                return Math.min(tamano, tamanoPorDefecto);
            default:
                return tamanoPorDefecto;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int anchoUtil = getWidth() - getPaddingLeft() - getPaddingRight();
        int altoUtil = getHeight() - getPaddingTop() - getPaddingBottom();
        if (anchoUtil <= 0 || altoUtil <= 0) {
            return;
        }

        float centroX = getPaddingLeft() + anchoUtil / 2f;
        float centroY = getPaddingTop() + altoUtil / 2f;

        // El trazo se centra sobre el path: se descuenta su grosor para que no se recorte.
        float grosor = paintSector.getStrokeWidth();
        float diametro = Math.min(anchoUtil, altoUtil) - grosor;
        if (diametro <= 0f) {
            return;
        }

        float radio = diametro / 2f;
        oval.set(centroX - radio, centroY - radio, centroX + radio, centroY + radio);

        if (vacio) {
            // Armario sin prendas: anillo de pista completo en gris oscuro Noir.
            canvas.drawArc(oval, ANGULO_INICIO, ANGULO_TOTAL, false, paintPista);
            return;
        }

        for (Segmento segmento : segmentos) {
            paintSector.setColor(segmento.getColor());
            canvas.drawArc(oval, segmento.getAnguloInicio(), segmento.getAnguloBarrido(), false, paintSector);
        }
    }

    // ------------------------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------------------------

    /**
     * Invalida la vista respetando el hilo de origen: {@code invalidate()} en el hilo UI,
     * {@code postInvalidate()} desde cualquier otro hilo (p. ej. callback de Room).
     */
    private void solicitarRedibujado() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    // ------------------------------------------------------------------------------------
    // Modelo de sector
    // ------------------------------------------------------------------------------------

    /**
     * Sector calculado de la dona. Inmutable; la UI puede usarlo para construir la leyenda.
     */
    public static final class Segmento {

        private final String etiqueta;
        private final int cantidad;
        private final float porcentaje;
        private final float anguloInicio;
        private final float anguloBarrido;
        @ColorInt private final int color;

        Segmento(@NonNull String etiqueta,
                 int cantidad,
                 float porcentaje,
                 float anguloInicio,
                 float anguloBarrido,
                 @ColorInt int color) {
            this.etiqueta = etiqueta;
            this.cantidad = cantidad;
            this.porcentaje = porcentaje;
            this.anguloInicio = anguloInicio;
            this.anguloBarrido = anguloBarrido;
            this.color = color;
        }

        @NonNull public String getEtiqueta() { return etiqueta; }
        public int getCantidad() { return cantidad; }
        public float getPorcentaje() { return porcentaje; }
        public float getAnguloInicio() { return anguloInicio; }
        public float getAnguloBarrido() { return anguloBarrido; }
        @ColorInt public int getColor() { return color; }

        @NonNull
        @Override
        public String toString() {
            return etiqueta + ": " + cantidad + " (" + Math.round(porcentaje) + "%)";
        }
    }
}