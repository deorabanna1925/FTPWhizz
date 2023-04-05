package com.seewhizz.ftpwhizz.adapter

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.ThumbnailUtils
import android.os.CancellationSignal
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.seewhizz.ftpwhizz.CodeEditorActivity
import com.seewhizz.ftpwhizz.MainActivity
import com.seewhizz.ftpwhizz.R
import com.seewhizz.ftpwhizz.databinding.ItemFileFolderBinding
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.CharSequence
import kotlin.Int
import kotlin.Long


class LocalFileFolderAdapter(
    private val context: Context,
    private val ftpClient: FTPClient,
    private val arrayList: ArrayList<File>
) :
    RecyclerView.Adapter<LocalFileFolderAdapter.ViewHolder>() {

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
            holder.icon.setImageBitmap(getThumbnail(model))
            val fileSize = model.length().toString()
            holder.size.text = sizeFormatter(fileSize.toLong())
        }
        holder.name.text = model.name
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        val localDateTime = timestampToLocalDateTime(model.lastModified().toString())
        holder.time.text = localDateTime!!.format(dateFormatter)
        holder.itemView.setOnClickListener {
            if (model.isDirectory) {
                (context as MainActivity).loadLocalFolder(model.absolutePath)
            } else {
                openDifferentFile(model)
            }
        }
        holder.itemView.setOnLongClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(model.name)
                .setPositiveButton("Delete") { dialog, _ ->
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete ${model.name} ?")
                        .setPositiveButton("Yes") { dialogInner, _ ->
                            model.delete()
                            (context as MainActivity).deleteRecursive(model)
                            dialogInner.dismiss()
                        }
                        .setNegativeButton("No") { dialogInner, _ ->
                            dialogInner.dismiss()
                        }
                        .show()
                    dialog.dismiss()
                }
                .setNegativeButton("Upload") {
                        dialog, _ ->
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Upload")
                        .setMessage("Are you sure you want to upload ${model.name} ?")
                        .setPositiveButton("Yes") { dialogInner, _ ->
                            (context as MainActivity).uploadFile(model)
                            dialogInner.dismiss()
                        }
                        .setNegativeButton("No") { dialogInner, _ ->
                            dialogInner.dismiss()
                        }
                        .show()
                    dialog.dismiss()
                }
                .show()
            true
        }
    }

    private fun openDifferentFile(model: File) {
        val mimeType = getMimeType(model.extension)
        if (mimeType == null) {
            val intent = Intent(context, CodeEditorActivity::class.java)
            intent.setDataAndType(
                FileProvider.getUriForFile(
                    context,
                    context.applicationContext.packageName + ".provider",
                    model
                ), getMimeType(model.extension)
            )
            intent.putExtra("filePath", model.absolutePath)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } else {
            when {
                mimeType.contains("image") -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            context.applicationContext.packageName + ".provider",
                            model
                        ), mimeType
                    )
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                }
                mimeType.contains("video") -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            context.applicationContext.packageName + ".provider",
                            model
                        ), mimeType
                    )
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                }
                mimeType.contains("audio") -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            context.applicationContext.packageName + ".provider",
                            model
                        ), mimeType
                    )
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                }
                else -> {
                    val intent = Intent(context, CodeEditorActivity::class.java)
                    intent.setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            context.applicationContext.packageName + ".provider",
                            model
                        ), getMimeType(model.extension)
                    )
                    intent.putExtra("filePath", model.absolutePath)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                }
            }
        }
    }

    private fun getThumbnail(file: File): Bitmap? {
        val mSize = Size(96, 96)
        val thumbnail: Bitmap?
        val ca = CancellationSignal()
        if (file.isFile) {
            when (file.extension.lowercase()) {
                "jpg", "jpeg", "png", "gif", "bmp" -> {
                    thumbnail = try {
                        ThumbnailUtils.createImageThumbnail(file, mSize, ca)
                    } catch (e: Exception) {
                        val drawable = ContextCompat.getDrawable(context, R.drawable.file)
                        (drawable as BitmapDrawable).bitmap
                    }
                    return thumbnail
                }
                "mp3", "wav", "ogg", "flac", "aac", "wma", "m4a" -> {
                    thumbnail = try {
                        ThumbnailUtils.createAudioThumbnail(file, mSize, ca)
                    } catch (e: Exception) {
                        val drawable = ContextCompat.getDrawable(context, R.drawable.file)
                        (drawable as BitmapDrawable).bitmap
                    }
                    return thumbnail
                }
                "mp4", "3gp", "mkv", "avi", "mov", "wmv", "mpg", "mpeg" -> {
                    thumbnail = try {
                        ThumbnailUtils.createVideoThumbnail(file, mSize, ca)
                    } catch (e: Exception) {
                        val drawable = ContextCompat.getDrawable(context, R.drawable.file)
                        (drawable as BitmapDrawable).bitmap
                    }
                    return thumbnail
                }
                else -> {
                    val drawable = ContextCompat.getDrawable(context, R.drawable.file)
                    return (drawable as BitmapDrawable).bitmap
                }
            }
        } else {
            thumbnail = null
        }
        return thumbnail
    }

    private fun getMimeType(extension: String): String? {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getMimeTypeFromExtension(extension)
    }

    private fun timestampToLocalDateTime(timestamp: String): String? {
        val date = Date(timestamp.toLong())
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        val localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        return localDateTime.format(dateFormatter)
    }

    private fun sizeFormatter(size: Long)
            : CharSequence
    ? {
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

    override fun getItemCount()
            : Int {
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
