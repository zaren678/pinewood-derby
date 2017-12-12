package com.johnanderson.pinewoodderbyapp.ext

fun <X> List<X>?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}