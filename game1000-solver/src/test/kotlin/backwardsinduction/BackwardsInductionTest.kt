package backwardsinduction

import org.junit.Test
import org.junit.experimental.categories.Category
import solver.models.Pos
import solver.backwardsinduction.BackwardsInductionSolution
import solver.backwardsinduction.findPairsToCalculate
import test.ManualTests
import java.io.*
import kotlin.test.assertEquals


class BackwardsInductionTest {

    @Category(ManualTests::class)
    @Test
    fun writeTest() {

        val ul = BackwardsInductionSolution();
        ul.createSolution();

        BufferedWriter(FileWriter(File("solutions/ultimate/ultimate-with-nested-eq.json")))
                .use { writer ->
                    ul.storeSolution(writer)
                }

    }

    @Category(ManualTests::class)
    @Test
    fun readTest() {

        val ul = BackwardsInductionSolution();

        BufferedReader(FileReader(File("solutions/ultimate/ultimate.json"))).use {
            ul.readSolution(it);
        }

    }

    @Test
    fun findPairs() {
        val vl = arrayOf(Pos.ONES, Pos.TENS, Pos.HUNDREDS)
        val res = findPairsToCalculate(vl);
        assertEquals(setOf(
                Pair(Pos.ONES, Pos.TENS),
                Pair(Pos.ONES, Pos.HUNDREDS),
                Pair(Pos.TENS, Pos.HUNDREDS)),res)
    }

    @Test
    fun findTwo() {
        val vl = arrayOf(Pos.ONES, Pos.HUNDREDS)
        val res = findPairsToCalculate(vl);
        assertEquals(setOf(
                Pair(Pos.ONES, Pos.HUNDREDS)),res)
    }


}