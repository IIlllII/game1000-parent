package lcp

import lcp.Rational.Companion.ZERO


/**
 * Linear Complementarity Problem (aka. LCP)
 * =================================================================================
 * (1) Mz + q >= 0
 * (2) z >= 0
 * (3) z'(Mz + q) = 0
 *
 * (1) and (2) are feasibility conditions.
 * (3) is complementarity condition (also written as w = Mz + q where w and z are orthogonal)
 * Lemke algorithm takes this (M, q) and a covering vector (d) and outputs a solution
 */
class LCP/* allocate and initialize with zero entries an LCP of dimension  n
     * this is the only method changing  lcpdim
     * exit with error if fails, e.g. if  n  not sensible
     */
(dimension: Int) {

    // M and q define the LCP
    internal var M: Array<Array<Rational>>
    internal var q: Array<Rational>

    // d vector for Lemke algo
    internal var d: Array<Rational>

    val isTrivial: Boolean
        get() {
            var isQPos = true
            var i = 0
            val len = q.size
            while (i < len) {
                if (q[i]!!.compareTo(0) < 0) {
                    isQPos = false
                }
                ++i
            }
            return isQPos
        }

    init {
        if (dimension < 1 || dimension > MAXLCPDIM) {
            throw RuntimeException(String.format( //TODO: RuntimeException not the cleanest thing to do here
                    "Problem dimension  n=%d not allowed.  Minimum  n is 1, maximum %d.",
                    dimension, MAXLCPDIM))
        }

        M = Array(dimension) { Array(dimension) { ZERO} }
        q = Array(dimension){ ZERO}
        d = Array(dimension) { ZERO}

    }

    //when is negate false?
    // TODO: how do I consolidate the code in these two methods elegantly... delegate?
    // TODO: These methods do not really have anything to do with LCP...
    fun payratmatcpy(frommatr: Array<Array<Rational>>, bnegate: Boolean,
                     btranspfrommatr: Boolean, nfromrows: Int, nfromcols: Int,
                     targrowoffset: Int, targcoloffset: Int) {
        for (i in 0 until nfromrows) {
            for (j in 0 until nfromcols) {
                val value = if (frommatr[i][j].isZero) ZERO else if (bnegate) frommatr[i][j].negate() else frommatr[i][j]
                SetMEntry(btranspfrommatr, targrowoffset, targcoloffset, i, j, value)
            }
        }
    }

    //integer to rational matrix copy
    fun intratmatcpy(frommatr: Array<IntArray>, bnegate: Boolean,
                     btranspfrommatr: Boolean, nfromrows: Int, nfromcols: Int,
                     targrowoffset: Int, targcoloffset: Int) {
        for (i in 0 until nfromrows) {
            for (j in 0 until nfromcols) {
                val value = if (frommatr[i][j] == 0) ZERO else if (bnegate) Rational.valueOf(-frommatr[i][j].toLong()) else Rational.valueOf(frommatr[i][j].toLong())
                SetMEntry(btranspfrommatr, targrowoffset, targcoloffset, i, j, value)
            }
        }
    }

    private fun SetMEntry(btranspfrommatr: Boolean, targrowoffset: Int, targcoloffset: Int, i: Int, j: Int, value: Rational) {
        if (btranspfrommatr) {
            M[j + targrowoffset][i + targcoloffset] = value
        } else {
            M[i + targrowoffset][j + targcoloffset] = value
        }
    }

    fun M(): Array<Array<Rational>> {
        return M
    }

    fun M(row: Int, col: Int): Rational {
        return M[row][col] as Rational
    }

    fun setM(row: Int, col: Int, value: Rational) {
        M[row][col] = value
    }

    fun q(): Array<Rational> {
        return q
    }

    fun q(row: Int): Rational {
        return q[row]
    }

    fun setq(row: Int, value: Rational) {
        q[row] = value
    }

    fun d(): Array<Rational> {
        return d
    }

    fun d(row: Int): Rational {
        return d[row]
    }

    fun setd(row: Int, value: Rational) {
        d[row] = value
    }

    fun size(): Int {
        return d.size
    }

    override fun toString(): String {
        val colpp = ColumnTextWriter()
        colpp.writeCol("M")
        for (i in 1 until size()) {
            colpp.writeCol("")
        }
        colpp.writeCol("d")
        colpp.writeCol("q")
        colpp.endRow()

        for (i in 0 until size()) {
            for (j in 0 until M[i].size) {
                //if (j > 0) output.append(", ");
                colpp.writeCol(if (M[i][j] === Rational.ZERO) "." else M[i][j].toString())
            }
            colpp.writeCol(d[i].toString())
            colpp.writeCol(q[i].toString())
            colpp.endRow()
        }
        return colpp.toString()
    }

    companion object {
        private val MAXLCPDIM = 2000       /* max LCP dimension                       */ //MKE: Why do we need a max?
    }
}