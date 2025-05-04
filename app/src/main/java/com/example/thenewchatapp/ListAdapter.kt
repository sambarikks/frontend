package com.example.thenewchatapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ListAdapter(
    private var documents: List<String>,
    private val onItemClick: (String) -> Unit,
    private val onSelectionChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<ListAdapter.DocumentViewHolder>() {

    private val selectedItems = mutableSetOf<String>()
    private var selectionMode = false

    inner class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val documentContent: TextView = view.findViewById(R.id.documentContent)
        val documentName: TextView = view.findViewById(R.id.documentName)
        val documentDate: TextView = view.findViewById(R.id.documentDate)
        val checkMark: ImageView = view.findViewById(R.id.checkMark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun getItemCount(): Int = documents.size

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val fileName = documents[position]
        val file = File(holder.itemView.context.filesDir, fileName)

        // ✅ 문서 내용 미리보기
        if (file.exists()) {
            holder.documentContent.text = file.readText()
        } else {
            holder.documentContent.text = ""
        }

        // ✅ 문서 제목 (확장자 제거 및 자동제목시만 텍스트 노트 유지)
        val pureTitle = fileName.removeSuffix(".mdocx").substringBefore("_")
        holder.documentName.text = pureTitle

        // ✅ 문서 작성일 (수정일 기준)
        val lastModified = file.lastModified()
        val date = Date(lastModified)
        val calendar = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()

        val dateText = when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("a h:mm", Locale.getDefault()).format(date)
            }
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
                SimpleDateFormat("M월 d일", Locale.getDefault()).format(date)
            }
            else -> {
                SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault()).format(date)
            }
        }

        holder.documentDate.text = dateText

        // ✅ 선택 모드 표시
        if (selectionMode) {
            holder.checkMark.visibility = View.VISIBLE
            holder.checkMark.setImageResource(
                if (selectedItems.contains(fileName)) R.drawable.ic_check_circle else R.drawable.ic_check_circle_outline
            )
        } else {
            holder.checkMark.visibility = View.GONE
        }

        // ✅ 클릭 이벤트 처리
        holder.itemView.setOnClickListener {
            if (selectionMode) {
                toggleSelection(fileName)
            } else {
                onItemClick(fileName)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!selectionMode) {
                selectionMode = true
                selectedItems.add(fileName)
                notifyDataSetChanged()
                onSelectionChanged(selectedItems.toList())
            }
            true
        }
    }

    // ✅ 선택 처리
    private fun toggleSelection(fileName: String) {
        if (selectedItems.contains(fileName)) {
            selectedItems.remove(fileName)
        } else {
            selectedItems.add(fileName)
        }

        if (selectedItems.isEmpty()) {
            selectionMode = false
        }

        notifyDataSetChanged()
        onSelectionChanged(selectedItems.toList())
    }

    // ✅ 선택된 아이템 반환
    fun getSelectedItems(): List<String> = selectedItems.toList()

    // ✅ 선택모드 초기화
    fun clearSelection() {
        selectedItems.clear()
        selectionMode = false
        notifyDataSetChanged()
    }
}
