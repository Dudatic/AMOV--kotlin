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
import pt.isec.a2022136610.safetysec.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class VideoRecordingManager(private val context: Context) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private val storage = FirebaseStorage.getInstance()

    // 1. Iniciar a Câmara
    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
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

    // 2. Gravar Vídeo (30s)
    fun startRecording30Seconds(
        onRecordingStart: () -> Unit,
        onRecordingEnd: () -> Unit,
        onVideoUploaded: (String) -> Unit
    ) {
        val videoCapture = this.videoCapture ?: return

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

        activeRecording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply {
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
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            stopRecordingAndUpload(onRecordingEnd, onVideoUploaded)
                        }, 30000)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            onRecordingEnd()
                            val uri = recordEvent.outputResults.outputUri
                            Log.d("VIDEO", "Gravação terminada: $uri")
                            uploadVideoToFirebase(uri, onVideoUploaded)
                        } else {
                            activeRecording?.close()
                            activeRecording = null
                            Log.e("VIDEO", "Erro na gravação: ${recordEvent.error}")
                            onRecordingEnd()
                        }
                    }
                }
            }
    }

    private fun stopRecordingAndUpload(onRecordingEnd: () -> Unit, onVideoUploaded: (String) -> Unit) {
        if (activeRecording != null) {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    // 3. Upload Corrigido
    private fun uploadVideoToFirebase(fileUri: Uri, onUrlReady: (String) -> Unit) {
        Toast.makeText(context, context.getString(R.string.video_sending), Toast.LENGTH_SHORT).show()

        val uniqueName = "alert_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.mp4"
        val ref = storage.reference.child("alert_videos").child(uniqueName)

        Log.d("VIDEO", "Attempting upload to bucket: ${storage.app.options.storageBucket} at path: ${ref.path}")

        val metadata = StorageMetadata.Builder()
            .setContentType("video/mp4")
            .build()

        ref.putFile(fileUri, metadata)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl
                    .addOnSuccessListener { uri ->
                        Log.d("VIDEO", "Upload Success! Public URL: $uri")
                        onUrlReady(uri.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e("VIDEO", "Upload finished, but failed to get URL: ${e.message}")
                        Toast.makeText(context, context.getString(R.string.video_error_link, e.message), Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Log.e("VIDEO", "Upload Failed completely", it)
                Toast.makeText(context, context.getString(R.string.video_upload_failed, it.message), Toast.LENGTH_LONG).show()
            }
    }
}