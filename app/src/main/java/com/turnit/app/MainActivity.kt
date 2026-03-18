package com.turnit.app
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
    private lateinit var binding:   ActivityMainBinding
    private lateinit var toggle:    ActionBarDrawerToggle
    private lateinit var security:  SecurityManager
    private lateinit var reqCtrl:   RequestController
    private lateinit var btnCtrl:   ButtonController
    private lateinit var adapter:   ChatAdapter
    private val msgs    = mutableListOf<ChatMsg>()
    private val models  = buildModels()
    private var model   = models[0]
    private var convId  = UUID.randomUUID().toString()
    private var pending = -1
    private var tier    = AppTier.Q
    private var svc: TurnItService? = null
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            svc = (b as TurnItService.LocalBinder).get()
        }
        override fun onServiceDisconnected(n: ComponentName) { svc = null }
    }
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding  = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        security = SecurityManager(this)
        reqCtrl  = RequestController(
            scope      = lifecycleScope,
            geminiKey  = BuildConfig.GEMINI_API_KEY,
            hfKey      = BuildConfig.HUGGINGFACE_API_KEY
        )
        btnCtrl = ButtonController(binding.btnSend)
        startSvc(); setupDrawer(); setupRecycler()
        applyBg(); setupBorder(); setupChip(); setupSendBtn()
        boot()
    }
    override fun onDestroy() {
        super.onDestroy(); unbindService(conn); reqCtrl.close()
    }
    private fun startSvc() {
        val i = Intent(this, TurnItService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(i) else startService(i)
        bindService(i, conn, Context.BIND_AUTO_CREATE)
    }
    private fun setupDrawer() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.drawer_open, R.string.drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle); toggle.syncState()
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_chat -> { newConv(); true }
                R.id.nav_history  -> { showHist(); true }
                R.id.nav_api_key  -> { toast("Enter key via setUserKey()"); true }
                R.id.nav_logout   -> {
                    security.clearSession()
                    reqCtrl.setHandshake("")
                    reqCtrl.setUserKey(null)
                    header(AppTier.Q, "Signed out"); true
                }
                else -> false
            }.also { binding.drawerLayout.closeDrawer(GravityCompat.START) }
        }
    }
    private fun boot() {
        lifecycleScope.launch {
            security.ensureDir()
            when (val h = security.readHandshake()) {
                is HandshakeResult.Valid -> {
                    reqCtrl.setHandshake(security.encodeForTransport(h.data))
                    header(tier, h.data.username)
                }
                is HandshakeResult.Missing ->
                    security.getCachedUid()?.let {
                        header(tier, security.getCachedUname() ?: "User")
                    } ?: toast("Please log in.")
                is HandshakeResult.Invalid ->
                    toast("Handshake corrupt: ${h.reason}")
            }
        }
    }
    private fun header(t: AppTier, uname: String) {
        tier = t
        val badge = when (t) {
            AppTier.QX      -> "QX"
            AppTier.Q       -> "Q"
            AppTier.UNKNOWN -> ""
        }
        supportActionBar?.title =
            if (badge.isEmpty()) "TurnIt" else "TurnIt $badge"
        val hv = binding.navView.getHeaderView(0)
        hv?.findViewById<TextView>(R.id.nav_header_username)?.text = uname
        hv?.findViewById<TextView>(R.id.nav_header_tier)?.let { tv ->
            tv.text = badge
            tv.visibility = if (badge.isEmpty()) View.GONE else View.VISIBLE
            tv.setTextColor(
                if (t == AppTier.QX) 0xFFF87171.toInt()
                else 0xFFC084FC.toInt())
        }
    }
    fun setUserKey(key: String?) {
        reqCtrl.setUserKey(key)
        toast(if (!key.isNullOrEmpty()) "Direct mode" else "Gateway (10 RPM)")
    }
    private fun setupRecycler() {
        adapter = ChatAdapter(msgs)
        binding.recyclerMessages.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.adapter = adapter
    }
    private fun applyBg() {
        binding.rootLayout.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFFBF40BF.toInt(), 0xFF702963.toInt(),
                0xFF000000.toInt())
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }
    }
    private fun setupBorder() {
        val sweep = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFFC084FC.toInt(), 0xFF7C3AED.toInt(),
                0xFFA78BFA.toInt(), 0xFF60A5FA.toInt(),
                0xFFF87171.toInt(), 0xFFC084FC.toInt())
        ).apply {
            gradientType = GradientDrawable.SWEEP_GRADIENT
            cornerRadius = (32 * resources.displayMetrics.density + .5f).toInt().toFloat()
        }
        val rd = RotateDrawable().apply {
            drawable = sweep; fromDegrees = 0f; toDegrees = 360f
            isPivotXRelative = true; pivotX = .5f
            isPivotYRelative = true; pivotY = .5f
        }
        binding.inputBorderContainer.background = rd
        ValueAnimator.ofInt(0, 10000).apply {
            duration = 4000
            repeatCount  = ValueAnimator.INFINITE
            repeatMode   = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { rd.level = it.animatedValue as Int }
            start()
        }
    }
    private fun setupChip() {
        binding.btnModelChip.text = model.displayName
        binding.btnModelChip.setOnClickListener {
            ModelSelectionDialog(models, model.modelId) { sel ->
                model = sel; binding.btnModelChip.text = sel.displayName
                val ld = binding.btnModelChip.background
                    as? android.graphics.drawable.LayerDrawable
                    ?: return@ModelSelectionDialog
                val g = ld.getDrawable(0)
                    as? GradientDrawable
                    ?: return@ModelSelectionDialog
                ValueAnimator.ofInt(85, 255, 85).apply {
                    duration = 600
                    addUpdateListener {
                        g.alpha = it.animatedValue as Int
                        binding.btnModelChip.invalidate()
                    }
                    start()
                }
            }.show(supportFragmentManager, "model")
        }
    }
    private fun setupSendBtn() {
        binding.btnSend.setOnTouchListener { v, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN) btnCtrl.animatePress()
            if (ev.action == MotionEvent.ACTION_UP) v.performClick()
            true
        }
        binding.btnSend.setOnClickListener {
            when (btnCtrl.state) {
                ButtonState.IDLE       -> send()
                ButtonState.PROCESSING -> stop()
                ButtonState.CANCELLING -> Unit
            }
        }
    }
    private fun send() {
        val txt = binding.etInput.text.toString().trim()
        if (txt.isEmpty()) return
        binding.etInput.setText("")
        addMsg(txt, ChatMsg.USER)
        pending = msgs.size
        addMsg("Thinking...", ChatMsg.AI)
        log("User", txt)
        btnCtrl.toProcessing()
        reqCtrl.send(txt, model,
            onResult = { r ->
                val badge = "\u26a1 ${r.latencyMs}ms  ${r.modelId}"
                update(pending, "${r.text}\n\n$badge")
            },
            onError  = { e -> update(pending, "Error: $e") }
        )
    }
    private fun stop() {
        reqCtrl.cancel()
        btnCtrl.toCancelling {
            val i = pending
            if (i >= 0 && i < msgs.size) {
                msgs[i] = ChatMsg("Stopped.", ChatMsg.AI)
                adapter.notifyItemChanged(i)
            }
            pending = -1
        }
    }
    private fun update(at: Int, text: String) {
        if (at >= 0 && at < msgs.size) {
            msgs[at] = ChatMsg(text, ChatMsg.AI)
            adapter.notifyItemChanged(at); scroll()
        }
        log("AI", text); btnCtrl.toIdle(); pending = -1
    }
    private fun addMsg(t: String, tp: Int) {
        msgs.add(ChatMsg(t, tp))
        adapter.notifyItemInserted(msgs.size - 1); scroll()
    }
    private fun log(r: String, t: String) = lifecycleScope.launch {
        security.appendLog(convId, r, t)
    }
    private fun newConv() {
        convId = UUID.randomUUID().toString()
        msgs.clear(); adapter.notifyDataSetChanged()
    }
    private fun showHist() = lifecycleScope.launch {
        toast("${security.listConversations().size} conversations")
    }
    private fun scroll() {
        val p = msgs.size - 1
        if (p >= 0) binding.recyclerMessages.post {
            binding.recyclerMessages.smoothScrollToPosition(p)
        }
    }
    private fun toast(m: String) =
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    // ---- 4-model selector: Gemini 1.5 Flash, Qwen 397B, Qwen 35B, Qwen 9B
    private fun buildModels() = listOf(
        ModelOption(
            "Gemini 1.5 Flash",
            "gemini-1.5-flash",
            "Google  -  Fast and multimodal",
            ModelOption.TYPE_GEMINI
        ),
        ModelOption(
            "Qwen 397B",
            "Qwen/Qwen2-72B-Instruct",
            "Alibaba  -  Maximum capability",
            ModelOption.TYPE_HUGGINGFACE
        ),
        ModelOption(
            "Qwen 35B",
            "Qwen/Qwen1.5-32B-Chat",
            "Alibaba  -  Balanced performance",
            ModelOption.TYPE_HUGGINGFACE
        ),
        ModelOption(
            "Qwen 9B",
            "Qwen/Qwen1.5-7B-Chat",
            "Alibaba  -  Lightweight",
            ModelOption.TYPE_HUGGINGFACE
        )
    )

    data class ChatMsg(val text: String, val type: Int) {
        companion object { const val USER = 0; const val AI = 1 }
    }
    inner class ChatAdapter(private val m: MutableList<ChatMsg>)
        : RecyclerView.Adapter<ChatAdapter.VH>() {
        override fun getItemViewType(p: Int) = m[p].type
        override fun onCreateViewHolder(parent: ViewGroup, t: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false))
        override fun onBindViewHolder(h: VH, pos: Int) {
            val msg = m[pos]
            if (msg.type == ChatMsg.USER) {
                h.cu.visibility = View.VISIBLE
                h.ca.visibility = View.GONE
                h.tu.text = msg.text
            } else {
                h.cu.visibility = View.GONE
                h.ca.visibility = View.VISIBLE
                h.ta.text = msg.text
            }
        }
        override fun getItemCount() = m.size
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val cu: LinearLayout = v.findViewById(R.id.container_user)
            val ca: LinearLayout = v.findViewById(R.id.container_ai)
            val tu: TextView     = v.findViewById(R.id.tv_user_message)
            val ta: TextView     = v.findViewById(R.id.tv_ai_message)
        }
    }
}
