/*
 * Project: TheNewChatApp
 * Version: 1.4.0
 * Last updated: 2025-05-01
 * Author: SiHyeon Cheon
 * Description: 메모 작성, 기존 파일 열기, 저장 버튼으로 목록창 이동 기능 포함 (자동 저장 완전 제거)
 */

package com.example.thenewchatapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var goToChatButton: ImageButton
    private lateinit var saveButton: Button

    private val customExtension = ".mdocx"
    private var openedFileName: String? = null // 현재 열려있는 문서 이름

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editText)
        goToChatButton = findViewById(R.id.goToChatBotButton)
        saveButton = findViewById(R.id.saveButton)

        // 전달받은 파일명이 있으면 해당 파일 열기
        openedFileName = intent.getStringExtra("fileName")
        openedFileName?.let { fileName ->
            val file = File(filesDir, fileName)
            if (file.exists()) {
                val content = file.readText()
                editText.setText(content)
            }
        }

        // 챗봇 버튼 클릭 -> ChatActivity로 이동
        goToChatButton.setOnClickListener {
            val conversation = editText.text.toString()
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("conversation", conversation)
            startActivity(intent)
        }

        // 저장 버튼 클릭 -> 파일 저장 후 목록으로 이동
        saveButton.setOnClickListener {
            saveCurrentDocument()
        }
    }

    // 명시적 저장만 담당
    private fun saveCurrentDocument() {
        val content = editText.text.toString().trim()

        // 내용이 없으면 저장 안 함
        if (content.isEmpty()) {
            Toast.makeText(this, "내용이 없습니다. 저장되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = openedFileName ?: generateFileName()
        val file = File(filesDir, fileName)
        file.writeText(content)

        Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, ListActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    // 새 문서용 파일명 생성
    private fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val time = formatter.format(Date())
        return "노트_$time$customExtension"
    }

    // 자동 저장 완전히 제거 (onPause 비워둠)
    override fun onPause() {
        super.onPause()
        // 자동 저장 없음
    }
}
