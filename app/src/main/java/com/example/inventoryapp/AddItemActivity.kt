package com.example.inventoryapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.inventoryapp.model.InventoryItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddItemActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_REQUEST = 2001
        private const val GALLERY_REQUEST = 2002
        private const val CAMERA_PERMISSION_REQUEST = 3001
    }

    private lateinit var etBarcode: EditText
    private lateinit var etName: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etLocation: EditText
    private lateinit var etNotes: EditText
    private lateinit var ivPhoto: ImageView
    private var photoPath: String? = null
    private var editingItemId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        etBarcode = findViewById(R.id.et_barcode)
        etName = findViewById(R.id.et_name)
        etQuantity = findViewById(R.id.et_quantity)
        etLocation = findViewById(R.id.et_location)
        etNotes = findViewById(R.id.et_notes)
        ivPhoto = findViewById(R.id.iv_photo)

        intent.getStringExtra("item_barcode")?.let { etBarcode.setText(it) }
        intent.getStringExtra("item_name")?.let { etName.setText(it) }
        intent.getIntExtra("item_quantity", 0).let {
            if (it > 0) etQuantity.setText(it.toString())
        }
        intent.getStringExtra("item_location")?.let { etLocation.setText(it) }
        editingItemId = intent.getLongExtra("item_id", -1L)

        if (editingItemId > 0) {
            title = "Edit Item"
        }

        findViewById<Button>(R.id.btn_take_photo).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST
                )
            } else {
                launchCamera()
            }
        }

        findViewById<Button>(R.id.btn_pick_gallery).setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST)
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveItem()
        }

        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun launchCamera() {
        val barcode = etBarcode.text.toString().ifBlank { "unknown" }
        val destFile = File("/sdcard/InventoryApp/photos/${barcode}.jpg")
        destFile.parentFile?.mkdirs()
        photoPath = destFile.absolutePath

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(destFile))
        startActivityForResult(cameraIntent, CAMERA_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CAMERA_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    photoPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            ivPhoto.setImageURI(Uri.fromFile(file))
                            Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            GALLERY_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val selectedUri = data?.data
                    if (selectedUri != null) {
                        ivPhoto.setImageURI(selectedUri)
                        photoPath = selectedUri.toString()
                    }
                }
            }
        }
    }

    private fun saveItem() {
        val barcode = etBarcode.text.toString().trim()
        val name = etName.text.toString().trim()
        val quantityStr = etQuantity.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val notes = etNotes.text.toString().trim()

        if (barcode.isEmpty()) {
            etBarcode.error = "Barcode is required"
            return
        }
        if (name.isEmpty()) {
            etName.error = "Item name is required"
            return
        }
        val quantity = quantityStr.toIntOrNull() ?: 0

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val item = InventoryItem(
            id = if (editingItemId > 0) editingItemId else 0L,
            barcode = barcode,
            name = name,
            quantity = quantity,
            location = location,
            notes = notes,
            photoPath = photoPath ?: "",
            lastUpdated = timestamp
        )

        val resultIntent = Intent().apply {
            putExtra("new_item", item)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}