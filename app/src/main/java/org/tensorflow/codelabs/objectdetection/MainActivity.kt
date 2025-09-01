package org.tensorflow.codelabs.objectdetection;

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.tensorflow.codelabs.objectdetection.R
import org.tensorflow.codelabs.objectdetection.ActivityIngredient

class MainActivity : AppCompatActivity() {

    private lateinit var addIngredientButton: ImageButton
    private lateinit var buttonRecipe: ImageButton // 버튼 이름 수정
    private lateinit var buttonIngredients: ImageButton // 버튼 이름 수정
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var buttonMyInfo: ImageButton
    private lateinit var textLoginPrompt: TextView
    // ActivityResultLauncher 선언
    private lateinit var loginActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var buttonLogout: ImageButton
    private var isLoggedIn: Boolean = false // 로그인 상태 확인 변수
    private lateinit var firebaseAuth: FirebaseAuth


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        firebaseAuth = FirebaseAuth.getInstance()

        // 버튼 초기화
        addIngredientButton = findViewById(R.id.addIngredientButton)
        buttonRecipe = findViewById(R.id.button_recipe) // button_recipe ID를 사용하여 초기화
        buttonIngredients = findViewById(R.id.button_ingredients)
        buttonLogout = findViewById(R.id.button_logout) // 로그아웃 버튼 초기화
        textLoginPrompt = findViewById(R.id.textLoginPrompt) // textLoginPrompt 초기화

        // Intent로 전달받은 사용자 이메일 표시
        val userEmail = intent.getStringExtra("userEmail")
        textLoginPrompt.text = userEmail?.substringBefore("@") ?: "로그인 정보 없음"

        // ActivityResultLauncher 초기화
        loginActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                isLoggedIn = true
                updateUI() // 로그인 후 UI 갱신
            }
        }

        // 버튼 클릭 리스너 설정
        addIngredientButton.setOnClickListener {
            val intent = Intent(this, ActivityIngredient::class.java) // 새로운 액티비티로 이동
            startActivity(intent)
        }

        // buttonRecipe 클릭 시 RecipeActivity로 이동
        buttonRecipe.setOnClickListener {
            val intent = Intent(this, RecipeActivity::class.java) // RecipeActivity로 이동
            startActivity(intent)
        }

        // "식재료" 버튼 클릭 시 SavedIngredientsActivity로 이동
        buttonIngredients.setOnClickListener {
            val intent = Intent(this, SavedIngredientsActivity::class.java)
            startActivity(intent)
        }

        buttonLogout.setOnClickListener {
            firebaseAuth.signOut()  // Firebase 로그아웃
            updateUI() // 로그아웃 후 UI 갱신
            // 로그아웃 후 SignInActivity로 이동
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish() // MainActivity 종료
        }

        // Firebase Analytics 초기화
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // UI 갱신
        updateUI()
    }

    private fun updateUI() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            // 로그인 상태일 때
            buttonLogout.visibility = Button.VISIBLE
            val email = user.email?.substringBefore("@") ?: "이메일 정보 없음" // Firebase 사용자 이메일에서 "@" 앞부분만 가져오기
            textLoginPrompt.text = "$email" // 이메일 정보 표시 (이메일의 앞 부분만)
        } else {
            // 로그인되지 않았을 때
            buttonLogout.visibility = Button.GONE
            textLoginPrompt.text = "로그인해주세요"
        }
    }


    // 로그인 상태에 따른 UI 업데이트
    override fun onStart() {
        super.onStart()
        updateUI() // 로그인 상태에 따라 UI를 업데이트
    }

}


