package com.turnit.app

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RotateDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.turnit.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var reqCtrl: RequestController
    
    // Custom typefaces
    private lateinit var tfDeltha: Typeface
    private lateinit var tfEquinox: Typeface
    private lateinit var tfSpaceGrotesk: Typeface

    private val msgs = mutableListOf<ChatMsg>()
    private val models = buildModels()
    private var model = models[0]
    private var convId = UUID.randomUUID().toString()
    private var pending = -1

    private var svc: TurnItService? = null
    private val svcConn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            svc = (b as TurnItService.LocalBinder).get()
        }
        override fun onServiceDisconnected(n: ComponentName) { svc = null }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Controller
        reqCtrl = RequestController(
            scope = lifecycleScope,
            geminiKey = BuildConfig.GEMINI_API_KEY,
            hfKey = BuildConfig.HUGGINGFACE_API_KEY
        )

        loadTypefaces()
        applyTypefaces()
        setupDrawer()
        setupRecycler()
        setupMovingBorder()
        applyHardwareBlur() // Fixed logic inside
        setupModelChip()
        setupSendButton()
        
        // Start Service
        val intent = Intent(this, TurnItService::class.java)
        bindService(intent, svcConn, Context.BIND_AUTO_CREATE)
    }

    private fun loadTypefaces() {
        // Try-catch prevents crash if assets are missing during build
        runCatching {
            tfDeltha = Typeface.createFromAsset(assets, "fonts/Deltha.ttf")
            tfEquinox = Typeface.createFromAsset(assets, "fonts/Equinox.ttf")
            tfSpaceGrotesk = Typeface.createFromAsset(assets, "fonts/SpaceGrotesk.ttf")
        }.onFailure {
            tfDeltha = Typeface.DEFAULT_BOLD
            tfEquinox = Typeface.MONOSPACE
            tfSpaceGrotesk = Typeface.SANS_SERIF
        }
    }

    private fun applyTypefaces() {
        binding.toolbar.title = "TurnIt QX"
        binding.btnModelChip.typeface = tfEquinox
        binding.etInput.typeface = tfSpaceGrotesk
    }

    private fun applyHardwareBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Only blur the background drawable layer, NOT the entire root/container
            // This ensures text remains sharp while the background looks frosted
            val blur = RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
            
            // Apply to the drawer background specifically
            binding.navView.setRenderEffect(blur)
            
            // If you have a dedicated background view for the nebula, apply it there:
            // binding.backgroundBlurLayer.setRenderEffect(blur)
        }
    }

    private fun setupMovingBorder() {
        val sweep = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF00D1FF.toInt(), 0xFF7B4FBF.toInt(), 0xFF00D1FF.toInt())
        ).apply {
            gradientType = GradientDrawable.SWEEP_GRADIENT
            cornerRadius = dp(12).toFloat()
        }
        
        val rd = RotateDrawable().apply {
            drawable = sweep
            fromDegrees = 0f; toDegrees = 360f
            isPivotXRelative = true; pivotX = 0.5f
            isPivotYRelative = true; pivotY = 0.5f
        }

        binding.inputBorderContainer.background = rd
        
        ValueAnimator.ofInt(0, 10000).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { rd.level = it.animatedValue as Int }
            start()
        }
    }

    private fun setupDrawer() {
        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, 0, 0)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        
        binding.navView.setNavigationItemSelectedListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupRecycler() {
        adapter = ChatAdapter(msgs)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.adapter = adapter
    }

    private fun setupModelChip() {
        binding.btnModelChip.text = model.displayName
        binding.btnModelChip.setOnClickListener {
            // Logic for showing a dialog to change models
            Toast.makeText(this, "Switching Model...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun sendMessage() {
        val txt = binding.etInput.text.toString().trim()
        if (txt.isEmpty()) return
        
        binding.etInput.setText("")
        addMsg(txt, ChatMsg.USER)
        
        pending = msgs.size
        addMsg("Thinking...", ChatMsg.AI)

        reqCtrl.send(
            prompt = txt,
            model = model,
            onResult = { r -> updateMsg(pending, r.text) },
            onError = { e -> updateMsg(pending, "Error: $e") }
        )
    }

    private fun addMsg(t: String, tp: Int) {
        msgs.add(ChatMsg(t, tp))
        adapter.notifyItemInserted(msgs.size - 1)
        binding.recyclerMessages.smoothScrollToPosition(msgs.size - 1)
    }

    private fun updateMsg(at: Int, text: String) {
        if (at in msgs.indices) {
            msgs[at] = ChatMsg(text, ChatMsg.AI)
            adapter.notifyItemChanged(at)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildModels() = listOf(
        ModelOption("QX Flash", "gemini-1.5-flash", "Rapid", ModelOption.TYPE_GEMINI),
        ModelOption("QX Apex", "Qwen/Qwen2.5-72B-Instruct", "Max Reasoning", ModelOption.TYPE_HUGGINGFACE)
    )

    data class ChatMsg(val text: String, val type: Int) {
        companion object { const val USER = 0; const val AI = 1 }
    }

    inner class ChatAdapter(private val m: List<ChatMsg>) : RecyclerView.Adapter<ChatAdapter.VH>() {
        override fun getItemViewType(p: Int) = m[p].type
        override fun onCreateViewHolder(parent: ViewGroup, t: Int): VH {
            val layout = if (t == ChatMsg.USER) R.layout.item_chat_user else R.layout.item_chat_ai
            return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false))
        }
        override fun onBindViewHolder(h: VH, pos: Int) {
            h.tv.text = m[pos].text
            h.tv.typeface = tfSpaceGrotesk
        }
        override fun getItemCount() = m.size
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tv: TextView = v.findViewById(R.id.tv_message_text)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(svcConn)
        reqCtrl.close()
    }
}
