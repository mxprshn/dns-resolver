package utils

fun log(message: String) = println(message)

fun log(throwable: Throwable, message: String) = println("$message: ${throwable.message}")