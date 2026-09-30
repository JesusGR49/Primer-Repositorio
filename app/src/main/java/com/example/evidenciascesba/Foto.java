package com.example.evidenciascesba;

import android.graphics.Bitmap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * CLASE Foto
 *
 * Guarda una fotografía junto con el momento exacto en que se capturó.
 * Gracias a esta clase podemos tener un HISTORIAL de fotografías y cada una
 * conserva su propia FECHA DE EVIDENCIA y su HORA DE CAPTURA (Nivel 1).
 */
public class Foto {

    private final Bitmap imagen;      // la fotografía ya con la fecha y hora impresas
    private final long momento;       // milisegundos en que se tomó la foto

    public Foto(Bitmap imagen, long momento) {
        this.imagen = imagen;
        this.momento = momento;
    }

    public Bitmap getImagen() {
        return imagen;
    }

    public long getMomento() {
        return momento;
    }

    /*
     * Devuelve la FECHA DE EVIDENCIA en formato 24/09/2026
     */
    public String getFecha() {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "MX"));
        return formato.format(new Date(momento));
    }

    /*
     * Devuelve la HORA DE CAPTURA en formato 21:35:04
     */
    public String getHora() {
        SimpleDateFormat formato = new SimpleDateFormat("HH:mm:ss", new Locale("es", "MX"));
        return formato.format(new Date(momento));
    }

    /*
     * Texto corto que se usa en la ficha y en el pie de la fotografía.
     */
    public String getFechaHora() {
        return getFecha() + "  " + getHora();
    }
}
