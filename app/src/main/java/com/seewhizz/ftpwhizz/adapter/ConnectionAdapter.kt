package com.seewhizz.ftpwhizz.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.seewhizz.ftpwhizz.AddFTPActivity
import com.seewhizz.ftpwhizz.MainActivity
import com.seewhizz.ftpwhizz.R
import com.seewhizz.ftpwhizz.databinding.ItemConnectionsListBinding
import com.seewhizz.ftpwhizz.model.ConnectionModel


class ConnectionAdapter(
    private val context: Context,
    private val arrayList: ArrayList<ConnectionModel>
) :
    RecyclerView.Adapter<ConnectionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemConnectionsListBinding = ItemConnectionsListBinding.inflate(
            LayoutInflater.from(
                context
            ), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = arrayList[position]
        holder.name.text = model.name
        holder.server.text = styledText("Server",model.server,0)
        holder.port.text = styledText("Port",model.port.toString(),0)
        holder.username.text = styledText("Username", model.username,0)
        holder.itemView.setOnClickListener {
            (context as AddFTPActivity).addToConnectionHistory(model)
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("connection", model.name)
            intent.putExtra("server", model.server)
            intent.putExtra("port", model.port)
            intent.putExtra("user", model.username)
            intent.putExtra("pass", model.password)
            context.startActivity(intent)
        }
    }

    private fun styledText(key: String, value: String, flag: Int): Spanned? {
        return Html.fromHtml("<b>$key</b> : $value",flag)
    }

    override fun getItemCount()
            : Int {
        return arrayList.size
    }

    class ViewHolder(binding: ItemConnectionsListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val name = binding.itemConnectionName
        val server = binding.itemConnectionServer
        val port = binding.itemConnectionPort
        val username = binding.itemConnectionUsername
    }
}
