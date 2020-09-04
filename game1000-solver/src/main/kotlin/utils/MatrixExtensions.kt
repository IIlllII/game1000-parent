package utils

import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector







/**
 * Checks whether a mixed strat is needed for a positive payoff.
 */
fun RealMatrix.needsMixedStrat() : Boolean {
    val colsHasUnderZero = getColums().map { i-> val num = i.toArray().find { j -> j < 0.0}; num != null }
    val colsUnder = colsHasUnderZero.fold(true) { a, b->a && b};

    val rowHasOverZero = getRows().map { i-> val num = i.toArray().find { j -> j > 0.0}; num != null }
    val rowsOver = rowHasOverZero.fold(true) { a, b->a && b};

    return rowsOver && colsUnder && this.columnDimension >= 2 && this.rowDimension >= 2;
}


fun RealMatrix.getColums() : Array<RealVector> {
    return Array(this.columnDimension, this::getColumnVector)

}

fun RealMatrix.getRows() : Array<RealVector> {
    return Array(this.rowDimension, this::getRowVector)
}
