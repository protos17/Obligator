package ru.protos.obligator

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform