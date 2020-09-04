package utils

/*
 * Copyright (c) Jonas Waage 03/09/2020
 */
fun <T> Pair<T,T>.swap() : Pair<T,T> {
    return Pair(this.second,this.first)
}