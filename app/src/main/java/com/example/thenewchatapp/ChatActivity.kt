package com.example.thenewchatapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thenewchatapp.R

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject



// 데이터 클래스: 하나의 채팅 메시지를 표현합니다.
data class ChatMessage(
    val message: String,
    val type: MessageType
)

// 메시지 타입을 정의합니다.
enum class MessageType {
    SENT,
    RECEIVED
}

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var goToEditButton: ImageButton

    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    // 결과 받기 위한 launcher
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

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom + imeInsets.bottom
            )
            insets
        }

        chatRecyclerView = findViewById(R.id.chat_recyclerView)
        messageEditText = findViewById(R.id.message_edit)
        sendButton = findViewById(R.id.send_btn)
        goToEditButton = findViewById(R.id.goToEditButton)

        goToEditButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        chatRecyclerView.layoutManager = layoutManager

        chatAdapter = ChatAdapter(chatMessages) { position, message ->
            val intent = Intent(this, ReceivedEditActivity::class.java).apply {
                putExtra("originalText", message)
                putExtra("messagePosition", position)
            }
            editMessageLauncher.launch(intent)
        }
        chatRecyclerView.adapter = chatAdapter

        val extraConversation = intent.getStringExtra("conversation")
        if (!extraConversation.isNullOrEmpty()) {
            val initialUserMessage = ChatMessage(extraConversation, MessageType.SENT)
            chatMessages.add(initialUserMessage)
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)

            val replyMessage = externalReply(extraConversation)
            val initialReplyMessage = ChatMessage(replyMessage, MessageType.RECEIVED)
            chatMessages.add(initialReplyMessage)
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
        }

        sendButton.setOnClickListener {
            val sentMessage = messageEditText.text.toString().trim()
            if (sentMessage.isNotEmpty()) {
                // 사용자가 보낸 메시지 추가
                val newSentMessage = ChatMessage(sentMessage, MessageType.SENT)
                chatMessages.add(newSentMessage)
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1) // **수정 위치 2**
                messageEditText.text.clear()

                // 외부 API 또는 로직에 따른 응답 메시지 생성 후 추가
                val replyMessage = externalReply(sentMessage)
                val newReceivedMessage = ChatMessage(replyMessage, MessageType.RECEIVED)
                chatMessages.add(newReceivedMessage)
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1) // **수정 위치 2**
            }
        }

    }

    private fun externalReply(sentMsg: String): String {
        return "Reply to: $sentMsg"
    }
}

// ChatAdapter 수정 버전 (ChatActivity 내부 클래스)
class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val onReceivedClick: (position: Int, message: String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            MessageType.SENT -> VIEW_TYPE_SENT
            MessageType.RECEIVED -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.sent_message_bubble, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.received_message_bubble, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message, onReceivedClick)
        }
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sentMessageBubble: TextView = itemView.findViewById(R.id.sent_message_bubble)
        fun bind(message: ChatMessage) {
            sentMessageBubble.text = message.message
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val receivedMessageBubble: TextView =
            itemView.findViewById(R.id.receive_message_bubble)

        fun bind(message: ChatMessage, onClick: (Int, String) -> Unit) {
            receivedMessageBubble.text = message.message
            receivedMessageBubble.setOnClickListener {
                onClick(adapterPosition, message.message)
            }
        }
    }

    suspend fun sendChatRequest(userMessage: String, onChunk: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("message", userMessage)
            }
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("http://10.0.2.2:8000/stream-chat") // URL 확인 필요
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            onChunk("Error: ${response.code}")
                        }
                        return@withContext
                    }
                    val source = response.body?.source() ?: run {
                        withContext(Dispatchers.Main) {
                            onChunk("Error: Response body is null")
                        }
                        return@withContext
                    }
                    val fixedBufferSize = 1024L

                    while (true) {
                        if (!source.request(1L)) break

                        val available = source.buffer.size
                        if (available > 0) {
                            val bytesToRead =
                                if (available >= fixedBufferSize) fixedBufferSize else available
                            val chunk = try {
                                source.readUtf8(bytesToRead)
                            } catch (readEx: Exception) {
                                withContext(Dispatchers.Main) {
                                    onChunk("Stream Read Error: ${readEx.message}")
                                }
                                break
                            }
                            withContext(Dispatchers.Main) {
                                onChunk(chunk)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onChunk("Stream Error: ${e.message}")
                }
            }
        }
    }
}
