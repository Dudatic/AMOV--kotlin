package pt.isec.a2022136610.safetysec.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.text.SimpleDateFormat
import java.util.Locale

class VideoRecordingManager(private val context: Context) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private val storage = FirebaseStorage.getInstance()

    // 1. Iniciar a Câmara (Preview + Preparar Gravação)
    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview (necessário para a câmara funcionar)
            val preview = androidx.camera.core.Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            // Configurar Gravador (Baixa qualidade para upload rápido)
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture
                )
            } catch (exc: Exception) {
                Log.e("VIDEO", "Erro ao iniciar câmara", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // 2. Gravar Vídeo (30s) e depois Upload
    fun startRecording30Seconds(
        onRecordingStart: () -> Unit,
        onRecordingEnd: () -> Unit,
        onVideoUploaded: (String) -> Unit
    ) {
        val videoCapture = this.videoCapture ?: return

        // Criar nome único para o ficheiro
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Movies/SafetySec")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // Iniciar Gravação
        activeRecording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply {
                // Ativar áudio se houver permissão
                if (androidx.core.content.PermissionChecker.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                    androidx.core.content.PermissionChecker.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d("VIDEO", "Gravação iniciada! A contar 30s...")
                        onRecordingStart()
                        // Pára automaticamente daqui a 30s
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            stopRecordingAndUpload(onRecordingEnd, onVideoUploaded)
                        }, 30000)
                    }
                    is VideoRecordEvent.Finalize -> {
                        onRecordingEnd() // Garante que a UI limpa se acabar por outro motivo
                        if (!recordEvent.hasError()) {
                            val uri = recordEvent.outputResults.outputUri
                            Log.d("VIDEO", "Gravação terminada: $uri")
                            uploadVideoToFirebase(uri, onVideoUploaded)
                        } else {
                            activeRecording?.close()
                            activeRecording = null
                            Log.e("VIDEO", "Erro na gravação: ${recordEvent.error}")
                        }
                    }
                }
            }
    }

    private fun stopRecordingAndUpload(onRecordingEnd: () -> Unit, onVideoUploaded: (String) -> Unit) {
        activeRecording?.stop()
        activeRecording = null
        onRecordingEnd()
    }

    // 3. Upload para Firebase Storage e obter URL
    private fun uploadVideoToFirebase(fileUri: Uri, onUrlReady: (String) -> Unit) {
        Toast.makeText(context, "Sending emergency recording...", Toast.LENGTH_SHORT).show()

        val ref = storage.reference.child("alert_videos/${System.currentTimeMillis()}.mp4")

        // Adicionar metadados para garantir que é tratado como vídeo
        val metadata = StorageMetadata.Builder()
            .setContentType("video/mp4")
            .build()

        ref.putFile(fileUri, metadata)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("VIDEO", "Upload Sucesso! URL: $uri")
                    onUrlReady(uri.toString())
                }
            }
            .addOnFailureListener {
                Log.e("VIDEO", "Falha no upload", it)
                // Mostra o erro específico no Toast
                Toast.makeText(context, "Falha no envio: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}