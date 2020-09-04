package lrs

import lcp.BigIntegerUtils
import lcp.BigIntegerUtils.greater
import lcp.BigIntegerUtils.negative
import lcp.BigIntegerUtils.one
import lcp.BigIntegerUtils.positive
import lcp.BigIntegerUtils.zero
import lcp.ColumnTextWriter
import lcp.Rational
import java.io.StringWriter
import java.math.BigInteger

class Dictionary {
    /** */
    /*                   Indexing after initialization                            */
    /*               Basis                                    Cobasis             */
    /*   ---------------------------------------    ----------------------------- */
    /*  |  i  |0|1| .... |lastdv|lastdv+1|...|m|   | j  | 0 | 1 | ... |d-1|  d  | */
    /*  |-----|+|+|++++++|++++++|--------|---|-|   |----|---|---|-----|---|+++++| */
    /*  |B[i] |0|1| .... |lastdv|lastdv+1|...|m|   |C[j]|m+1|m+2| ... |m+d|m+d+1| */
    /*   -----|+|+|++++++|++++++|????????|???|?|    ----|???|???|-----|???|+++++| */
    /*                                                                            */
    /* Row[i] is row location for B[i]         Col[j] is column location for C[j] */
    /*  -----------------------------              -----------------------------  */
    /* |   i   |0|1| ..........|m-1|m|            | j    | 0 | 1 | ... |d-1| d  | */
    /* |-------|+|-|-----------|---|-|            |------|---|---|---  |---|++++| */
    /* |Row[i] |0|1|...........|m-1|m|            |Col[j]| 1 | 2 | ... | d |  0 | */
    /* --------|+|*|***********|***|*|             ------|***|***|*****|***|++++| */
    /*                                                                            */
    /*  + = remains invariant   * = indices may be permuted ? = swapped by pivot  */
    /*                                                                            */
    /*  m = number of input rows   n= number of input columns                     */
    /*  input dimension inputd = n-1 (H-rep) or n (V-rep)                         */
    /*  lastdv = inputd-nredundcol  (each redundant column removes a dec. var)    */
    /*  working dimension d=lastdv-nlinearity (an input linearity removes a slack) */
    /*  obj function in row 0, index 0=B[0]  col 0 has index m+d+1=C[d]           */
    /*  H-rep: b-vector in col 0, A matrix in columns 1..n-1                      */
    /*  V-rep: col 0 all zero, b-vector in col 1, A matrix in columns 1..n        */
    /** */

    internal lateinit var A: Array<Array<BigInteger>>        // TODO: do not allow interaction via row and cols, but through bas/cob indexes
    internal var d: Int = 0                    // A has d+1 columns, col 0 is b-vector
    internal var m: Int = 0                    // A has m+1 rows, row 0 is cost row

    private var _lexflag: Boolean = false        // true if lexmin basis for this vertex

    // basis, row location indices
    internal lateinit var B: IntArray
    internal lateinit var rows: IntArray // TODO: abstract away use of rows and cols... just interact via bas/cob indexes

    // cobasis, column location indices
    internal lateinit var C: IntArray
    internal lateinit var cols: IntArray

    internal var lastdv: Int = 0        /* index of last dec. variable after preproc    */

    var isNonNegative = true
        private set
    private var _det: BigInteger? = null                 /* current determinant of basis                 */
    private var _gcd: Array<BigInteger>? = null        /* Gcd of each row of numerators               */
    private var _lcm: Array<BigInteger>? = null        /* Lcm for each row of input denominators      */
    //private long d_orig;					// value of d as A was allocated  (E.G.)
    private var homogeneous = true
    private var obj: Rational? = null                    // objective function value

    //	List<Integer> linearities = new ArrayList<Integer>();

    private var lexmindirty = false

    constructor(`in`: HPolygon, Q: LrsAlgorithm) {
        val m_A = `in`.numRows
        d = `in`.dimension
        isNonNegative = `in`.nonnegative

        /* nonnegative flag set means that problem is d rows "bigger"     */
        /* since nonnegative constraints are not kept explicitly          */
        m = if (`in`.nonnegative) m_A + d else m_A

        B = IntArray(m + 1)
        rows = IntArray(m + 1)

        C = IntArray(d + 1)
        cols = IntArray(d + 1)

        //d_orig = d;

        A = Array(m_A + 1) { Array(d + 1) { BigInteger.ZERO} }

        /* Initializations */
        _lexflag = true
        _det = BigInteger.ONE
        obj = Rational.ZERO

        /*m+d+1 is the number of variables, labelled 0,1,2,...,m+d  */
        /*  initialize array to zero   */
        for (i in 0..m_A) {
            for (j in 0..d) {
                A[i][j] = BigInteger.ZERO
            }
        }

        //Q.facet = new long[d + 1];
        //this.redundcol = new long[d + 1];


        _gcd = Array(m + 1) { BigInteger.ZERO}
        _lcm = Array(m + 1) { BigInteger.ZERO}
        Q.saved_C = LongArray(d + 1)

        lastdv = d      /* last decision variable may be decreased */
        /* if there are redundant columns          */

        /*initialize basis and co-basis indices, and row col locations */
        /*if nonnegative, we label differently to avoid initial pivots */
        /* set basic indices and rows */
        if (isNonNegative) {
            for (i in 0..m) {
                B[i] = i
                if (i <= d) {
                    rows[i] = 0 /* no row for decision variables */
                } else {
                    rows[i] = i - d
                }
            }
        } else {
            for (i in 0..m) {
                if (i == 0) {
                    B[0] = 0
                } else {
                    B[i] = d + i
                }
                rows[i] = i
            }
        }
        for (j in 0 until d) {
            if (isNonNegative) {
                C[j] = m + j + 1
            } else {
                C[j] = j + 1
            }
            cols[j] = j + 1
        }
        C[d] = m + d + 1
        cols[d] = 0

        readDic(0, `in`.matrix, Q.verbose)
    }

    constructor(src: Dictionary) {
        copy(src)
    }


    fun det(): BigInteger? {
        return _det
    }

    fun gcd(i: Int): BigInteger {
        return _gcd!![i]
    }

    fun lcm(i: Int): BigInteger {
        return _lcm!![i]
    }

    fun cost(s: Int): BigInteger {
        val col = cols[s]
        return A[0][col]
    }

    operator fun get(r: Int, s: Int): BigInteger {
        val row = rows[r]
        val col = cols[s]
        return A[row][col]
    }

    fun b(r: Int): BigInteger {
        val row = rows[r]
        return A[row][0]
    }

    fun lexflag(): Boolean {
        if (lexmindirty) {
            _lexflag = lexmincol(0)
            lexmindirty = false
        }
        return _lexflag
    }

    fun copy(src: Dictionary) {
        this.d = src.d
        this.m = src.m
        this.isNonNegative = src.isNonNegative
        this._det = src._det
        this._lexflag = src._lexflag
        this.lexmindirty = src.lexmindirty
        this.obj = src.obj
        this.lastdv = src.lastdv
        this._det = src._det

        this.B = IntArray(src.B.size)
        for (i in this.B.indices) {
            this.B[i] = src.B[i]
        }

        this.C = IntArray(src.C.size)
        for (i in this.C.indices) {
            this.C[i] = src.C[i]
        }

        this.cols = IntArray(src.cols.size)
        for (i in this.cols.indices) {
            this.cols[i] = src.cols[i]
        }

        this.rows = IntArray(src.rows.size)
        for (i in this.rows.indices) {
            this.rows[i] = src.rows[i]
        }

        this._gcd = Array(src._gcd!!.size) {i -> src._gcd!![i]}

        this._lcm = Array(src._lcm!!.size) {i -> src._lcm!![i]}

        this.A = Array(src.A.size) {i-> Array(src.A[i].size) {j-> src.A[i][j] }}
    }

    private fun readDic(hull: Int, matrix: Array<Array<Rational>>, verbose: Boolean) {
        //init matrix
        A[0][0] = BigInteger.ONE
        _lcm!![0] = BigInteger.ONE
        _gcd!![0] = BigInteger.ONE

        //should this be m or m_A
        for (i in 1..matrix.size)
        /* read in input matrix row by row                 */ {
            _lcm!![i] = BigInteger.ONE    /* Lcm of denominators */
            _gcd!![i] = BigInteger.ZERO    /* Gcd of numerators */
            for (j in hull until matrix[i - 1].size)
            /* hull data copied to cols 1..d */ {
                val rat = matrix[i - 1][j]
                A[i][j] = rat.num
                A[0][j] = rat.den
                if (!one(A[0][j])) {
                    _lcm!![i] = BigIntegerUtils.lcm(_lcm!![i], A[0][j])    /* update lcm of denominators */
                }
                _gcd!![i] = _gcd!![i].gcd(A[i][j])    /* update gcd of numerators   */
            }

            if (hull != 0) {
                A[i][0] = BigInteger.ZERO    /*for hull, we have to append an extra column of zeroes */
                if (!one(A[i][1]) || !one(A[0][1])) {/* all rows must have a BigInteger.ONE in column BigInteger.ONE */
                    // TODO: Q->polytope = false;
                }
            }

            if (!zero(A[i][hull]))
            /* for H-rep, are zero in column 0     */ {
                homogeneous = false    /* for V-rep, all zero in column 1     */
            }

            if (greater(_gcd!![i], BigInteger.ONE) || greater(_lcm!![i], BigInteger.ONE)) {
                for (j in 0..d) {
                    var tmp = A[i][j].divide(_gcd!![i])    /*reduce numerators by Gcd  */
                    tmp = tmp.multiply(_lcm!![i])                /*remove denominators */
                    A[i][j] = tmp.divide(A[0][j])                /*reduce by former denominator */
                }
            }
        }

        /* 2010.4.26 patch */
        /* set up Gcd and Lcm for nonexistent nongative inequalities */
        if (isNonNegative) {
            for (i in matrix.size + 1 until _lcm!!.size) {
                _lcm!![i] = BigInteger.ONE
                //}
                //for (int i = matrix.length + 1; i < Gcd.length; ++i) {
                _gcd!![i] = BigInteger.ONE
            }
        }

        if (homogeneous && verbose) {
            println()
            print("*Input is homogeneous, column 1 not treated as redundant")
        }
    }

    /*private void setRow(int hull, int row, Rational[] values, boolean isLinearity)
    {
    	  BigInteger[] oD = new BigInteger[d];
    	  oD[0] = BigInteger.ONE;

    	  int i = row;
    	  _lcm[i] = BigInteger.ONE;     // Lcm of denominators
    	  _gcd[i] = ZERO;     // Gcd of numerators
    	  for (int j = hull; j <= d; ++j)       // hull data copied to cols 1..d
    	  {
    		  A[i][j] = values[j-hull].num;
    	      oD[j] = values[j-hull].den;
	          if (!one(oD[j])) {
    	            _lcm[i] = BigIntegerUtils.lcm(_lcm[i], oD[j]);      // update lcm of denominators
	          }
	          _gcd[i] = _gcd[i].gcd(A[i][j]);   // update gcd of numerators
    	  }

    	  if (hull != 0)
    	  {
    		  A[i][0] = ZERO;        // for hull, we have to append an extra column of zeroes
	          if (!one(A[i][1]) || !one(oD[1])) {       // all rows must have a BigInteger.ONE in column BigInteger.ONE
    	            //Q->polytope = FALSE;
	          }
    	  }
    	  if (!zero(A[i][hull])) {  // for H-rep, are zero in column 0
    	       //homogeneous = false; // for V-rep, all zero in column 1
    	  }

    	  if (greater(_gcd[i], BigInteger.ONE) || greater(_lcm[i], BigInteger.ONE)) {
    	        for (int j = 0; j <= d; j++)
    	          {
    	        	A[i][j] = A[i][j].divide(_gcd[i]).multiply(_lcm[i]).divide(oD[j]);
    	            //exactdivint (A[i][j], Gcd[i], Temp);        //reduce numerators by Gcd
    	            //mulint (Lcm[i], Temp, Temp);        		//remove denominators
    	            //exactdivint (Temp, oD[j], A[i][j]);       	//reduce by former denominator
    	          }
    	  }
    	 if (isLinearity)        // input is linearity
	     {
    	      linearities.add(row);
	     }

    	  // 2010.4.26   Set Gcd and Lcm for the non-existant rows when nonnegative set
    	  if (nonnegative && row == m) {
    		  for (int j = 1; j <= d; ++j) {
    			  _lcm[m + j] = BigInteger.ONE;
    			  _gcd[m + j] = BigInteger.ONE;
    		  }
    	  }
    }*/

    /*private void setObj(int hull, Rational[] values, boolean max)
    {
	  if (max) {
	       //Q->maximize=TRUE;
	  } else {
	       //Q->minimize=TRUE;
	       for(int i=0; i<= d; ++i) {
	          values[i]= values[i].negate();
	       }
       }

	  setRow(hull, 0, values, false);
    }*/

    fun cobasis(): IntArray {
        val cobasis = IntArray(d)
        for (i in cobasis.indices) {
            cobasis[cols[i] - 1] = i
        }
        return cobasis
    }


    /* Qpivot routine for array A              */
    /* indices bas, cob are for Basis B and CoBasis C    */
    /* corresponding to row Row[bas] and column       */
    /* Col[cob]   respectively                       */
    fun pivot(r: Int, s: Int, debug: Boolean) {
        lexmindirty = true

        val row = rows[r]
        val col = cols[s]

        /* Ars=A[r][s]    */
        val Ars = A[row][col]
        if (positive(Ars) && negative(_det!!) || negative(Ars) && positive(_det!!)) {
            _det = _det!!.negate()    /*adjust determinant to new sign */
        }


        for (i in A.indices) {
            if (i != row) {
                for (j in 0..d) {
                    if (j != col) {
                        /* A[i][j]=(A[i][j]*Ars-A[i][s]*A[r][j])/P->det; */
                        val Nt = A[i][j].multiply(Ars)
                        val Ns = A[i][col].multiply(A[row][j])
                        A[i][j] = Nt.subtract(Ns).divide(_det!!)
                    }
                }
            }
        }
        if (positive(Ars) || zero(Ars)) {
            for (j in 0..d)
            /* no need to change sign if Ars neg */
            /*   A[r][j]=-A[r][j];              */
                if (!zero(A[row][j]))
                    A[row][j] = A[row][j].negate()
        }                /* watch out for above "if" when removing this "}" ! */
        else {
            for (i in A.indices) {
                if (!zero(A[i][col])) {
                    A[i][col] = A[i][col].negate()
                }
            }
        }

        /*  A[r][s]=P->det;                  */
        A[row][col] = _det!!       /* restore old determinant */
        _det = if (negative(Ars)) Ars.negate() else Ars /* always keep positive determinant */

        /* set the new rescaled objective function value */
        obj = Rational(
                _gcd!![0].multiply(A[0][0]).negate(), //num
                _det!!.multiply(_lcm!![0]))                //den
    }

    fun lexmin(s: Int): Boolean {
        return lexmincol(cols[s])
    }

    /*test if basis is lex-min for vertex or ray, if so true */
    /* false if a_r,g=0, a_rs !=0, r > s          */
    private fun lexmincol(compareCol: Int): Boolean {
        /*do lexmin test for vertex if col=0, otherwise for ray */
        for (i in lastdv + 1..m) {
            val row = rows[i]
            if (zero(A[row][compareCol])) {    /* necessary for lexmin to fail */
                for (j in 0 until d) {
                    val col = cols[j]
                    if (B[i] > C[j])
                    /* possible pivot to reduce basis */ {
                        if (zero(A[row][0]))
                        /* no need for ratio test, any pivot feasible */ {
                            if (!zero(A[row][col])) {
                                return false
                            }
                        } else if (negative(A[row][col]) && ismin(row, col)) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }                /* end of lexmin */

    /*test if A[r][s] is a min ratio for col s */
    private fun ismin(row: Int, col: Int): Boolean {
        for (i in 1 until A.size)
            if (i != row && negative(A[i][col]) && BigIntegerUtils.comprod(A[i][0], A[row][col], A[i][col], A[row][0]) !== 0) {
                return false
            }

        return true
    }

    /* find min index ratio -aig/ais, ais<0 */
    /* if multiple, checks successive basis columns */
    /* recoded Dec 1997                     */
    fun ratio(s: Int, debug: Boolean)    /*find lex min. ratio */: Int {
        val col = cols[s]

        val minratio = IntArray(m + 1)
        var nstart = 0
        var ndegencount = 0
        var degencount = 0

        /* search rows with negative coefficient in dictionary */
        /*  minratio contains indices of min ratio cols        */
        for (j in lastdv + 1..m) {
            if (negative(A[rows[j]][col])) {
                minratio[degencount++] = j
            }
        }

        if (debug) {
            print("  Min ratios: ")
            for (i in 0 until degencount)
                print(String.format(" %d ", B[minratio[i]]))
        }

        if (degencount == 0) {
            return degencount    /* non-negative pivot column */
        }

        var ratiocol = 0            /* column being checked, initially rhs */
        var start = 0            /* starting location in minratio array */
        var bindex = d + 1        /* index of next basic variable to consider */
        var cindex = 0            /* index of next cobasic variable to consider */
        var basicindex = d        /* index of basis inverse for current ratio test, except d=rhs test */

        while (degencount > 1)
        /*keep going until unique min ratio found */ {
            if (B[bindex] == basicindex)
            /* identity col in basis inverse */ {
                if (minratio[start] == bindex) {
                    /* remove this index, all others stay */
                    start++
                    degencount--
                }
                bindex++
            } else {
                /* perform ratio test on rhs or column of basis inverse */
                var firstime = true /*For ratio test, true on first pass,else false */
                /*get next ratio column and increment cindex */
                if (basicindex != d) {
                    ratiocol = cols[cindex++]
                }

                var Nmin: BigInteger? = null
                var Dmin: BigInteger? = null //they get initialized before they are used
                for (j in start until start + degencount) {
                    val i = rows[minratio[j]]    /* i is the row location of the next basic variable */
                    var comp = 1        /* 1:  lhs>rhs;  0:lhs=rhs; -1: lhs<rhs */

                    if (firstime) {
                        firstime = false    /*force new min ratio on first time */
                    } else {
                        if (positive(Nmin!!) || negative(A[i][ratiocol])) {
                            if (negative(Nmin) || positive(A[i][ratiocol])) {
                                comp = BigIntegerUtils.comprod(Nmin, A[i][col], A[i][ratiocol], Dmin!!)
                            } else {
                                comp = -1
                            }
                        } else if (zero(Nmin) && zero(A[i][ratiocol])) {
                            comp = 0
                        }

                        if (ratiocol == 0) {
                            comp = -comp    /* all signs reversed for rhs */
                        }
                    }

                    if (comp == 1) {
                        /*new minimum ratio */
                        nstart = j
                        Nmin = A[i][ratiocol]
                        Dmin = A[i][col]
                        ndegencount = 1
                    } else if (comp == 0) {
                        /* repeated minimum */
                        minratio[nstart + ndegencount++] = minratio[j]
                    }
                }
                degencount = ndegencount
                start = nstart
            }
            basicindex++        /* increment column of basis inverse to check next */

            if (debug) {
                print(String.format(" ratiocol=%d degencount=%d ", ratiocol, degencount))
                print("  Min ratios: ")
                for (i in start until start + degencount)
                    print(String.format(" %d ", B[minratio[i]]))
            }
        }

        return minratio[start]
    }


    /* print the integer m by n array A
	   with B,C,Row,Col vectors         */
    override fun toString(): String {
        val newline = System.getProperty("line.separator")
        val output = StringWriter()
        output.write(newline)

        output.write(" Basis    ")
        for (i in 0..m) {
            output.write(String.format("%d ", B[i]))
        }
        output.write(newline)

        output.write(" Row ")
        for (i in 0..m) {
            output.write(String.format("%d ", rows[i]))
        }
        output.write(newline)

        output.write(" Co-Basis ")
        for (i in 0..d) {
            output.write(String.format("%d ", C[i]))
        }
        output.write(newline)

        output.write(" Column ")
        for (i in 0..d) {
            output.write(String.format("%d ", cols[i]))
        }
        output.write(newline)

        output.write(String.format(" det=%s", _det!!.toString()))
        output.write(newline)

        val colpp = ColumnTextWriter()
        colpp.writeCol("A")
        for (j in 0..d) {
            colpp.writeCol(C[j])
        }
        colpp.endRow()
        var i = 0
        while (i <= m) {
            colpp.writeCol(B[i])
            for (j in 0..d) {
                colpp.writeCol(A[rows[i]][cols[j]].toString())
            }
            if (i == 0 && isNonNegative) { //skip basic rows - don't exist!
                i = d
            }
            colpp.endRow()
            ++i
        }
        output.write(colpp.toString())
        return output.toString()
    }

    /*print the long precision integer in row r col s of matrix A */
    /*private void pimat (int r, int s, BigInteger Nt, String name, StringWriter output)
	{
		if (s == 0) {
			output.write(String.format("%s[%d][%d]=", name, B[r], C[s]));
		} else {
			output.write(String.format("[%d]=", C[s]));
		}
		output.write(String.format("%s", Nt.toString()));
	}*/
}