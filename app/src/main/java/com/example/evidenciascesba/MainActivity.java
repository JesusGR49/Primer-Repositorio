package com.example.evidenciascesba;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/*
 * ============================================================
 *  EVIDENCIAS CESBA - Aplicación de cámara
 *  Programación Móvil 1
 *
 *  MEJORAS IMPLEMENTADAS
 *
 *  NIVEL 1
 *    1. Botón "Tomar otra fotografía" (la anterior queda en el historial)
 *    2. Eliminar fotografía (la actual o cualquiera del historial)
 *    3. Fecha de evidencia  -> se imprime dentro de la foto
 *    4. Hora de captura     -> se imprime dentro de la foto
 *
 *  NIVEL 2
 *    5. Contador de fotografías capturadas
 *    6. Estado de la evidencia (Pendiente / En proceso / Terminada)
 *
 *  NIVEL 3
 *    7. Galería de evidencias (miniaturas de todas las fotos)
 *    8. Guardar fotografía en el dispositivo (carpeta EvidenciasCESBA)
 *    9. Compartir evidencias (WhatsApp, correo, etc.)
 *   10. Generar ficha de evidencias (resumen en texto)
 * ============================================================
 */
public class MainActivity extends AppCompatActivity {

    // ------------------------------------------------------------
    // COMPONENTES DE LA INTERFAZ
    // ------------------------------------------------------------
    private ImageView imgFoto;
    private EditText edtTitulo;
    private EditText edtDescripcion;
    private Button btnTomarFoto;
    private Button btnEliminarFoto;
    private Button btnGuardar;
    private Button btnGuardarGaleria;
    private Button btnCompartir;
    private Button btnFicha;
    private TextView txtEstado;
    private TextView txtFechaHora;
    private TextView txtContador;
    private TextView txtEstadoEvidencia;
    private TextView txtGaleriaVacia;
    private Spinner spnEstado;
    private LinearLayout contenedorGaleria;

    // ------------------------------------------------------------
    // DATOS DE LA APLICACIÓN
    // ------------------------------------------------------------

    /*
     * HISTORIAL de fotografías.
     * Cada vez que se toma "otra foto" la anterior NO se pierde:
     * se queda guardada en esta lista (Nivel 1 y Nivel 3).
     */
    private final ArrayList<Foto> historial = new ArrayList<>();

    /* Posición de la fotografía que se está mostrando en grande (-1 = ninguna). */
    private int posicionActual = -1;

    /* Estados posibles de la evidencia (Nivel 2). */
    private static final String[] ESTADOS = {"Pendiente", "En proceso", "Terminada"};
    private String estadoEvidencia = ESTADOS[0];

    /* Bandera que indica si la evidencia ya fue guardada con el botón naranja. */
    private boolean evidenciaGuardada = false;

    /*
     * ActivityResultLauncher
     *
     * Se encarga de recibir el resultado después de utilizar la cámara.
     */
    private final ActivityResultLauncher<Intent> launcherCamara =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        /*
                         * Verificamos si la cámara terminó correctamente.
                         */
                        if (resultado.getResultCode() == RESULT_OK) {
                            Intent datos = resultado.getData();
                            if (datos != null && datos.getExtras() != null) {
                                /*
                                 * Recuperamos la miniatura generada por la cámara.
                                 */
                                Bitmap fotografiaBitmap = (Bitmap) datos.getExtras().get("data");

                                if (fotografiaBitmap == null) {
                                    txtEstado.setText("La cámara no devolvió ninguna imagen");
                                    return;
                                }

                                /*
                                 * MEJORAS 3 y 4 (Nivel 1)
                                 * Guardamos el momento exacto de la captura y lo
                                 * imprimimos dentro de la propia fotografía.
                                 */
                                long momento = System.currentTimeMillis();
                                Bitmap conSello = estamparFechaHora(fotografiaBitmap, momento);

                                /*
                                 * MEJORA 1 (Nivel 1)
                                 * La foto nueva se agrega al historial; la anterior
                                 * se conserva y se puede volver a ver en la galería.
                                 */
                                historial.add(new Foto(conSello, momento));
                                posicionActual = historial.size() - 1;
                                evidenciaGuardada = false;

                                actualizarInterfaz();
                                txtEstado.setText("Fotografía tomada correctamente");
                                Toast.makeText(
                                        MainActivity.this,
                                        "Fotografía recibida",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        } else {
                            txtEstado.setText("La fotografía fue cancelada");
                            Toast.makeText(
                                    MainActivity.this,
                                    "Operación cancelada",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    // ------------------------------------------------------------
    // CICLO DE VIDA
    // ------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgFoto = findViewById(R.id.imgFoto);
        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnEliminarFoto = findViewById(R.id.btnEliminarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnGuardarGaleria = findViewById(R.id.btnGuardarGaleria);
        btnCompartir = findViewById(R.id.btnCompartir);
        btnFicha = findViewById(R.id.btnFicha);
        txtEstado = findViewById(R.id.txtEstado);
        txtFechaHora = findViewById(R.id.txtFechaHora);
        txtContador = findViewById(R.id.txtContador);
        txtEstadoEvidencia = findViewById(R.id.txtEstadoEvidencia);
        txtGaleriaVacia = findViewById(R.id.txtGaleriaVacia);
        spnEstado = findViewById(R.id.spnEstado);
        contenedorGaleria = findViewById(R.id.contenedorGaleria);

        configurarEstados();

        // Botón azul: abre la cámara (y dice "TOMAR OTRA" cuando ya hay fotos).
        btnTomarFoto.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            launcherCamara.launch(intent);
        });

        // MEJORA 2 (Nivel 1): eliminar la fotografía que se está mostrando.
        btnEliminarFoto.setOnClickListener(v -> {
            if (posicionActual >= 0) {
                confirmarEliminar(posicionActual);
            }
        });

        // Botón naranja original: valida y "guarda" la evidencia.
        btnGuardar.setOnClickListener(v -> guardarEvidencia());

        // MEJORA 8 (Nivel 3): guardar la foto en el dispositivo.
        btnGuardarGaleria.setOnClickListener(v -> guardarFotoEnDispositivo());

        // MEJORA 9 (Nivel 3): compartir por WhatsApp, correo, etc.
        btnCompartir.setOnClickListener(v -> compartirEvidencias());

        // MEJORA 10 (Nivel 3): generar la ficha de evidencias.
        btnFicha.setOnClickListener(v -> mostrarFicha());

        actualizarInterfaz();
    }

    // ------------------------------------------------------------
    // MEJORA 6 (NIVEL 2): ESTADO DE LA EVIDENCIA
    // ------------------------------------------------------------

    /*
     * Llena el Spinner con los tres estados y escucha los cambios.
     */
    private void configurarEstados() {
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ESTADOS
        );
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnEstado.setAdapter(adaptador);

        spnEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> padre, View vista, int posicion, long id) {
                estadoEvidencia = ESTADOS[posicion];
                pintarEstado();
                txtEstado.setText("Estado cambiado a: " + estadoEvidencia);
            }

            @Override
            public void onNothingSelected(AdapterView<?> padre) {
                // No se necesita hacer nada.
            }
        });

        pintarEstado();
    }

    /*
     * Cambia el color del texto según el estado seleccionado:
     * rojo = Pendiente, naranja = En proceso, verde = Terminada.
     */
    private void pintarEstado() {
        String color;
        if (estadoEvidencia.equals("Terminada")) {
            color = "#2E7D32";
        } else if (estadoEvidencia.equals("En proceso")) {
            color = "#EF6C00";
        } else {
            color = "#C62828";
        }
        txtEstadoEvidencia.setText("Estado actual: " + estadoEvidencia);
        txtEstadoEvidencia.setTextColor(Color.parseColor(color));
    }

    // ------------------------------------------------------------
    // MEJORAS 3 y 4 (NIVEL 1): FECHA Y HORA DENTRO DE LA FOTO
    // ------------------------------------------------------------

    /*
     * Dibuja sobre la fotografía una franja oscura con la fecha de la
     * evidencia y la hora exacta de la captura.
     *
     * Se trabaja sobre una COPIA porque el Bitmap que entrega la cámara
     * es de solo lectura y la aplicación se cerraría al intentar dibujar en él.
     */
    private Bitmap estamparFechaHora(Bitmap original, long momento) {
        Bitmap copia = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas lienzo = new Canvas(copia);

        float alto = copia.getHeight();
        float ancho = copia.getWidth();

        float tamanioTexto = alto * 0.07f;
        if (tamanioTexto < 18f) {
            tamanioTexto = 18f;
        }

        // Franja semitransparente en la parte inferior.
        Paint franja = new Paint();
        franja.setColor(Color.argb(160, 0, 0, 0));
        lienzo.drawRect(0, alto - (tamanioTexto * 2.6f), ancho, alto, franja);

        // Texto blanco encima de la franja.
        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setColor(Color.WHITE);
        texto.setTextSize(tamanioTexto);
        texto.setFakeBoldText(true);

        Foto temporal = new Foto(original, momento);
        float margen = tamanioTexto * 0.4f;
        lienzo.drawText("Fecha: " + temporal.getFecha(), margen, alto - (tamanioTexto * 1.45f), texto);
        lienzo.drawText("Hora: " + temporal.getHora(), margen, alto - (tamanioTexto * 0.35f), texto);

        return copia;
    }

    // ------------------------------------------------------------
    // MEJORA 2 (NIVEL 1): ELIMINAR FOTOGRAFÍA
    // ------------------------------------------------------------

    /*
     * Pregunta antes de borrar para evitar eliminar una evidencia por error.
     */
    private void confirmarEliminar(final int posicion) {
        Foto foto = historial.get(posicion);

        new AlertDialog.Builder(this)
                .setTitle("Eliminar fotografía")
                .setMessage("¿Deseas eliminar la fotografía tomada el "
                        + foto.getFecha() + " a las " + foto.getHora() + "?")
                .setPositiveButton("Eliminar", (dialogo, boton) -> {
                    historial.remove(posicion);

                    // Recalculamos qué foto se debe mostrar después de borrar.
                    if (historial.isEmpty()) {
                        posicionActual = -1;
                    } else if (posicionActual >= historial.size()) {
                        posicionActual = historial.size() - 1;
                    }

                    actualizarInterfaz();
                    txtEstado.setText("Fotografía eliminada del historial");
                    Toast.makeText(this, "Fotografía eliminada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ------------------------------------------------------------
    // MEJORA 7 (NIVEL 3): GALERÍA DE EVIDENCIAS
    // ------------------------------------------------------------

    /*
     * Crea una miniatura por cada foto del historial.
     * Un toque muestra la foto en grande; dejar presionado permite eliminarla.
     */
    private void pintarGaleria() {
        contenedorGaleria.removeAllViews();

        if (historial.isEmpty()) {
            txtGaleriaVacia.setVisibility(View.VISIBLE);
            return;
        }
        txtGaleriaVacia.setVisibility(View.GONE);

        for (int i = 0; i < historial.size(); i++) {
            final int posicion = i;

            ImageView miniatura = new ImageView(this);
            LinearLayout.LayoutParams medidas =
                    new LinearLayout.LayoutParams(convertirADp(96), convertirADp(96));
            medidas.setMargins(0, 0, convertirADp(10), 0);
            miniatura.setLayoutParams(medidas);
            miniatura.setScaleType(ImageView.ScaleType.CENTER_CROP);
            miniatura.setImageBitmap(historial.get(i).getImagen());
            miniatura.setContentDescription("Evidencia " + (i + 1));

            // El borde naranja indica cuál es la foto que se ve en grande.
            int borde = convertirADp(3);
            miniatura.setPadding(borde, borde, borde, borde);
            miniatura.setBackgroundColor(
                    i == posicionActual
                            ? Color.parseColor("#FF6F0C")
                            : Color.parseColor("#C9CEDA")
            );

            miniatura.setOnClickListener(v -> {
                posicionActual = posicion;
                actualizarInterfaz();
                txtEstado.setText("Mostrando la fotografía " + (posicion + 1)
                        + " de " + historial.size());
            });

            miniatura.setOnLongClickListener(v -> {
                confirmarEliminar(posicion);
                return true;
            });

            contenedorGaleria.addView(miniatura);
        }
    }

    /* Convierte dp a píxeles para poder crear las miniaturas desde el código. */
    private int convertirADp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------
    // MEJORA 5 (NIVEL 2): CONTADOR + ACTUALIZACIÓN DE LA PANTALLA
    // ------------------------------------------------------------

    /*
     * Un solo método que deja toda la pantalla al día:
     * foto grande, fecha, hora, contador, botones y galería.
     */
    private void actualizarInterfaz() {
        boolean hayFotos = !historial.isEmpty();

        if (hayFotos) {
            Foto foto = historial.get(posicionActual);
            imgFoto.setImageBitmap(foto.getImagen());
            txtFechaHora.setText(
                    "Fecha de evidencia: " + foto.getFecha()
                            + "\nHora de captura: " + foto.getHora()
                            + "\nMostrando la foto " + (posicionActual + 1)
                            + " de " + historial.size()
            );
            btnTomarFoto.setText("TOMAR OTRA FOTOGRAFÍA");
        } else {
            imgFoto.setImageResource(android.R.drawable.ic_menu_gallery);
            txtFechaHora.setText("Aún no hay fecha ni hora de captura");
            btnTomarFoto.setText("TOMAR FOTOGRAFÍA");
        }

        // CONTADOR DE FOTOGRAFÍAS (Nivel 2)
        txtContador.setText("Fotografías capturadas: " + historial.size());

        // Los botones que necesitan una foto se desactivan si no hay ninguna.
        btnEliminarFoto.setEnabled(hayFotos);
        btnGuardarGaleria.setEnabled(hayFotos);
        btnCompartir.setEnabled(hayFotos);
        btnFicha.setEnabled(hayFotos);

        pintarGaleria();
    }

    // ------------------------------------------------------------
    // BOTÓN GUARDAR EVIDENCIA (validaciones)
    // ------------------------------------------------------------
    private void guardarEvidencia() {
        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();

        if (historial.isEmpty()) {
            Toast.makeText(this, "Primero toma una fotografía", Toast.LENGTH_SHORT).show();
            return;
        }
        if (titulo.isEmpty()) {
            edtTitulo.setError("Escribe un título para la evidencia");
            edtTitulo.requestFocus();
            return;
        }
        if (descripcion.isEmpty()) {
            edtDescripcion.setError("Describe brevemente la evidencia");
            edtDescripcion.requestFocus();
            return;
        }

        evidenciaGuardada = true;
        txtEstado.setText("Evidencia \"" + titulo + "\" guardada con "
                + historial.size() + " fotografía(s) · Estado: " + estadoEvidencia);
        Toast.makeText(this, "Evidencia guardada correctamente", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------
    // MEJORA 8 (NIVEL 3): GUARDAR LA FOTOGRAFÍA EN EL DISPOSITIVO
    // ------------------------------------------------------------

    /*
     * Copia la fotografía a la galería del teléfono, dentro de la
     * carpeta Pictures/EvidenciasCESBA, usando MediaStore.
     */
    private void guardarFotoEnDispositivo() {
        if (posicionActual < 0) {
            Toast.makeText(this, "No hay fotografía que guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        Foto foto = historial.get(posicionActual);
        String nombre = "EvidenciaCESBA_" + foto.getMomento() + ".jpg";

        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Images.Media.DISPLAY_NAME, nombre);
        valores.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        valores.put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/EvidenciasCESBA");
        valores.put(MediaStore.Images.Media.IS_PENDING, 1);

        ContentResolver resolver = getContentResolver();
        Uri destino = resolver.insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                valores
        );

        if (destino == null) {
            Toast.makeText(this, "No se pudo crear el archivo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            OutputStream salida = resolver.openOutputStream(destino);
            if (salida != null) {
                foto.getImagen().compress(Bitmap.CompressFormat.JPEG, 95, salida);
                salida.close();
            }

            // Al quitar IS_PENDING la foto ya se ve en la galería del teléfono.
            valores.clear();
            valores.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(destino, valores, null, null);

            txtEstado.setText("Guardada en Pictures/EvidenciasCESBA/" + nombre);
            Toast.makeText(this, "Fotografía guardada en el dispositivo",
                    Toast.LENGTH_LONG).show();

        } catch (IOException error) {
            Log.e("EvidenciasCESBA", "Error al guardar la foto", error);
            resolver.delete(destino, null, null);
            Toast.makeText(this, "Error al guardar: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------
    // MEJORA 9 (NIVEL 3): COMPARTIR EVIDENCIAS
    // ------------------------------------------------------------

    /*
     * Escribe las fotos en la carpeta temporal de la aplicación, obtiene
     * un permiso temporal con FileProvider y abre el menú para compartir
     * (WhatsApp, Gmail, Drive, etc.).
     */
    private void compartirEvidencias() {
        if (historial.isEmpty()) {
            Toast.makeText(this, "No hay evidencias que compartir", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ArrayList<Uri> archivos = prepararArchivosTemporales();

            Intent intent;
            if (archivos.size() > 1) {
                intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, archivos);
            } else {
                intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, archivos.get(0));
            }

            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_SUBJECT, tituloOPorDefecto());
            intent.putExtra(Intent.EXTRA_TEXT, generarFicha());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Compartir evidencia con..."));
            txtEstado.setText("Compartiendo " + archivos.size() + " fotografía(s)");

        } catch (IOException error) {
            Log.e("EvidenciasCESBA", "Error al preparar los archivos", error);
            Toast.makeText(this, "No se pudo compartir: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Guarda cada foto del historial en cache/evidencias y devuelve
     * la lista de direcciones (Uri) que se pueden compartir.
     */
    private ArrayList<Uri> prepararArchivosTemporales() throws IOException {
        File carpeta = new File(getCacheDir(), "evidencias");
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta temporal");
        }

        ArrayList<Uri> direcciones = new ArrayList<>();

        for (int i = 0; i < historial.size(); i++) {
            File archivo = new File(carpeta, "evidencia_" + (i + 1) + ".jpg");

            FileOutputStream salida = new FileOutputStream(archivo);
            historial.get(i).getImagen().compress(Bitmap.CompressFormat.JPEG, 95, salida);
            salida.close();

            direcciones.add(FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    archivo
            ));
        }

        return direcciones;
    }

    // ------------------------------------------------------------
    // MEJORA 10 (NIVEL 3): FICHA DE EVIDENCIAS
    // ------------------------------------------------------------

    /*
     * Arma un resumen en texto con los datos de la evidencia y
     * el detalle de todas las fotografías capturadas.
     */
    private String generarFicha() {
        StringBuilder ficha = new StringBuilder();

        ficha.append("FICHA DE EVIDENCIA - CESBA\n");
        ficha.append("==============================\n\n");
        ficha.append("Título: ").append(tituloOPorDefecto()).append("\n");

        String descripcion = edtDescripcion.getText().toString().trim();
        ficha.append("Descripción: ")
                .append(descripcion.isEmpty() ? "(sin descripción)" : descripcion)
                .append("\n");

        ficha.append("Estado: ").append(estadoEvidencia).append("\n");
        ficha.append("Evidencia registrada: ").append(evidenciaGuardada ? "Sí" : "No").append("\n");
        ficha.append("Total de fotografías: ").append(historial.size()).append("\n\n");

        ficha.append("DETALLE DE LAS FOTOGRAFÍAS\n");
        for (int i = 0; i < historial.size(); i++) {
            Foto foto = historial.get(i);
            ficha.append("  ").append(i + 1).append(". ")
                    .append(foto.getFecha()).append(" a las ").append(foto.getHora())
                    .append("\n");
        }

        ficha.append("\nGenerado por EvidenciasCESBA");
        return ficha.toString();
    }

    /*
     * Muestra la ficha en un AlertDialog con la opción de compartirla como texto.
     */
    private void mostrarFicha() {
        final String ficha = generarFicha();

        new AlertDialog.Builder(this)
                .setTitle("Ficha de evidencias")
                .setMessage(ficha)
                .setPositiveButton("Compartir ficha", (dialogo, boton) -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, tituloOPorDefecto());
                    intent.putExtra(Intent.EXTRA_TEXT, ficha);
                    startActivity(Intent.createChooser(intent, "Compartir ficha con..."));
                })
                .setNegativeButton("Cerrar", null)
                .show();

        txtEstado.setText("Ficha generada con " + historial.size() + " fotografía(s)");
    }

    /* Devuelve el título escrito o uno genérico si el campo está vacío. */
    private String tituloOPorDefecto() {
        String titulo = edtTitulo.getText().toString().trim();
        return titulo.isEmpty() ? "Evidencia sin título" : titulo;
    }
}
