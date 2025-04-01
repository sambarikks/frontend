package com.example.thenewchatapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thenewchatapp.R

// 데이터 클래스: 하나의 채팅 메시지를 표현합니다.
data class ChatMessage(
    val message: String,
    val type: MessageType
)

// 메시지 타입을 정의합니다. SENT: 사용자가 보낸 메시지, RECEIVED: 응답 메시지
enum class MessageType {
    SENT,
    RECEIVED
}

// RecyclerView 어댑터: 채팅 메시지 목록을 화면에 표시합니다.
class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    // 각 메시지의 타입에 따라 다른 뷰홀더를 리턴합니다.
    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            MessageType.SENT -> VIEW_TYPE_SENT
            MessageType.RECEIVED -> VIEW_TYPE_RECEIVED
        }
    }

    // 뷰홀더 생성: 메시지 타입에 따라 다른 레이아웃을 인플레이트합니다.
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

    // 뷰홀더에 데이터를 바인딩합니다.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    // 사용자가 보낸 메시지를 위한 뷰홀더 클래스
    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 레이아웃 내의 TextView를 찾습니다.
        private val sentMessageBubble: TextView = itemView.findViewById(R.id.sent_message_bubble)
        fun bind(message: ChatMessage) {
            sentMessageBubble.text = message.message
        }
    }

    // 응답 메시지를 위한 뷰홀더 클래스
    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 레이아웃 내의 TextView를 찾습니다.
        private val receivedMessageBubble: TextView = itemView.findViewById(R.id.receive_message_bubble)
        fun bind(message: ChatMessage) {
            receivedMessageBubble.text = message.message
        }
    }
}


// ChatActivity: 채팅 화면을 담당하는 액티비티
class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var goToEditButton: ImageButton

    // 채팅 메시지들을 저장하는 리스트
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 소프트 입력 모드를 adjustResize로 설정하여 키보드가 올라올 때 레이아웃이 조정되도록 함
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_chat)

        // 시스템 인셋과 키보드(IME) 인셋을 처리하여 적절한 패딩을 줍니다.
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            // 키보드가 올라올 때, imeInsets.bottom 만큼 패딩을 추가합니다.
            v.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom + imeInsets.bottom
            )
            insets
        }

        // 뷰 초기화 (activity_chat.xml에 정의된 ID 사용)
        chatRecyclerView = findViewById(R.id.chat_recyclerView)
        messageEditText = findViewById(R.id.message_edit)
        sendButton = findViewById(R.id.send_btn)
        goToEditButton = findViewById(R.id.goToEditButton)  // goToEditButton 초기화

        // goToEditButton 클릭 시 MainActivity로 전환
        goToEditButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        // **수정 위치 1: RecyclerView의 레이아웃 매니저 설정**
        // LinearLayoutManager에 stackFromEnd를 true로 설정하여 채팅이 최신 메시지부터 보이도록 함
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true  // 자동 스크롤을 위한 설정
        chatRecyclerView.layoutManager = layoutManager

        // 어댑터 설정
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.adapter = chatAdapter

        // 외부 액티비티(예: 메모장에서 전달된 대화 내용)가 있는지 확인
        val extraConversation = intent.getStringExtra("conversation")
        if (!extraConversation.isNullOrEmpty()) {
            // 전달받은 대화 내용을 사용자 메시지로 추가
            val initialUserMessage = ChatMessage(extraConversation, MessageType.SENT)
            chatMessages.add(initialUserMessage)
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1) // **수정 위치 2**

            // 예시로 외부 메시지에 대한 응답 생성 후 추가
            val replyMessage = externalReply(extraConversation)
            val initialReplyMessage = ChatMessage(replyMessage, MessageType.RECEIVED)
            chatMessages.add(initialReplyMessage)
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1) // **수정 위치 2**
        }

        // sendButton 클릭 시 사용자가 입력한 메시지를 처리
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

    /**
     * 외부 응답 생성 함수 예시.
     * 실제 서비스에서는 API 호출이나 기타 로직을 통해 응답 메시지를 생성합니다.
     */
    private fun externalReply(sentMsg: String): String {
        return "Reply to: $sentMsg"
    }
}