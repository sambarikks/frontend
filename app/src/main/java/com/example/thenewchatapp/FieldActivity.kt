package com.example.thenewchatapp

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.getValue
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.thenewchatapp.MainActivity.Companion.prefs

class FieldActivity : AppCompatActivity() {

    private lateinit var btnPlus: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnVoice: ImageButton
    private lateinit var btnDropdown: ImageButton
    private val viewModel: FieldViewModel by viewModels()
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var backButton: ImageButton

    private val easyCommandMap = mutableMapOf(
        "요약" to mutableListOf("간단 요약: 이 내용을 간단하게 요약해줘"),
        "다듬기" to mutableListOf("문장 정리: 더 자연스럽게 바꿔줘"),
        "늘리기" to mutableListOf("내용 확장: 더 길게 써줘")
    )

    // 필드 목록
    private val fields = listOf(
        Field("목적", ""), Field("주제", ""), Field("독자", ""), Field("형식 혹은 구조", ""),
        Field("근거자료", ""), Field("어조", ""), Field("분량, 문체, 금지어 등", ""), Field("추가사항", "")
    )

    private val fieldKeys = listOf(
        "목적","주제","독자","형식 혹은 구조",
        "근거자료","어조","분량, 문체, 금지어 등","추가사항"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field)

        btnVoice = findViewById(R.id.btnVoice)
        btnPlus = findViewById(R.id.btnPlus)
        recyclerView = findViewById(R.id.recyclerView)
        btnDropdown = findViewById(R.id.btnFieldDropdown)
        fragmentContainer = findViewById(R.id.fragmentContainer)
        backButton = findViewById(R.id.backButton)

        // ② 초기 상태: 목록 보이고, 상세화면 숨기기
        recyclerView.visibility      = View.VISIBLE
        fragmentContainer.visibility = View.GONE

        // 시스템 바/IME 패딩 처리
        findViewById<View>(R.id.mainLayout)?.let { mainView ->
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                v.setPadding(sys.left, sys.top, sys.right, sys.bottom + ime.bottom)
                insets
            }
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)

        // ViewModel 기본 제목 초기화 (한 번만)
        viewModel.initTitles(fieldKeys)

        // ② 드롭다운 메뉴: ViewModel 에 저장된 제목(label) 사용
        btnDropdown.setOnClickListener { anchor ->btnVoice
            PopupMenu(this, anchor).apply {
                fields.forEach { menu.add(it.title) }
                setOnMenuItemClickListener { item ->
                    showDetail(item.title.toString())
                    true
                }
                show()
            }
        }

            // **1) ViewModel에 기본 필드명 채우기**
            val fieldKeys = fields.map { it.title }
            viewModel.initTitles(fieldKeys)

            // RecyclerView 설정
            recyclerView.layoutManager = LinearLayoutManager(this)
            val fieldAdapter = FieldAdapter(
                fields      = fields,
                titleMap    = viewModel.titles.value   ?: emptyMap(),
                contentMap  = viewModel.contents.value ?: emptyMap()
            ) { field ->
                showDetail(field.title)
            }
            recyclerView.adapter = fieldAdapter

            viewModel.titles.observe(this)   { fieldAdapter.notifyDataSetChanged() }
            viewModel.contents.observe(this) { fieldAdapter.notifyDataSetChanged() }

            // ➕ 버튼 팝업 메뉴
        btnPlus.setOnClickListener { anchor ->
            // ▲ Gravity.TOP 지정: 메뉴를 버튼 위로 띄움
            val popup = PopupMenu(this@FieldActivity, anchor, Gravity.TOP)
            popup.apply {
                    menu.add("챗봇").setOnMenuItemClickListener {
                        startActivity(Intent(this@FieldActivity, FieldChatActivity::class.java))
                        true
                    }
                    menu.add("일반 글 화면").setOnMenuItemClickListener {
                        startActivity(Intent(this@FieldActivity, MainActivity::class.java))
                        true
                    }
                    show()
                }
            }

            // 음성 버튼 (추후 구현)
            btnVoice.setOnClickListener {
                Toast.makeText(this, "음성 인식이 시작됩니다.", Toast.LENGTH_SHORT).show()
            }

            backButton.setOnClickListener {
                onBackPressed()
            }

        }
    override fun onBackPressed() {
        // 백스택에 Fragment가 쌓여 있으면
        if (supportFragmentManager.backStackEntryCount > 0) {
            // 1) Fragment를 팝해서 상세→목록으로 전환
            supportFragmentManager.popBackStack()
            // 2) fragmentContainer(상세 화면 컨테이너) 숨기기
            fragmentContainer.visibility = View.GONE
            // 3) recyclerView(필드 목록) 보이기
            recyclerView.visibility = View.VISIBLE
        } else {
            // 백스택이 비어 있으면 원래 Activity 뒤로가기
            super.onBackPressed()
        }
    }

    private fun showDetail(title: String) {
        recyclerView.visibility      = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                FieldDetailFragment.newInstance(title, viewModel.getContent(title))
            )
            .addToBackStack(null)
            .commit()
    }
}
