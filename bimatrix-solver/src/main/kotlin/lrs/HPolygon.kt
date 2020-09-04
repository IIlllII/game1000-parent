package lrs

import lcp.Rational

class HPolygon(var matrix: Array<Array<Rational>>, nonnegative: Boolean) {
    var nonnegative = true
    var incidence = true
        // for recording the full cobasis at lexmin points
        set(value) {
            field = value
            if (value && !printcobasis)
                printcobasis = true
        }

    var printcobasis = true //incidence implies printcobasis
    //hull = false
    var linearities = IntArray(0)

    val numCols: Int
    val numRows: Int
    val dimension: Int

    init {
        numRows = matrix.size
        numCols = if (numRows > 0) matrix[0].size else 0
        dimension = numCols - 1

        this.nonnegative = nonnegative
    }


    /* for H-rep, are zero in column 0     */
    fun homogeneous(): Boolean {
        var ishomo = true
        for (row in matrix) {
            if (row.size < 1 || !row[0].isZero) {
                ishomo = false
            }
        }
        return ishomo
    }
}