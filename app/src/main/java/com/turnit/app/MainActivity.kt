package com.turnit.app

import android.animation.ValueAnimator
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.turnit.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var reqCtrl: RequestController
    private val msgs = mutableListOf<Pair<String, Int>>()
    private val models = buildModels()
    private var model = models[0]

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reqCtrl = RequestController(lifecycleScope, BuildConfig.GEMINI_API_KEY, BuildConfig.HUGGINGFACE_API_KEY)
        
        setupRecycler()
        binding.toolbar.post { setupAnimatedRGBLogo() }
        
        // Clear blurs from text areas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.navView.setRenderEffect(null)
            binding.inputBorderContainer.setRenderEffect(null)
        }

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun setupAnimatedRGBLogo() {
        val logoText = binding.toolbar.getChildAt(0) as? TextView ?: return
        val colors = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFF0000.toInt())
        val textWidth = logoText.paint.measureText(logoText.text.toString())
        val shader = LinearGradient(0f, 0f, textWidth, 0f, colors, null, Shader.TileMode.REPEAT)
        logoText.paint.shader = shader
        val matrix = Matrix()
        ValueAnimator.ofFloat(0f, textWidth).apply {
            duration = 2500; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { 
                matrix.setTranslate(it.animatedValue as Float, 0f)
                shader.setLocalMatrix(matrix); logoText.invalidate() 
            }
            start()
        }
    }

    private fun buildModels() = listOf(
        ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Google", ModelOption.TYPE_GEMINI),
        ModelOption("Gemini 2.5 Fast", "gemini-2.5-flash", "Google", ModelOption.TYPE_GEMINI),
        ModelOption("Qwen 2.5 72B", "Qwen/Qwen2.5-72B-Instruct:novita", "Alibaba", ModelOption.TYPE_HUGGINGFACE),
        ModelOption("Qwen 3.5 397B", "Qwen/Qwen3.5-397B-A17B:novita", "Alibaba Vision", ModelOption.TYPE_HUGGINGFACE),
        ModelOption("Qwen 3.5 35B", "Qwen/Qwen3.5-35B-A3B:novita", "Alibaba Vision", ModelOption.TYPE_HUGGINGFACE)
    )

    private fun sendMessage() {
        val txt = binding.etInput.text.toString().trim()
        if (txt.isEmpty()) return
        binding.etInput.setText("")
        msgs.add(txt to 0) // User
        val pos = msgs.size
        msgs.add("Thinking..." to 1) // AI
        binding.recyclerMessages.adapter?.notifyDataSetChanged()
        
        reqCtrl.send(txt, model, null, { r -> 
            msgs[pos] = r to 1
            binding.recyclerMessages.adapter?.notifyItemChanged(pos)
        }, { e -> 
            msgs[pos] = "Error: $e" to 1
            binding.recyclerMessages.adapter?.notifyItemChanged(pos)
        })
    }

    private fun setupRecycler() {
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.adapter = ChatAdapter(msgs)
    }

    // --- Inner Classes ---
    inner class ChatAdapter(private val m: List<Pair<String, Int>>) : RecyclerView.Adapter<VH>() {
        override fun getItemViewType(p: Int) = m[p].second
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_chat_message, p, false))
        override fun onBindViewHolder(h: VH, p: Int) {
            val (text, type) = m[p]
            h.tv.text = text
            // Apply Glassmorphism Background
            h.tv.setBackgroundResource(R.drawable.bg_glass_bubble)
            h.tv.setTextColor(Color.WHITE)
        }
        override fun getItemCount() = m.size
    }
    class VH(v: View) : RecyclerView.ViewHolder(v) { val tv: TextView = v.findViewById(R.id.tv_message) }
}
