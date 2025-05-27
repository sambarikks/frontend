// ✅ ChatActivity.kt - 최종 통합본

package com.example.thenewchatapp

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// 메시지 데이터 클래스

data class ChatMessage(
    var message: String,
    val type: MessageType,
    val isResult: Boolean = false
)

enum class MessageType {
    SENT,
    RECEIVED,
    LOADING,
    LOADING_RESULT // 결과 생성 전용 로딩
}

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var goToEditButton: ImageButton
    private lateinit var createResultButton: ImageButton

    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val editMessageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val editedText = result.data?.getStringExtra("editedText")
            val position = result.data?.getIntExtra("messagePosition", -1) ?: -1
            if (position in chatMessages.indices && editedText != null) {
                chatMessages[position] = chatMessages[position].copy(message = editedText)
                chatAdapter.notifyItemChanged(position)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_chat)

        // 시스템 바/IME 패딩 처리
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom + ime.bottom)
            insets
        }

        // 뷰 바인딩
        chatRecyclerView = findViewById(R.id.chat_recyclerView)
        messageEditText = findViewById(R.id.message_edit)
        sendButton = findViewById(R.id.send_btn)
        goToEditButton = findViewById(R.id.goToEditButton)
        createResultButton = findViewById(R.id.CreateResultButton)

        // 결과 생성 버튼 클릭
        createResultButton.setOnClickListener {
            // 1) 결과 전용 로딩 메시지 추가
            val loading = ChatMessage("결과 생성 중 입니다...", MessageType.LOADING_RESULT)
            chatMessages.add(loading)
            chatAdapter.notifyItemInserted(chatMessages.lastIndex)
            chatRecyclerView.smoothScrollToPosition(chatMessages.lastIndex)

            // 2) 실제 API 호출
            CoroutineScope(Dispatchers.Main).launch {
                // TODO: 실제 AI 호출 로직으로 교체
                delay(1500)
                // 로딩 제거
                val idx = chatMessages.indexOf(loading)
                if (idx >= 0) {
                    chatMessages.removeAt(idx)
                    chatAdapter.notifyItemRemoved(idx)
                }
                // 결과 메시지 추가
                val result = ChatMessage("결과 생성된 글입니다.", MessageType.RECEIVED, isResult = true)
                chatMessages.add(result)
                chatAdapter.notifyItemInserted(chatMessages.lastIndex)
                chatRecyclerView.smoothScrollToPosition(chatMessages.lastIndex)
            }
        }

        // MainActivity로 편집 이동
        goToEditButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }

        // RecyclerView 설정
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatAdapter = ChatAdapter(chatMessages) { pos, text ->
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("originalText", text)
                putExtra("messagePosition", pos)
            }
            editMessageLauncher.launch(intent)
        }
        chatRecyclerView.adapter = chatAdapter

        // 초기 대화 처리
        intent.getStringExtra("conversation")?.takeIf { it.isNotEmpty() }?.let {
            chatMessages += ChatMessage(it, MessageType.SENT)
            chatAdapter.notifyItemInserted(chatMessages.lastIndex)
            chatRecyclerView.smoothScrollToPosition(chatMessages.lastIndex)
            val reply = externalReply(it)
            chatMessages += ChatMessage(reply, MessageType.RECEIVED)
            chatAdapter.notifyItemInserted(chatMessages.lastIndex)
            chatRecyclerView.smoothScrollToPosition(chatMessages.lastIndex)
        }

        // 전송 버튼 클릭
        sendButton.setOnClickListener {
            val text = messageEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                // 사용자 메시지 추가
                chatMessages += ChatMessage(text, MessageType.SENT)
                chatAdapter.notifyItemInserted(chatMessages.lastIndex)
                chatRecyclerView.smoothScrollToPosition(chatMessages.lastIndex)
                messageEditText.text.clear()

                // 일반 로딩 메시지 추가
                val loading = ChatMessage("답변 생성 중 입니다...", MessageType.LOADING)
                chatMessages += loading
                chatAdapter.notifyItemInserted(chatMessages.lastIndex)
                chatRecyclerView.smoothScrollToPosition(chatMessages.lastIndex)

                // AI 응답 스트리밍
                CoroutineScope(Dispatchers.Main).launch {
                    val response = ChatMessage("", MessageType.RECEIVED)
                    chatAdapter.sendChatRequest(text) { chunk ->
                        if (loading in chatMessages) {
                            val idx = chatMessages.indexOf(loading)
                            chatMessages.removeAt(idx)
                            chatAdapter.notifyItemRemoved(idx)
                        }
                        if (response !in chatMessages) {
                            chatMessages += response
                            chatAdapter.notifyItemInserted(chatMessages.lastIndex)
                        }
                        response.message += chunk
                        chatAdapter.notifyItemChanged(chatMessages.indexOf(response))
                        chatRecyclerView.smoothScrollToPosition(chatMessages.lastIndex)
                    }
                }
            }
        }
    }

    private fun externalReply(sentMsg: String): String = "Reply to: $sentMsg"

    class ChatAdapter(
        private val messages: List<ChatMessage>,
        private val onReceivedClick: (Int, String) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val VIEW_TYPE_SENT = 1
            private const val VIEW_TYPE_RECEIVED = 2
            private const val VIEW_TYPE_RESULT = 3
            private const val VIEW_TYPE_LOADING = 4
            private const val VIEW_TYPE_LOADING_RESULT = 5
        }

        override fun getItemViewType(position: Int): Int = with(messages[position]) {
            when {
                type == MessageType.SENT               -> VIEW_TYPE_SENT
                type == MessageType.LOADING_RESULT     -> VIEW_TYPE_LOADING_RESULT
                type == MessageType.LOADING            -> VIEW_TYPE_LOADING
                type == MessageType.RECEIVED && isResult -> VIEW_TYPE_RESULT
                else                                   -> VIEW_TYPE_RECEIVED
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            LayoutInflater.from(parent.context).run {
                when (viewType) {
                    VIEW_TYPE_SENT           -> SentViewHolder(inflate(R.layout.sent_message_bubble, parent, false))
                    VIEW_TYPE_LOADING_RESULT -> ResultLoadingViewHolder(inflate(R.layout.result_loading_message_bubble, parent, false))
                    VIEW_TYPE_LOADING        -> LoadingViewHolder(inflate(R.layout.loading_message_bubble, parent, false))
                    VIEW_TYPE_RESULT         -> ResultViewHolder(inflate(R.layout.result_received_message_bubble, parent, false))
                    else                     -> ReceivedViewHolder(inflate(R.layout.received_message_bubble, parent, false))
                }
            }

        override fun getItemCount(): Int = messages.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = messages[position]
            when (holder) {
                is SentViewHolder           -> holder.bind(msg)
                is ReceivedViewHolder       -> holder.bind(msg)
                is ResultViewHolder         -> holder.bind(msg, onReceivedClick)
                is LoadingViewHolder        -> holder.bind()
                is ResultLoadingViewHolder  -> holder.bind()
            }
        }

        class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tv: TextView = view.findViewById(R.id.sent_message_bubble)
            fun bind(msg: ChatMessage) { tv.text = msg.message }
        }

        class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tv: TextView = view.findViewById(R.id.receive_message_bubble)
            fun bind(msg: ChatMessage) { tv.apply { text = msg.message; setOnClickListener(null) } }
        }

        class ResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tv: TextView = view.findViewById(R.id.result_receive_message_bubble)
            fun bind(msg: ChatMessage, click: (Int, String) -> Unit) {
                tv.text = msg.message
                tv.setOnClickListener { click(adapterPosition, msg.message) }
            }
        }

        class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tv: TextView = view.findViewById(R.id.loading_text)
            private var animator: ValueAnimator? = null
            fun bind() {
                val base = tv.currentTextColor
                animator?.cancel()
                animator = ValueAnimator.ofFloat(0.3f, 1f).apply {
                    duration = 1000
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener {
                        val a = (it.animatedValue as Float * 255).toInt()
                        tv.setTextColor(ColorUtils.setAlphaComponent(base, a))
                    }
                    start()
                }
                tv.text = "답변 생성 중 입니다..."
            }
        }

        class ResultLoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tv: TextView = view.findViewById(R.id.result_loading_text)
            private var animator: ValueAnimator? = null
            fun bind() {
                val base = tv.currentTextColor
                animator?.cancel()
                animator = ValueAnimator.ofFloat(0.3f, 1f).apply {
                    duration = 1000
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener {
                        val a = (it.animatedValue as Float * 255).toInt()
                        tv.setTextColor(ColorUtils.setAlphaComponent(base, a))
                    }
                    start()
                }
                tv.text = "결과 생성 중 입니다..."
            }
        }

        fun sendChatRequest(userMessage: String, onChunk: (String) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                val client = OkHttpClient()
                val body = JSONObject().put("message", userMessage).toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val req = Request.Builder()
                    .url("http://54.252.159.52:5000/stream-chat")
                    .post(body)
                    .build()
                try {
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use onChunk("Error: ${resp.code}")
                        val src = resp.body?.source() ?: return@use onChunk("Error: null body")
                        while (src.request(1)) {
                            val chunk = src.buffer.readUtf8(minOf(1024L, src.buffer.size))
                            withContext(Dispatchers.Main) { onChunk(chunk) }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { onChunk("Stream Error: ${e.message}") }
                }
            }
        }
    }
}
