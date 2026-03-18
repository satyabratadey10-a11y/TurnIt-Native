package com.turnit.app
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
class ModelSelectionDialog(
    private val models: List<ModelOption>,
    private val currentId: String,
    private val onSelected: (ModelOption) -> Unit
) : BottomSheetDialogFragment() {
    override fun onCreateView(
        i: LayoutInflater, c: ViewGroup?, s: Bundle?
    ): View {
        val v = i.inflate(R.layout.fragment_model_selection, c, false)
        val rv = v.findViewById<RecyclerView>(R.id.rv_models)
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = Adapter()
        return v
    }
    override fun onStart() {
        super.onStart()
        (view?.parent as? View)?.setBackgroundColor(
            android.graphics.Color.TRANSPARENT)
    }
    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
            VH(LayoutInflater.from(p.context)
                .inflate(R.layout.item_model_option, p, false))
        override fun onBindViewHolder(h: VH, pos: Int) {
            val m = models[pos]
            h.name.text = m.displayName
            h.desc.text = m.description
            h.check.visibility =
                if (m.modelId == currentId) View.VISIBLE else View.INVISIBLE
            h.itemView.setOnClickListener { onSelected(m); dismiss() }
        }
        override fun getItemCount() = models.size
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name:  TextView  = v.findViewById(R.id.tv_model_name)
            val desc:  TextView  = v.findViewById(R.id.tv_model_desc)
            val check: ImageView = v.findViewById(R.id.iv_model_check)
        }
    }
}
