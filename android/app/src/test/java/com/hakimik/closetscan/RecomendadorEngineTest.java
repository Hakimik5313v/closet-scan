package com.hakimik.closetscan;

import com.hakimik.closetscan.engine.RecomendadorEngine;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pruebas unitarias para el motor de compatibilidad cromática (RecomendadorEngine).
 * Certifica el cumplimiento de los principios de teoría del color y robustez matemática.
 */
public class RecomendadorEngineTest {

    @Test
    public void testAfinidadBlancoConNegro() {
        int afinidad = RecomendadorEngine.calcularAfinidad("Blanco", "Negro");
        assertTrue("Blanco y Negro debe tener alta afinidad (>=90%)", afinidad >= 90);
    }

    @Test
    public void testAfinidadMismoColorMonocromatico() {
        int afinidad = RecomendadorEngine.calcularAfinidad("Negro", "Negro");
        assertEquals("Mismo color debe retornar afinidad base de 70%", 70, afinidad);
    }

    @Test
    public void testAfinidadColorDesconocido() {
        int afinidad = RecomendadorEngine.calcularAfinidad("ColorInexistente", "Blanco");
        assertEquals("Color no catalogado debe retornar afinidad neutra de 50%", 50, afinidad);
    }

    @Test
    public void testAfinidadCompletaConCalzado() {
        int afinidad = RecomendadorEngine.calcularAfinidadCompleta("Blanco", "Negro", "Blanco");
        assertTrue("El porcentaje debe estar en el rango [15, 100]", afinidad >= 15 && afinidad <= 100);
    }

    @Test
    public void testDescripcionAfinidad() {
        String perfecta = RecomendadorEngine.obtenerDescripcionAfinidad(95);
        assertTrue("Afinidad >= 90 debe indicar combinación perfecta", perfecta.contains("perfecta"));

        String arriesgada = RecomendadorEngine.obtenerDescripcionAfinidad(45);
        assertTrue("Afinidad 45 debe indicar arriesgada", arriesgada.contains("arriesgada"));
    }
}

