/*
 * Project: TheNewChatApp
 * Version: 3.0.0
 * Last updated: 2025-05-24
 * Author: SiHyeon Cheon
 *
 * [Description]
 * 작성된 문서 목록을 표시하고, 문서를 생성/삭제/편집하거나 챗봇과 연결할 수 있는 홈 화면 역할을 담당.
 * 상단에는 로고와 앱 이름, 아이콘 메뉴(햄버거·검색·점3개)를 포함하며 스크롤에 따라 동작이 달라지는 UX를 지원.
 *
 * [주요 기능]
 * - 작성된 문서 목록을 2열 그리드로 표시
 * - + 버튼으로 새 문서 작성 (MainActivity), 작문 도우미(ChatActivity), 요구사항 입력(FieldActivity) 연결
 * - 문서 항목 클릭 시 열기, 길게 클릭 시 선택 모드 진입 (제목 수정, 삭제 가능)
 * - 문서 저장은 내부 저장소에 `.mdocx` 파일로 관리되며, 수정 시 자동 반영됨
 * - 하단 FAB 버튼들은 선택 모드일 경우 자동 숨김 처리
 * - 앱 상단 인터페이스에 로고 이미지, 앱 이름, 아이콘 메뉴 포함
 * - 스크롤 시 로고는 위로 사라지고, 아이콘 메뉴는 상단에 고정됨
 * - 햄버거·검색·점3개 아이콘에 클릭 리스너 등록 (후속 기능 연결 준비)
 */



package com.example.thenewchatapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var editText: EditText
    private lateinit var backButton: ImageButton
    private lateinit var goToChatBotButton: ImageButton
    private var saveButton: Button? = null

    private val customExtension = ".mdocx"
    private var currentFileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleEditText = findViewById(R.id.titleEditText)
        editText = findViewById(R.id.editText)
        backButton = findViewById(R.id.backButton)
        goToChatBotButton = findViewById(R.id.goToChatBotButton)
//        saveButton = findViewById<Button?>(R.id.saveButton)

        saveButton?.setOnClickListener {
            val content = editText.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "입력한 내용이 없어 저장하지 않았습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveIfNeeded(forceSave = true)
        }

        backButton.setOnClickListener {
            finish()
        }

        goToChatBotButton.setOnClickListener {
            val userInput = editText.text.toString().trim()
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("conversation", userInput)
            startActivity(intent)
        }

        val fileName = intent.getStringExtra("fileName")
        if (!fileName.isNullOrEmpty()) {
            val file = File(filesDir, fileName)
            if (file.exists()) {
                val pureTitle = file.name.removeSuffix(customExtension).substringBefore("_")
                if (!pureTitle.startsWith("텍스트 노트 ")) {
                    titleEditText.setText(pureTitle)
                }
                editText.setText(file.readText())
                currentFileName = fileName
            }
        }
    }

    override fun onBackPressed() {
        saveIfNeeded(forceSave = false)
        super.onBackPressed()
    }

    private fun saveIfNeeded(forceSave: Boolean = false) {
        val content = editText.text.toString().trim()
        if (content.isEmpty() && !forceSave) {
            Toast.makeText(this, "입력한 내용이 없어 저장하지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val inputTitle = titleEditText.text.toString().trim()
        val currentTime = Date()

        val saveFileName = when {
            currentFileName != null -> currentFileName!!
            inputTitle.isNotEmpty() -> inputTitle + customExtension
            else -> "텍스트 노트 " + SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(currentTime) + customExtension
        }

        val file = File(filesDir, saveFileName)
        file.writeText(content)
        Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()

        if (currentFileName == null) {
            currentFileName = saveFileName
        }
    }
}

