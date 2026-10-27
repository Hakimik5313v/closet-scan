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
     * Precarga 4 prendas semilla solo si el armario está vacío.
     * Constructor de PrendaEntity: (nombre, categoria, estilo, colorHex, colorNombre, rutaFotoOriginal, rutaFotoPng)
     */
    public void sembrarPrendasSiVacio() {
        executor.execute(() -> {
            if (db.prendaDao().contarTotalPrendas() == 0) {
                PrendaEntity p1 = new PrendaEntity(
                        "Camisa Oxford Blanca", "Superior", "Clásico",
                        "#F5F0E8", "Blanco Marfil", null, null);
                p1.material = "Algodón";
                db.prendaDao().insertarPrenda(p1);

                PrendaEntity p2 = new PrendaEntity(
                        "Pantalón Carbón", "Inferior", "Formal",
                        "#2E2E2E", "Grafito Oscuro", null, null);
                p2.material = "Lana";
                db.prendaDao().insertarPrenda(p2);

                PrendaEntity p3 = new PrendaEntity(
                        "Blazer Medianoche", "Superior", "Elegante",
                        "#1B263B", "Azul Medianoche", null, null);
                p3.material = "Lino";
                db.prendaDao().insertarPrenda(p3);

                PrendaEntity p4 = new PrendaEntity(
                        "Zapatillas Champán", "Calzado", "Casual",
                        "#D4AF37", "Oro Viejo", null, null);
                p4.material = "Cuero";
                db.prendaDao().insertarPrenda(p4);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
