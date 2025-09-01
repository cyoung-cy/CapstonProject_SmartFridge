package org.tensorflow.codelabs.objectdetection

import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.tensorflow.codelabs.objectdetection.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeActivity : AppCompatActivity() {

    private lateinit var ingredientEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>() // 레시피 리스트 초기화
    private val db = FirebaseFirestore.getInstance() // Firestore 인스턴스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)

        ingredientEditText = findViewById(R.id.ingredientEditText)
        searchButton = findViewById(R.id.searchButton)
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView)

        // RecyclerView 설정
        recipeRecyclerView.layoutManager = LinearLayoutManager(this)

        // RecipeAdapter 초기화 (클릭 리스너 추가)
        recipeAdapter = RecipeAdapter(recipeList) { recipe, _ ->
            // 레시피 클릭 시 상세 정보 화면으로 이동
            val intent = Intent(this, RecipeDetailActivity::class.java).apply {
                putExtra("menu_name", recipe.name) // 레시피 이름 전달
                putExtra("instructions", recipe.instructions)
                putExtra("ingredients", recipe.ingredients) // ingredients는 String임
            }
            startActivity(intent)
        }
        recipeRecyclerView.adapter = recipeAdapter

        // 전체 레시피 목록 가져오기
        fetchAllRecipes()

        ingredientEditText.hint = "식재료를 입력하세요."

        // 검색 버튼 클릭 리스너
        searchButton.setOnClickListener {
            val ingredient = ingredientEditText.text.toString().trim()
            if (ingredient.isNotEmpty()) {
                searchRecipes(ingredient) // 검색 기능 호출
            } else {
                ingredientEditText.error = "식재료를 입력하세요." // 입력이 비어있을 경우 에러 메시지
            }
        }
    }

    private fun fetchAllRecipes() {
        CoroutineScope(Dispatchers.IO).launch {
            val recipes = mutableListOf<Recipe>()
            db.collection("recipes")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val menuName = document.getString("menu_name") ?: ""
                        val instructions = document.getString("instructions") ?: ""
                        val ingredientsList = document.get("ingredients") as? List<String> ?: emptyList()
                        val ingredientsStr = ingredientsList.joinToString(", ")
                        val recipeId = document.id // 레시피 ID 가져오기
                        recipes.add(Recipe(name = menuName, instructions = instructions, ingredients = ingredientsStr, id = recipeId))
                    }
                    // UI 업데이트
                    CoroutineScope(Dispatchers.Main).launch {
                        updateUI(recipes)
                    }
                }
                .addOnFailureListener { exception ->
                    println("Error getting documents: $exception")
                }
        }
    }

    private fun searchRecipes(ingredient: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val recipes = mutableListOf<Recipe>()
            db.collection("recipes")
                .whereArrayContains("ingredients", ingredient) // ingredients 필드에서 재료 검색
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val menuName = document.getString("menu_name") ?: ""
                        val instructions = document.getString("instructions") ?: ""

                        // ingredients 필드를 List<String>으로 가져오기
                        val ingredientsList = document.get("ingredients") as? List<String> ?: emptyList()
                        val ingredients = ingredientsList.joinToString(", ") // List를 String으로 변환

                        // Recipe 객체 생성
                        val recipeId = document.id // 레시피 ID 가져오기
                        recipes.add(Recipe(name = menuName, instructions = instructions, ingredients = ingredients, id = recipeId)) // ID 추가
                    }
                    // 결과를 UI에 업데이트
                    CoroutineScope(Dispatchers.Main).launch {
                        updateUI(recipes)
                    }
                }
                .addOnFailureListener { exception ->
                    // 에러 처리
                    println("Error getting documents: $exception")
                }
        }
    }

    private fun updateUI(recipes: List<Recipe>) {
        recipeList.clear() // 기존 레시피 리스트 초기화
        recipeList.addAll(recipes) // 새로 검색된 레시피 추가
        recipeAdapter.notifyDataSetChanged() // 데이터 변경 시 어댑터에 알리기
    }
}