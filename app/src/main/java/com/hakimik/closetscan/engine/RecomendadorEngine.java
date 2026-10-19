package com.hakimik.closetscan.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Motor de recomendación cromática para el sistema de matching de outfits.
 *
 * Utiliza una matriz de compatibilidad de colores basada en teoría del color
 * (colores neutros, complementarios y análogos) para sugerir prendas que combinan.
 *
 * Proyecto: Closet/Scan — Vestier Digital Interactivo con IA
 * Asignatura: Seminario de Investigación — UNICOLOMBO
 * Estudiante: Ricardo Antonio Lopez Perez
 */
public class RecomendadorEngine {

    /**
     * Matriz de compatibilidad cromática.
     * Clave: color de prenda superior. Valor: array de colores que combinan bien para la inferior.
     */
    private static final Map<String, String[]> MATRIZ_COMPATIBILIDAD = new HashMap<>();

    static {
        // Colores neutros — combinan con casi todo
        MATRIZ_COMPATIBILIDAD.put("Blanco",      new String[]{"Negro", "Azul marino", "Beige", "Gris", "Verde oliva", "Rojo", "Mostaza"});
        MATRIZ_COMPATIBILIDAD.put("Negro",       new String[]{"Blanco", "Gris", "Beige", "Rojo", "Azul rey", "Dorado"});
        MATRIZ_COMPATIBILIDAD.put("Gris",        new String[]{"Blanco", "Negro", "Azul marino", "Borgoña", "Verde bosque"});
        MATRIZ_COMPATIBILIDAD.put("Beige",       new String[]{"Blanco", "Marrón", "Verde oliva", "Azul marino", "Negro"});

        // Colores terrosos
        MATRIZ_COMPATIBILIDAD.put("Marrón",      new String[]{"Beige", "Blanco", "Verde oliva", "Mostaza", "Naranja tostado"});
        MATRIZ_COMPATIBILIDAD.put("Verde oliva", new String[]{"Beige", "Marrón", "Blanco", "Negro", "Mostaza"});
        MATRIZ_COMPATIBILIDAD.put("Mostaza",     new String[]{"Marrón", "Azul marino", "Blanco", "Negro", "Verde oliva"});

        // Colores fríos
        MATRIZ_COMPATIBILIDAD.put("Azul marino", new String[]{"Blanco", "Beige", "Gris", "Negro", "Mostaza"});
        MATRIZ_COMPATIBILIDAD.put("Azul rey",    new String[]{"Blanco", "Negro", "Gris claro"});

        // Colores cálidos
        MATRIZ_COMPATIBILIDAD.put("Rojo",        new String[]{"Negro", "Blanco", "Gris", "Azul marino"});
        MATRIZ_COMPATIBILIDAD.put("Borgoña",     new String[]{"Gris", "Negro", "Beige", "Blanco"});
    }

    /**
     * Calcula el porcentaje de afinidad cromática entre dos colores.
     *
     * @param colorSuperior  Nombre del color de la prenda superior
     * @param colorInferior  Nombre del color de la prenda inferior
     * @return               Porcentaje de afinidad de 0 a 100
     */
    public static int calcularAfinidad(String colorSuperior, String colorInferior) {
        String[] compatibles = MATRIZ_COMPATIBILIDAD.get(colorSuperior);

        if (compatibles == null) return 50; // color no catalogado → afinidad neutral

        for (int i = 0; i < compatibles.length; i++) {
            if (compatibles[i].equalsIgnoreCase(colorInferior)) {
                // Los primeros en la lista tienen mayor afinidad
                int afinidad = 100 - (i * 10);
                return Math.max(afinidad, 60); // mínimo 60% si está en la lista
            }
        }

        // Mismo color → depende del tono
        if (colorSuperior.equalsIgnoreCase(colorInferior)) return 70;

        return 30; // colores que no combinan bien
    }

    /**
     * Retorna una sugerencia textual basada en el nivel de afinidad.
     *
     * @param porcentaje Porcentaje de afinidad calculado
     * @return           Mensaje descriptivo para mostrar en la UI
     */
    public static String obtenerDescripcionAfinidad(int porcentaje) {
        if (porcentaje >= 90) return "🔥 Combinación perfecta";
        if (porcentaje >= 75) return "✅ Combina muy bien";
        if (porcentaje >= 60) return "👍 Buena combinación";
        if (porcentaje >= 40) return "⚠️ Combinación arriesgada";
        return "❌ No se recomienda esta combinación";
    }
}
