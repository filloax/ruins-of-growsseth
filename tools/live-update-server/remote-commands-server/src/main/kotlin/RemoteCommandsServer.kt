package com.ruslan.remotecommands

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.lang.StringBuilder
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

fun main() {
    val portMc = 20000
    val portInput = 20001

    RemoteCommandsServer(portMc, portInput).run()
}

class RemoteCommandsServer(
    val portMc: Int,
    val portInput: Int,
) {
    var clientMc: Socket? = null

    private fun handleInputClient(clientSocket: Socket) {
        val address = clientSocket.inetAddress.hostAddress
        val inputReader = clientSocket.getInputStream().bufferedReader()
        val inputWriter = clientSocket.getOutputStream().printWriter()

        while (!clientSocket.isClosed) {
            val message = inputReader.readLine() ?: break

            logInput("Received from client ${address}: $message")

            clientMc?.let { mc -> synchronized(mc) {
                onInputMessage(clientSocket, inputWriter, message, mc)
            } } ?: run {
                logInput("No MC client connected, will reply with failure")
                inputWriter.println("no-mc")
            }
        }

        logInput("Client disconnected: $address")
    }

    // Weird read because mod uses NIO and sends data in a (evidently, not sure why) particular way
    private fun readFromMcSocket(mcSocket: Socket): String? {
        val inputStream = mcSocket.getInputStream()
        val buffer = ByteArray(1024)
        val numRead = inputStream.read(buffer)
        return if (numRead > 0)
            try {
                String(buffer, 0, numRead, Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        else
            null
    }

    private fun onInputMessage(clientSocket: Socket, inputWriter: PrintWriter, message: String, mcSocket: Socket) {
        val writer = mcSocket.getOutputStream().printWriter()
        if (message == "") {
            logInput("Empty input received, not sending")
            return
        }
        try {
            writer.println(message)
            logInput("Sent $message to client, waiting for response...")

            mcSocket.soTimeout = 20 * 1000
            try {
                val response = readFromMcSocket(mcSocket)
                if (response != null) {
                    logInput("Received $response from client")
                    inputWriter.println("mc-response: $response")
                } else {
                    logInput("MC was closed before response or error in response (try restarting server and client)")
                    inputWriter.println("mc-response-null")
                }
            } catch (e: SocketTimeoutException) {
                logInput("Socket timeout from mc client")
                inputWriter.println("mc-err: socket-timeout")
            }
            mcSocket.soTimeout = 0
        } catch (e: IOException) {
            e.printStackTrace()
            inputWriter.println("mc-err: ${e.message}")
        }
    }

    fun run() {
        val serverSocketMc = ServerSocket(portMc)
        val serverSocketInput = ServerSocket(portInput)

        thread(name="mc-thread", start=true) {
            while(true) {
                logMc("Listening on port $portMc...")
                val conn = serverSocketMc.accept()
                val name = conn.inetAddress.hostAddress
                logMc("Accepted client connection from $name")
                clientMc = conn

                // Wait for client mc to be disconnected
                while(true) {
                    Thread.sleep(1000)
                    if (conn.isClosed) break
                }
                clientMc = null

                logMc("Disconnected client $name")
            }
        }

        while(true) {
            logInput("Listening on port $portInput...")
            val inputClient = serverSocketInput.accept()
            logInput("Accepted connection from ${inputClient.inetAddress.hostAddress}...")
            thread(name="input-client-${inputClient.port}", start=true) {
                handleInputClient(inputClient)
            }
        }
    }

    private fun logInput(msg: String) {
        val ts = DateTimeFormatter.ISO_LOCAL_TIME.format(LocalDateTime.now())
        println("[IN] [$ts] $msg")
    }
    private fun logMc(msg: String) {
        val ts = DateTimeFormatter.ISO_LOCAL_TIME.format(LocalDateTime.now())
        println("[MC] [$ts] $msg")
    }

    private fun OutputStream.printWriter() = PrintWriter(this, true, Charsets.UTF_8)
}