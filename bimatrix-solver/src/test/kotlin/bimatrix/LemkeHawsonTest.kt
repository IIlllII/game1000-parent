package comparison

import bimatrix.R
import org.junit.Test
import bimatrix.*
import lrs.LrsAlgorithm

/**
 *
 * This seems to work great!!
 *
 * How that algo works, I have no freaking clue!
 *
 */
class BimatrixMyGameTest {

    @Test
    fun testSolver() {

        val program = BimatrixSolver()

        val a = arrayOf(
                arrayOf(-14.R,2.R),
                arrayOf(15.R,-13.R),
                arrayOf(14.R,-9.R)
        )
        val b = arrayOf(
                arrayOf(14.R,-2.R),
                arrayOf(-15.R,13.R),
                arrayOf(-14.R,9.R)
        )

        val lrs = LrsAlgorithm()
        val eqs = program.findAllEq(lrs, a, b)

        val vl = eqs.get(0);

        println("Payoff1: ${vl.payoff1}")
        println("Payoff2: ${vl.payoff2}")

        println("Vec1: ${vl.probVec1!!.toList()}")
        println("Vec2: ${vl.probVec2!!.toList()}")

    }


    @Test
    fun testSolverPure() {

        val program = BimatrixSolver()

        val a = arrayOf(
                arrayOf(0.R,4.R),
                arrayOf(-4.R,0.R)
        )
        val b = arrayOf(
                arrayOf(0.R,-4.R),
                arrayOf(4.R,0.R)
        )

        val lrs = LrsAlgorithm()
        val eqs = program.findAllEq(lrs, a, b)

        val vl = eqs.get(0);

        println("Payoff1: ${vl.payoff1}")
        println("Payoff2: ${vl.payoff2}")

        println("Vec1: ${vl.probVec1!!.toList()}")
        println("Vec2: ${vl.probVec2!!.toList()}")

    }


}
