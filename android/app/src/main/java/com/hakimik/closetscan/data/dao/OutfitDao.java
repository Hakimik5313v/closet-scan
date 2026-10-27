package com.hakimik.closetscan.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hakimik.closetscan.data.entity.OutfitEntity;

import java.util.List;

/**
 * DAO para operaciones de persistencia de Outfits combinados en el Vestier Digital.
 */
@Dao
public interface OutfitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertarOutfit(OutfitEntity outfit);

    @Query("SELECT * FROM outfits ORDER BY fechaCreacion DESC")
    LiveData<List<OutfitEntity>> obtenerTodosLosOutfits();

    @Query("SELECT COUNT(*) FROM outfits")
    int contarOutfits();

    @Delete
    void eliminarOutfit(OutfitEntity outfit);
}
