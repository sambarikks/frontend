/*
 * Project: TheNewChatApp
 * Version: 2.0.0
 * Last updated: 2025-05-04
 * Author: SiHyeon Cheon
 *
 * [Description]
 * 메모 작성 및 저장, 불러오기 기능을 담당하는 메인 에디터 화면.
 *
 * [주요 기능]
 * - 기존 문서 열기 및 불러오기
 * - 새 문서 작성 및 자동/수동 저장 기능
 * - 사용자가 입력한 제목과 자동 제목(텍스트 노트 MMdd_HHmmss) 규칙 적용
 * - 사용자가 입력하지 않을 경우 제목 칸은 빈 상태 유지 (자동 제목만 파일에 사용됨)
 * - 저장 시 기존 문서를 열어둔 상태일 경우 덮어쓰기 처리 (중복 저장 방지)
 * - ChatActivity로 글 내용을 전달해 AI와 대화 시작 가능
 * - 빈 내용일 경우 저장하지 않음
 * - 수동 저장 버튼과 뒤로가기 버튼 모두 저장 트리거로 동작
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
        saveButton = findViewById<Button?>(R.id.saveButton)

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

