package org.tensorflow.codelabs.objectdetection

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.tensorflow.codelabs.objectdetection.R

class DetectionResultDialogFragment : DialogFragment() {

    // 이 메서드를 통해 생성할 때 결과 텍스트를 전달합니다.
    companion object {
        fun newInstance(resultText: String): DetectionResultDialogFragment {
            val fragment = DetectionResultDialogFragment()
            val args = Bundle().apply {
                putString("result", resultText)
            }
            fragment.arguments = args
            return fragment
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_detection_result, container, false)

        // 결과 텍스트를 가져옵니다.
        val resultText = arguments?.getString("result") ?: "결과 없음"
        val resultTextView = view.findViewById<TextView>(R.id.resultTextView)
        resultTextView.text = resultText

        // 닫기 버튼 설정
        val closeButton = view.findViewById<Button>(R.id.buttonClose)
        closeButton.setOnClickListener {
            dismiss() // 다이얼로그 닫기
        }

        return view
    }
}
