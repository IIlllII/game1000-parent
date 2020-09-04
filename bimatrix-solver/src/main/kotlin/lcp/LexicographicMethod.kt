package lcp

// This class seems specific to the representation of the Tableau?
// See if I can decouple from specifics of Tableau
// See if I can decouple from LCP specifics (read: z0leave)
//
// This class takes an var to enter the basis and determines which
// var should leave.
//
// This needs to be split into two parts, one part belongs in Lemke, the other in Tableau
class LexicographicMethod(basisSize: Int, private val start: LCP) {
    var tested: IntArray? = null        //TODO: make private
    var comparisons: IntArray? = null   //TODO: make private
    private val leavecand: IntArray    /* leavecand [0..numcand-1] = candidates (rows) for leaving var */

    //region Minimum Leaving Variable
    private var z0leave: Boolean = false

    init {
        leavecand = IntArray(basisSize)
        InitStatistics(basisSize + 1)
    }

    /**
     * z0leave
     * ==========================================================
     * @return the state of the z0leave boolean following the
     * last call to minVar().  Indicates that z0 can leave the
     * basis, but the lex-minratio test is performed fully,
     * so the returned value might not be the index of  z0.
     */
    fun z0leave(): Boolean {
        return z0leave
    }

    /**
     * minVar
     * ===========================================================
     * @return the leaving variable in  VARS, given by lexmin row,
     * when  enter  in VARS is entering variable
     * only positive entries of entering column tested
     * boolean  *z0leave  indicates back that  z0  can leave the
     * basis, but the lex-minratio test is performed fully,
     * so the returned value might not be the index of  z0
     */
    @Throws(LemkeAlgorithm.RayTerminationException::class)
    fun lexminratio(A: Tableau, enter: Int): Int {
        z0leave = false

        // TODO: I think I can comment for perf... (but a unit test will fail)
        // I could pass in a cobasic index instead
        A.vars().assertNotBasic(enter)
        val col = A.vars().col(enter)

        var numcand = 0
        for (i in leavecand.indices) {                    /* start with  leavecand = { i | A[i][col] > 0 } */
            if (A.isPositive(i, col)) {
                leavecand[numcand++] = i
            }
        }

        if (numcand == 0) {
            rayTermination(A, enter)
        } /*else if (numcand == 1) {
            RecordStats(0, numcand);
            z0leave = IsLeavingRowZ0(leavecand[0]);
        }*/
        else {
            processCandidates(A, col, leavecand, numcand)
        }

        return A.vars().rowVar(leavecand[0])
    }                                                                   /* end of lexminvar (col, *z0leave); */


    private fun IsLeavingRowZ0(A: Tableau, row: Int): Boolean {
        return A.vars().rowVar(row) === A.vars().z(0)
    }

    /**
     * processCandidates
     * ================================================================
     * as long as there is more than one leaving candidate perform
     * a minimum ratio test for the columns of  j  in RHS, W(1),... W(n)
     * in the tableau.  That test has an easy known result if
     * the test column is basic or equal to the entering variable.
     */
    private fun processCandidates(A: Tableau, enterCol: Int, leavecand: IntArray, numcand: Int) {
        var numcand = numcand
        numcand = ProcessRHS(A, enterCol, leavecand, numcand)
        var j = 1
        while (numcand > 1) {
            if (j >= A.RHS())
            /* impossible, perturbed RHS should have full rank */
                throw RuntimeException("lex-minratio test failed") //TODO
            RecordStats(j, numcand)

            val `var` = A.vars().w(j)
            if (A.vars().isBasic(`var`)) {                                  /* testcol < 0: W(j) basic, Eliminate its row from leavecand */
                val wRow = A.vars().row(`var`)
                numcand = removeRow(wRow, leavecand, numcand) // TODO: revmove r
            } else {                                                                        /* not a basic testcolumn: perform minimum ratio tests */
                val wCol = A.vars().col(`var`) // TODO: get s           				/* since testcol is the  jth  unit column                    */
                if (wCol != enterCol) {                                                /* otherwise nothing will change */
                    numcand = minRatioTest(A, enterCol, wCol, leavecand, numcand)
                }
            }
            j++
        }
    }

    private fun ProcessRHS(A: Tableau, enterCol: Int, leavecand: IntArray, numcand: Int): Int {
        var numcand = numcand
        RecordStats(0, numcand)

        numcand = minRatioTest(A, enterCol, A.RHS(), leavecand, numcand)

        for (i in 0 until numcand) {            // seek  z0  among the first-col leaving candidates
            z0leave = IsLeavingRowZ0(A, leavecand[i])
            if (z0leave) {
                break
            }
            /* alternative, to force z0 leaving the basis:
             * return whichvar[leavecand[i]];
             */
        }
        return numcand
    }

    private fun removeRow(row: Int, candidates: IntArray, numCandidates: Int): Int {
        var numCandidates = numCandidates
        for (i in 0 until numCandidates) {
            if (candidates[i] == row) {
                candidates[i] = candidates[--numCandidates]        /* shuffling of leavecand allowed */
                break
            }
        }
        return numCandidates
    }

    //TODO: should this be package visible?  Better way to test this?
    internal fun minRatioTest(A: Tableau, col: Int, testcol: Int, candidates: IntArray, numCandidates: Int): Int {
        var newnum = 0

        for (i in 1 until numCandidates) {                       /* investigate remaining candidates                  */

            val sgn = A.ratioTest(                                      /* sign of  A[l_0,t] / A[l_0,col] - A[l_i,t] / A[l_i,col]   */
                    candidates[0], candidates[i], col, testcol)            /* note only positive entries of entering column considered */

            if (sgn == 0) {                                             /* new ratio is the same as before                          */
                ++newnum
                candidates[newnum] = candidates[i]
            } else if (sgn == 1) {                                        /* new smaller ratio detected */
                newnum = 0
                candidates[newnum] = candidates[i]
            }
        }
        return newnum + 1                                              /* leavecand[0..newnum]  contains the new candidates */
    }


    //region Statistics

    /*
     * initialize statistics for minimum ratio test
     */
    private fun InitStatistics(sizeBasisPlusZ0: Int) {
        val tstd = IntArray(sizeBasisPlusZ0)
        val comp = IntArray(sizeBasisPlusZ0)
        for (i in 0 until sizeBasisPlusZ0) {
            comp[i] = 0
            tstd[i] = comp[i]
        }

        tested = tstd;
        comparisons = comp;
    }

    private fun RecordStats(idx: Int, numCandidates: Int) {
        val cmp = comparisons;
        val tstd = tested;

        if (tstd != null) tstd[idx] += 1
        if (cmp != null) cmp[idx] += numCandidates
    }


    //region Exception Routines

    @Throws(LemkeAlgorithm.RayTerminationException::class)
    private fun rayTermination(A: Tableau, enter: Int) {
        val error = String.format("Ray termination when trying to enter %s", A.vars().toString(enter))
        throw LemkeAlgorithm.RayTerminationException(error, A, start)
    }
}
