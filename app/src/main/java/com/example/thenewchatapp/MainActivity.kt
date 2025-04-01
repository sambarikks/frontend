//2025-04-01
//일반글 <-> 챗봇 전환 코드 수정(일반글 내용 유지, 단 챗봇에서 추가한 내용 일반글로 안넘어감)
//말풍선 내려가도록 수정
//입력필드 올라오도록 수정

package com.example.thenewchatapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    // EditText: 메모 내용을 입력하는 영역
    private lateinit var editText: EditText
    // 버튼: 현재 메모(대화) 내용을 ChatActivity로 전달
    private lateinit var goToChatButton: ImageButton
    // 내부 저장소에 저장할 파일명
    private val fileName = "notepad.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // XML에 정의된 뷰를 초기화합니다.
        editText = findViewById(R.id.editText)
        // XML 버튼 ID가 goToChatBotButton으로 되어 있더라도,
        // 실제 기능은 ChatActivity 전환이므로 변수명만 변경합니다.
        goToChatButton = findViewById(R.id.goToChatBotButton)

        // 텍스트가 변경될 때마다 자동으로 파일에 저장합니다.
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let { saveTextToFile(it.toString()) }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 버튼 클릭 시, 입력된 메모(대화) 내용을 ChatActivity로 전달합니다.
        goToChatButton.setOnClickListener {
            val conversation = editText.text.toString()
            // ChatActivity로 전환 (이전 ChatbotActivity 대신 ChatActivity 사용)
            val intent = Intent(this, ChatActivity::class.java)
            // 전달할 데이터를 "conversation" 키에 담아 Intent에 추가합니다.
            intent.putExtra("conversation", conversation) // 이 호출이 있어야 ChatActivity로 화면 전환됨
            startActivity(intent)
        }
    }

    // 주어진 텍스트를 내부 저장소의 파일에 저장하는 함수입니다.
    private fun saveTextToFile(text: String) {
        val file = File(filesDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(text.toByteArray())
        }
    }

    // 액티비티가 백그라운드로 전환되기 전에 현재 텍스트를 저장합니다.
    override fun onPause() {
        super.onPause()
        saveTextToFile(editText.text.toString())
    }
}
