package utils

import org.apache.commons.math3.fraction.BigFraction
import org.apache.commons.math3.linear.*
import org.junit.Test
import kotlin.test.assertEquals


class SupportTest {

    @Test
    fun testPermMake() {

        val vl = findSupports(3,2)

        assertEquals(vl.second, setOf(setOf(1,2)))

        assertEquals(vl.first, setOf(setOf(1,2),setOf(1,3),setOf(2,3)))
    }


    @Test
    fun testRes() {
        val vg = getEquationCoefficients(
                Array2DRowRealMatrix(
                        arrayOf(
                                doubleArrayOf(1.0,-1.0),
                                doubleArrayOf(-1.0,1.0)
                        ),false),
                        setOf(1,2),
                        setOf(1,2)
                )

        assertEquals(Array2DRowRealMatrix(arrayOf(doubleArrayOf(-1.0,1.0), doubleArrayOf(1.0,-1.0))),vg.first)

        assertEquals(Array2DRowRealMatrix(arrayOf(doubleArrayOf(1.0,-1.0), doubleArrayOf(-1.0,1.0))),vg.second)

        val x = makeStandardForm(vg.first)

        val solver = LUDecomposition(x).solver

        val constants = ArrayRealVector(doubleArrayOf(0.0, 0.0, 1.0), false)
        val solution = solver.solve(constants)

        println(solution);

    }


    @Test
    fun testResLargerGame() {
        val vg = getEquationCoefficients(
                Array2DRowRealMatrix(
                        arrayOf(
                                doubleArrayOf(-14.0,2.0),
                                doubleArrayOf(15.0,-13.0),
                                doubleArrayOf(14.0,-9.0)
                        ),false),
                setOf(1,2),
                setOf(1,2)
        )

        assertEquals(Array2DRowRealMatrix(arrayOf(doubleArrayOf(14.0,-15.0), doubleArrayOf(-2.0,13.0))),vg.first)

        assertEquals(Array2DRowRealMatrix(arrayOf(doubleArrayOf(-14.0,2.0), doubleArrayOf(15.0,-13.0))),vg.second)

        val x = makeStandardForm(vg.first)

        val solver = LUDecomposition(x).solver

        val constants = ArrayRealVector(doubleArrayOf(0.0, 0.0, 1.0), false)
        val solution = solver.solve(constants)

        println(solution);

    }

    @Test
    fun testResLargerGameDifferentSupport() {
        val matrix = Array2DRowRealMatrix(
                arrayOf(
                        doubleArrayOf(-14.0,2.0),
                        doubleArrayOf(15.0,-13.0),
                        doubleArrayOf(14.0,-9.0)
                ),false);

        val vg = getEquationCoefficients(
                matrix,
                setOf(1,3),
                setOf(1,2)
        )

        assertEquals(Array2DRowRealMatrix(arrayOf(doubleArrayOf(14.0,-14.0), doubleArrayOf(-2.0,9.0))),vg.first)
        assertEquals(Array2DRowRealMatrix(arrayOf(doubleArrayOf(-14.0,2.0), doubleArrayOf(14.0,-9.0))),vg.second)

        val x = makeStandardForm(vg.first)
        val y = makeStandardForm(vg.second)

        val solutionA = solve(x)
        val solutionB = solve(y)

        println("$solutionA -- $solutionB");

    }


    operator fun BigFraction.unaryMinus() : BigFraction {
        return this.multiply(-1)
    }

    @Test
    fun allSupports() {
        val matrix = Array2DRowRealMatrix(
                arrayOf(
                        doubleArrayOf(-14.0,2.0),
                        doubleArrayOf(15.0,-13.0),
                        doubleArrayOf(14.0,-9.0)
                ),false);

        val v = findSupports(matrix.rowDimension, matrix.columnDimension)

        v.first.forEach { s1 ->
            v.second.forEach { s2 ->

                val vg = getEquationCoefficients(matrix, s1, s2)

                val x = makeStandardForm(vg.first)
                val y = makeStandardForm(vg.second)

                val solutionA = solve(x)
                val solutionB = solve(y)

                println("$s1,$s2 -> $solutionA -- $solutionB");

            }
        }



    }

}