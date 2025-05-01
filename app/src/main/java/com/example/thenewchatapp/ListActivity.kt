package com.example.thenewchatapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var listAdapter: ListAdapter
    private lateinit var createButton: FloatingActionButton

    private val customExtension = ".mdocx"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        createButton = findViewById(R.id.buttonCreateDocument)
        createButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        loadDocumentsAgain()
    }

    // ✅ RecyclerView 새로고침
    private fun loadDocumentsAgain() {
        val documentList = loadDocuments()
        listAdapter = ListAdapter(
            documentList,
            onClick = { fileName -> openDocument(fileName) },
            onMenuAction = { fileName, action ->
                when (action) {
                    "delete" -> confirmDelete(fileName)
                }
            }
        )
        recyclerView.adapter = listAdapter
    }

    // ✅ 확장자 필터링
    private fun loadDocuments(): List<String> {
        val files = filesDir.listFiles()
        return files
            ?.filter { it.isFile && it.name.endsWith(customExtension) }
            ?.map { it.name }
            ?: emptyList()
    }

    // ✅ 문서 클릭 시 MainActivity로 이동
    private fun openDocument(fileName: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("fileName", fileName)
        startActivity(intent)
    }

    // ✅ 개별 파일 삭제 확인
    private fun confirmDelete(fileName: String) {
        AlertDialog.Builder(this)
            .setTitle("문서 삭제")
            .setMessage("정말로 '$fileName' 을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> deleteDocument(fileName) }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteDocument(fileName: String) {
        val file = File(filesDir, fileName)
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show()
                loadDocumentsAgain()
            } else {
                Toast.makeText(this, "삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "파일이 존재하지 않습니다", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onResume() {
        super.onResume()
        loadDocumentsAgain()
    }
}
