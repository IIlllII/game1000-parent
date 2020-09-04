package lcp

import lcp.Rational.Companion.ZERO
import java.math.BigInteger

// TODO: Make inner static class of Tableau
class TableauVariables/* init tableau variables:                      */
/* Z(0)...Z(n)  nonbasic,  W(1)...W(n) basic    */
/* This is for setting up a complementary basis/cobasis */
(private val n: Int) {
    /* VARS   = 0..2n = Z(0) .. Z(n) W(1) .. W(n)           */
    /* ROWCOL = 0..2n,  0 .. n-1: tabl rows (basic vars)    */
    /*                  n .. 2n:  tabl cols  0..n (cobasic) */
    private val bascobas: IntArray          /* VARS  -> ROWCOL                      */
    private val whichvar: IntArray          /* ROWCOL -> VARS, inverse of bascobas  */

    init {

        bascobas = IntArray(2 * n + 1)
        whichvar = IntArray(2 * n + 1)

        for (i in 0..n) {
            bascobas[z(i)] = n + i
            whichvar[n + i] = z(i)
        }
        for (i in 1..n) {
            bascobas[w(i)] = i - 1
            whichvar[i - 1] = w(i)
        }
    }


    /* create string  s  representing  v  in  VARS,  e.g. "w2"    */
    /* return value is length of that string                      */
    fun toString(`var`: Int): String {
        return if (!isZVar(`var`))
            String.format("w%d", `var` - n)
        else
            String.format("z%d", `var`)
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("bascobas: [")
        for (i in bascobas.indices) {
            output.append(String.format(" %d", bascobas[i]))
        }
        output.append(" ]")
        return output.toString()
    }

    fun swap(enterVar: Int, leaveVar: Int, leaveRow: Int, enterCol: Int) {
        bascobas[leaveVar] = enterCol + n
        whichvar[enterCol + n] = leaveVar

        bascobas[enterVar] = leaveRow
        whichvar[leaveRow] = enterVar
    }

    fun z(idx: Int): Int {
        return idx
    }

    fun w(idx: Int): Int {
        return idx + n
    }

    fun rowVar(row: Int): Int {
        return whichvar[row]
    }

    internal fun colVar(col: Int): Int {
        return whichvar[col + n]
    }

    internal fun row(`var`: Int): Int {
        return bascobas[`var`]
    }

    internal fun col(`var`: Int): Int {
        return bascobas[`var`] - n
    }

    fun isBasic(`var`: Int): Boolean {
        return bascobas[`var`] < n
    }

    fun isZVar(`var`: Int): Boolean {
        return `var` <= n
    }

    fun size(): Int {
        return n
    }

    /**
     * TODO: This might belong in the Lemke Algorithm and not here.
     * complement of  v  in VARS, error if  v==Z(0).
     * this is  W(i) for Z(i)  and vice versa, i=1...n
     */
    fun complement(`var`: Int): Int {
        if (`var` == z(0))
            throw RuntimeException("Attempt to find complement of z0.") //TODO
        return if (!isZVar(`var`)) z(`var` - n) else w(`var`)
    }


    /* assert that  v  in VARS is a basic variable         */
    /* otherwise error printing  info  where               */
    fun assertBasic(`var`: Int) {
        if (!isBasic(`var`)) {
            val error = String.format("Cobasic variable %s should be basic.", toString(`var`))
            throw RuntimeException(error) //TODO
        }
    }

    /* assert that  v  in VARS is a cobasic variable       */
    /* otherwise error printing  info  where               */
    fun assertNotBasic(`var`: Int) {
        if (isBasic(`var`)) {
            val error = String.format("Basic variable %s should be cobasic.", toString(`var`))
            throw RuntimeException(error) //TODO
        }
    }

}


// TODO: try to keep all row/col logic encapsulated.
// Users deal just with vars
// TODO: decouple the LCP specifics
// TODO: better constructor, make sure scalefactors are correct
class Tableau
//region Init

/* VARS   = 0..2n = Z(0) .. Z(n) W(1) .. W(n)           */
/* ROWCOL = 0..2n,  0 .. n-1: tabl rows (basic vars)    */
/*                  n .. 2n:  tabl cols  0..n (cobasic) */
(private val nrows: Int) {
    private val A: Array<Array<BigInteger>>
    private val ncols: Int

    private var det: BigInteger? = null     /* determinant                  */

    /* scale factors for variables z
     * scfa[Z(0)]   for  d,  scfa[RHS] for  q
     * scfa[Z(1..n)] for cols of  M
     * result variables to be multiplied with these
     */
    private val scfa: Array<BigInteger>

    /*  v in VARS, v cobasic:  TABCOL(v) is v's tableau col */
    /*  v  basic:  TABCOL(v) < 0,  TABCOL(v)+n   is v's row */
    private val vars: TableauVariables


    fun RHS(): Int {
        return ncols - 1
    }                        /*  q-column of tableau    */

    fun vars(): TableauVariables {
        return vars
    }

    init {
        this.ncols = nrows + 2

        A = Array(nrows) { Array(ncols) { BigInteger.ZERO} } //Changed, was null
        scfa = Array(ncols) {BigInteger.ZERO} //Changed was null

        vars = TableauVariables(nrows)

        det = BigInteger.valueOf(-1) // TODO: how do I know this? Specific to LCP?
    }

    /* fill tableau from  M, q, d   */
    /* TODO: decouple Tableau from LCP... have LCP return the Tableau
     * constructor for Tableau should be of lhs and rhs?
     */
    fun fill(M: Array<Array<Rational>>, q: Array<Rational>, d: Array<Rational>) {
        if (M.size != q.size || M.size != d.size || M.size > 1 && M.size != M[0].size)
            throw RuntimeException("LCP components not consistent dimension") //TODO

        if (M.size != nrows)
            throw RuntimeException("LCP dimension does not fit in Tableau size") //TODO

        for (j in 0 until ncols) {
            val scaleFactor = ComputeScaleFactor(j, M, q, d)
            scfa[j] = scaleFactor

            /* fill in col  j  of  A    */
            for (i in 0 until nrows) {
                val rat = RatForRowCol(i, j, M, q, d)

                /* cols 0..n of  A  contain LHS cobasic cols of  Ax = b     */
                /* where the system is here         -Iw + dz_0 + Mz = -q    */
                /* cols of  q  will be negated after first min ratio test   */
                /* A[i][j] = num * (scfa[j] / den),  fraction is integral       */

                A[i][j] = scaleFactor.divide(rat.den).multiply(rat.num)
            }
        }   /* end of  for(j=...)   */
    }       /* end of filltableau()         */


    /* compute lcm  of denominators for  col  j  of  A                   */
    /* Necessary for converting fractions to integers and back again    */
    private fun ComputeScaleFactor(col: Int, M: Array<Array<Rational>>, q: Array<Rational>, d: Array<Rational>): BigInteger {
        var lcm = BigInteger.valueOf(1)
        for (i in 0 until nrows) {
            val den = RatForRowCol(i, col, M, q, d).den

            lcm = BigIntegerUtils.lcm(lcm, den)

            // TODO
            //if (col == 0 && lcm.GetType() == typeof(DefaultMP))
            //    record_sizeinbase10 = (int)MP.SizeInBase(lcm, 10) /* / 4*/;
        }
        return lcm
    }

    private fun RatForRowCol(row: Int, col: Int, M: Array<Array<Rational>>, q: Array<Rational>, d: Array<Rational>): Rational {
        return if (col == 0)
            d[row]
        else if (col == ncols - 1)
            q[row]
        else
            M[row][col - 1]
    }


    //region Pivot
    /* --------------- pivoting and related routines -------------- */

    /**
     * Pivot tableau on the element  A[row][col] which must be nonzero
     * afterwards tableau normalized with positive determinant
     * and updated tableau variables
     * @param leave (r) VAR defining row of pivot element
     * @param enter (s) VAR defining col of pivot element
     */
    fun pivot(leave: Int, enter: Int) {
        val row = vars.row(leave)
        val col = vars.col(enter)
        vars.swap(enter, leave, row, col)          /* update tableau variables                                     */
        pivotOnRowCol(row, col)
    }

    private fun pivotOnRowCol(row: Int, col: Int) {
        var pivelt = A[row][col]             /* pivelt anyhow later new determinant  */

        if (pivelt.compareTo(BigInteger.ZERO) == 0)
            throw RuntimeException("Trying to pivot on a zero") //TODO

        var negpiv = false
        if (pivelt.compareTo(BigInteger.ZERO) < 0) {
            negpiv = true
            pivelt = pivelt.negate()
        }

        for (i in 0 until nrows) {
            if (i != row)
            /*  A[row][..]  remains unchanged       */ {
                val aicol = A[i][col]       //TODO: better name for this variable
                val nonzero = aicol.compareTo(BigInteger.ZERO) != 0
                for (j in 0 until ncols) {
                    if (j != col) {
                        var tmp1 = A[i][j].multiply(pivelt)
                        if (nonzero) {
                            val tmp2 = aicol.multiply(A[row][j])
                            if (negpiv) {
                                tmp1 = tmp1.add(tmp2)
                            } else {
                                tmp1 = tmp1.subtract(tmp2)
                            }
                        }
                        A[i][j] = tmp1.divide(det!!)           /*  A[i,j] = (A[i,j] A[row,col] - A[i,col] A[row,j])/ det     */
                        //A[i,j] = (A[i,j] * A[row,col] - A[i,col] * A[row,j]) / det;
                    }
                }
                if (nonzero && !negpiv) {
                    A[i][col] = aicol.negate()                 /* row  i  has been dealt with, update  A[i][col]  safely   */
                }
            }
        }

        A[row][col] = det!!
        if (negpiv)
            negRow(row)
        det = pivelt                                   /* by construction always positive      */
    }


    /* negate tableau row.  Used in  pivot()        */
    private fun negRow(row: Int) {
        for (j in 0 until ncols)
            if (A[row][j].compareTo(BigInteger.ZERO) != 0)
            //non-zero
                A[row][j] = A[row][j].negate()            // a = -a
    }


    /* negate tableau column  col   */
    fun negCol(col: Int) {
        for (i in 0 until nrows)
            A[i][col] = A[i][col].negate()                    // a = -a
    }

    //region Results
    fun result(`var`: Int): Rational {
        val rv: Rational
        if (vars.isBasic(`var`))
        /*  var  is a basic variable */ {
            val row = vars.row(`var`)
            var scaleFactor = BigInteger.valueOf(1)
            val scaleIdx = vars.rowVar(row)
            if (scaleIdx >= 0 && scaleIdx < scfa.size - 1)
            /* Z(i):  scfa[i]*rhs[row] / (scfa[RHS]*det)         */ {
                scaleFactor = scfa[scaleIdx]
            }
            //else                                                  /* W(i):  rhs[row] / (scfa[RHS]*det)     */

            val num = scaleFactor.multiply(A[row][RHS()])
            val den = det!!.multiply(scfa[RHS()])
            val birat = Rational(num, den)

            try {
                rv = Rational(birat.num.toLong(), birat.den.toLong())
            } catch (ex: Exception) {
                val error = String.format(
                        "Fraction too large for basic variable %s: %s/%s",
                        vars.toString(`var`), num.toString(), den.toString())
                throw RuntimeException(error, ex) //TODO
                //Console.Out.WriteLine(error);
            }

        } else { // i is non-basic
            rv = ZERO
        }
        return rv
    }


    /**
     * Lex helper to encapsulate BigInteger.
     *
     * @return
     * sign of  A[a,testcol] / A[a,col] - A[b,testcol] / A[b,col]
     * (assumes only positive entries of col are considered)
     */
    fun ratioTest(rowA: Int, rowB: Int, col: Int, testcol: Int): Int {
        return A[rowA][testcol].multiply(A[rowB][col]).compareTo(
                A[rowB][testcol].multiply(A[rowA][col]))
    }


    //region Info

    fun isPositive(row: Int, col: Int): Boolean {
        return A[row][col].compareTo(BigInteger.ZERO) > 0
    }

    fun isZero(row: Int, col: Int): Boolean {
        return A[row][col].compareTo(BigInteger.ZERO) == 0
    }

    fun valToString(row: Int, col: Int): String {
        val value = A[row][col]
        return if (value.compareTo(BigInteger.ZERO) == 0) "." else value.toString()
    }

    fun detToString(): String {
        return det!!.toString()
    }      // what does this even mean when the matrix is not square?

    fun scaleToString(scaleIdx: Int): String {
        return scfa[scaleIdx].toString()
    }


    //only used for tests...
    operator fun get(row: Int, col: Int): Long {
        return A[row][col].toLong()
    }

    operator fun set(row: Int, col: Int, value: Long) {
        A[row][col] = BigInteger.valueOf(value)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(String.format("Determinant: %s", detToString()))

        val colpp = ColumnTextWriter()
        colpp.endRow()
        colpp.writeCol("var")
        colpp.alignLeft()

        /* headers describing variables */
        for (j in 0..RHS()) {
            if (j == RHS()) {
                colpp.writeCol("RHS")
            } else {
                val `var` = vars().toString(vars().colVar(j))
                colpp.writeCol(`var`)
            }
        }
        colpp.endRow()
        colpp.writeCol("scfa")                          /* scale factors                */
        for (j in 0..RHS()) {
            val scfa: String
            if (j == RHS() || vars().isZVar(vars().colVar(j))) {      /* col  j  is some  Z or RHS:  scfa    */
                scfa = scaleToString(j)
            } else {                                                   /* col  j  is some  W           */
                scfa = "1"
            }
            colpp.writeCol(scfa)
        }
        colpp.endRow()
        for (i in 0 until vars().size())
        /* print row  i                 */ {
            val `var` = vars().toString(vars().rowVar(i))
            colpp.writeCol(`var`)
            for (j in 0..RHS()) {
                val value = valToString(i, j)
                colpp.writeCol(value)
            }
            colpp.endRow()
        }
        colpp.endRow()
        sb.append(colpp.toString())
        return sb.toString()
    }
}
