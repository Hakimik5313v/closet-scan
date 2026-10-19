package com.hakimik.closetscan.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de persistencia que representa una combinación (outfit) guardada por el usuario.
 */
@Entity(tableName = "outfits")
public class OutfitEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** ID de la prenda superior (camisa, camiseta, blusa) — referencia a prendas.id */
    public int idPrendaSuperior;

    /** ID de la prenda inferior (pantalón, falda, short) — referencia a prendas.id */
    public int idPrendaInferior;

    /** ID del calzado — referencia a prendas.id. Puede ser 0 si no se seleccionó */
    public int idCalzado;

    /** Ocasión del outfit: Casual, Trabajo, Evento, Deporte */
    public String ocasion;

    /** Porcentaje de afinidad cromática calculado por el motor de recomendación (0-100) */
    public int porcentajeAfinidad;

    /** Si el usuario lo marcó como outfit favorito */
    public boolean esFavorito;

    /** Timestamp de creación del outfit */
    public long fechaCreacion;

    // Constructor vacío requerido por Room ORM
    public OutfitEntity() {}

    /** Constructor para crear un outfit desde el vestier interactivo */
    public OutfitEntity(int idPrendaSuperior, int idPrendaInferior, int idCalzado,
                        String ocasion, int porcentajeAfinidad) {
        this.idPrendaSuperior = idPrendaSuperior;
        this.idPrendaInferior = idPrendaInferior;
        this.idCalzado = idCalzado;
        this.ocasion = ocasion;
        this.porcentajeAfinidad = porcentajeAfinidad;
        this.esFavorito = false;
        this.fechaCreacion = System.currentTimeMillis();
    }
}
