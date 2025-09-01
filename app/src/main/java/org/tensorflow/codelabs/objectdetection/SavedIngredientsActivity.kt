package org.tensorflow.codelabs.objectdetection

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.tensorflow.codelabs.objectdetection.R

class SavedIngredientsActivity : AppCompatActivity() {

    private lateinit var listViewSavedIngredients: ListView
    private lateinit var spinnerCategory: Spinner
    private lateinit var buttonRecipe: Button
    private var currentCategory: String = "전체"
    private val db = FirebaseFirestore.getInstance()
    private val selectedIngredients = mutableListOf<String>()  // 선택된 식재료 목록
    private val savedIngredients = mutableListOf<String>() // 저장된 식재료 목록을 위한 변수 추가

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_ingredients)

        val buttonRecommendRecipes = findViewById<Button>(R.id.buttonRecommendRecipes)
        // 버튼 초기 상태 설정
        buttonRecipe = findViewById(R.id.buttonRecipe)
        buttonRecipe.visibility = View.GONE // 시작 시 버튼 숨기기


        listViewSavedIngredients = findViewById(R.id.listViewSavedIngredients)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        buttonRecipe = findViewById(R.id.buttonRecipe)

        val categories = arrayOf("전체", "냉장", "냉동", "유통기한")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentCategory = categories[position]
                loadIngredients(currentCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // "레시피 추천받기" 버튼 클릭 리스너
        buttonRecommendRecipes.setOnClickListener {
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, savedIngredients)

            // ListView 설정: 다중 선택 가능하도록 설정
            listViewSavedIngredients.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            listViewSavedIngredients.adapter = adapter

            // 선택된 항목 추적
            listViewSavedIngredients.setOnItemClickListener { _, _, position, _ ->
                val selectedItem = savedIngredients[position]
                // 선택된 항목을 선택하거나 해제
                if (listViewSavedIngredients.isItemChecked(position)) {
                    selectedIngredients.add(selectedItem)
                } else {
                    selectedIngredients.remove(selectedItem)
                }
                // 선택된 항목이 하나 이상이면 "레시피 보러가기" 버튼을 보이게 함
                if (selectedIngredients.isNotEmpty()) {
                    buttonRecipe.visibility = View.VISIBLE
                } else {
                    buttonRecipe.visibility = View.GONE
                }
            }

            // 선택된 재료들을 출력
            val selectedIngredientsList = selectedIngredients.joinToString(", ")
            Toast.makeText(this, "선택된 재료: $selectedIngredientsList", Toast.LENGTH_SHORT).show()
        }

        buttonRecipe.setOnClickListener {
            if (selectedIngredients.isNotEmpty()) {
                // Intent를 사용하여 RecipeActivity로 이동
                val intent = Intent(this, RecipeActivity::class.java)
                intent.putStringArrayListExtra("selectedIngredients", ArrayList(selectedIngredients))
                startActivity(intent)
            } else {
                Toast.makeText(this, "선택된 재료가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // ListView 아이템 클릭 리스너
        listViewSavedIngredients.setOnItemClickListener { _, _, position, _ ->
            val ingredientName = (listViewSavedIngredients.adapter.getItem(position) as String)
            showIngredientDetails(ingredientName)
        }

        // ListView 아이템 길게 클릭 리스너
        listViewSavedIngredients.setOnItemLongClickListener { _, _, position, _ ->
            val ingredientName = (listViewSavedIngredients.adapter.getItem(position) as String)
            showDeleteConfirmationDialog(ingredientName)
            true
        }

    }

    private fun loadIngredients(category: String) {
        val collectionName = when (category) {
            "냉장" -> "refrigeratedItems"
            "냉동" -> "frozenItems"
            "전체" -> null // "전체"일 경우에는 냉장과 냉동을 모두 불러오도록 처리
            else -> null
        }

        if (category == "전체") {
            // 전체 카테고리 선택 시 냉장, 냉동 데이터 모두 불러오기
            val allCollections = listOf("refrigeratedItems", "frozenItems")

            savedIngredients.clear() // 이전 목록을 초기화

            allCollections.forEach { collection ->
                db.collection(collection)
                    .get()
                    .addOnSuccessListener { documents ->
                        savedIngredients.addAll(documents.map { it.getString("name") }.filterNotNull())
                        val adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_1,
                            savedIngredients
                        )
                        listViewSavedIngredients.adapter = adapter
                    }
            }
        } else if (collectionName != null) {
            // 냉장 또는 냉동 카테고리일 경우
            db.collection(collectionName)
                .get()
                .addOnSuccessListener { documents ->
                    savedIngredients.clear() // 이전 목록을 초기화
                    savedIngredients.addAll(documents.map { it.getString("name") }.filterNotNull())
                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, savedIngredients)
                    listViewSavedIngredients.adapter = adapter
                }
                .addOnFailureListener {
                    Toast.makeText(applicationContext, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
        } else if (category == "유통기한") {
            // 유통기한에 따른 정렬
            val allCollections = listOf("refrigeratedItems", "frozenItems")

            savedIngredients.clear() // 이전 목록을 초기화

            allCollections.forEach { collection ->
                db.collection(collection)
                    .orderBy("expiryDate")
                    .get()
                    .addOnSuccessListener { documents ->
                        savedIngredients.addAll(documents.map { it.getString("name") }.filterNotNull())
                        val adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_1,
                            savedIngredients
                        )
                        listViewSavedIngredients.adapter = adapter
                    }
            }
        } else {
            Toast.makeText(applicationContext, "지원되지 않는 카테고리입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 선택한 식재료를 리스트에 추가/제거하는 함수
    private fun toggleSelection(ingredient: String) {
        if (selectedIngredients.contains(ingredient)) {
            selectedIngredients.remove(ingredient)
        } else {
            selectedIngredients.add(ingredient)
        }
        updateRecipeButtonVisibility()
    }

    // 레시피 버튼의 가시성을 업데이트하는 함수
    private fun updateRecipeButtonVisibility() {
        buttonRecipe.visibility = if (selectedIngredients.isNotEmpty()) View.VISIBLE else View.INVISIBLE
    }

    // 선택된 식재료로 레시피를 검색하는 함수
    private fun searchRecipesByIngredients(selectedIngredients: List<String>) {
        db.collection("recipes")
            .whereArrayContainsAny("ingredients", selectedIngredients)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val recipeList = documents.map { it.getString("recipeName") ?: "Unknown Recipe" }
                    showRecipeList(recipeList)
                } else {
                    Toast.makeText(applicationContext, "선택된 식재료로 찾을 수 있는 레시피가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, "레시피 검색에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 레시피 목록을 다이얼로그로 표시하는 함수
    private fun showRecipeList(recipes: List<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("레시피 목록")
        builder.setItems(recipes.toTypedArray()) { _, _ -> }
        builder.setPositiveButton("닫기") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    private fun showIngredientDetails(name: String) {
        val collections = listOf("refrigeratedItems", "frozenItems") // 두 테이블 검색
        var foundData = false // 데이터를 찾았는지 여부를 저장하는 플래그

        // 모든 테이블을 검색하고 나서 오류 처리
        val processCompletion = {
            if (!foundData) {
                showErrorDialog("재료 정보를 불러올 수 없습니다.\n이름($name)에 해당하는 데이터가 없습니다.")
            }
        }

        // 첫 번째 컬렉션부터 검색 시작
        for (collection in collections) {
            db.collection(collection)
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val details = documents.documents.firstOrNull()
                        if (details != null) {
                            // 데이터를 찾았으면 표시하고 종료
                            if (!foundData) {
                                foundData = true
                                displayIngredientDetails(name, details)
                            }
                        }
                    }

                    // 두 번째 테이블까지 검색했는데도 데이터를 못 찾은 경우
                    if (collections.indexOf(collection) == collections.lastIndex) {
                        processCompletion() // 마지막 테이블까지 검색했으면 처리 완료
                    }
                }
                .addOnFailureListener { e ->
                    showErrorDialog("데이터를 불러오는 중 오류가 발생했습니다: ${e.message}")
                }
        }
    }

    // 데이터를 다이얼로그로 표시하는 함수
    private fun displayIngredientDetails(name: String, details: DocumentSnapshot) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(name)

        // 필드 값 가져오기
        val registrationDate = details.getString("registrationDate") ?: "알 수 없음"
        val expiryDate = details.getString("expiryDate") ?: "알 수 없음"
        val categoryNumber = details.getLong("categoryNumber") ?: -1

        // categoryNumber를 한글로 변환
        val categoryName = getCategoryName(categoryNumber.toInt())

        // 세부정보 메시지 생성
        val message = """
        등록한 날짜: $registrationDate
        유통기한: $expiryDate
        카테고리: $categoryName
    """.trimIndent()
        builder.setMessage(message)

        // 이미지 처리
        val photoPath = details.getString("photoPath")
        if (!photoPath.isNullOrEmpty()) {
            val imageView = ImageView(this)
            val imageBitmap = BitmapFactory.decodeFile(photoPath)
            if (imageBitmap != null) {
                imageView.setImageBitmap(imageBitmap)
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                builder.setView(imageView)
            }
        }

        // 다이얼로그 표시
        builder.setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // 카테고리 번호를 한글 이름으로 변환
    private fun getCategoryName(categoryNumber: Int): String {
        return when (categoryNumber) {
            1 -> "곡류"
            2 -> "두류"
            3 -> "서류"
            4 -> "채소류"
            5 -> "과일류"
            6 -> "식육류"
            7 -> "어패류"
            8 -> "유제품"
            9 -> "달걀류"
            10 -> "음료"
            11 -> "조미료 및 기타"
            else -> "알 수 없음"
        }
    }

    // 오류 메시지를 표시하는 함수
    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("오류")
        builder.setMessage(message)
        builder.setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    private fun showDeleteConfirmationDialog(name: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("삭제 확인")
        builder.setMessage("정말로 '$name' 식재료를 삭제하시겠습니까?")

        builder.setPositiveButton("삭제") { dialog, _ ->
            // '냉장'과 '냉동' 컬렉션에서 삭제를 시도합니다.
            val collections = listOf("refrigeratedItems", "frozenItems")
            var deleted = false

            // 각 컬렉션에 대해 삭제를 시도
            for (collection in collections) {
                db.collection(collection)
                    .whereEqualTo("name", name)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            // 문서가 존재하면 삭제
                            documents.documents.first().reference.delete().addOnSuccessListener {
                                // 삭제가 완료되면 Toast 메시지와 함께 목록 갱신
                                Toast.makeText(applicationContext, "'$name' 식재료가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                loadIngredients(currentCategory)  // 목록 갱신
                            }
                        } else if (collections.indexOf(collection) == collections.lastIndex) {
                            // 두 컬렉션에서 모두 찾을 수 없으면 이 메시지 출력
                            Toast.makeText(applicationContext, "삭제할 식재료를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

}
