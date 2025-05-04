package com.example.thenewchatapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var listAdapter: ListAdapter
    private lateinit var createButton: ImageButton
    private lateinit var bottomBar: LinearLayout
    private lateinit var buttonRename: TextView
    private lateinit var buttonDelete: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        recyclerView = findViewById(R.id.recyclerView)
        createButton = findViewById(R.id.buttonCreateDocument)
        bottomBar = findViewById(R.id.bottomBar)
        buttonRename = findViewById(R.id.buttonRename)
        buttonDelete = findViewById(R.id.buttonDelete)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        createButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        loadDocumentsAndCreateAdapter()

        buttonRename.setOnClickListener {
            val selected = listAdapter.getSelectedItems()
            if (selected.size == 1) {
                showRenameDialog(selected.first())
            }
        }

        buttonDelete.setOnClickListener {
            val selected = listAdapter.getSelectedItems()
            if (selected.isNotEmpty()) {
                showDeleteDialog(selected)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDocumentsAndCreateAdapter()
    }

    private fun loadDocumentsAndCreateAdapter() {
        val documentList = loadDocuments().map { file -> file.name }

        listAdapter = ListAdapter(documentList,
            onItemClick = { fileName ->
                openDocument(fileName)
            },
            onSelectionChanged = { selectedDocs ->
                bottomBar.visibility = if (selectedDocs.isNotEmpty()) View.VISIBLE else View.GONE
                createButton.visibility = if (selectedDocs.isNotEmpty()) View.GONE else View.VISIBLE
            }
        )

        recyclerView.adapter = listAdapter
    }

    private fun loadDocuments(): List<File> {
        return filesDir.listFiles()?.filter {
            it.name.endsWith(".mdocx")
                    // || !it.name.contains(".") // 모든 확장자 보기
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    private fun openDocument(fileName: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("fileName", fileName)
        startActivity(intent)
    }

    private fun showRenameDialog(fileName: String) {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("제목 수정")
            .setView(editText)
            .setPositiveButton("확인") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val file = File(filesDir, fileName)
                    val newFile = File(filesDir, newName + ".mdocx")
                    file.renameTo(newFile)
                    loadDocumentsAndCreateAdapter()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteDialog(selected: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("삭제")
            .setMessage("정말 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                selected.forEach {
                    val file = File(filesDir, it)
                    if (file.exists()) file.delete()
                }
                loadDocumentsAndCreateAdapter()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
