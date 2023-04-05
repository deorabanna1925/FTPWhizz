package com.seewhizz.ftpwhizz

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.seewhizz.ftpwhizz.adapter.ConnectionAdapter
import com.seewhizz.ftpwhizz.databinding.ActivityAddFtpBinding
import com.seewhizz.ftpwhizz.model.ConnectionModel
import com.seewhizz.ftpwhizz.shared.Prefs
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.util.concurrent.Executors


class AddFTPActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var binding: ActivityAddFtpBinding

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var arrayList = ArrayList<ConnectionModel>()
    private val MAX_HISTORY_ITEMS = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityAddFtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)


        setSupportActionBar(binding.toolbar)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.isNestedScrollingEnabled = false
        binding.recyclerView.setHasFixedSize(true)
        arrayList = getConnectionHistory()
        arrayList.reverse()
        if (arrayList.size == 0) {
            binding.addFtpLayout.visibility = View.VISIBLE
            binding.connectionTitle.visibility = View.GONE

        } else {
            binding.connectionTitle.visibility = View.VISIBLE
            binding.addFtpLayout.visibility = View.GONE
        }

        binding.connectionButton.setOnClickListener {
            if (binding.addFtpLayout.visibility == View.VISIBLE){
                binding.addFtpLayout.visibility = View.GONE
                binding.connectionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_add_24))
            }else {
                binding.connectionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_remove_24))
                binding.addFtpLayout.visibility = View.VISIBLE
            }
        }

        binding.recyclerView.adapter = ConnectionAdapter(this, arrayList)

        binding.loginButton.setOnClickListener {
            if(checkIfFieldIsNotEmpty()) {
                executor.execute {
                    val name = binding.connectionInput.text.toString()
                    val server = binding.serverInput.text.toString()
                    val port = binding.portInput.text.toString().toIntOrNull() ?: 21
                    val username = binding.userInput.text.toString()
                    val password = binding.passwordInput.text.toString()
                    if (ftpConnect(server, port, username, password)) {
                        addToConnectionHistory(ConnectionModel(name, server, port, username, password))
                        handler.post {
                            arrayList = ArrayList()
                            arrayList = getConnectionHistory()
                            arrayList.reverse()
                            binding.recyclerView.adapter = ConnectionAdapter(this, arrayList)
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            intent.putExtra("connection", binding.connectionInput.text.toString())
                            intent.putExtra("server", server)
                            intent.putExtra("port", port)
                            intent.putExtra("user", username)
                            intent.putExtra("pass", password)
                            startActivity(intent)
                        }
                    } else {
                        handler.post {
                            Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkIfFieldIsNotEmpty(): Boolean {
        if (binding.connectionInput.text.toString().isEmpty()) {
            binding.connectionInputLayout.error = "Please enter a name"
            return false
        }else{
            binding.connectionInputLayout.error = ""
        }
        if (binding.serverInput.text.toString().isEmpty()) {
            binding.serverInputLayout.error = "Please enter a server"
            return false
        } else {
            binding.serverInputLayout.error = ""
        }

        if (binding.userInput.text.toString().isEmpty()) {
            binding.userInputLayout.error = "Please enter a username"
            return false
        } else {
            binding.userInputLayout.error = ""
        }
        if (binding.passwordInput.text.toString().isEmpty()) {
            binding.passwordInputLayout.error = "Please enter a password"
            return false
        } else {
            binding.passwordInputLayout.error = ""
        }
        return true
    }

    private fun ftpConnect(host: String, port: Int, user: String, password: String): Boolean {
        try {
            val ftpClient = FTPClient()
            ftpClient.connect(host, port)
            return if (FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                ftpClient.login(user, password)
                if (FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
                    true
                } else {
                    ftpClient.disconnect()
                    false
                }
            } else {
                ftpClient.disconnect()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getConnectionHistory(): ArrayList<ConnectionModel> {
        val previousConnection: String = prefs.connectionHistory
        if (previousConnection == "") return ArrayList()
        val gson = Gson()
        val listType = object : TypeToken<ArrayList<ConnectionModel?>?>() {}.type
        return gson.fromJson(previousConnection, listType)
    }

    fun addToConnectionHistory(connections: ConnectionModel) {
        val connectionArrayList: ArrayList<ConnectionModel> = getConnectionHistory()
        connectionArrayList.add(connections)
        if (connectionArrayList.size > MAX_HISTORY_ITEMS) connectionArrayList.removeAt(0)
        val listType = object : TypeToken<ArrayList<ConnectionModel>>() {}.type
        val json = Gson().toJson(connectionArrayList, listType)
        prefs.connectionHistory = json
    }

    fun editConnection(model: ConnectionModel) {
        binding.connectionInput.setText(model.name)
        binding.serverInput.setText(model.server)
        binding.portInput.setText(model.port.toString())
        binding.userInput.setText(model.username)
        binding.passwordInput.setText(model.password)
        binding.addFtpLayout.visibility = View.VISIBLE
        binding.connectionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_remove_24))
    }

    fun deleteConnection(model: ConnectionModel) {
        val connectionArrayList: ArrayList<ConnectionModel> = getConnectionHistory()
        connectionArrayList.remove(model)
        val listType = object : TypeToken<ArrayList<ConnectionModel>>() {}.type
        val json = Gson().toJson(connectionArrayList, listType)
        prefs.connectionHistory = json
        arrayList = ArrayList()
        arrayList = getConnectionHistory()
        arrayList.reverse()
        binding.recyclerView.adapter = ConnectionAdapter(this, arrayList)
    }

}
