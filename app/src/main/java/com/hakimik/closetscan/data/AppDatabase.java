package com.hakimik.closetscan.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.hakimik.closetscan.data.dao.PrendaDao;
import com.hakimik.closetscan.data.entity.OutfitEntity;
import com.hakimik.closetscan.data.entity.PrendaEntity;

/**
 * Base de datos principal de Closet/Scan usando Room ORM.
 *
 * Contiene las tablas:
 *  - prendas: inventario del armario digital
 *  - outfits: combinaciones guardadas por el usuario
 *
 * Se implementa con el patrón Singleton para tener una sola instancia activa.
 *
 * Proyecto: Closet/Scan — Vestier Digital Interactivo con IA
 * Asignatura: Seminario de Investigación — UNICOLOMBO
 * Estudiante: Ricardo Antonio Lopez Perez
 */
@Database(
    entities = {PrendaEntity.class, OutfitEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "closet_scan_db";
    private static volatile AppDatabase instance;

    // DAOs disponibles
    public abstract PrendaDao prendaDao();

    /**
     * Retorna la instancia única de la base de datos (Singleton thread-safe).
     * @param context Contexto de la aplicación Android
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return instance;
    }
}
