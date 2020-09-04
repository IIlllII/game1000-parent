package comparison

import bimatrix.BimatrixSolver
import bimatrix.Equilibria
import lcp.Rational
import lrs.LrsAlgorithm
import org.junit.Test
import kotlin.test.assertEquals

class BimatrixSolverTest {
    @Test
    fun testFindAllEqWithDegeneracy() {
        val program = BimatrixSolver()
        val payoff1 = arrayOf(arrayOf(Rational.valueOf("3"), Rational.valueOf("3")), arrayOf(Rational.valueOf("2"), Rational.valueOf("5")), arrayOf(Rational.valueOf("0"), Rational.valueOf("6")))

        val payoff2 = arrayOf(arrayOf(Rational.valueOf("3"), Rational.valueOf("3")), arrayOf(Rational.valueOf("2"), Rational.valueOf("6")), arrayOf(Rational.valueOf("3"), Rational.valueOf("1")))

        val probs1 = arrayOf(arrayOf(Rational.valueOf("1"), Rational.valueOf("0"), Rational.valueOf("0")), arrayOf(Rational.valueOf("1"), Rational.valueOf("0"), Rational.valueOf("0")), arrayOf(Rational.valueOf("0"), Rational.valueOf("1/3"), Rational.valueOf("2/3")))
        val probs2 = arrayOf(arrayOf(Rational.valueOf("1"), Rational.valueOf("0")), arrayOf(Rational.valueOf("2/3"), Rational.valueOf("1/3")), arrayOf(Rational.valueOf("1/3"), Rational.valueOf("2/3")))
        val epayoffs1 = arrayOf(Rational.valueOf("3"), Rational.valueOf("3"), Rational.valueOf("4"))
        val epayoffs2 = arrayOf(Rational.valueOf("3"), Rational.valueOf("3"), Rational.valueOf("8/3"))

        val lrs = LrsAlgorithm()
        val eqs = program.findAllEq(lrs, payoff1, payoff2)
        checkResults(eqs, probs1, probs2, epayoffs1, epayoffs2)
    }

    @Test
    fun testFindAllEqNoDegeneracy() {
        val program = BimatrixSolver()
        val payoff1 = arrayOf(arrayOf(Rational.valueOf("3"), Rational.valueOf("3")), arrayOf(Rational.valueOf("2"), Rational.valueOf("5")), arrayOf(Rational.valueOf("0"), Rational.valueOf("6")))

        val payoff2 = arrayOf(arrayOf(Rational.valueOf("3"), Rational.valueOf("2")), arrayOf(Rational.valueOf("2"), Rational.valueOf("6")), arrayOf(Rational.valueOf("3"), Rational.valueOf("1")))

        val probs1 = arrayOf(arrayOf(Rational.valueOf("1"), Rational.valueOf("0"), Rational.valueOf("0")), arrayOf(Rational.valueOf("4/5"), Rational.valueOf("1/5"), Rational.valueOf("0")), arrayOf(Rational.valueOf("0"), Rational.valueOf("1/3"), Rational.valueOf("2/3")))
        val probs2 = arrayOf(arrayOf(Rational.valueOf("1"), Rational.valueOf("0")), arrayOf(Rational.valueOf("2/3"), Rational.valueOf("1/3")), arrayOf(Rational.valueOf("1/3"), Rational.valueOf("2/3")))
        val epayoffs1 = arrayOf(Rational.valueOf("3"), Rational.valueOf("9/3"), Rational.valueOf("4"))
        val epayoffs2 = arrayOf(Rational.valueOf("3"), Rational.valueOf("14/5"), Rational.valueOf("8/3"))

        val lrs = LrsAlgorithm()
        val eqs = program.findAllEq(lrs, payoff1, payoff2)
        checkResults(eqs, probs1, probs2, epayoffs1, epayoffs2)
    }

    private fun checkResults(eqs: Equilibria, probs1: Array<Array<Rational>>, probs2: Array<Array<Rational>>, epayoffs1: Array<Rational>, epayoffs2: Array<Rational>) {
        assertEquals(3, eqs.count())
        for (i in 0..2) {
            val eq = eqs.get(i)
            assertEquals(epayoffs1[i], eq.payoff1)
            assertEquals(epayoffs2[i], eq.payoff2)
            assertEquals(3, eq.probVec1!!.size)

            val vec = eq.probVec1;
            for (j in 0..2) {
                assertEquals(probs1[i][j], vec!![j] )
            }

            val vec2 = eq.probVec2;
            assertEquals(2, eq.probVec2!!.size)
            for (j in 0..1) {
                assertEquals(probs2[i][j], vec2!![j] )
            }
        }
    }
}