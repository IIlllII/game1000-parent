package lrs

import lcp.BigIntegerUtils.comprod
import lcp.BigIntegerUtils.negative
import lcp.BigIntegerUtils.one
import lcp.BigIntegerUtils.positive
import lcp.BigIntegerUtils.reducearray
import lcp.BigIntegerUtils.zero
import lcp.Rational
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.*


class LrsAlgorithm : Lrs {

    lateinit internal var P: Dictionary

    internal var volume: Rational? = null
    internal var bound: Rational? = null
    //long unbounded;		/* lp unbounded */

    /* initially holds order used to find starting  */
    /* basis, default: m,m-1,...,2,1                */
    //long[] facet;		/* cobasic indices for restart in needed        */
    //long[] temparray;		/* for sorting indices, dimensioned to d        */
    //long[] isave, jsave;	/* arrays for estimator, malloc'ed at start     */


    internal lateinit var sumdet: BigInteger        /* sum of determinants */
    internal lateinit var count: LongArray            /* count[0]=rays [1]=verts. [2]=base [3]=pivots [4]=integer vertices*/
    internal var totalnodes: Long = 0        /* count total number of tree nodes evaluated   */

    /* given by inputd-nredundcol                   */

    //long runs;			/* probes for estimate function                 */
    //long seed = 1234L;			/* seed for random number generator             */
    //double[] cest = new double[10];		/* ests: 0=rays,1=vert,2=bases,3=vol,4=int vert */

    /**** flags  **********                          */
    //boolean allbases;		/* true if all bases should be printed          */
    //boolean bound;                 /* true if upper/lower bound on objective given */
    //boolean dualdeg;		/* true if start dictionary is dual degenerate  */
    //long etrace;		/* turn off debug at basis # strace             */
    //boolean geometric;		/* true if incident vertex prints after each ray */
    internal var getvolume = false        /* do volume calculation                        */
    internal var givenstart = false        /* true if a starting cobasis is given          */

    //boolean lponly;		/* true if only lp solution wanted              */
    //boolean maximize;		/* flag for LP maximization                     */
    //boolean minimize;		/* flag for LP minimization                     */

    internal var maxdepth = MAXD.toLong()        /* max depth to search to in tree              */
    internal var mindepth = (-MAXD).toLong()        /* do not backtrack above mindepth              */
    internal var depth = 0L
    internal var deepest = 0L        /* max depth ever reached in search             */

    //boolean nash;                  /* true for computing nash equilibria           */

    internal var printcobasis = false        /* true if all cobasis should be printed        */
    internal var frequency = 0        /* frequency to print cobasis indices           */

    internal var incidence = true             /* print all tight inequalities (vertices/rays) */
    internal var printslack = false        /* true if indices of slack inequal. printed    */

    //boolean truncate;              /* true: truncate tree when moving from opt vert*/

    //boolean restart;		/* true if restarting from some cobasis         */
    //long strace;		/* turn on  debug at basis # strace             */

    // TODO: change to log levels
    internal var debug = false
    internal var verbose = false

    internal var inequality: IntArray? = null        // indices of inequalities corr. to cobasic ind

    private var redundancies: MutableList<Int>? = null

    /* Variables for saving/restoring cobasis,  db */
    internal var saved_count = LongArray(3)    /* How often to print out current cobasis */
    internal var saved_C: LongArray? = null
    internal var saved_det: BigInteger? = null
    internal var saved_depth: Long = 0
    internal var saved_d: Long = 0

    internal var saved_flag: Long = 0        /* There is something in the saved cobasis */

    /* Variables for cacheing dictionaries, db */
    private var cacheTail: CacheEntry? = null
    private var cacheTries = 0
    private var cacheMisses = 0

    //TODO: fix defaults
    init {
        //for (i = 0; i < 10; i++)
        //  {
        //    Q.count[i] = 0L;
        //    Q.cest[i] = 0.0;
        //  }
        /* initialize flags */
        //Q.allbases = false;
        //Q.bound = false;            /* upper/lower bound on objective function given */
        //Q.debug = false;

        printcobasis = true

        // TODO: set up log levels...
        debug = false
        verbose = true
        if (debug == true) {
            verbose = true
        }
    }

    fun run(`in`: VPolygon): HPolygon {
        // hull == true
        throw RuntimeException("Not Impl")
    }

    override fun run(`in`: HPolygon): VPolygon? {
        // TODO: put all this reset member vars in an initRun() method
        sumdet = BigInteger.ZERO
        totalnodes = 0
        count = LongArray(10)
        count[2] = 1L

        P = Dictionary(`in`, this)
        val arr = IntArray(P.B.size)
        arr[0] = 2
        inequality = arr
        redundancies = ArrayList()

        cacheTail = null
        cacheTries = 0
        cacheMisses = 0

        if (debug) {
            log.trace(P.toString())
            log.trace("exiting lrs_read_dic")
        }

        /** */
        /* Step 2: Find a starting cobasis from default of specified order               */
        /*         P is created to hold  active dictionary data and may be cached        */
        /*         Lin is created if necessary to hold linearity space                   */
        /*         Print linearity space if any, and retrieve output from first dict.    */
        /** */
        if (!getfirstbasis(P, `in`.linearities, 0)) {
            return null
        }

        /* Pivot to a starting dictionary                      */
        /* There may have been column redundancy               */
        /* If so the linearity space is obtained and redundant */
        /* columns are removed. User can access linearity space */
        /* from lrs_mp_matrix Lin dimensions nredundcol x d+1  */

        //int startcol = 0;
        //if (Q->homogeneous && Q->hull)
        //   startcol++;			/* col zero not treated as redundant   */

        //for (int col = startcol; col < Q->nredundcol; col++) {	/* print linearity space               */
        //   lrs_printoutput (Q, Lin[col]);	/* Array Lin[][] holds the coeffs.     */
        //}

        /** */
        /* Step 3: Terminate if lponly option set, otherwise initiate a reverse          */
        /*         search from the starting dictionary. Get output for each new dict.    */
        /** */

        /* We initiate reverse search from this dictionary       */
        /* getting new dictionaries until the search is complete */
        /* User can access each output line from output which is */
        /* vertex/ray/facet from the lrs_mp_vector output         */
        /* prune is TRUE if tree should be pruned at current node */

        val output = Array<BigInteger>(`in`.numCols) { BigInteger.ZERO}    /* output holds one line of output from dictionary     */
        val solution = VPolygon()
        do {
            //if (!lrs.checkbound(P)) {
            if (getvertex(P, output, solution)) { // check for lexmin vertex
                printoutput(output, solution)
            }
            // since I am not iterating by column, I think it messes up the order
            // get cobasis from Dictionary sorted by lowest col for parity
            val cobasis = P.cobasis()
            for (i in cobasis.indices) {
                if (getsolution(P, output, solution, cobasis[i])) {
                    printoutput(output, solution)
                }
            }
            //}
        } while (getnextbasis(P/*, prune*/))

        printsummary(`in`)

        return solution
    }

    private fun printsummary(`in`: HPolygon) {
        log.info("end")
        /*if (dualdeg)
		{
			System.out.println("*Warning: Starting dictionary is dual degenerate");
			System.out.println("*Complete enumeration may not have been produced");
			if (maximize) {
				System.out.println("*Recommendation: Add dualperturb option before maximize in input file");
			} else {
				System.out.println("*Recommendation: Add dualperturb option before minimize in input file");
			}
		}*/

        /*if (unbounded)
		{
			System.out.println("*Warning: Starting dictionary contains rays");
			System.out.println("*Complete enumeration may not have been produced");
			if (maximize) {
				System.out.println("*Recommendation: Change or remove maximize option or add bounds");
			} else {
				System.out.println("*Recommendation: Change or remove minimize option or add bounds");
			}
		}*/

        /*if (truncate) {
			System.out.println("*Tree truncated at each new vertex");
		}*/
        if (maxdepth < MAXD) {
            log.info(String.format("*Tree truncated at depth %d", maxdepth))
        }
        /*if (maxoutput > 0) {
			System.out.println(String.format("*Maximum number of output lines = %ld", maxoutput));
		}*/


        log.info("*Sum of det(B)=$sumdet")

        /* next block with volume rescaling must come before estimates are printed */

        /*if (getvolume)
		{
			volume = rescalevolume (P, Q);

			if (polytope) {
				System.out.println("*Volume=" + volume.toString());
			} else {
				System.out.println("*Pseudovolume=" + volume.toString());
			}
		}*/

        /*if (hull)
		{
			fprintf (lrs_ofp, "\n*Totals: facets=%ld bases=%ld", count[0], count[2]);

			if (nredundcol > homogeneous)	// don't count column 1 as redundant if homogeneous
			{
				fprintf (lrs_ofp, " linearities=%ld", nredundcol - homogeneous);
				fprintf (lrs_ofp, " facets+linearities=%ld",nredundcol-homogeneous+count[0]);
			}


			if ((cest[2] > 0) || (cest[0] > 0))
			{
				fprintf (lrs_ofp, "\n*Estimates: facets=%.0f bases=%.0f", count[0] + cest[0], count[2] + cest[2]);
				if (getvolume)
				{
					rattodouble (Q->Nvolume, Q->Dvolume, &x);
					for (i = 2; i < d; i++)
						cest[3] = cest[3] / i;	//adjust for dimension
					fprintf (lrs_ofp, " volume=%g", cest[3] + x);
				}

				fprintf (lrs_ofp, "\n*Total number of tree nodes evaluated: %ld", Q->totalnodes);
				fprintf (lrs_ofp, "\n*Estimated total running time=%.1f secs ",(count[2]+cest[2])/Q->totalnodes*get_time () );

			}

		} else */        /* output things specific to vertex/ray computation */
        run {
            val sb = StringBuilder()
            sb.append(String.format("*Totals: vertices=%d rays=%d bases=%d integer_vertices=%d ", count[1], count[0], count[2], count[4]))

            if (redundancies!!.size > 0) {
                sb.append(String.format(" linearities=%d", redundancies!!.size))
            }
            if (count[0] + redundancies!!.size > 0) {
                sb.append(" vertices+rays")
                if (redundancies!!.size > 0) {
                    sb.append("+linearities")
                }
                sb.append(String.format("=%d", redundancies!!.size.toLong() + count[0] + count[1]))
            }
            log.info(sb.toString())

            /*if ((cest[2] > 0) || (cest[0] > 0))
            {
                fprintf (lrs_ofp, "\n*Estimates: vertices=%.0f rays=%.0f", count[1]+cest[1], count[0]+cest[0]);
                fprintf (lrs_ofp, " bases=%.0f integer_vertices=%.0f ",count[2]+cest[2], count[4]+cest[4]);

                if (getvolume)
                {
                    rattodouble (Nvolume, Dvolume, &x);
                    for (i = 2; i <= d - homogeneous; i++) {
                        cest[3] = cest[3] / i;	// adjust for dimension
                    }
                    fprintf (lrs_ofp, " pseudovolume=%g", cest[3] + x);
                }
                System.out.println();
                System.out.println(String.format("*Total number of tree nodes evaluated: %d", totalnodes));
                System.out.print(String.format("*Estimated total running time=%.1f secs ",(count[2]+cest[2])/totalnodes*get_time()));
            }*/

            /*if (restart || allbases) {       // print warning
                System.out.println("*Note! Duplicate vertices/rays may be present");
            } else if ((count[0] > 1 && !homogeneous)) {
                System.out.println("*Note! Duplicate rays may be present");
            }*/
        }


        if (!verbose) {
            return
        }


        log.info(String.format("*Input size m=%d rows n=%d columns working dimension=%d", P.m, `in`.numCols, P.d))
        /*if (hull) {
			  System.out.println(String.format(" working dimension=%d", d - 1 + homogeneous));
		  } else {
			  System.out.println(String.format(" working dimension=%d", P.d));
		  }		*/

        val scob = StringBuilder()
        scob.append("*Starting cobasis defined by input rows")
        val temparray = arrayOfNulls<Int>(P.lastdv)
        for (i in 0 until `in`.linearities.size) {
            temparray[i] = `in`.linearities[i]
        }
        for (i in `in`.linearities.size until P.lastdv) {
            temparray[i] = inequality!![P.C[i - `in`.linearities.size] - P.lastdv]
        }
        for (i in 0 until P.lastdv) {
            reorder(temparray.requireNoNulls())
        }
        for (i in 0 until P.lastdv) {
            scob.append(String.format(" %d", temparray[i]))
        }
        log.info(scob.toString())
        log.info(String.format("*Dictionary Cache: max size= %d misses= %d/%d   Tree Depth= %d", 0/*dict_count*/, cacheMisses, cacheTries, deepest))
    }

    private fun printoutput(output: Array<BigInteger>, solution: VPolygon) {
        val sb = StringBuilder()
        val vertex = arrayOfNulls<Rational>(output.size)
        if (zero(output[0]))
        /*non vertex */ {
            for (i in output.indices) {
                val ratStr = output[i].toString()
                vertex[i] = Rational.valueOf(ratStr)
                sb.append(" $ratStr ")
            }
        } else {                /* vertex   */
            sb.append(" 1 ")
            vertex[0] = Rational.ONE
            for (i in 1 until output.size) {
                vertex[i] = Rational(output[i], output[0])
                sb.append(" " + vertex[i].toString() + " ")
            }
        }
        log.info(sb.toString())
        solution.vertices.add(vertex.requireNoNulls())
    }

    /*
	public void lrs(Tableau first)
	{
		List<Object> vertices = new ArrayList<Object>();
		Tableau A = first;
		int d = A.vars().

		int j = 1;
		while (true)
		{
			while (j <= d)
			{
				int v = N(j); // jth index of the cobasic variables?
				int u = reverse(B, v); //
				if (u >= 0)
				{
					pivot(B, u, v); // new basis found //
					if (lexmin(B, 0)) {
						output current vertex;
					}
					j = 1;
				}
				else j = j + 1;
			}
			selectpivot(B, r, j); //backtrack //
			pivot(B, r, N(j));
			j = j + 1;
			if (j > d && B == Bstar) break;
		}
	}
	*/

    //rv[0] = r (leaving row)
    //rv[1] = s (entering column)
    /* select pivot indices using lexicographic rule   */
    /* returns TRUE if pivot found else FALSE          */
    /* pivot variables are B[*r] C[*s] in locations Row[*r] Col[*s] */
    fun selectpivot(P: Dictionary): IntArray {
        val out = IntArray(2)
        out[0] = 0
        var r = out[0]
        out[1] = P.d
        var s = out[1]

        /*find positive cost coef */
        var j = 0
        while (j < P.d && !positive(P.cost(j))) {
            ++j
        }

        if (j < P.d) {

            /* pivot column found! */
            s = j

            /*find min index ratio */
            r = P.ratio(s, debug)
            if (r != 0) {
                out[0] = r
                out[1] = s
                //return true;		/* unbounded */
            }
        }
        return out
        //return false;
    }

    /**
     * find reverse indices
     * true if B[r] C[s] is a reverse lexicographic pivot
     * is true and returns u = B(i) if and only if
     * (i) w(0)v < 0,
     * (ii) u = B(i) = lexminratio(B, v) != 0, and
     * (iii) setting w = w(0) - a(0)w(i) /a(i), we have w(j) >= 0, for all j in N, j < u
     * Returns false (-1) otherwise... TODO: can I return 0?
     */
    fun reverse(P: Dictionary, s: Int): Int {
        var r = -1 // return value (-1 if false)

        val enter = P.C[s]
        val col = P.cols[s]

        log.trace(String.format("+reverse: col index %d C %d Col %d ", s, enter, col))

        if (!negative(P.cost(s))) {
            log.trace(" Pos/Zero Cost Coeff")
            return -1
        }

        r = P.ratio(s, debug)
        if (r == 0)
        /* we have a ray */ {
            log.trace(" Pivot col non-negative:  ray found")
            return -1
        }

        //int row = P.rows[r];

        /* check cost row after "pivot" for smaller leaving index    */
        /* ie. j s.t.  A[0][j]*A[row][col] < A[0][col]*A[row][j]     */
        /* note both A[row][col] and A[0][col] are negative          */

        var i = 0
        while (i < P.d && P.C[i] < P.B[r]) {
            if (i != s) {
                val j = P.cols[i]
                if (positive(P.cost(i)) || negative(P.get(r, i)))
                /*or else sign test fails trivially */ {
                    if (!negative(P.cost(i)) && !positive(P.get(r, i)) || comprod(P.cost(i), P.get(r, s), P.cost(s), P.get(r, i)) === -1) {            /*+ve cost found */
                        log.trace(String.format("Positive cost found: index %d C %d Col %d", i, P.C[i], j))
                        return -1
                    }
                }
            }
            i++
        }
        log.trace(String.format("+end of reverse : indices r %d s %d ", r, s))
        return r
    }

    /* gets first basis, false if none              */
    /* P may get changed if lin. space Lin found    */
    /* no_output is true supresses output headers   */
    private fun getfirstbasis(D: Dictionary, linearity: IntArray, hull: Int): Boolean {
        if (linearity.size > 0 && D.isNonNegative) {
            log.warn("*linearity and nonnegative options incompatible - all linearities are skipped")
            log.warn("*add nonnegative constraints explicitly and remove nonnegative option")
        }


        /* default is to look for starting cobasis using linearies first, then     */
        /* filling in from last rows of input as necessary                         */
        /* linearity array is assumed sorted here                                  */
        /* note if restart/given start inequality indices already in place         */
        /* from nlinearity..d-1   													*/
        for (i in linearity.indices) {    /* put linearities first in the order */
            inequality!![i] = linearity[i]
        }

        var k = if (givenstart) P.d else linearity.size            /* index for linearity array   */

        for (i in D.m downTo 1) {
            var j = 0
            while (j < k && inequality!![j] != i) {
                ++j            /* see if i is in inequality  */
            }
            if (j == k) {
                inequality!![k++] = i
            }
        }

        if (log.isTraceEnabled) {
            val sb = StringBuilder()
            sb.append("*Starting cobasis uses input row order")
            for (i in 0 until D.m) {
                sb.append(String.format(" %d", inequality!![i]))
            }
            log.trace(sb.toString())
        }

        /* for voronoi convert to h-description using the transform                  */
        /* a_0 .. a_d-1 . (a_0^2 + ... a_d-1 ^2)-2a_0x_0-...-2a_d-1x_d-1 + x_d >= 0 */
        /* note constant term is stored in column d, and column d-1 is all ones      */
        /* the other coefficients are multiplied by -2 and shifted one to the right  */
        if (log.isTraceEnabled) {
            log.trace(D.toString())
        }

        for (j in 0..D.d) {
            D.A[0][j] = BigInteger.ZERO // TODO: why is this being assigned here?
        }

        /* Now we pivot to standard form, and then find a primal feasible basis       */
        /* Note these steps MUST be done, even if restarting, in order to get         */
        /* the same index/inequality correspondance we had for the original prob.     */
        /* The inequality array is used to give the insertion order                   */
        /* and is defaulted to the last d rows when givenstart=false                  */

        if (P.isNonNegative) {
            /* no need for initial pivots here, labelling already done */
            P.lastdv = D.d
        } else if (!getabasis(D, inequality!!, linearity, hull)) {
            return false
        }

        /** */
        /* now we start printing the output file  unless no output requested */
        /** */
        log.info("V-representation")
        log.info("begin")
        log.info(String.format("***** %d rational", D.d + 1))


        /* Reset up the inequality array to remember which index is which input inequality */
        /* inequality[B[i]-lastdv] is row number of the inequality with index B[i]              */
        /* inequality[C[i]-lastdv] is row number of the inequality with index C[i]              */

        for (i in 1..D.m) {
            inequality!![i] = i
        }

        if (linearity.size > 0) {                                /* some cobasic indices will be removed */
            for (i in linearity.indices) {        /* remove input linearity indices */
                inequality!![linearity[i]] = 0
            }
            k = 1                                                /* counter for linearities         */
            for (i in 1..P.m - linearity.size) {
                while (k <= P.m && inequality!![k] == 0) {
                    k++                                        /* skip zeroes in corr. to linearity */
                }
                inequality!![i] = inequality!![k++]
            }
        }

        if (log.isTraceEnabled) {
            val sb = StringBuilder()
            sb.append("inequality array initialization:")
            for (i in 1..D.m - linearity.size) {
                sb.append(String.format(" %d", inequality!![i]))
            }
            log.trace(sb.toString())
        }


        /* Do dual pivots to get primal feasibility */
        if (!primalfeasible(D)) {
            log.warn("No feasible solution")
            return false
        }


        /* re-initialize cost row to -det */
        for (j in 1..D.d) {
            D.A[0][j] = D.det()!!.negate()
        }
        D.A[0][0] = BigInteger.ZERO    /* zero optimum objective value */


        /* reindex basis to 0..m if necessary */
        /* we use the fact that cobases are sorted by index value */
        if (debug) {
            log.trace(D.toString())
        }

        while (D.C[0] <= D.m) {
            val i = D.C[0]
            val j = inequality!![D.B[i] - D.lastdv]
            inequality!![D.B[i] - D.lastdv] = inequality!![D.C[0] - D.lastdv]
            inequality!![D.C[0] - D.lastdv] = j
            D.C[0] = D.B[i]
            D.B[i] = i
            reorder(D.C, D.cols, 0, D.d)
        }

        if (log.isTraceEnabled) {
            val sb = StringBuilder()
            sb.append(String.format("*Inequality numbers for indices %d .. %d : ", D.lastdv + 1, D.m + D.d))
            for (i in 1..D.m) {
                sb.append(String.format(" %d ", inequality!![i]))
            }
            log.trace(sb.toString())
        }

        if (debug) {
            log.trace(D.toString())
        }

        return true
    }

    /** */
    /* getnextbasis in reverse search order  */
    /** */

    /* gets next reverse search tree basis, FALSE if none  */
    /* switches to estimator if maxdepth set               */
    /* backtrack TRUE means backtrack from here            */
    private fun getnextbasis(D: Dictionary/*, boolean backtrack*/): Boolean {
        var i = 0
        var j = 0

        var backtrack = false //TODO
        if (backtrack && depth == 0L) {
            return false                       /* cannot backtrack from root      */
        }

        //if (maxoutput > 0 && count[0]+Qcount[1] >= maxoutput)
        //   return FALSE;                      /* output limit reached            */

        while (j < D.d || D.B[D.m] !== D.m)
        /*main while loop for getnextbasis */ {
            if (depth >= maxdepth) {
                backtrack = true
                if (maxdepth == 0L)
                /* estimate only */
                    return false    /* no nextbasis  */
            }

            //if ( Q->truncate && negative(D->A[0][0]))   /* truncate when moving from opt. vertex */
            //     backtrack = TRUE;

            if (backtrack)
            /* go back to prev. dictionary, restore i,j */ {
                backtrack = false

                if (cacheTail != null) {
                    val entry = popCache()
                    D.copy(entry!!.dict)
                    i = entry.bas
                    j = entry.cob
                    depth = entry.depth
                    log.trace(String.format(" Cached Dict. restored to depth %d", depth))
                } else {
                    --depth
                    var vars = selectpivot(D)
                    vars = doPivot(D, vars[0], vars[1])
                    i = vars[0]
                    j = vars[1]
                }

                if (debug) {
                    log.trace(String.format(" Backtrack Pivot: indices i=%d j=%d depth=%d", i, j, depth))
                    log.trace(D.toString())
                }

                j++            /* go to next column */
            }            /* end of if backtrack  */

            if (depth < mindepth) {
                break
            }

            /* try to go down tree */
            while (j < D.d) {
                i = reverse(D, j)
                if (i >= 0) {
                    break
                }
                j++
            }

            if (j == D.d) {
                backtrack = true
            } else { /*reverse pivot found */

                pushCache(D, i, j, depth)

                ++depth
                if (depth > deepest) {
                    deepest = depth
                }

                doPivot(D, i, j)

                count[2]++
                totalnodes++

                //save_basis (D); TODO
                //if (strace == count[2])
                //	debug = true;
                //if (etrace == count[2])
                //	debug = false;
                return true        /*return new dictionary */
            }
        }
        return false            /* done, no more bases */
    }


    private fun doPivot(D: Dictionary, r: Int, s: Int): IntArray {
        if (log.isTraceEnabled()) {
            log.trace(String.format(" pivot  B[%d]=%d  C[%d]=%d ", r, D.B[r], s, D.C[s]))
            log.trace(D.toString())
        }

        count[3]++    /* count the pivot */
        D.pivot(r, s, debug)

        if (log.isTraceEnabled()) {
            log.trace(String.format(" depth=%d det=%s", depth, D.det().toString()))
        }

        val vars = intArrayOf(r, s)
        update(D, vars)    /*Update B,C,i,j */
        return vars
    }


    /* Do dual pivots to get primal feasibility */
    /* Note that cost row is all zero, so no ratio test needed for Dual Bland's rule */
    private fun primalfeasible(P: Dictionary): Boolean {
        var primalinfeasible = true
        val m = P.m
        val d = P.d

        /*temporary: try to get new start after linearity */

        while (primalinfeasible) {
            var i = P.lastdv + 1
            while (i <= m && !negative(P.b(i))) {
                ++i
            }
            if (i <= m) {
                var j = 0        /*find a positive entry for in row */
                while (j < d && !positive(P.get(i, j))) {
                    ++j
                }
                if (j >= d) {
                    return false    /* no positive entry */
                }
                val vars = doPivot(P, i, j)
                i = vars[0]
                j = vars[1]
            } else {
                primalinfeasible = false
            }
        }
        return true
    }

    /* check if column indexed by col in this dictionary */
    /* contains output                                   */
    /* col=0 for vertex 1....d for ray/facet             */
    private fun getsolution(P: Dictionary, output: Array<BigInteger>, solution: VPolygon, s: Int): Boolean {
        val col = P.cols[s]
        // we know col != 0 from here down...
        if (!negative(P.cost(s))) {        // check for rays: negative in row 0 , positive if lponly
            return false
        }

        /*  and non-negative for all basic non decision variables */
        var j = P.lastdv + 1 /* cobasic index     */
        while (j <= P.m && !negative(P.get(j, s))) {
            ++j
        }

        if (j <= P.m) {
            return false
        }

        if (P.lexmin(s)) {
            if (debug) {
                log.trace(String.format(" lexmin ray in col=%d ", col))
                log.trace(P.toString())
            }
            return getray(P, col, output, solution)
        }

        return false            /* no more output in this dictionary */
    }

    /*Print out solution in col and return it in output   */
    /*redcol =n for ray/facet 0..n-1 for linearity column */
    /*hull=1 implies facets will be recovered             */
    /* return FALSE if no output generated in column col  */
    private fun getray(P: Dictionary, col: Int, output: Array<BigInteger>, solution: VPolygon): Boolean {
        if (debug) {
            log.trace(P.toString())
        }

        ++count[0]
        if (printcobasis) {
            printcobasis(P, solution, col)
        }

        var i = 1
        for (j in output.indices)
        /* print solution */ {
            if (j == 0) {    /* must have a ray, set first column to zero */
                output[0] = BigInteger.ZERO
            } else {
                output[j] = getnextoutput(P, i, col)
                i++
            }
        }
        reducearray(output)
        /* printslack for rays: 2006.10.10 */
        /* printslack inequality indices  */
        /*
	   if (printslack)
	    {
	       fprintf(lrs_ofp,"\nslack ineq:");
	       for(int i = lastdv + 1; i <= P.m; i++)
	         {
	           if (!zero(P.A[P.Row[i]][col]))
	                 fprintf(lrs_ofp," %d ", inequality[P.B[i] - lastdv]);
	         }
	    }
		 */
        return true
    }

    /* get A[B[i]][col] and return to out */
    private fun getnextoutput(P: Dictionary, i: Int, col: Int): BigInteger {
        if (P.isNonNegative)
        /* if m+i basic get correct value from dictionary          */
        /* the slack for the inequality m-d+i contains decision    */
        /* variable x_i. We first see if this is in the basis      */
        /* otherwise the value of x_i is zero, except for a ray    */
        /* when it is one (det/det) for the actual column it is in */ {
            for (j in P.lastdv + 1..P.m) {
                if (inequality!![P.B[j] - P.lastdv] == P.m - P.d + i) {
                    return P.A[P.rows[j]][col]
                }
            }
            /* did not find inequality m-d+i in basis */
            return if (i == col) {
                P.det()!!
            } else {
                BigInteger.ZERO
            }
        } else {
            return P.A[P.rows[i]][col]
        }
    }

    /*Print out current vertex if it is lexmin and return it in output */
    /* return FALSE if no output generated  */
    private fun getvertex(P: Dictionary, output: Array<BigInteger>, solution: VPolygon): Boolean {
        if (P.lexflag()) {
            ++count[1]
        }

        if (debug) {
            log.trace(P.toString())
        }

        sumdet = sumdet.add(P.det())

        /*print cobasis if printcobasis=TRUE and count[2] a multiple of frequency */
        /* or for lexmin basis, except origin for hull computation - ugly!        */
        if (printcobasis) {
            if (P.lexflag() || frequency > 0 && count[2] == count[2] / frequency * frequency) {
                printcobasis(P, solution, 0)
            }
        }

        if (!P.lexflag()) {    /* not lexmin, and not printing forced */
            return false
        }

        /* copy column 0 to output */
        output[0] = P.det()!!

        /* extract solution */
        var j = 1
        var i = 1
        while (j < output.size) {
            output[j] = getnextoutput(P, i, 0)
            ++j
            ++i
        }

        reducearray(output)
        if (one(output[0])) {
            ++count[4]               /* integer vertex */
        }

        /* uncomment to print nonzero basic variables

	 printf("\n nonzero basis: vars");
	  for(i=1;i<=lastdv; i++)
	   {
	    if ( !zero(A[Row[i]][0]) )
	         printf(" %d ",B[i]);
	   }
		 */

        /* printslack inequality indices  */

        /*if (Q->printslack)
	    {
	       fprintf(lrs_ofp,"\nslack ineq:");
	       for(i=lastdv+1;i<=P->m; i++)
	         {
	           if (!zero(A[Row[i]][0]))
	                 fprintf(lrs_ofp," %d ", Q->inequality[B[i]-lastdv]);
	         }
	    }*/

        return true
    }

    /* col is output column being printed */
    private fun printcobasis(P: Dictionary, solution: VPolygon, col: Int) {
        val sb = StringBuilder()
        sb.append(String.format("V#%d R#%d B#%d h=%d facets ", count[1], count[0], count[2], depth))

        var rflag = -1                /* used to find inequality number for ray column */
        val cobasis = arrayOfNulls<Int>(P.d) //TODO: should I make this a list? perf?
        for (i in cobasis.indices) {
            cobasis[i] = inequality!![P.C[i] - P.lastdv]
            if (P.cols[i] == col) { //can make if (i == s)
                rflag = cobasis[i]!!    /* look for ray index */
            }
        }
        for (i in cobasis.indices) {
            reorder(cobasis.requireNoNulls())
        }
        for (i in cobasis.indices) {
            sb.append(String.format(" %d", cobasis[i]))

            // perhaps I need to have a special name for the result column
            if (col != 0 && rflag == cobasis[i]) {
                sb.append("*") // missing cobasis element for ray
            }
        }

        /* get and print incidence information */
        var nincidence: Long       /* count number of tight inequalities */
        if (col == 0) {
            nincidence = P.d.toLong()
        } else {
            nincidence = (P.d - 1).toLong()
        }

        var firstime = true
        val incidenceList = ArrayList<Int>()
        Collections.addAll<Int>(incidenceList, *cobasis)
        for (i in P.lastdv + 1..P.m) {
            if (zero(P.b(i))) {
                if (col == 0 || zero(P.A[P.rows[i]][col])) {
                    ++nincidence
                    if (incidence) {
                        if (firstime) {
                            sb.append(" :")
                            firstime = false
                        }
                        sb.append(String.format(" %d", inequality!![P.B[i] - P.lastdv]))
                        incidenceList.add(inequality!![P.B[i] - P.lastdv])
                    }
                }
            }
        }
        sb.append(String.format(" I#%d", nincidence))

        sb.append(String.format(" det=%s", P.det()))
        val vol = rescaledet(P)    /* scales determinant in case input rational */
        sb.append(String.format(" in_det=%s", vol.toString()))

        log.info(sb.toString())
        if (P.lexflag()) {
            solution.cobasis.add(incidenceList.toTypedArray())
        }
    }

    /* rescale determinant to get its volume */
    /* Vnum/Vden is volume of current basis  */
    private fun rescaledet(P: Dictionary): Rational {
        var gcdprod = BigInteger.ONE
        var vden = BigInteger.ONE
        for (i in 0 until P.d) {
            if (P.B[i] <= P.m) {
                gcdprod = gcdprod.multiply(P.gcd(inequality!![P.C[i] - P.lastdv]))
                vden = vden.multiply(P.lcm(inequality!![P.C[i] - P.lastdv]))
            }
        }
        val vnum = P.det()!!.multiply(gcdprod)
        return Rational(vnum, vden)
    }

    private fun popCache(): CacheEntry? {
        ++cacheTries

        var rv: CacheEntry? = null
        if (cacheTail == null) {
            ++cacheMisses
        } else {
            rv = cacheTail
            cacheTail = cacheTail!!.prev
        }
        return rv
    }

    private fun pushCache(d: Dictionary, i: Int, j: Int, depth: Long) {
        val copy = Dictionary(d)
        log.trace(String.format("Saving dict at depth %d", depth))

        val entry = CacheEntry(copy, i, j, depth)
        entry.prev = cacheTail
        cacheTail = entry
    }

    /* Pivot Ax<=b to standard form */
    /*Try to find a starting basis by pivoting in the variables x[1]..x[d]        */
    /*If there are any input linearities, these appear first in order[]           */
    /* Steps: (a) Try to pivot out basic variables using order                    */
    /*            Stop if some linearity cannot be made to leave basis            */
    /*        (b) Permanently remove the cobasic indices of linearities           */
    /*        (c) If some decision variable cobasic, it is a linearity,           */
    /*            and will be removed.                                            */
    internal fun getabasis(P: Dictionary, order: IntArray, linearitiesIn: IntArray, hull: Int): Boolean {
        val nredundcol = 0L        /* will be calculated here */

        val linearities = ArrayList<Int>()
        for (i in linearitiesIn.indices) {
            linearities.add(linearitiesIn[i])
        }

        if (log.isTraceEnabled) {
            val sb = StringBuilder()
            sb.append("getabasis from inequalities given in order")
            for (i in 0 until P.m) {
                sb.append(String.format(" %d", order[i]))
            }
            log.trace(sb.toString())
        }

        for (j in 0 until P.m) {
            var i = 0
            while (i <= P.m && P.B[i] != P.d + order[j]) {
                i++            /* find leaving basis index i */
            }

            if (j < linearities.size && i > P.m)
            /* cannot pivot linearity to cobasis */ {
                if (debug) {
                    log.trace(P.toString())
                }
                log.warn("Cannot find linearity in the basis")
                return false
            }

            if (i <= P.m) {            /* try to do a pivot */
                var k = 0
                while (P.C[k] <= P.d && zero(P.get(i, k))) {
                    ++k
                }

                if (P.C[k] <= P.d) {
                    val rs = doPivot(P, i, k)
                    i = rs[0]
                    k = rs[1]
                } else if (j < linearities.size) {            /* cannot pivot linearity to cobasis */
                    if (zero(P.b(i))) {
                        log.warn(String.format("*Input linearity in row %d is redundant--converted to inequality", order[j]))
                        linearities[j] = 0
                    } else {
                        if (debug) {
                            log.trace(P.toString())
                        }
                        log.warn(String.format("*Input linearity in row %d is inconsistent with earlier linearities", order[j]))
                        log.warn("*No feasible solution")
                        return false
                    }
                }
            }
        }

        /* update linearity array to get rid of redundancies */
        var k = 0            /* counters for linearities         */
        while (k < linearities.size) {
            if (linearities[k] == 0) {
                linearities.removeAt(k)
            } else {
                ++k
            }
        }

        /* column dependencies now can be recorded  */
        /* redundcol contains input column number 0..n-1 where redundancy is */
        k = 0
        while (k < P.d && P.C[k] <= P.d) {
            if (P.C[k] <= P.d) {        /* decision variable still in cobasis */
                redundancies!!.add(P.C[k] - hull)    /* adjust for hull indices */
                ++k
            }
        }

        /* now we know how many decision variables remain in problem */
        P.lastdv = P.d - redundancies!!.size



        if (log.isTraceEnabled) {
            log.trace(String.format("end of first phase of getabasis: lastdv=%d nredundcol=%d", P.lastdv, redundancies!!.size))

            val sb = StringBuilder()
            sb.append("redundant cobases:")
            for (i in 0 until nredundcol) {
                sb.append(redundancies!!.get(i.toInt()))
            }
            log.trace(sb.toString())

            log.trace(P.toString()) // TODO: finer?
        }

        /* Remove linearities from cobasis for rest of computation */
        /* This is done in order so indexing is not screwed up */

        for (i in linearities.indices) {                /* find cobasic index */
            k = 0
            while (k < P.d && P.C[k] != linearities[i] + P.d) {
                ++k
            }
            if (k >= P.d) {
                log.warn("Error removing linearity")
                return false
            }
            if (!removecobasicindex(P, k)) {
                return false
            }
            // TODO: no need if we use P.d all the time to reset d = P.d;
        }
        if (debug && linearities.size > 0) {
            log.trace(P.toString())
        }
        /* set index value for first slack variable */

        /* Check feasability */
        if (givenstart) {
            var i = P.lastdv + 1
            while (i <= P.m && !negative(P.A[P.rows[i]][0])) {
                ++i
            }
            if (i <= P.m) {
                log.warn("*Infeasible startingcobasis - will be modified")
            }
        }
        return true
    }

    /* remove the variable C[k] from the problem */
    /* used after detecting column dependency    */
    private fun removecobasicindex(P: Dictionary, k: Int): Boolean {
        log.trace(String.format("removing cobasic index k=%d C[k]=%d", k, P.C[k]))
        val cindex = P.C[k]        /* cobasic index to remove              */
        val deloc = P.cols[k]        /* matrix column location to remove     */

        for (i in 1..P.m) {/* reduce basic indices by 1 after index */
            if (P.B[i] > cindex) {
                P.B[i]--
            }
        }

        for (j in k until P.d)
        /* move down other cobasic variables    */ {
            P.C[j] = P.C[j + 1] - 1    /* cobasic index reduced by 1           */
            P.cols[j] = P.cols[j + 1]
        }

        if (deloc != P.d) {
            /* copy col d to deloc */
            for (i in 0..P.m) {
                P.A[i][deloc] = P.A[i][P.d]
            }

            /* reassign location for moved column */
            var j = 0
            while (P.cols[j] != P.d) {
                j++
            }

            P.cols[j] = deloc
        }

        P.d--
        if (debug) {
            log.trace(P.toString())
        }
        return true
    }

    private class CacheEntry(val dict: Dictionary, val bas: Int, val cob: Int, val depth: Long) {

        var prev: CacheEntry? = null
    }

    companion object {
        private val log = LoggerFactory.getLogger(LrsAlgorithm::class.java)
        private val MAXD = 27 //TODO


        /*reorder array a in increasing order with one misplaced element at index newone */
        /*elements of array b are updated to stay aligned with a */
        private fun reorder(a: IntArray, b: IntArray, newone: Int, range: Int) {
            var newone = newone
            while (newone > 0 && a[newone] < a[newone - 1]) {
                var temp = a[newone]
                a[newone] = a[newone - 1]
                a[newone - 1] = temp
                temp = b[newone]
                b[newone] = b[newone - 1]
                b[--newone] = temp
            }

            while (newone < range - 1 && a[newone] > a[newone + 1]) {
                var temp = a[newone]
                a[newone] = a[newone + 1]
                a[newone + 1] = temp
                temp = b[newone]
                b[newone] = b[newone + 1]
                b[++newone] = temp
            }
        }


        /*update the B,C arrays after a pivot */
        /*   involving B[bas] and C[cob]           */
        private fun update(dict: Dictionary, vars: IntArray) {
            var i = vars[0]
            var j = vars[1]

            val leave = dict.B[i]
            val enter = dict.C[j]
            dict.B[i] = enter
            reorder(dict.B, dict.rows, i, dict.m + 1)
            dict.C[j] = leave
            reorder(dict.C, dict.cols, j, dict.d)

            /* restore i and j to new positions in basis */
            i = 1
            while (dict.B[i] != enter) {
                i++
            }        /*Find basis index */
            vars[0] = i

            j = 0
            while (dict.C[j] != leave) {
                j++
            }        /*Find co-basis index */
            vars[1] = j
        }

        /*reorder array in increasing order with one misplaced element */
        fun reorder(a: Array<Int>) {
            for (i in 0 until a.size - 1) {
                if (a[i] > a[i + 1]) {
                    val temp = a[i]
                    a[i] = a[i + 1]
                    a[i + 1] = temp
                }
            }
            for (i in a.size - 2 downTo 0) {
                if (a[i] > a[i + 1]) {
                    val temp = a[i]
                    a[i] = a[i + 1]
                    a[i + 1] = temp
                }
            }
        }
    }
}
