package com.hakimik.closetscan.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de persistencia que representa una prenda en el armario digital.
 * Almacena metadatos y atributos visuales detectados en el escaneo.
 */
@Entity(tableName = "prendas")
public class PrendaEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Nombre o etiqueta de la prenda (ej: "Camisa blanca cuadros") */
    public String nombre;

    /** Categoría: Superior, Inferior, Calzado, Accesorio */
    public String categoria;

    /** Estilo: Casual, Formal, Deportivo, Elegante */
    public String estilo;

    /** Color principal extraído por IA en formato HEX (ej: "#FFFFFF") */
    public String colorHex;

    /** Nombre del color en español (ej: "Blanco", "Azul marino", "Beige") */
    public String colorNombre;

    /** Ruta al archivo PNG de la foto original tomada con CameraX */
    public String rutaFotoOriginal;

    /**
     * Ruta al archivo PNG con fondo recortado transparente (salida del módulo IA).
     * Este archivo se usa para superponer la prenda en el vestier interactivo.
     */
    public String rutaFotoPng;

    /** Timestamp de registro de la prenda */
    public long fechaCreacion;

    /** Si el usuario la marcó como favorita */
    public boolean esFavorita;

    // Constructor vacío requerido por Room ORM
    public PrendaEntity() {}

    /** Constructor completo para registrar desde el módulo de escaneo */
    public PrendaEntity(String nombre, String categoria, String estilo,
                        String colorHex, String colorNombre,
                        String rutaFotoOriginal, String rutaFotoPng) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.estilo = estilo;
        this.colorHex = colorHex;
        this.colorNombre = colorNombre;
        this.rutaFotoOriginal = rutaFotoOriginal;
        this.rutaFotoPng = rutaFotoPng;
        this.fechaCreacion = System.currentTimeMillis();
        this.esFavorita = false;
    }

    @Override
    public String toString() {
        return "PrendaEntity{id=" + id + ", nombre='" + nombre + "', categoria='" + categoria + "'}";
    }
}
