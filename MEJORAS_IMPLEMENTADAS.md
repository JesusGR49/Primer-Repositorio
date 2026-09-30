# EVIDENCIAS CESBA — Mejoras implementadas

Práctica de Programación Móvil 1 · Jesús Emmanuel García Rodríguez

## Resumen de las mejoras

| Nivel | Mejora | Dónde está en el código |
|---|---|---|
| 1 | Botón "Tomar otra fotografía" (la anterior queda en el historial) | `historial` + `launcherCamara` en `MainActivity.java` |
| 1 | Eliminar fotografía (botón rojo o miniatura presionada) | `confirmarEliminar()` |
| 1 | Fecha de evidencia impresa dentro de la foto | `estamparFechaHora()` y `Foto.getFecha()` |
| 1 | Hora de captura impresa dentro de la foto | `estamparFechaHora()` y `Foto.getHora()` |
| 2 | Contador de fotografías | `txtContador` en `actualizarInterfaz()` |
| 2 | Estado de la evidencia (Pendiente / En proceso / Terminada) | `configurarEstados()` y `pintarEstado()` |
| 3 | Galería de evidencias (miniaturas) | `pintarGaleria()` |
| 3 | Guardar fotografía en el dispositivo | `guardarFotoEnDispositivo()` (MediaStore) |
| 3 | Compartir evidencias (WhatsApp, correo…) | `compartirEvidencias()` (FileProvider) |
| 3 | Generar ficha de evidencias | `generarFicha()` y `mostrarFicha()` |

Mínimos que pidió el profesor: Nivel 1 ≥ 2 ✔ (se hicieron 4) · Nivel 2 ≥ 1 ✔ (se hicieron 2) · Nivel 3 ≥ 2 ✔ (se hicieron 4).

## Archivos que se modificaron o crearon

```
app/src/main/java/com/example/evidenciascesba/MainActivity.java   (modificado)
app/src/main/java/com/example/evidenciascesba/Foto.java           (NUEVO)
app/src/main/res/layout/activity_main.xml                         (modificado)
app/src/main/res/xml/rutas_archivos.xml                           (NUEVO - FileProvider)
app/src/main/AndroidManifest.xml                                  (modificado - provider y queries)
respaldo_original/                                                (copia de los archivos originales)
```

No se agregó ninguna librería externa: todo funciona con lo que ya traía el proyecto.

## Cómo probarla (orden recomendado para las capturas)

1. Abrir la app → se ve el contador en 0 y la galería vacía.
2. **TOMAR FOTOGRAFÍA** → la foto aparece con la fecha y la hora impresas abajo;
   el contador pasa a 1 y el botón cambia a "TOMAR OTRA FOTOGRAFÍA".
3. **TOMAR OTRA FOTOGRAFÍA** → el contador sube a 2 y en la galería salen las dos
   miniaturas (la seleccionada tiene borde naranja). Tocar la primera para volver a verla.
4. Cambiar el **estado** a "En proceso" y luego a "Terminada" → cambia el color.
5. Escribir título y descripción → **GUARDAR EVIDENCIA** (probar primero con el
   título vacío para que se vea la validación).
6. **GUARDAR FOTO** → revisar la galería del teléfono, carpeta `Pictures/EvidenciasCESBA`.
7. **COMPARTIR** → elegir WhatsApp o Gmail (solo para ver que abre el menú).
8. **GENERAR FICHA DE EVIDENCIAS** → aparece el resumen en un AlertDialog.
9. **ELIMINAR FOTOGRAFÍA** → sale la confirmación; al aceptar baja el contador.

> La cámara solo funciona bien en un **dispositivo físico**; en el emulador hay que
> activar la cámara virtual en la configuración del AVD.

## Detalles técnicos por si el profe pregunta

- **Historial:** `ArrayList<Foto>`; `posicionActual` indica cuál se ve en grande (-1 = ninguna).
- **Fecha/hora dentro de la foto:** se hace una copia mutable del Bitmap
  (`copy(Bitmap.Config.ARGB_8888, true)`) porque el que entrega la cámara es de
  solo lectura, y encima se dibuja con `Canvas` y `Paint`.
- **Guardar en el dispositivo:** `MediaStore` con `RELATIVE_PATH` e `IS_PENDING`;
  al ser minSdk 29 no hace falta pedir permisos de almacenamiento.
- **Compartir:** las fotos se copian a `cacheDir/evidencias`, se convierten en
  `content://` con `FileProvider` y se mandan con `Intent.ACTION_SEND` /
  `ACTION_SEND_MULTIPLE` más `FLAG_GRANT_READ_URI_PERMISSION`.
- La app usa la miniatura que devuelve `ACTION_IMAGE_CAPTURE` (la que ya traía el
  código original), por eso las imágenes son de baja resolución.
