package com.seewhizz.ftpwhizz.utils

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors


/**
 * This utility class implements a method that removes a non-empty directory
 * on a FTP server.
 */
object FTPUtils {
    /**
     * Removes a non-empty directory by delete all its sub files and
     * sub directories recursively. And finally remove the directory.
     */
    @Throws(IOException::class)
    fun removeDirectory(
        ftpClient: FTPClient, parentDir: String,
        currentDir: String
    ) {
        val executor: Executor = Executors.newSingleThreadExecutor()
        executor.execute {
            var dirToList = parentDir
            if (currentDir != "") {
                dirToList += "/$currentDir"
            }
            val subFiles: Array<FTPFile?>?
            try {
                subFiles = ftpClient.listFiles(dirToList)
                if (subFiles != null && subFiles.isNotEmpty()) {
                    for (aFile in subFiles) {
                        val currentFileName = aFile!!.name
                        if (currentFileName == "." || currentFileName == "..") {
                            // skip parent directory and the directory itself
                            continue
                        }
                        var filePath = (parentDir + "/" + currentDir + "/"
                                + currentFileName)
                        if (currentDir == "") {
                            filePath = "$parentDir/$currentFileName"
                        }
                        if (aFile.isDirectory) {
                            // remove the sub directory
                            removeDirectory(ftpClient, dirToList, currentFileName)
                        } else {
                            // delete the file
                            val deleted = ftpClient.deleteFile(filePath)
                            if (deleted) {
                                println("DELETED the file: $filePath")
                            } else {
                                println(
                                    "CANNOT delete the file: "
                                            + filePath
                                )
                            }
                        }
                    }

                    // finally, remove the directory itself
                    val removed = ftpClient.removeDirectory(dirToList)
                    if (removed) {
                        println("REMOVED the directory: $dirToList")
                    } else {
                        println("CANNOT remove the directory: $dirToList")
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}