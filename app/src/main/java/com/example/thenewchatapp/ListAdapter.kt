package com.example.thenewchatapp

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ListAdapter(
    private val documents: List<String>,
    private val onClick: (String) -> Unit,
    private val onMenuAction: (fileName: String, action: String) -> Unit
) : RecyclerView.Adapter<ListAdapter.DocumentViewHolder>() {

    inner class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val documentContent: TextView = view.findViewById(R.id.documentContent)  // 문서 내용 표시 영역
        val documentName: TextView = view.findViewById(R.id.documentName)        // 제목 표시 영역
        val menuButton: ImageButton = view.findViewById(R.id.menuButton)         // 점 3개 메뉴 버튼
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val fileName = documents[position]

        // 제목 설정 (파일명)
        holder.documentName.text = fileName

        // 문서 내용 읽어서 미리보기로 표시
        val file = File(holder.itemView.context.filesDir, fileName)
        if (file.exists()) {
            val content = file.readText()
            holder.documentContent.text = content
        } else {
            holder.documentContent.text = "(파일이 존재하지 않습니다)"
        }

        // 문서 클릭 → MainActivity 열기
        holder.itemView.setOnClickListener {
            onClick(fileName)
        }

        // 점 3개 버튼 클릭 → PopupMenu
        holder.menuButton.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view, Gravity.END) // Gravity.END → 오른쪽 정렬

            popup.menuInflater.inflate(R.menu.document_item_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_delete -> {
                        onMenuAction(fileName, "delete")
                        true
                    }
                    else -> false
                }
            }

            // 아이콘 표시 강제 적용 (일부 테마에서 필요)
            try {
                val fields = popup.javaClass.declaredFields
                for (field in fields) {
                    if (field.name == "mPopup") {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(popup)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popup.show()
        }
    }

    override fun getItemCount(): Int = documents.size
}
