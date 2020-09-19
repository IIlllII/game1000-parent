package utils

import lcp.Rational
import org.apache.commons.math3.fraction.BigFraction
import org.apache.commons.math3.fraction.BigFractionField
import org.apache.commons.math3.linear.*
import java.lang.IllegalArgumentException


val Int.BF : BigFraction
    get() = BigFraction(this)


val Long.BF : BigFraction
    get() = BigFraction(this)

val Rational.BF : BigFraction
    get() = BigFraction(this.num,this.den)



fun makeStandardForm(arr : Array2DRowFieldMatrix<BigFraction>) : Array2DRowFieldMatrix<BigFraction> {
    val next = Array2DRowFieldMatrix(BigFractionField.getInstance(),arr.rowDimension+1,arr.columnDimension+1)

    next.setSubMatrix(arr.dataRef,0,0)
    val a = Array<BigFraction>(next.rowDimension) { BigFraction.MINUS_ONE }
    val b = Array<BigFraction>(next.columnDimension) { BigFraction.ONE }

    next.setColumn(next.columnDimension-1,a)
    next.setRow(next.rowDimension-1,b)

    next.setEntry(next.rowDimension-1,next.columnDimension-1, BigFraction.ZERO)
    return next
}

/**
 * checkout FieldLUDecomposition and BigRealField
 */
fun solve(arr : Array2DRowFieldMatrix<BigFraction>) : FieldVector<BigFraction> {
    val solver = FieldLUDecomposition(arr).solver

    val backing = Array<BigFraction>(arr.columnDimension)
    { i -> if(i == arr.columnDimension-1) BigFraction.ONE else BigFraction.ZERO}
    val vec = ArrayFieldVector(backing)

    val constants = ArrayFieldVector(vec, false)
    return solver.solve(constants)
}


/**
 * checkout FieldLUDecomposition and BigRealField
 */
fun solve(arr : Array2DRowRealMatrix) : RealVector {
    val solver = LUDecomposition(arr).solver

    val vec = DoubleArray(arr.columnDimension) {i -> if(i == arr.columnDimension-1) 1.0 else 0.0}
    val constants = ArrayRealVector(vec, false)
    return solver.solve(constants)
}

/**
 * Takes the coefficients and makes the equations into standard form so they can be solved using matrix techniques
 */
fun makeStandardForm(arr : Array2DRowRealMatrix) : Array2DRowRealMatrix {
    val next = Array2DRowRealMatrix(arr.rowDimension+1,arr.columnDimension+1)
    next.setSubMatrix(arr.dataRef,0,0)
    val a = DoubleArray(next.rowDimension) { -1.0 }
    val b = DoubleArray(next.columnDimension) { 1.0 }

    next.setColumn(next.columnDimension-1,a)
    next.setRow(next.rowDimension-1,b)

    next.setEntry(next.rowDimension-1,next.columnDimension-1,0.0)
    return next
}

fun getEquationCoefficients(arr : Array2DRowFieldMatrix<BigFraction>, s1: Set<Int>, s2 : Set<Int>)
        : Pair<Array2DRowFieldMatrix<BigFraction>,Array2DRowFieldMatrix<BigFraction>>{

    val A = arr;
    val B = arr.scalarMultiply(BigFraction.MINUS_ONE)

    val sigma = s2.map { j ->
        s1.map { i ->
            val coE = B.getEntry(i-1,j-1)
            coE
        }.toTypedArray()
    }.toTypedArray()

    val num = s1.map { j->
        s2.map { i ->
            val coE = A.getEntry(j-1,i-1);
            coE
        }.toTypedArray()
    }.toTypedArray()

    return Pair(Array2DRowFieldMatrix(sigma,false),Array2DRowFieldMatrix(num,false))
}



fun getEquationCoefficients(arr : Array2DRowRealMatrix, s1: Set<Int>, s2 : Set<Int>) : Pair<Array2DRowRealMatrix,Array2DRowRealMatrix>{

    val A = arr;
    val B = arr.scalarMultiply(-1.0)

    val sigma = s2.map { j ->
        s1.map { i ->
            val coE = B.getEntry(i-1,j-1)
            coE
        }.toDoubleArray()
    }.toTypedArray()

    val num = s1.map { j->
        s2.map { i ->
            val coE = A.getEntry(j-1,i-1);
            coE
        }.toDoubleArray()
    }.toTypedArray()

    return Pair(Array2DRowRealMatrix(sigma,false),Array2DRowRealMatrix(num,false))
}


fun findSupports(rows : Int, cols : Int) : Pair<Set<Set<Int>>,Set<Set<Int>>> {

    val support = rows.coerceAtMost(cols);

    val aSupports = makeGenerationsOfSize((1..rows).toList(),support)

    val bSupport = makeGenerationsOfSize((1..cols).toList(),support)

    return Pair(aSupports,bSupport);
}

fun <T> makePermutationsOfSize(seq : List<T>, size : Int) : Set<List<T>> {
    if(size > seq.size || size < 0) {
        throw IllegalArgumentException("Size $size does not work with list size ${seq.size}")
    }
    return makePerm(seq, emptyList(),0,size)
}

/**
 *
 */
private fun <T> makePerm(seq : List<T>, current:List<T>, depth : Int, maxDepth : Int) : Set<List<T>> {
    return if(depth == maxDepth) {
        setOf(current);
    } else {
        seq.flatMap { i -> makePerm(seq.minus(i),current+i,depth+1,maxDepth) }.toSet()
    }
}

fun <T> makeGenerationsOfSize(seq : List<T>, size : Int) : Set<Set<T>> {
    if(size > seq.size) {
        throw IllegalArgumentException("Size $size does not work with list size ${seq.size}")
    }
    return makeGen(seq, emptySet(),0,size)
}

/**
 * Very inefficient since it makes permutations and filter, but ok for my use
 */
private fun <T> makeGen(seq : List<T>, current:Set<T>, depth : Int, maxDepth : Int) : Set<Set<T>> {
    return if(depth == maxDepth) {
        setOf(current);
    } else {
        seq
                .flatMap { i -> makeGen(seq.minus(i),current+i,depth+1,maxDepth) }
                .toSet()
    }
}