package lcp

import org.slf4j.LoggerFactory

class LemkeAlgorithm {

    //region State
    var pivotCount: Int = 0
        private set /* no. of Lemke pivot iterations, including the first to pivot z0 in    */
    val recordDigits = 0 /* MP digit record */  //MKE: TODO
    var duration: Long = 0
        private set

    private var z0leave: Boolean = false

    //YUK remove this lateinit stuff
    lateinit var A: Tableau
    lateinit var lex: LexicographicMethod // Performs MinRatio Test

    private var origLCP: LCP? = null

    //region Event Callbacks

    //YUK remove this lateinit stuff
    lateinit var leavingVarHandler: LeavingVariableDelegate       //interactivevar OR lexminvar

    var onComplete: OnCompleteDelegate? = null                   //chain: binitabl (secondcall), boutsol, blexstats
    var onInit: OnInitDelegate? = null                           //binitabl (first call)
    var onPivot: OnPivotDelegate? = null                         //bdocupivot
    var onTableauChange: OnTableauChangeDelegate? = null         //bouttabl
    var onLexComplete: OnLexCompleteDelegate? = null             //blexstats
    var onLexRayTermination: OnLexRayTerminationDelegate? = null

    /**
     * LCP result
     * current basic solution turned into  solz [0..n-1]
     * note that Z(1)..Z(n)  become indices  0..n-1
     * gives a warning if conversion to ordinary rational fails
     * and returns 1, otherwise 0
     */
    private val lcpSolution: Array<Rational>
        get() {
            val z = Array(A.vars().size()) { Rational.ZERO}
            for (i in 1..z.size) {
                val `var` = A.vars().z(i)
                z[i - 1] = A.result(`var`)
            }
            return z
        }


    @Throws(InvalidLCPException::class, TrivialSolutionException::class)
    private fun init(lcp: LCP) {
        checkInputs(lcp.q(), lcp.d())
        origLCP = lcp
        A = Tableau(lcp.d().size)
        A.fill(lcp.M(), lcp.q(), lcp.d())
        if (onInit != null)
            onInit!!.onInit("After filltableau", A)
        lex = LexicographicMethod(A.vars().size(), lcp)

        this.leavingVarHandler = object : LeavingVariableDelegate {
            @Throws(RayTerminationException::class)
            override fun getLeavingVar(enter: Int): Int {
                return lex.lexminratio(A, enter)
            }

            override fun canZ0Leave(): Boolean {
                return lex.z0leave()
            }
        }
    }

    /**
     * asserts that  d >= 0  and not  q >= 0  (o/w trivial sol)
     * and that q[i] < 0  implies  d[i] > 0
     */
    @Throws(InvalidLCPException::class, TrivialSolutionException::class)
    fun checkInputs(rhsq: Array<Rational>, vecd: Array<Rational>) {
        var isQPos = true
        var i = 0
        val len = rhsq.size
        while (i < len) {
            if (vecd[i].compareTo(0) < 0) {
                throw InvalidLCPException(String.format("Covering vector  d[%d] = %s negative. Cannot start Lemke.", i + 1, vecd[i].toString()))
            } else if (rhsq[i].compareTo(0) < 0) {
                isQPos = false
                if (vecd[i].isZero) {
                    throw InvalidLCPException(String.format("Covering vector  d[%d] = 0  where  q[%d] = %s  is negative. Cannot start Lemke.", i + 1, i + 1, rhsq[i].toString()))
                }
            }
            ++i
        }
        if (isQPos) {
            throw TrivialSolutionException("No need to start Lemke since  q>=0. Trivial solution  z=0.")
        }
    }

    private fun complementaryPivot(enter: Int, leave: Int): Boolean {
        if (onPivot != null)
            onPivot!!.onPivot(leave, enter, A.vars())

        A.pivot(leave, enter)
        return z0leave
    }

    @Throws(RayTerminationException::class, InvalidLCPException::class, TrivialSolutionException::class)
    @JvmOverloads
    fun run(lcp: LCP, maxcount: Int = 0): Array<Rational> {
        init(lcp)

        z0leave = false
        var enter = A.vars().z(0)                        /* z0 enters the basis to obtain lex-feasible solution      */
        var leave = nextLeavingVar(enter)

        A.negCol(A.RHS())                                /* now give the entering q-col its correct sign             */
        if (onTableauChange != null) {
            onTableauChange!!.onTableauChange("After negcol", A)
        }

        pivotCount = 1
        val before = System.currentTimeMillis()
        while (true) {
            if (complementaryPivot(enter, leave)) {
                break // z0 will have a value of zero but may still be basic... amend?
            }

            if (onTableauChange != null)
                onTableauChange!!.onTableauChange("", A) //TODO: Is there a constant for the empty string?

            // selectpivot
            enter = A.vars().complement(leave)
            leave = nextLeavingVar(enter)

            if (pivotCount++ == maxcount)
            /* maxcount == 0 is equivalent to infinity since pivotcount starts at 1 */ {
                log.warn(String.format("------- stop after %d pivoting steps --------", maxcount))
                break
            }
        }
        duration = System.currentTimeMillis() - before

        if (onComplete != null)
            onComplete!!.onComplete("Final tableau", A) // hook up two tabl output functions to a chain delegate where the flags are analyzzed

        if (onLexComplete != null)
            onLexComplete!!.onLexComplete(lex)

        return lcpSolution               // LCP solution  z  vector
    }

    @Throws(RayTerminationException::class)
    private fun nextLeavingVar(enter: Int): Int {
        try {
            val rv = leavingVarHandler.getLeavingVar(enter)
            z0leave = leavingVarHandler.canZ0Leave()
            return rv
        } catch (ex: RayTerminationException) {
            if (onLexRayTermination != null) {
                onLexRayTermination!!.onLexRayTermination(enter, A, origLCP)
            }
            throw ex
        }

    }

    //delegates
    interface LeavingVariableDelegate {
        @Throws(RayTerminationException::class)
        fun getLeavingVar(enter: Int): Int

        fun canZ0Leave(): Boolean
    }

    interface OnInitDelegate {
        fun onInit(message: String, A: Tableau)
    }

    interface OnCompleteDelegate {
        fun onComplete(message: String, A: Tableau)
    }

    interface OnPivotDelegate {
        fun onPivot(leave: Int, enter: Int, vars: TableauVariables)
    }

    interface OnTableauChangeDelegate {
        fun onTableauChange(message: String, A: Tableau)
    }

    interface OnLexCompleteDelegate {
        fun onLexComplete(lex: LexicographicMethod)
    }

    interface OnLexRayTerminationDelegate {
        fun onLexRayTermination(enter: Int, end: Tableau, start: LCP?)
    }

    open class LemkeException(message: String) : Exception(message)

    class InvalidLCPException(message: String) : LemkeException(message)

    class TrivialSolutionException(message: String) : LemkeException(message)

    class RayTerminationException(error: String, var A: Tableau, var start: LCP?) : LemkeException(
            StringBuilder()
                    .append(error)
                    .append(System.getProperty("line.separator"))
                    .append(start?.toString() ?: "LCP is null")
                    .append(System.getProperty("line.separator"))
                    .append(A.toString())
                    .toString()
    )

    companion object {
        private val log = LoggerFactory.getLogger(LemkeAlgorithm::class.java)
    }
}

/**
 * solve LCP via Lemke's algorithm,
 * solution in  solz [0..lcpdim-1]
 * exit with error if ray termination
 */
