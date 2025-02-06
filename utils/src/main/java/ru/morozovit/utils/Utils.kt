package ru.morozovit.utils

fun String.shorten(chars: Int) =
    if (length <= chars)
        this
    else
        substring(0, chars - 3) + "..."