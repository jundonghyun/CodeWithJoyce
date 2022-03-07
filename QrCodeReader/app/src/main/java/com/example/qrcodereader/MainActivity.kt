package com.example.qrcodereader

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.qrcodereader.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.jar.Manifest

class MainActivity: AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 1
    private val PERMISSIONS_REQUIRED = arrayOf(android.Manifest.permission.CAMERA)
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var isDetected = false

    override fun onResume() {
        super.onResume()
        isDetected = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        if(!hasPermissions(this)){
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        }
        else{
            startCamera()
        }
    }

    fun getImageAnalysis(): ImageAnalysis{
        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        val imageAnalysis = ImageAnalysis.Builder().build()

        imageAnalysis.setAnalyzer(cameraExecutor,
            QRCodeAnalyzer(object: onDetectListener{
                override fun onDetect(msg: String) {
                    if(!isDetected){
                        isDetected = true

                        val intent = Intent(this@MainActivity, ResultAcitivty::class.java)
                        intent.putExtra("msg", msg)
                        startActivity(intent)
                    }
                }
                }))
        return imageAnalysis
    }

    fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSIONS_REQUEST_CODE){
            if(PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()){
                Toast.makeText(this@MainActivity, "권한 요청이 승인되었습니다.", Toast.LENGTH_LONG).show()
                startCamera()
            }
            else{
                Toast.makeText(this@MainActivity, "권한 요청이 거부되었습니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    fun startCamera(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = getPriview()
            val imageAnalysis = getImageAnalysis()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    fun getPriview(): Preview {
        val preview: Preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.barcodePreview.surfaceProvider)

        return preview
    }
}