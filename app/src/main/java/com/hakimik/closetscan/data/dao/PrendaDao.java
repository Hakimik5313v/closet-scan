package com.hakimik.closetscan.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.hakimik.closetscan.data.entity.PrendaEntity;

import java.util.List;

/**
 * DAO (Data Access Object) para operaciones CRUD de prendas en la base de datos local.
 * Implementación generada automáticamente por Room en tiempo de compilación.
 */
@Dao
public interface PrendaDao {

    // ─── CREATE ────────────────────────────────────────────────────────────────

    /** Inserta una nueva prenda en el armario. Retorna el ID generado. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertarPrenda(PrendaEntity prenda);

    // ─── READ ──────────────────────────────────────────────────────────────────

    /**
     * Retorna todas las prendas del armario, observables con LiveData.
     * La UI se actualiza automáticamente cuando cambian los datos.
     */
    @Query("SELECT * FROM prendas ORDER BY fechaCreacion DESC")
    LiveData<List<PrendaEntity>> obtenerTodasLasPrendas();

    /** Busca prendas por categoría (ej: "Superior", "Inferior", "Calzado") */
    @Query("SELECT * FROM prendas WHERE categoria = :categoria ORDER BY fechaCreacion DESC")
    LiveData<List<PrendaEntity>> obtenerPrendasPorCategoria(String categoria);

    /** Busca prendas por color (para el motor de recomendación cromática) */
    @Query("SELECT * FROM prendas WHERE colorNombre = :colorNombre")
    List<PrendaEntity> obtenerPrendasPorColor(String colorNombre);

    /** Obtiene solo las prendas marcadas como favoritas */
    @Query("SELECT * FROM prendas WHERE esFavorita = 1 ORDER BY fechaCreacion DESC")
    LiveData<List<PrendaEntity>> obtenerFavoritas();

    /** Cuenta el total de prendas en el armario (para el Dashboard) */
    @Query("SELECT COUNT(*) FROM prendas")
    int contarTotalPrendas();

    /** Cuenta prendas por categoría (para el gráfico de dona del Dashboard) */
    @Query("SELECT COUNT(*) FROM prendas WHERE categoria = :categoria")
    int contarPrendasPorCategoria(String categoria);

    // ─── UPDATE ────────────────────────────────────────────────────────────────

    /** Actualiza los datos de una prenda existente */
    @Update
    void actualizarPrenda(PrendaEntity prenda);

    /** Marca o desmarca una prenda como favorita */
    @Query("UPDATE prendas SET esFavorita = :esFavorita WHERE id = :id")
    void toggleFavorita(int id, boolean esFavorita);

    // ─── DELETE ────────────────────────────────────────────────────────────────

    /** Elimina una prenda del armario */
    @Delete
    void eliminarPrenda(PrendaEntity prenda);

    /** Elimina todas las prendas (solo para pruebas / reset de la app) */
    @Query("DELETE FROM prendas")
    void eliminarTodas();
}
