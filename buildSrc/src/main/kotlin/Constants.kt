package com.ruslan.gradle

const val BASE_PROJECT = ":base"
const val COMMON_JAVA = "commonJava"
const val COMMON_RESOURCES = "commonResources"

val PRE_COMPILE_TASKS = listOf(
    "restoreSources",
)