package com.hakimik.closetscan;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.hakimik.closetscan.data.entity.PrendaEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * PrendaAdapter — Adaptador con selección de prendas, tipografía limpia y sin stickers genéricos.
 */
public class PrendaAdapter extends RecyclerView.Adapter<PrendaAdapter.PrendaViewHolder> {

    public interface OnPrendaClickListener {
        void onPrendaClick(PrendaEntity prenda, int posicion);
    }

    public interface OnFavoritoClickListener {
        void onFavoritoClick(PrendaEntity prenda, int posicion);
    }

    private List<PrendaEntity> lista = new ArrayList<>();
    private int selectedPosition = RecyclerView.NO_POSITION;
    private OnPrendaClickListener listener;
    private OnFavoritoClickListener favoritoListener;

    public void setOnPrendaClickListener(OnPrendaClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoritoClickListener(OnFavoritoClickListener listener) {
        this.favoritoListener = listener;
    }

    public void setLista(List<PrendaEntity> nuevaLista) {
        this.lista = nuevaLista != null ? nuevaLista : new ArrayList<>();
        notifyDataSetChanged();
    }

    public PrendaEntity getSelectedPrenda() {
        if (selectedPosition >= 0 && selectedPosition < lista.size()) {
            return lista.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public PrendaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_prenda, parent, false);
        return new PrendaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PrendaViewHolder holder, int position) {
        PrendaEntity prenda = lista.get(position);

        holder.tvNombre.setText(prenda.nombre != null ? prenda.nombre : "Sin nombre");

        // Foto real capturada de la prenda (o etiqueta de texto si aún no tiene foto)
        boolean loaded = false;
        if (prenda.rutaFotoOriginal != null && !prenda.rutaFotoOriginal.isEmpty()) {
            try {
                java.io.File f = new java.io.File(prenda.rutaFotoOriginal);
                if (f.exists()) {
                    holder.imgFotoRealPrenda.setImageURI(android.net.Uri.fromFile(f));
                    loaded = true;
                } else if (prenda.rutaFotoOriginal.startsWith("content://") || prenda.rutaFotoOriginal.startsWith("file://")) {
                    holder.imgFotoRealPrenda.setImageURI(android.net.Uri.parse(prenda.rutaFotoOriginal));
                    loaded = true;
                }
            } catch (Exception ignored) {}
        }
        if (loaded) {
            holder.imgFotoRealPrenda.setVisibility(View.VISIBLE);
            holder.tvLetraPrenda.setVisibility(View.GONE);
        } else {
            holder.imgFotoRealPrenda.setImageDrawable(null);
            holder.imgFotoRealPrenda.setVisibility(View.GONE);
            holder.tvLetraPrenda.setVisibility(View.VISIBLE);
        }

        // Etiqueta tipográfica en lugar de stickers
        String catLabel = prenda.categoria != null ? prenda.categoria.toUpperCase() : "PRENDA";
        holder.tvLetraPrenda.setText("[ " + catLabel + " ]");

        // Categoría y Estilo
        String catText = catLabel;
        if (prenda.estilo != null && !prenda.estilo.isEmpty()) {
            catText += " • " + prenda.estilo.toUpperCase();
        }
        holder.tvCategoria.setText(catText);

        // Color como texto descriptivo limpio
        String colorTexto = "Color: ";
        if (prenda.colorNombre != null && !prenda.colorNombre.isEmpty()) {
            colorTexto += prenda.colorNombre;
        } else if (prenda.colorHex != null) {
            colorTexto += prenda.colorHex;
        } else {
            colorTexto += "Estándar";
        }
        holder.tvColorDesc.setText(colorTexto);

        // Estado visual de selección limpia (solo borde blanco nítido)
        boolean isSelected = (position == selectedPosition);
        if (isSelected) {
            holder.cardPrenda.setStrokeColor(Color.parseColor("#FFFFFF"));
            holder.cardPrenda.setStrokeWidth(6);
            holder.frameFotoPrenda.setBackgroundColor(Color.parseColor("#282834"));
            holder.tvLetraPrenda.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            holder.cardPrenda.setStrokeColor(Color.parseColor("#444450"));
            holder.cardPrenda.setStrokeWidth(2);
            holder.frameFotoPrenda.setBackgroundColor(Color.parseColor("#1B1B20"));
            holder.tvLetraPrenda.setTextColor(Color.parseColor("#666675"));
        }

        // Botón de Favorito (❤️)
        if (holder.btnFavoritoPrenda != null) {
            holder.btnFavoritoPrenda.setText(prenda.esFavorita ? "❤️" : "♡");
            holder.btnFavoritoPrenda.setTextColor(prenda.esFavorita ? Color.parseColor("#FF4081") : Color.parseColor("#CCCCCC"));
            holder.btnFavoritoPrenda.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && favoritoListener != null) {
                    favoritoListener.onFavoritoClick(prenda, pos);
                }
            });
        }

        // Listener al tocar
        holder.itemView.setOnClickListener(v -> {
            int prevPos = selectedPosition;
            if (selectedPosition == holder.getAdapterPosition()) {
                selectedPosition = RecyclerView.NO_POSITION;
            } else {
                selectedPosition = holder.getAdapterPosition();
            }

            if (prevPos != RecyclerView.NO_POSITION) notifyItemChanged(prevPos);
            if (selectedPosition != RecyclerView.NO_POSITION) notifyItemChanged(selectedPosition);

            if (listener != null && selectedPosition != RecyclerView.NO_POSITION) {
                listener.onPrendaClick(prenda, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class PrendaViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardPrenda;
        FrameLayout frameFotoPrenda;
        ImageView imgFotoRealPrenda;
        TextView tvLetraPrenda;
        TextView btnFavoritoPrenda;
        TextView tvNombre, tvCategoria, tvColorDesc;

        PrendaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPrenda         = itemView.findViewById(R.id.cardPrenda);
            frameFotoPrenda    = itemView.findViewById(R.id.frameFotoPrenda);
            imgFotoRealPrenda  = itemView.findViewById(R.id.imgFotoRealPrenda);
            tvLetraPrenda      = itemView.findViewById(R.id.tvLetraPrenda);
            btnFavoritoPrenda  = itemView.findViewById(R.id.btnFavoritoPrenda);
            tvNombre         = itemView.findViewById(R.id.tvNombre);
            tvCategoria      = itemView.findViewById(R.id.tvCategoria);
            tvColorDesc      = itemView.findViewById(R.id.tvColorDesc);
        }
    }
}
