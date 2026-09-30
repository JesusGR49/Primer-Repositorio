package com.example.evidenciascesba;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // COMPONENTES DE LA INTERFAZ
    private ImageView imgFoto;
    private EditText edtTitulo;
    private EditText edtDescripcion;
    private Button btnTomarFoto;
    private Button btnGuardar;
    private TextView txtEstado;

    // VARIABLE PARA SABER SI EXISTE UNA FOTO
    private Bitmap fotografiaBitmap = null;

    /*
     * ActivityResultLauncher
     *
     * Se encarga de recibir el resultado
     * después de utilizar la cámara.
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
                                fotografiaBitmap = (Bitmap) datos.getExtras().get("data");

                                /*
                                 * Mostramos la fotografía en el ImageView.
                                 */
                                imgFoto.setImageBitmap(fotografiaBitmap);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgFoto = findViewById(R.id.imgFoto);
        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        txtEstado = findViewById(R.id.txtEstado);

        btnTomarFoto.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            launcherCamara.launch(intent);
        });
    }
}
