package com.ruslan.gradle

import org.gradle.api.Project

val Project.socketIoLibs: List<String>
    get() = listOf(
        "org.json:json:20231013",
        "org.java-websocket:Java-WebSocket:1.5.7",
        "com.squareup.okio:okio:3.6.0",
        "com.squareup.okhttp3:okhttp:4.12.0",
        "io.socket:engine.io-client:2.1.0",
        "io.socket:socket.io-client:2.1.1"
    )