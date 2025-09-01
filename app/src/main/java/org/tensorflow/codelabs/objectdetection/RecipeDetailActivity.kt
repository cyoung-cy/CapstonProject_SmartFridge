package org.tensorflow.codelabs.objectdetection

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.tensorflow.codelabs.objectdetection.R

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var menuNameTextView: TextView
    private lateinit var ingredientsLayout: LinearLayout // 재료 레이아웃 추가
    private lateinit var instructionsTextView: TextView
    private lateinit var recipeImageView: ImageView // 이미지 뷰 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        menuNameTextView = findViewById(R.id.menuNameTextView)
        ingredientsLayout = findViewById(R.id.ingredientsLayout) // 재료 레이아웃 초기화
        instructionsTextView = findViewById(R.id.instructionsTextView)
        recipeImageView = findViewById(R.id.recipeImageView) // 이미지 뷰 초기화

        // Firestore에서 레시피 데이터 가져오기
        val db = FirebaseFirestore.getInstance()
        val menuName = intent.getStringExtra("menu_name") // Intent로부터 레시피 이름 가져오기

        if (menuName != null) {
            db.collection("recipes")
                .whereEqualTo("menu_name", menuName) // menu_name 필드로 검색
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val document = result.documents[0] // 첫 번째 문서 가져오기
                        val ingredients =
                            document.get("ingredients") as? List<String> ?: listOf("재료 없음")
                        val instructions = document.getString("instructions") ?: "요리법 없음"
                        val imageUrl = document.getString("image") ?: ""

                        // 로그로 데이터 출력
                        Log.d("Firestore", "Menu Name: $menuName")
                        Log.d("Firestore", "Ingredients: $ingredients")
                        Log.d("Firestore", "Instructions: $instructions")
                        Log.d("Firestore", "Image URL: $imageUrl")

                        // TextView에 데이터 설정
                        menuNameTextView.text = menuName
                        instructionsTextView.text = formatInstructions(instructions)

                        // 재료와 구매 버튼 추가
                        addIngredientsWithButtons(ingredients)

                        // Glide를 사용하여 이미지 로드
                        if (imageUrl.isNotEmpty()) {
                            Glide.with(this)
                                .load("$imageUrl?timestamp=${System.currentTimeMillis()}") // 캐시 무효화
                                .error(R.drawable.placeholder_image) // 실패 시 대체 이미지
                                .into(recipeImageView) // 이미지 뷰에 로드
                        } else {
                            recipeImageView.setImageResource(R.drawable.placeholder_image) // URL이 비어있을 경우 대체 이미지 설정
                        }
                    } else {
                        Log.d("Firestore", "No matching recipes found")
                        Toast.makeText(this, "레시피를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("FirebaseError", "Error getting document: ", exception)
                    Toast.makeText(this, "데이터를 가져오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.d("Firestore", "No menu name provided")
            Toast.makeText(this, "레시피 이름이 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addIngredientsWithButtons(ingredients: List<String>) {
        ingredientsLayout.removeAllViews() // 기존 뷰 제거

        for (ingredient in ingredients) {
            val ingredientLayout = LinearLayout(this)
            ingredientLayout.orientation = LinearLayout.HORIZONTAL

            val ingredientTextView = TextView(this)
            ingredientTextView.text = ingredient
            ingredientTextView.layoutParams =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val buyButton = Button(this)
            buyButton.text = "구매"
            buyButton.setOnClickListener {
                openShoppingPopup(ingredient) // 팝업창 열기
            }

            ingredientLayout.addView(ingredientTextView)
            ingredientLayout.addView(buyButton)
            ingredientsLayout.addView(ingredientLayout)
        }
    }

    private fun openShoppingPopup(ingredient: String) {
        // 팝업창 레이아웃 인플레이터
        val popupView = layoutInflater.inflate(R.layout.popup_shopping_sites, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // 네이버 버튼 클릭 리스너
        popupView.findViewById<ImageButton>(R.id.naverButton).setOnClickListener {
            openShoppingSite("naver", ingredient)
            popupWindow.dismiss() // 팝업 닫기
        }

        // 쿠팡 버튼 클릭 리스너
        popupView.findViewById<ImageButton>(R.id.coupangButton).setOnClickListener {
            openShoppingSite("coupang", ingredient)
            popupWindow.dismiss() // 팝업 닫기
        }


        // 팝업창을 화면에 띄우기
        popupWindow.isFocusable = true
        popupWindow.showAtLocation(
            findViewById(R.id.main),
            Gravity.CENTER,
            0,
            0
        ) // mainLayout은 Activity의 루트 레이아웃 ID

        val closeButton: ImageButton = popupView.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            // PopupWindow 닫기
            popupWindow.dismiss()
        }
    }

    private fun openShoppingSite(site: String, ingredient: String) {
        val query = Uri.encode(ingredient) // 재료명 URL 인코딩
        val url = when (site) {
            "naver" -> "https://search.shopping.naver.com/search/all?query=$query" // 네이버 URL
            "coupang" -> "https://www.coupang.com/np/search?component=&q=$query" // 쿠팡 URL
            else -> return
        }

        // 브라우저에서 URL 열기
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun formatInstructions(instructions: String): String {
        // 정규식을 사용해 숫자+점 패턴 앞에서 줄바꿈 추가
        return instructions
            .replace("\\n", "\n") // 기존 문자열에서 \n을 실제 줄바꿈으로 변환
            .replace("(\\d+\\.\\s)".toRegex(), "\n$1") // "숫자. " 앞에 줄바꿈 추가
            .trim() // 불필요한 앞뒤 공백 제거
    }
}
