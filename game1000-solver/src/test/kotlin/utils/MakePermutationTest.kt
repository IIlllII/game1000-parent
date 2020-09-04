package utils

import org.junit.Test
import utils.makeGenerationsOfSize
import utils.makePermutationsOfSize
import kotlin.test.assertEquals



class MakePermutationTest {

    @Test
    fun testPermMake() {

        val vl = makePermutationsOfSize(listOf(1,2),2)
        assertEquals(setOf(listOf(1,2), listOf(2,1)),vl)

    }


    @Test
    fun testPermMore() {

        val vl = makePermutationsOfSize(listOf(1,2,3),2)
        assertEquals(setOf(listOf(1,2), listOf(2,1),listOf(1,3), listOf(3,1), listOf(2,3),listOf(3,2)),vl)

    }


    @Test
    fun testGenMore() {

        val vl = makeGenerationsOfSize(listOf(1,2,3),2)
        assertEquals(setOf(setOf(1,2), setOf(1,3), setOf(3,2)),vl)

    }
}