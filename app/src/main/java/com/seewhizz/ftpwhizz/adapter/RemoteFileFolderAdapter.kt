package com.seewhizz.ftpwhizz.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.seewhizz.ftpwhizz.MainActivity
import com.seewhizz.ftpwhizz.R
import com.seewhizz.ftpwhizz.databinding.ItemFileFolderBinding
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class RemoteFileFolderAdapter(
    private val context: Context,
    private val ftpClient: FTPClient,
    private val arrayList: ArrayList<FTPFile>
) :
    RecyclerView.Adapter<RemoteFileFolderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemFileFolderBinding = ItemFileFolderBinding.inflate(
            LayoutInflater.from(
                context
            ), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = arrayList[position]
        if (model.isDirectory) {
            holder.icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.folder))
            holder.size.text = ""
        } else {
            holder.icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.file))
            holder.size.text = sizeFormatter(model.size)
        }
        holder.itemView.setOnClickListener {

            val materialDialog = MaterialAlertDialogBuilder(context)
            materialDialog.setTitle(model.name)
            materialDialog.setMessage("What do you want to do?")
            if (model.isDirectory) {
                materialDialog.setNeutralButton("Open") { dialog, _ ->
                    (context as MainActivity).getFiles(model.name)
                    dialog.dismiss()
                }
            }
            materialDialog.setNegativeButton("Delete") { dialog, _ ->
                val materialDeleteDialog = MaterialAlertDialogBuilder(context)
                if(model.isDirectory){
                    materialDeleteDialog.setTitle("Delete folder")
                    materialDeleteDialog.setMessage("Are you sure you want to delete this folder?")
                    materialDeleteDialog.setNeutralButton("Delete all files") { dialogInner, _ ->
                        (context as MainActivity).deleteRemoteFolder(model)
                        dialogInner.dismiss()
                    }
                } else {
                    materialDeleteDialog.setTitle("Delete file")
                    materialDeleteDialog.setMessage("Are you sure you want to delete this file?")
                    materialDeleteDialog.setNeutralButton("Delete all files") { dialogInner, _ ->
                        (context as MainActivity).deleteRemoteFile(model)
                        dialogInner.dismiss()
                    }
                }
                materialDeleteDialog.setNegativeButton("No") { dialogInner, _ ->
                    dialogInner.dismiss()
                }
                materialDeleteDialog.show()
                dialog.dismiss()
            }
            materialDialog.setPositiveButton("Download") { dialog, _ ->
                if (model.isDirectory) {
                    (context as MainActivity).prepareDownloadFolder(ftpClient, model.name)
                } else {
                    (context as MainActivity).downloadFile(model)
                }
                dialog.dismiss()
            }
            materialDialog.show()
        }
        holder.name.text = model.name
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        val localDateTime =
            model.timestamp.time.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        holder.time.text = localDateTime.format(dateFormatter)
    }

    private fun sizeFormatter(size: Long): CharSequence {
        return if (size < 1024) {
            "$size B"
        } else if (size < 1024 * 1024) {
            (size / 1024).toString() + " KB"
        } else if (size < 1024 * 1024 * 1024) {
            (size / 1024 / 1024).toString() + " MB"
        } else {
            (size / 1024 / 1024 / 1024).toString() + " GB"
        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class ViewHolder(binding: ItemFileFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val icon = binding.itemFileFolderIcon
        val name = binding.itemFileFolderName
        val size = binding.itemFileFolderSize
        val time = binding.itemFileFolderTime
    }
}
