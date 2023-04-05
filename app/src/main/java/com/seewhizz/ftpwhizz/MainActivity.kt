package com.seewhizz.ftpwhizz

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.seewhizz.ftpwhizz.adapter.LocalFileFolderAdapter
import com.seewhizz.ftpwhizz.adapter.RemoteFileFolderAdapter
import com.seewhizz.ftpwhizz.databinding.ActivityMainBinding
import com.seewhizz.ftpwhizz.databinding.DialogCreateFileFolderBinding
import com.seewhizz.ftpwhizz.utils.FTPUtils
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.io.CopyStreamAdapter
import org.apache.commons.net.io.CopyStreamException
import java.io.*
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var ftpClient = FTPClient()
    private var localRootPath = ""
    private var remoteRootPath = ""
    private var connection = ""
    private var server = ""
    private var port = 0
    private var user = ""
    private var pass = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.extras?.let {
            connection = it.getString("connection")!!
            server = it.getString("server")!!
            port = it.getInt("port")
            user = it.getString("user")!!
            pass = it.getString("pass")!!
        }

        localRootPath =
            ContextCompat.getExternalFilesDirs(this, null)[0].toString() + "/" + connection

        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(R.string.app_name)

        binding.toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.local_button -> {
                        binding.recyclerViewLocal.visibility = View.VISIBLE
                        binding.recyclerViewRemote.visibility = View.GONE
                        binding.localRootPath.text = localRootPath
                    }
                    R.id.remote_button -> {
                        binding.recyclerViewLocal.visibility = View.GONE
                        binding.recyclerViewRemote.visibility = View.VISIBLE
                        binding.remoteRootPath.text = remoteRootPath
                    }
                }
            }
        }

        binding.toggleButton.check(R.id.remote_button)

        executor.execute {
            if (ftpConnect(server, port, user, pass)) {
                getFiles("")
            } else {
                Toast.makeText(this, "FTP connection failed", Toast.LENGTH_SHORT).show()
            }
        }

        loadLocalFiles()

        supportActionBar!!.subtitle = "$user@$server:$port"

    }

    fun deleteRemoteFile(file: FTPFile) {
        executor.execute {
            try {
                if (ftpClient.deleteFile(file.name)) {
                    handler.post {
                        Snackbar.make(binding.root, "File deleted", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    handler.post {
                        Snackbar.make(binding.root, "File delete failed", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ftpClient = FTPClient()
                if (ftpConnect(server, port, user, pass)) {
                    deleteRemoteFile(file)
                } else {
                    Toast.makeText(this, "FTP connection failed", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    fun deleteRemoteFolder(file: FTPFile) {
        executor.execute {
            try {
                if (ftpClient.removeDirectory(file.name)) {
                    handler.post {
                        Snackbar.make(binding.root, "Folder deleted", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    handler.post {
                        FTPUtils.removeDirectory(ftpClient, remoteRootPath, file.name)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ftpClient = FTPClient()
                if (ftpConnect(server, port, user, pass)) {
                    deleteRemoteFolder(file)
                } else {
                    Toast.makeText(this, "FTP connection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun ftpConnect(host: String, port: Int, user: String, password: String): Boolean {
        return try {
            ftpClient.connect(host, port)
            ftpClient.login(user, password)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            FTPReply.isPositiveCompletion(ftpClient.replyCode)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getFiles(newpath: String) {
        executor.execute {
            if (newpath != "") {
                try {
                    ftpClient.changeWorkingDirectory(newpath)
                    val files: Array<FTPFile> = ftpClient.listFiles()
                    remoteRootPath = ftpClient.printWorkingDirectory()
                    handler.post {
                        printFileDetails(files)
                        binding.remoteRootPath.text = remoteRootPath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ftpClient = FTPClient()
                    if (ftpConnect(server, port, user, pass)) {
                        getFiles(newpath)
                    } else {
                        Toast.makeText(this, "FTP connection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val files: Array<FTPFile> = ftpClient.listFiles()
                remoteRootPath = ftpClient.printWorkingDirectory()
                handler.post {
                    binding.remoteRootPath.text = remoteRootPath
                    printFileDetails(files)
                }
            }
        }
    }


    fun downloadFile(remoteFile: FTPFile) {
        executor.execute {
            try {
                val file = File(localRootPath + "/" + remoteFile.name)
                if (file.exists()) {
                    Snackbar.make(binding.root, "File already exists!", Snackbar.LENGTH_SHORT)
                        .show()
                    return@execute
                } else {
                    file.createNewFile()
                }
                ftpClient.retrieveFile(remoteFile.name, FileOutputStream(file))
                Snackbar.make(binding.root, "File downloaded!", Snackbar.LENGTH_SHORT).show()
                loadLocalFiles()
            } catch (e: Exception) {
                e.printStackTrace()
                if (ftpConnect(server, port, user, pass)) {
                    downloadFile(remoteFile)
                } else {
                    Toast.makeText(this, "FTP connection failed", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }


    private fun printFileDetails(files: Array<FTPFile>) {
        val arrayList: ArrayList<FTPFile> = ArrayList()
        for (file in files) {
            if (!file.name.equals(".") && !file.name.equals("..") && !file.name.equals(".htaccess")) {
                arrayList.add(file)
            }
        }
        binding.recyclerViewRemote.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRemote.isNestedScrollingEnabled = false
        binding.recyclerViewRemote.setHasFixedSize(true)
        arrayList.sortByDescending { it.isDirectory }
        binding.recyclerViewRemote.adapter = RemoteFileFolderAdapter(this, ftpClient, arrayList)
    }

    private fun loadLocalFiles() {
        val localPath = localRootPath
        File(localPath).mkdir()
        val directory = File(localPath)
        val files: Array<File> = directory.listFiles()!!
        val arrayList: ArrayList<File> = ArrayList()
        for (file in files) {
            arrayList.add(file)
        }
        binding.localRootPath.text = localRootPath
        binding.recyclerViewLocal.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLocal.isNestedScrollingEnabled = false
        binding.recyclerViewLocal.setHasFixedSize(true)
        binding.recyclerViewLocal.adapter = LocalFileFolderAdapter(this, ftpClient, arrayList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (binding.recyclerViewRemote.visibility == View.VISIBLE) {
                    if (binding.remoteRootPath.text != "/") {
                        getFiles("..")
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Exit")
                            .setMessage("Are you sure you want to exit?")
                            .setPositiveButton("Yes") { _, _ ->
                                finish()
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                } else if (binding.recyclerViewLocal.visibility == View.VISIBLE) {
                    if (binding.localRootPath.text != "/storage/emulated/0/Android/data/com.seewhizz.ftpwhizz/files") {
                        localRootPath = localRootPath.substring(0, localRootPath.lastIndexOf("/"))
                        loadLocalFolder(localRootPath)
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Exit")
                            .setMessage("Are you sure you want to exit?")
                            .setPositiveButton("Yes") { _, _ ->
                                finish()
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
                return true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_create_local_file -> {
                createNewLocalFile()
                return true
            }
            R.id.action_create_local_folder -> {
                createNewLocalFolder()
                return true
            }
            R.id.action_refresh -> {
                if (binding.recyclerViewRemote.visibility == View.VISIBLE) {
                    getFiles("")
                } else if (binding.recyclerViewLocal.visibility == View.VISIBLE) {
                    loadLocalFiles()
                }
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory)
            for (child in fileOrDirectory.listFiles()!!)
                deleteRecursive(child)
        fileOrDirectory.delete()
        loadLocalFiles()
    }

    private fun downloadFolder(ftpClient: FTPClient, remotePath: String, localPath: String) {
        executor.execute {
            try {
                File(localPath).mkdir()
                val remoteFiles = ftpClient.listFiles(remotePath)
                for (remoteFile: FTPFile in remoteFiles) {
                    if (remoteFile.name != "." && remoteFile.name != "..") {
                        val remoteFilePath = remotePath + "/" + remoteFile.name
                        val localFilePath = localPath + "/" + remoteFile.name
                        if (remoteFile.isDirectory) {
                            File(localFilePath).mkdirs()
                            downloadFolder(ftpClient, remoteFilePath, localFilePath)
                        } else {
                            handler.post {
                                Snackbar.make(
                                    binding.root,
                                    "Downloading file: " + remoteFile.name,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                            val outputStream: OutputStream =
                                BufferedOutputStream(FileOutputStream(localFilePath))
                            if (!ftpClient.retrieveFile(remoteFilePath, outputStream)) {
                                handler.post {
                                    Snackbar.make(
                                        binding.root,
                                        "Failed to download file: " + remoteFile.name,
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            outputStream.close()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (ftpConnect(server, port, user, pass)) {
                    downloadFolder(ftpClient, remotePath, localPath)
                } else {
                    Toast.makeText(this, "FTP connection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun loadLocalFolder(absolutePath: String) {
        localRootPath = absolutePath
        val directory = File(absolutePath)
        val files: Array<File> = directory.listFiles()!!
        val arrayList: ArrayList<File> = ArrayList()
        for (file in files) {
            arrayList.add(file)
        }
        binding.localRootPath.text = localRootPath
        binding.recyclerViewLocal.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLocal.isNestedScrollingEnabled = false
        binding.recyclerViewLocal.setHasFixedSize(true)
        binding.recyclerViewLocal.adapter = LocalFileFolderAdapter(this, ftpClient, arrayList)
    }

    fun prepareDownloadFolder(ftpClient: FTPClient, name: String) {
        val localPath = localRootPath
        val localFolder = "$localPath/$name"
        downloadFolder(ftpClient, name, localFolder)
    }

    fun createNewLocalFolder() {
        val builder = MaterialAlertDialogBuilder(this)
        val inflater = layoutInflater
        val dialogDesign = DialogCreateFileFolderBinding.inflate(inflater)
        builder.setView(dialogDesign.root)
        builder.setTitle("Create new folder")
        builder.setPositiveButton("Create") { dialog, _ ->
            val folderName = dialogDesign.fileFolderNameEditText.text.toString()
            val localPath = localRootPath
            val localFolder = "$localPath/$folderName"
            File(localFolder).mkdir()
            loadLocalFiles()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun createNewLocalFile() {
        val builder = MaterialAlertDialogBuilder(this)
        val inflater = layoutInflater
        val dialogDesign = DialogCreateFileFolderBinding.inflate(inflater)
        builder.setView(dialogDesign.root)
        builder.setTitle("Create new file")
        dialogDesign.fileFolderName.helperText = "file_name.extension"
        builder.setPositiveButton("Create") { dialog, _ ->
            if (!dialogDesign.fileFolderNameEditText.text.toString().contains(".")) {
                Snackbar.make(binding.root, "File extension is required", Snackbar.LENGTH_SHORT)
                    .show()
                return@setPositiveButton
            }
            val fileName = dialogDesign.fileFolderNameEditText.text.toString()
            val localPath = localRootPath
            val localFile = "$localPath/$fileName"
            File(localFile).createNewFile()
            loadLocalFiles()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    fun refreshRemoteFiles() {
        val remotePath = binding.remoteRootPath.text.toString()
        getFiles(remotePath)
    }

    fun uploadFile(model: File) {
        executor.execute {
            var remotePath = binding.remoteRootPath.text.toString()
            if (model.isDirectory) {
                remotePath += "/${model.name}"
                ftpClient.makeDirectory(remotePath)
                uploadFolder(model, remotePath)
            } else {
                uploadFile(model, remotePath)
            }
        }
    }

    fun uploadFolder(model: File, remotePath: String) {
        executor.execute {
            val files = model.listFiles()
            for (file in files) {
                if (file.isDirectory) {
                    val newRemotePath = "$remotePath/${file.name}"
                    ftpClient.makeDirectory(newRemotePath)
                    uploadFolder(file, newRemotePath)
                } else {
                    uploadFile(file, remotePath)
                }
            }
        }
    }

    fun uploadFile(model: File, remotePath: String) {
        executor.execute {
            val inputStream: InputStream = BufferedInputStream(FileInputStream(model))
            val remoteFilePath = "$remotePath/${model.name}"
            if (!ftpClient.storeFile(remoteFilePath, inputStream)) {
                handler.post {
                    Snackbar.make(
                        binding.root,
                        "Failed to upload file: " + model.name,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            inputStream.close()
        }
    }


}