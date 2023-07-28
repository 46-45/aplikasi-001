package com.example.fr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 1

    private lateinit var cameraPreview: SurfaceView
    private lateinit var captureButton: Button
    private lateinit var responseTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var imagePreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraPreview = findViewById(R.id.camera_preview)
        captureButton = findViewById(R.id.btn_capture)
        responseTextView = findViewById(R.id.tv_response)
        distanceTextView = findViewById(R.id.tv_distance)
        imagePreview = findViewById(R.id.image_preview)

        captureButton.setOnClickListener {
            takePictureFromCamera()
        }
    }

    private fun takePictureFromCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imagePreview.setImageBitmap(imageBitmap) // Display the captured image in ImageView
            sendImageToAPI(imageBitmap)
        }
    }

    private fun sendImageToAPI(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "image.png", byteArray.toRequestBody("image/png".toMediaTypeOrNull()))
                    .build()

                val request = Request.Builder()
                    .url("https://f19b-35-185-9-95.ngrok.io/predict")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                val apiLatitude = 0.435378
                val apiLongitude = 101.8858446

                val location = Location("API")
                location.latitude = apiLatitude
                location.longitude = apiLongitude

                val distance = calculateDistanceFromApiLocation(location)
                val responseMessage = if (distance <= 20) "Didalam radius" else "Diluar radius"

                runOnUiThread {
                    responseTextView.text = "$responseBody\n$responseMessage"
                    distanceTextView.text = "Jarak dari lokasi: $distance meter"
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "Error sending image to API", e)
            }
        }
    }

    private fun calculateDistanceFromApiLocation(apiLocation: Location): Float {
        val currentLocation = Location("Current")
        // TODO: Get the current latitude and longitude of the device using LocationManager or FusedLocationProviderClient
        // For simplicity, let's assume you have the currentLocation values already.

        // Replace these values with the actual current latitude and longitude
        currentLocation.latitude = 0.435378
        currentLocation.longitude = 101.8858446

        return currentLocation.distanceTo(apiLocation)
    }
}
