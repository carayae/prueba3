package com.example.prueba3

import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prueba3.camera.CameraAppViewModel
import com.example.prueba3.camera.Pantalla
import com.example.prueba3.exeptions.SinPermisoException
import com.example.prueba3.form.FormRecepcionViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cameraAppVm: CameraAppViewModel by viewModels()
        lateinit var cameraController: LifecycleCameraController

        val lanzadorPermisos = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                when {
                    (it[android.Manifest.permission.ACCESS_FINE_LOCATION]
                        ?: false) or (it[android.Manifest.permission.ACCESS_COARSE_LOCATION]
                        ?: false) -> {
                        Log.v("callback RequestMultiplePermissions", "permiso ubicacion granted")
                        cameraAppVm.onPermisoUbicacionOk()
                    }

                    (it[android.Manifest.permission.CAMERA] ?: false) -> {
                        Log.v("callback RequestMultiplePermissions", "permiso camara granted")
                        cameraAppVm.onPermisoCamaraOk()
                    }

                    else -> {
                    }
                }
            }

        fun setupCamara() {
            cameraController = LifecycleCameraController(this)
            cameraController.bindToLifecycle(this)
            cameraController.cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA
        }


        @RequiresApi(Build.VERSION_CODES.O)
        fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            cameraAppVm.lanzadorPermisos = lanzadorPermisos
            setupCamara()
            setContent {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AppUI(cameraController)
                }
            }
        }

    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun generarNombreSegunFechaHastaSegundo():String = LocalDateTime.now().toString().replace(Regex("[T:.-]"), "").substring(0, 14)

@RequiresApi(Build.VERSION_CODES.O)
fun crearArchivoImagenPrivado(contexto: Context): File = File(contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${generarNombreSegunFechaHastaSegundo()}.jpg"
)

fun uri2imageBitmap(uri: Uri, contexto:Context) =
    BitmapFactory.decodeStream(
        contexto.contentResolver.openInputStream(uri)
    ).asImageBitmap()

fun tomarFotografia(cameraController: CameraController, archivo:File,
                    contexto:Context, imagenGuardadaOk:(uri:Uri)->Unit) {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(archivo).build()
    cameraController.takePicture(outputFileOptions,
        ContextCompat.getMainExecutor(contexto), object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults:
                                      ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.also {
                    Log.v("tomarFotografia()::onImageSaved", "Foto guardada en ${it.toString()}")
                            imagenGuardadaOk(it)
                }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("tomarFotografia()", "Error: ${exception.message}")
            }
        })


}

fun getUbicacion(contexto: Context, onUbicacionOk:(location: Location) ->
Unit):Unit {
try {
    val servicio =
        LocationServices.getFusedLocationProviderClient(contexto)
    val tarea =
        servicio.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
    tarea.addOnSuccessListener {
        onUbicacionOk(it)
    }
    } catch (e:SecurityException) {
        throw SinPermisoException(e.message?:"No tiene permisos para conseguir la ubicación")
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppUI(cameraController: CameraController) {
    val contexto = LocalContext.current
    val formRecepcionVm: FormRecepcionViewModel = viewModel()
    val cameraAppViewModel:CameraAppViewModel = viewModel()
    when(cameraAppViewModel.pantalla.value) {
        Pantalla.FORM -> {
            PantallaFormUI(
                formRecepcionVm,
                tomarFotoOnClick = {
                    cameraAppViewModel.cambiarPantallaFoto()
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.CAMERA))
                },
                actualizarUbicacionOnClick = {
                    cameraAppViewModel.onPermisoUbicacionOk = {
                        getUbicacion(contexto) {
                            formRecepcionVm.latitud.value = it.latitude
                            formRecepcionVm.longitud.value = it.longitude
                        }
                    }
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            )
        }
        Pantalla.FOTO -> {
            PantallaFotoUI(formRecepcionVm, cameraAppViewModel,
                cameraController)
        }
        else -> {
            Log.v("AppUI()", "when else, no debería entrar aquí")
        }
    }
}

@Composable
fun PantallaFormUI( formRecepcionVm:FormRecepcionViewModel,
                    tomarFotoOnClick:() -> Unit = {},
                    actualizarUbicacionOnClick:() -> Unit = {}
) {
    val contexto = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Fotografía de la recepción de la encomienda:")
        Button(onClick = {
            tomarFotoOnClick()
        }) {
            Text("Tomar Fotografía")
        }
        formRecepcionVm.fotoRecepcion.value?.also {
            Box(Modifier.size(200.dp, 100.dp)) {
                Image(
                    painter = BitmapPainter(uri2imageBitmap(it,
                        contexto)),
                    contentDescription = "Imagen recepción encomienda")
            }
        }
        Text("La ubicación es: lat: ${formRecepcionVm.latitud.value} y long: ${formRecepcionVm.longitud.value}")
        Button(onClick = {
            actualizarUbicacionOnClick()
        }) {
            Text("Actualizar Ubicación")
        }
        Spacer(Modifier.height(100.dp))

    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PantallaFotoUI(formRecepcionVm:FormRecepcionViewModel, appViewModel:
CameraAppViewModel, cameraController: CameraController) {
    val contexto = LocalContext.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                controller = cameraController
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    Button(onClick = {
        tomarFotografia(
            cameraController,
            crearArchivoImagenPrivado(contexto),
            contexto
        ) {
            formRecepcionVm.fotoRecepcion.value = it
            appViewModel.cambiarPantallaForm()
        }
    }) {
        Text("Tomar foto")
    }
}


@Composable
fun <UbicacionVM> MapaUI() {
    val contexto = LocalContext.current
    val viewModel:UbicacionVM = viewModel()
    val latitud = viewModel
    val longitud = viewModel
    AndroidView(
        factory = {
            MapView(it).apply {
                setTileSource(TileSourceFactory.MAPNIK)

                org.osmdroid.config.Configuration.getInstance().userAgentValue =
                    contexto.packageName
                controller.setZoom(9.0)
            }
        }, update = {
            /* Cada vez que se modifican las variables de estado
            * se vuelve a ejecutar el bloque update */

            // remueve los marcadores anteriores si existieran
            it.overlays.removeIf { true }
            // redibuja el mapa
            it.invalidate()

            // configura una coordenada
            val geoPoint = GeoPoint(1.34334, 5.54543534)
            // mueve el mapa hacia esa coordenada
            it.controller.animateTo(
                geoPoint
            )

            // crea el marcador
            val marcador = Marker(it)
            marcador.position = geoPoint
            marcador.setAnchor(Marker.ANCHOR_CENTER,
                Marker.ANCHOR_CENTER)
            // coloca el marcador en el mapa
            it.overlays.add(marcador)
        }
    )
}