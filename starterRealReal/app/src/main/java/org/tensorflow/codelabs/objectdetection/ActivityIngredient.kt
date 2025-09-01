
package org.tensorflow.codelabs.objectdetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import android.graphics.Bitmap
import com.tensorflow.codelabs.objectdetection.R
import com.tensorflow.codelabs.objectdetection.databinding.ActivityMainBinding
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import org.tensorflow.codelabs.objectdetection.SavedIngredientsActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody

class ActivityIngredient : AppCompatActivity(), View.OnClickListener {
    private lateinit var db: FirebaseFirestore // Firestore 데이터베이스 참조

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var saveButton: Button
    private lateinit var imageView: ImageView
    private lateinit var currentImageBitmap: Bitmap

    companion object {
        const val TAG = "TFLite - ODT"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        private const val MAX_FONT_SIZE = 96F
        const val REQUEST_CAMERA_PERMISSION_CODE: Int = 100  // 고유 요청 코드 추가


    }

    private lateinit var captureImageFab: Button
    private lateinit var inputImageView: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var currentPhotoPath: String
    private val REQUEST_PERMISSION = 101
    private val REQUEST_IMAGE_GALLERY = 102
    private val PICK_IMAGE_REQUEST = 1
    private val READ_STORAGE_PERMISSION_REQUEST_CODE = 1001
    private val GALLERY_REQUEST_CODE = 1002



    // 갤러리에서 이미지를 선택한 후 호출되는 콜백
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                try {
                    // 선택한 이미지를 비트맵으로 변환
                    val bitmap = getBitmapFromUri(imageUri)
                    if (bitmap != null) {

                        // 갤러리 이미지의 경로 설정
                        val file = File(getRealPathFromURI(imageUri))
                        currentPhotoPath = file.absolutePath
                        // 감지 수행
                        setViewAndDetect(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load image from gallery: ${e.message}")
                }
            }
        }
    }
    private fun getRealPathFromURI(contentUri: Uri): String {
        var result = ""
        val cursor = contentResolver.query(contentUri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            if (idx >= 0) {
                result = cursor.getString(idx)
            } else {
                // Fallback if column index is not available
                result = contentUri.path ?: ""
            }
            cursor.close()
        }
        return result
    }


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredient)

        // Firestore 인스턴스 가져오기
        db = FirebaseFirestore.getInstance()


        // UI 요소 초기화
        captureImageFab = findViewById(R.id.captureImageFab)
        inputImageView = findViewById(R.id.imageView)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)

        // 저장된 식재료 버튼 클릭 리스너 설정
        val savedIngredientsButton: Button = findViewById(R.id.savedIngredientsButton)
        savedIngredientsButton.setOnClickListener {
            val intent = Intent(this, SavedIngredientsActivity::class.java)
            startActivity(intent) // 새로운 액티비티 시작
        }

        // 갤러리 버튼 클릭 리스너 설정
        val galleryButton: Button = findViewById(R.id.openGalleryButton)
        galleryButton.setOnClickListener {
            requestGalleryPermission()
        }

        captureImageFab.setOnClickListener(this)

    }

    override fun onDestroy() {
        mBinding = null
        super.onDestroy()
    }

    private var isDialogShown = false


    private fun performDetection(bitmap: Bitmap): List<Detection> {
        // 이미지 처리 후 텐서 이미지로 변환
        val tensorImage = TensorImage.fromBitmap(bitmap)

        // 모델을 불러와서 객체 감지 수행
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.5f)  // 신뢰도 기준 설정
            .build()

        val objectDetector = ObjectDetector.createFromFileAndOptions(
            this,
            "model.tflite",  // 모델 파일 경로
            options
        )

        // 모델에 이미지 전달 후 감지 결과 반환
        return objectDetector.detect(tensorImage)
    }
    // 권한을 확인하고 요청하는 메서드
    private fun requestGalleryPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_STORAGE_PERMISSION_REQUEST_CODE)
        } else {
            openGallery()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent() // 카메라 실행
                } else {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (requestCode == READ_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(this, "갤러리 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    /**
     * onClick(v: View?)
     *      Detect touches on the UI components
     */
    override fun onClick(v: View?) {
        Log.d(TAG, "onClick 호출 - ID: ${v?.id}")
        when (v?.id) {
            R.id.captureImageFab -> {
                try {
                    requestCameraPermission() // 권한 확인 및 요청
                    dispatchTakePictureIntent()
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
            R.id.openGalleryButton -> {
                openGallery()
            }
        }
    }

    private fun requestCameraPermission() {
        Log.d(TAG, "requestCameraPermission 호출")
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION_CODE
            )
        } else {
            dispatchTakePictureIntent()
        }
    }

    /**
     * openGallery():
     *      갤러리를 열어 이미지를 선택할 수 있는 기능
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    /**
     * onActivityResult():
     *      선택한 갤러리 이미지를 가져와서 비트맵으로 변환하고 감지 실행
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 카메라로 사진 촬영 시
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                // 촬영한 이미지를 비트맵으로 가져옴
                val bitmap = getCapturedImage()
                inputImageView.setImageBitmap(bitmap) // UI에 비트맵 설정

                // 감지 결과를 비동기로 처리
                CoroutineScope(Dispatchers.IO).launch {
                    val detectionResults = performDetection(bitmap) // 감지 수행
                    withContext(Dispatchers.Main) {
                        showDetectionResults(detectionResults) // 감지 결과 표시
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process captured image: ${e.message}")
            }
        }
    }

    /**
     * getBitmapFromUri():
     *      선택한 이미지 URI를 비트맵으로 변환
     */
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bitmap from URI: ${e.message}")
            null
        }
    }


    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    private suspend fun runObjectDetection(bitmap: Bitmap) {
        // Step 1: create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.5f)
            .build()

        val detector = ObjectDetector.createFromFileAndOptions(
            this, // the application context
            "model.tflite", // assets 폴더에 있는 모델 파일명
            options
        )

        // Step 3: feed given image to the model and print the detection result
        val results = detector.detect(image)

        // 결과를 팝업창으로 표시하기
        showDetectionResults(results)

        // Step 4: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}, ${category.score.times(100).toInt()}%"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }

        // 감지 결과를 텍스트로 표시
        val detectionResultText = resultToDisplay.joinToString("\n") { it.text } // 각 결과를 줄바꿈으로 구분
        Log.d(TAG, "Formatted Detection Results: $detectionResultText") // 로그에 출력
        // UI에 결과를 표시하려면 runOnUiThread를 사용하여 UI 스레드에서 업데이트합니다.
        runOnUiThread {
            tvPlaceholder.text = detectionResultText // 텍스트 뷰에 감지 결과 표시
        }

        // Draw the detection result on the bitmap and show it.
        val imgWithResult = drawDetectionResult(bitmap, resultToDisplay)
        runOnUiThread {
            inputImageView.setImageBitmap(imgWithResult)
        }


    }

    private fun debugPrint(results: List<Detection>) {
        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.d(TAG, "Detected object: ${i} ")
            Log.d(TAG, "  boundingBox: (${box.left}, ${box.top}) - (${box.right}, ${box.bottom})")

            for ((j, category) in obj.categories.withIndex()) {
                Log.d(TAG, "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d(TAG, "    Confidence: ${confidence}%")
            }
        }
    }

    /**
     * setViewAndDetect(bitmap: Bitmap)
     *      Set image to view and call object detection
     */
    private fun setViewAndDetect(bitmap: Bitmap) {
        // Display capture image
        inputImageView.setImageBitmap(bitmap)
        tvPlaceholder.visibility = View.INVISIBLE

        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        // Run ODT and display result
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val detectionResults = performDetection(bitmap)
                val detectionName = detectionResults.firstOrNull()?.categories?.firstOrNull()?.label ?: "Unknown"

                // 번역 수행
                val translatedName = translateToKorean(detectionName)

                // UI 스레드에서 팝업 표시
                withContext(Dispatchers.Main) {
                    showInputDialog(translatedName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during detection or translation", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "객체 인식 또는 번역 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * getCapturedImage():
     *      Decodes and crops the captured image from camera.
     */
    private fun getCapturedImage(): Bitmap {
        // Get the dimensions of the View
        val targetW: Int = inputImageView.width
        val targetH: Int = inputImageView.height

        // 로그로 경로 확인
        Log.d(TAG, "currentPhotoPath: $currentPhotoPath")

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // 로그로 원본 사진 크기 출력
            Log.d(TAG, "Photo width: $photoW, Photo height: $photoH")

            // Determine how much to scale down the image
            val scaleFactor: Int = max(1, min(photoW / targetW, photoH / targetH))

            // 로그로 스케일 팩터 출력
            Log.d(TAG, "Scale factor: $scaleFactor")

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }
        val exifInterface = ExifInterface(currentPhotoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        // 로그로 이미지 회전 정보 출력
        Log.d(TAG, "Image orientation: $orientation")

        val bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
        if (bitmap == null) {
            Log.e(TAG, "Failed to decode image from $currentPhotoPath")
            throw IllegalArgumentException("Unable to decode image file.")
        }

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 270f)
            }
            else -> {
                bitmap
            }
        }
    }

    /**
     * getSampleImage():
     *      Get image form drawable and convert to bitmap.
     */
    private fun getSampleImage(drawable: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, drawable, BitmapFactory.Options().apply {
            inMutable = true
        })
    }

    /**
     * rotateImage():
     *     Decodes and crops the captured image from camera.
     */
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }
    private fun initializePhotoPath() {
        // 초기화 코드 예시
        val file = createImageFile()
        currentPhotoPath = file.absolutePath
    }
    /**
     * createImageFile():
     *     Generates a temporary image file for the Camera app to write to.
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    /**
     * dispatchTakePictureIntent():
     *     Start the Camera app to take a photo.
     */
    private fun dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent 호출")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (e: IOException) {
                    Log.e(TAG, e.message.toString())
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "org.tensorflow.codelabs.objectdetection.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)

                    // 사진이 저장된 후 경로를 currentPhotoPath에 할당
                    currentPhotoPath = it.absolutePath
                    Log.d(TAG, "사진 경로: $currentPhotoPath")
                }
            }
        }
    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      Draw a box around each objects and show the object's name.
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)


            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                it.text, box.left + margin,
                box.top + tagSize.height().times(1F), pen
            )
        }
        return outputBitmap
    }
    private suspend fun showDetectionResults(results: List<Detection>) {

        // 감지된 결과에서 첫 번째 객체의 이름을 가져온다고 가정
        val detectionName = results.firstOrNull()?.categories?.firstOrNull()?.label ?: "알 수 없음"
        //detectionName 인식된 결과를 가져온 후 파파고 번역
        Log.d("DetectionResults", "인식된 객체: $detectionName")

        // 파파고 API를 통해 번역
        val translatedName = try {
            translateToKorean(detectionName)
        } catch (e: Exception) {
            Log.e("PapagoError", "번역 실패: ${e.message}")
            "번역 실패"
        }
        Log.d("DetectionResults", "번역된 객체: $translatedName")
        showInputDialog(translatedName)
    }
    //파파고 API
    private suspend fun translateToKorean(text: String): String {
        val clientId = "5hmblvjyrl"
        val clientSecret = "ts8IFHskItIyvZnaDHRMNGZHkZObeoBbYUQeCxTz"
        val url = "https://naveropenapi.apigw.ntruss.com/nmt/v1/translation"

        val requestBody = FormBody.Builder()
            .add("source", "en") // 원본 언어 설정
            .add("target", "ko") // 목표 언어 설정
            .add("text", text)    // 번역할 텍스트
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-NCP-APIGW-API-KEY-ID", clientId) // 새로운 헤더로 변경
            .addHeader("X-NCP-APIGW-API-KEY", clientSecret) // 새로운 헤더로 변경
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val json = JSONObject(responseBody)
                    val translatedText = json.getJSONObject("message")
                        .getJSONObject("result")
                        .getString("translatedText")
                    translatedText
                } else {
                    Log.e("PapagoError", "번역 실패: ${response.code}")
                    throw Exception("번역 요청 실패: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("PapagoError", "번역 실패: ${e.message}")
                throw e  // 예외를 다시 던져서 호출한 곳에서 처리
            }
        }
    }


    private fun showInputDialog(detectionName: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                val dialogBuilder = AlertDialog.Builder(this@ActivityIngredient)
                val inflater = layoutInflater
                val dialogView = inflater.inflate(R.layout.dialog_input, null)
                dialogBuilder.setView(dialogView)

                // UI 요소 초기화
                val editTextName = dialogView.findViewById<EditText>(R.id.editTextName)
                val editTextExpiryDate = dialogView.findViewById<EditText>(R.id.editTextExpiryDate)
                val textViewRegistrationDate = dialogView.findViewById<TextView>(R.id.textViewRegistrationDate)
                val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
                val radioGroupStorage = dialogView.findViewById<RadioGroup>(R.id.radioGroupStorage)

                // 이름 자동 입력
                editTextName.setText(detectionName)

                // 현재 날짜 자동 입력
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                textViewRegistrationDate.text = currentDate

                // Spinner 데이터 설정
                val types = arrayOf("곡류", "두류", "서류", "채소류", "과일류", "식육류", "어패류", "유제품", "달걀류", "음료", "조미료 및 기타")
                val adapter = ArrayAdapter(this@ActivityIngredient, android.R.layout.simple_spinner_item, types)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerType.adapter = adapter

                // 유통기한 EditText 클릭 시 DatePickerDialog 표시
                editTextExpiryDate.setOnClickListener {
                    showDatePicker(editTextExpiryDate)
                }

                dialogBuilder.setPositiveButton("확인") { dialog, _ ->
                    // 입력된 데이터 처리
                    val name = editTextName.text.toString()
                    val expiryDate = editTextExpiryDate.text.toString()
                    val categoryNumber = spinnerType.selectedItemPosition + 1 // 카테고리 넘버
                    val registrationDate = currentDate
                    val storageType = when (radioGroupStorage.checkedRadioButtonId) {
                        R.id.radioRefrigerated -> "refrigeratedItems"
                        R.id.radioFrozen -> "frozenItems"
                        else -> null
                    }
                    if (storageType != null) {
                        //currentPhotoPath가 설정된 상태인지 확인
                        if (::currentPhotoPath.isInitialized) {
                            val data = hashMapOf(
                                "name" to name,
                                "photoPath" to currentPhotoPath,
                                "registrationDate" to currentDate,
                                "expiryDate" to expiryDate,
                                "categoryNumber" to categoryNumber
                            )
                            db.collection(storageType)
                                .document()
                                .set(data)
                                .addOnSuccessListener {
                                    Log.d(TAG, "데이터 저장 성공")
                                    Toast.makeText(applicationContext, "데이터가 성공적으로 추가되었습니다.", Toast.LENGTH_SHORT).show()
                                    finish() // ActivityIngredient로 복귀
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "데이터 저장 실패", e)
                                    Toast.makeText(applicationContext, "데이터 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(applicationContext, "사진 경로가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(applicationContext, "저장 방식을 선택해주세요.", Toast.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }

                    .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }

                val dialog = dialogBuilder.create()
                dialog.show()
            }
        }
    }


    // DatePickerDialog를 표시하는 함수
    private fun showDatePicker(editText: EditText) {
        // 현재 날짜 가져오기
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // DatePickerDialog 생성
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // 선택한 날짜를 표시 (월은 0부터 시작하므로 +1)
                editText.setText(String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay))
            },
            year, month, day
        )

        datePickerDialog.show() // 다이얼로그 표시
    }

}

/**
 * DetectionResult
 *      A class to store the visualization info of a detected object.
 */
data class DetectionResult(val boundingBox: RectF, val text: String)
