package org.tensorflow.codelabs.objectdetection

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.tensorflow.codelabs.objectdetection.databinding.ActivitySigninBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        auth = FirebaseAuth.getInstance()


        binding.textView.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passET.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { // 이메일 형식 검사
                    firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                        if (it.isSuccessful) {
                            val intent = Intent(this, MainActivity::class.java)
                            // 이메일 정보를 전달
                            intent.putExtra("userEmail", email)
                            startActivity(intent)
                            finish() // 로그인 후 현재 액티비티 종료
                        } else {
                            Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "이메일 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "빈 칸을 모두 입력해주세요!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // 이미 로그인된 사용자가 있다면 자동으로 MainActivity로 이동
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) { // currentUser가 null이 아닌지 확인
            val email = currentUser.email ?: "Unknown" // 이메일이 null인 경우 기본값 설정
            val intent = Intent(this, MainActivity::class.java)
            // 이미 로그인된 사용자의 이메일 정보 전달
            intent.putExtra("userEmail", email)
            startActivity(intent)
            finish() // 로그인된 상태에서 로그인 화면을 다시 보지 않도록 종료
        }
    }
}
