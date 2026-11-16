package com.hakimik.closetscan;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hakimik.closetscan.data.AppDatabase;
import com.hakimik.closetscan.data.entity.PrendaEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PrendaViewModel — ViewModel reactivo del Armario Digital.
 *
 * Conecta la base de datos Room (AppDatabase) con la UI de forma reactiva
 * mediante LiveData. Todas las operaciones de escritura se ejecutan en un
 * hilo de background (ExecutorService) para no bloquear el hilo principal.
 *
 * Sin referencias a Activity → cero riesgo de memory leaks.
 */
public class PrendaViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final LiveData<List<PrendaEntity>> todasLasPrendas;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PrendaViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        todasLasPrendas = db.prendaDao().obtenerTodasLasPrendas();
    }

    /** Expone el LiveData de todas las prendas del armario. */
    public LiveData<List<PrendaEntity>> getPrendas() {
        return todasLasPrendas;
    }

    /** Inserta una prenda en background. */
    public void insertar(PrendaEntity prenda) {
        executor.execute(() -> db.prendaDao().insertarPrenda(prenda));
    }

    /** Elimina una prenda en background. */
    public void eliminar(PrendaEntity prenda) {
        executor.execute(() -> db.prendaDao().eliminarPrenda(prenda));
    }

    /** Actualiza una prenda en background. */
    public void actualizar(PrendaEntity prenda) {
        executor.execute(() -> db.prendaDao().actualizarPrenda(prenda));
    }

    /** Inserta un outfit guardado en background. */
    public void insertarOutfit(com.hakimik.closetscan.data.entity.OutfitEntity outfit) {
        executor.execute(() -> db.outfitDao().insertarOutfit(outfit));
    }

    /** Expone el LiveData de todos los outfits. */
    public LiveData<List<com.hakimik.closetscan.data.entity.OutfitEntity>> getOutfits() {
        return db.outfitDao().obtenerTodosLosOutfits();
    }

    /**
     * En producción y entrega final, el armario inicia limpio para que el usuario registre sus prendas reales.
     */
    public void sembrarPrendasSiVacio() {
        // Armario limpio para producción y pruebas reales
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
