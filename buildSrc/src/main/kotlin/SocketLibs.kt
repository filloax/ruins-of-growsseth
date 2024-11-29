package com.ruslan.gradle

import org.gradle.api.Project

val Project.socketIoLibs: List<String>
    get() = listOf(
        "org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5",
        "org.json:json:20231013",
        "org.java-websocket:Java-WebSocket:1.5.4",
        "com.squareup.okio:okio:3.4.0",
        "com.squareup.okhttp3:okhttp:4.11.0",
        "io.socket:engine.io-client:2.1.0",
        "io.socket:socket.io-client:2.1.0"
    )