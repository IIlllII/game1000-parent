package utils

import getPermutations
import org.junit.Test


class TestPerm {

    @Test
    fun test(){
        val wl = getPermutations(15,6,3).filter { i-> !i.contains(0) };
        println("$wl")

        val wat = getPermutations(9,6,2).filter { i-> !i.contains(0) };
        println("$wat")

        val hun = getPermutations(4,6,2).filter { i-> !i.contains(0) };
        println("$hun")

        val out = wl.flatMap { i-> wat.flatMap { j-> hun.map { k->
            i+j+k;
        } } }

        println("$out")

    }


}