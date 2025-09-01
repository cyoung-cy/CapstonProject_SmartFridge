package org.tensorflow.codelabs.objectdetection

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        // 로그인 상태 확인
        val user = firebaseAuth.currentUser

        if (user != null) {
            // 로그인된 경우 MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // 로그인되지 않은 경우 SignInActivity로 이동
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        // SplashActivity 종료
        finish()
    }
}
