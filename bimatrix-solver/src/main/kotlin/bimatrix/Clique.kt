package bimatrix

import java.util.ArrayList

class BipartiteClique {
    var left: IntArray? = null // vertex indices
    var right: IntArray? = null // vertex indicies
}


/**
 * connected components and their maximal cliques in bipartite graphs
 *
 * update 19 April 2004:
 *
 * sort cliques in output
 *
 * update 15 August 1998:
 *
 * - candidate passed to  candtry12  without  poscand,  similarly
 * candidates and not their stack positions stored in nonconnected-list
 * (removes serious bug)
 *
 * - if CAND1 and CLIQUE1 both empty, terminate search; dito CAND2 and CLIQUE2
 * - outgraph left in;
 *
 * 8 March 1998
 * @author Bernhard von Stengel
 *
 * For a bipartite graph given as a set of pairs  (i,j), it outputs
 * - the connected components of that graph, and for each component
 * - the maximal product sets   U x V
 * so that all  (i,j)  in U x V  are edges of the graph
 * (so these are the maximal complete bipartite subgraphs or CLIQUES).
 *
 * INPUT:
 * The edges (i, j) are given by pairs of nonnegative integers separated
 * by blanks on standard input.
 *
 * OUTPUT:
 * On standard output,
 * a headline for each connected component, then
 * the cliques  U x V  listing  U  and  V  separately as lists
 * of integers, separated by  "x"  and each set enclosed in braces,
 * one clique per line.
 *
 * METHOD:
 * Connected components by a primitive version of union-find,
 * cliques with a variant of the algorithm by
 * [BK] C. Bron and J. Kerbosch, Finding all cliques of an undirected
 * graph, Comm. ACM 16:9 (1973), 575-577.
 *
 * APPROXIMATE STORAGE REQUIREMENTS:
 * for integer arrays, 4 bytes per integer, using constants
 * MAXINP1, MAXINP2  max. node indices in input
 * MAXEDGES          max. no. edges in input
 * MAXM, MAXN        max. dimension of incidence matrix
 * per connected component
 * 2 x MAXM x MAXN  integers for incidence matrix and stack
 * [2 MB  if MAXM = MAXN = 700 ]
 * 3 x MAXEDGES  integers for edge list
 * [0.6 MB  if  MAXEDGES = 50000 ]
 * 3 x MAXINP1  integers for input nodes and list of components
 * [60 kB   if  MAXINP1 = MAXINP2 = 5000 ]
 * If these constants are exceeded certain edges will be rejected
 * from the input with an error message.  Program shouldn't crash.
 * No error value is returned by  main().
 *
 * DETAILS OF METHODS:
 *
 * a) Connected components
 *
 * Designed for minimum storage requirement, running time
 * possibly quadratic in number of edges.
 * For each node that is read, a component  co1[i]  resp.  co2[j]
 * ( i  left node,  j  right node) is kept, initially  0  if node
 * is not yet input.  (Isolated nodes are treated as absent.)
 * For an edge  (i, j),  i and j  must be put in the same component.
 * Each component  co  points to the first edge in  edgelist,
 * where the edges are linked.  Merging two components is done
 * by traversing the edgelist with the higher number, updating
 * the component number of the nodes therein, and prepending
 * it to the list of the other component.
 * Components and edges are numbered starting with 1,  so "no
 * component" and the end of an edgelist is represented by 0.
 *
 * Sets are represented by C arrays, if starting with 0
 * (as usually in C),  then the elements of a  k-set are the
 * array elements [0..k) i.e. [0..k-1],  if starting with 1
 * they are [1..k].
 *
 * A possible improvement is to keep extra lists of the
 * equivalence classes for the nodes for each component so only
 * these have to be updated, which makes it faster.
 *
 * b) Clique enumeration
 *
 * The procedure  extend  recursively extends a current set of pairs
 * clique1, clique2  that eventually will form a maximal clique.
 * In [BK], this is only a single set  COMPSUB (here called CLIQUE),
 * here two sets are used since the graph is bipartite.
 * Cliques of a bipartite graph are equivalent to the cliques of
 * the ordinary graph obtained by connecting all left nodes by
 * themselves and all right nodes by themselves, except for
 * the cliques consisting exclusively of left or right points.
 *
 * The recursive calls use a self-made stack  stk  containing
 * local small arrays of variable size.  Intervals of this stack
 * are indicated by their endpoints which ARE local variables
 * to the recursive call.  The top of the stack  tos  is passed
 * as a parameter.
 *
 * The extension is done by adding points from a set CAND of
 * candidates to CLIQUE.  Throughout, the points in CAND are
 * connected to all points in  CLIQUE,  which holds at initialization
 * when CAND contains all points and CLIQUE is empty.
 *
 * Traversing the backtracking tree:  Extending its depth is
 * done by picking  c  (cand  in the code below) from CAND,
 * adding  c  to CLIQUE, removing all points not connected to  c
 * from CAND, and handing the new sets CLIQUE and CAND
 * to the recursive call.
 * For extending the backtracking tree in its breadth, this is
 * done in a loop (called backtracking cycle in [BK]) where repeatedly
 * different candidates  c  are added to CLIQUE (after the respective
 * return from the recursive call).  In order to avoid the output
 * of cliques that are not maximal, an additional set NOT is passed
 * down as a parameter to the recursive call of  extend.
 * This set NOT contains candidates  c  that
 * - are all connected to the elements in CLIQUE but
 * - have already been tried out, that is, all extensions of CLIQUE
 * containing any point in NOT have already been generated [BK, p.577].
 *
 * Hence, the recursive call proceeds downwards by
 * - removing  c  from CAND and adding it to CLIQUE
 * - removing all points disconnected from  c  from the new
 * sets  NOT  and  CAND  used in the recursive call.
 * After extension,  c  is then moved to  NOT  and the next
 * candidate is tried.
 *
 * To reduce the breadth of the backtracking tree, the first
 * candidate (or the subsequent ones) are chosen such that
 * as early as possible there is a node in NOT connected to all
 * remaining candidates.  Then NOT will never become empty and
 * hence no clique will be output, so the backtracking tree can
 * be pruned here.  This is done by choosing first a  fixpoint  fixp
 * in the set  NOT  or  CAND, such that after extension, when
 * fixp  is definitely in  NOT,  only points disconnected to  fixp
 * are added.  Their number is the smallest possible.
 *
 * This is version2 of the algorithm in [BK]:
 * a - pick  fixp  in NOT or CAND  with the smallest number of
 * disconnections to the other nodes in CAND,
 * b - if  fixp  is a candidate, try it out as a candidate, i.e.
 * extend  CLIQUE  with  fixp  (procedures  candtry  below),
 * and then move   fixp  to  NOT after extension.
 * c - then try out only points disconnected to  fixp,  as
 * determined in a.  (In contrast to [BK], we compute
 * a local list of these disconnected points while looking
 * for the smallest number of disconnections.)
 *
 * Amendments for the bipartite graph are here:  a  is done
 * by inspecting both sides of the graph.
 * For the single extension in  b  (if  fixp  is a candidate)
 * and the extensions in  c , only the sets NOT and CAND
 * on the other side of the candidate used for extension
 * have to be updated.  Hence,  NOT and CAND are kept as
 * separate sets  NOT1,  NOT2  and CAND1, CAND2.
 */
class CliqueAlgorithm {
    /* largest stack usage for full graph */

    private class Edge {
        internal var node1: Int = 0
        internal var node2: Int = 0
        internal var nextedge: Int = 0
    }

    fun run(vertexPairs: Array<IntArray>): List<BipartiteClique> {
        val firstedge = IntArray(MAXCO)
        val edgelist = arrayOfNulls<Edge>(MAXEDGES) //TODO: can we bound size a bit more with dynamic info?
        for (i in edgelist.indices) {
            edgelist[i] = Edge()
        }

        val numco = getconnco(firstedge, edgelist.requireNoNulls(), vertexPairs)
        return workonco(numco, firstedge, edgelist.requireNoNulls())
    }

    fun run(equilibria: Equilibria) {
        val vertexPairs = Array(equilibria.count()) { IntArray(2) }
        for (i in vertexPairs.indices) {
            vertexPairs[i][0] = equilibria[i].vertex1!!
            vertexPairs[i][1] = equilibria[i].vertex2!!
        }
        equilibria.setCliques(run(vertexPairs))
    }


    /**
     * recurses down by moving  cand  from  CAND1  to  clique1  and
     * then to NOT1  after extension.
     * clique1  is extended by  cand  where all points in  NOT2 and CAND2
     * relate to  cand.
     * pre:  cand  is in CAND1
     * post: cand  is moved from  CAND1  to  NOT1
     * CAND1 may be shuffled,  o/w stack unchanged
     */
    private fun candtry1(stk: IntArray, /* stack */
                         connected: Array<BooleanArray>,
                         cand: Int, /* the candidate from NODES1  to be added to CLIQUE */
                         clique1: IntArray, cliqsize1: Int, /* CLIQUE so far in NODES1 */
                         clique2: IntArray, cliqsize2: Int, /* CLIQUE so far in NODES2 */
                         sn1: Int, sc1: Int, ec1: Int, /* start NOT1, start CAND1, end CAND1 */
                         sn2: Int, sc2: Int, ec2: Int, /* start NOT2, start CAND2, end CAND2 */
                         tos: Int, /* top of stack */
                         orignode1: IntArray,
                         orignode2: IntArray,
                         cliques: MutableList<BipartiteClique>
    ) {
        var cliqsize1 = cliqsize1
        var ec1 = ec1
        var tos = tos
        var i: Int
        var j: Int
        val snnew: Int
        val scnew: Int
        val ecnew: Int

        clique1[cliqsize1++] = cand
        /* remove  cand  from CAND1 by replacing it with the last element of CAND1 */
        i = sc1
        while (i < ec1) {
            if (cand == stk[i]) {
                stk[i] = stk[--ec1]
                break
            }
            i++
        }
        /* stk[ec1] is free now but will after extension be needed again */
        /* fill new sets NOT2, CAND2 */
        snnew = tos
        j = sn2
        while (j < sc2) {
            if (connected[cand][stk[j]])
                stk[tos++] = stk[j]
            j++
        }
        scnew = tos
        j = sc2
        while (j < ec2) {
            if (connected[cand][stk[j]])
                stk[tos++] = stk[j]
            j++
        }
        ecnew = tos

        extend(stk, connected, clique1, cliqsize1, clique2, cliqsize2,
                sn1, sc1, ec1, snnew, scnew, ecnew, tos, orignode1, orignode2, cliques)

        /* remove  cand  from  clique1,
	   put  cand  into  NOT1  by increasing  *sc1  and moving
	   the node at position  *sc1  to the end of CAND1 */
        cliqsize1--
        stk[ec1++] = stk[sc1]
        stk[sc1] = cand
    }

    /* -------------------------------------------------- */
    /* recurses down by moving  cand  from  CAND2  to  clique2  and
	   then to NOT2  after extension;
	   clique2  is extended by  cand  where all points in  NOT1 and CAND1
		     relate to  cand.
	   pre:  cand  is in CAND2
	   post: cand  is moved from  CAND2  to  NOT2
		 CAND2 may be shuffled,  o/w stack unchanged
	 */
    private fun candtry2(stk: IntArray, /* stack */
                         connected: Array<BooleanArray>,
                         cand: Int, /* the candidate from NODES2  to be added to CLIQUE */
                         clique1: IntArray, cliqsize1: Int, /* CLIQUE so far in NODES1 */
                         clique2: IntArray, cliqsize2: Int, /* CLIQUE so far in NODES2 */
                         sn1: Int, sc1: Int, ec1: Int, /* start NOT1, start CAND1, end CAND1 */
                         sn2: Int, sc2: Int, ec2: Int, /* start NOT2, start CAND2, end CAND2 */
                         tos: Int, /* top of stack */
                         orignode1: IntArray,
                         orignode2: IntArray,
                         cliques: MutableList<BipartiteClique>
    ) {
        var cliqsize2 = cliqsize2
        var ec2 = ec2
        var tos = tos
        var i: Int
        var j: Int
        val snnew: Int
        val scnew: Int
        val ecnew: Int

        clique2[cliqsize2++] = cand
        /* remove  cand  from CAND2 by replacing it with the last element of CAND2 */
        j = sc2
        while (j < ec2) {
            if (cand == stk[j]) {
                stk[j] = stk[--ec2]
                break
            }
            j++
        }
        /* stk[ec2] is free now but will after extension be needed again */
        /* fill new sets NOT1, CAND1 */
        snnew = tos
        i = sn1
        while (i < sc1) {
            if (connected[stk[i]][cand])
                stk[tos++] = stk[i]
            i++
        }
        scnew = tos
        i = sc1
        while (i < ec1) {
            if (connected[stk[i]][cand])
                stk[tos++] = stk[i]
            i++
        }
        ecnew = tos

        extend(stk, connected, clique1, cliqsize1, clique2, cliqsize2,
                snnew, scnew, ecnew, sn2, sc2, ec2, tos, orignode1, orignode2, cliques)

        /* remove  cand  from  clique2,
	   put  cand  into  NOT2  by increasing  *sc2  and moving
	   the node at position  sc2  to the end of CAND1 */
        cliqsize2--
        stk[ec2++] = stk[sc2]
        stk[sc2] = cand
    }

    /* -------------------------------------------------- */

    /* extends the current set CLIQUE or outputs it if
	   NOT and CAND are empty.

	   pre:  CLIQUE = clique1[0, cliqsize1], clique2[0, cliqsize2]
		 NOT1 = stk[sn1, sc1],  CAND1= stk[sc1, ec1]
		 NOT2 = stk[sn2, sc2],  CAND2= stk[sc2, ec2]
		 sn1 <= sc1 <= ec1, sn2 <= sc2 <= ec2
		 all cliques extending  CLIQUE
		 containing a node in  NOT1  or  NOT2  have already been generated
	   post: output of all maximal cliques extending  CLIQUE  with
	         candidates from  CAND1  or  CAND2  but not from NOT1, NOT2.
	 */
    private fun extend(stk: IntArray, /* stack */
                       connected: Array<BooleanArray>,
                       clique1: IntArray, cliqsize1: Int, /* CLIQUE so far in NODES1 */
                       clique2: IntArray, cliqsize2: Int, /* CLIQUE so far in NODES2 */
                       sn1: Int, sc1: Int, ec1: Int, /* start NOT1, start CAND1, end CAND1 */
                       sn2: Int, sc2: Int, ec2: Int, /* start NOT2, start CAND2, end CAND2 */
                       tos: Int, /* top of stack,   tos >= ec1, ec2  */
                       orignode1: IntArray, /* original node numbers as input */
                       orignode2: IntArray,
                       cliques: MutableList<BipartiteClique>
    ) {
        var sc1 = sc1
        var sc2 = sc2
        var tos = tos
        /* if no further extension is possible then
	   output the current CLIQUE if applicable, and return */

        /* no clique or candidates on left: */
        if (sc1 == ec1 && cliqsize1 == 0) return
        /* no clique or candidates on right: */
        if (sc2 == ec2 && cliqsize2 == 0) return

        if (sc1 == ec1 && sc2 == ec2) {
            /*  CAND is empty  */
            if (sn1 == sc1 && sn2 == sc2) {
                /*  NOT is empty, otherwise do nothing  */
                addClique(clique1, cliqsize1, clique2, cliqsize2, orignode1, orignode2, cliques)
            }
        } else {  /*  CAND not empty */
            var bfixin1 = false
            var bcandfix = false

            val cmax = max(ec1 - sc1, ec2 - sc2)  /* the larger of |CAND1|, |CAND2|  */

            /* stack positions */
            /* reserve two arrays of size cmax on the stack */
            var posfix = -1
            val firstlist = tos
            tos += cmax
            var savelist = tos
            tos += cmax

            /* find fixpoint  fixp (a node of the graph) in  NOT  or  CAND
	   which has the smallest possible number of disconnections  minnod
	   to CAND  */
            var minnod = cmax + 1
            val fixpointdata = intArrayOf(savelist, firstlist, posfix, minnod)

            /* look for  fixp  in NODES1  */
            if (findfixpoint(stk, connected, fixpointdata, sn1, ec1, sc2, ec2, true)) {
                savelist = fixpointdata[0]
                posfix = fixpointdata[2]
                minnod = fixpointdata[3]

                bfixin1 = true
                bcandfix = posfix >= sc1
            }

            /* look for  fixp  in nodes2  */
            if (findfixpoint(stk, connected, fixpointdata, sn2, ec2, sc1, ec1, false)) {
                savelist = fixpointdata[0]
                posfix = fixpointdata[2]
                minnod = fixpointdata[3]

                bfixin1 = false
                bcandfix = posfix >= sc2
            }

            /* now:  fixp     = the node that is the fixpoint,
		 posfix   = its position on the stack,
		 bfixin1  = fixp  is in NODES1
		 bcandfix = fixp  is a candidate
		 stk[savelist, +minnod] = nodes disconnected to fixp
		 which are all either in CAND1 or in CAND2;
			 */
            /*    top of stack can be reset to  savelist+minnod  where
	      if savelist  is the second of the two lists, recopy it
	      to avoid that stk[firstlist, +cmax] is wasted
			 */
            if (savelist != firstlist) {
                var i: Int
                i = 0
                while (i < minnod) {
                    stk[firstlist + i] = stk[savelist + i]
                    i++
                }
                savelist = firstlist
            }
            tos = savelist + minnod

            if (bfixin1) {  /* fixpoint in NODES1  */
                if (bcandfix) {      /* fixpoint is a candidate */
                    val fixp = stk[fixpointdata[2]]
                    candtry1(stk, connected, fixp,
                            clique1, cliqsize1, clique2, cliqsize2,
                            sn1, sc1, ec1, sn2, sc2, ec2, tos, orignode1, orignode2, cliques)
                    ++sc1
                }
                /* fixpoint is now in NOT1, try all the nodes disconnected to it */
                for (j in 0 until minnod) {
                    candtry2(stk, connected, stk[savelist + j],
                            clique1, cliqsize1, clique2, cliqsize2,
                            sn1, sc1, ec1, sn2, sc2, ec2, tos, orignode1, orignode2, cliques)
                    ++sc2
                }
            } else {         /* fixpoint in NODES2  */
                if (bcandfix) {     /* fixpoint is a candidate */
                    val fixp = stk[fixpointdata[2]]
                    candtry2(stk, connected, fixp,
                            clique1, cliqsize1, clique2, cliqsize2,
                            sn1, sc1, ec1, sn2, sc2, ec2, tos, orignode1, orignode2, cliques)
                    ++sc2
                }
                /* fixpoint is now in NOT2, try all the nodes disconnected to it */
                for (j in 0 until minnod) {
                    candtry1(stk, connected, stk[savelist + j],
                            clique1, cliqsize1, clique2, cliqsize2,
                            sn1, sc1, ec1, sn2, sc2, ec2, tos, orignode1, orignode2, cliques)
                    ++sc1
                }
            }
        }
    }


    /* pre:  enough space on stack for the two lists  savelist,  tmplist
	   post: *minnod contains the new minimum no. of disconnections
		 stk[*savelist + *minnod] contains the candidates disconnected to
		 the fixpoint
	 */
    private fun findfixpoint(stk: IntArray, /* stack */
                             connected: Array<BooleanArray>,
                             stackpos: IntArray, /* position of [0] savelist [1] tmplist [2] fixpoint on the stack [3] minnod */
                             sninspect: Int, ecinspect: Int,
                             scother: Int, ecother: Int,
                             binspect1: Boolean  /* inspected nodes are in class1, o/w class2 */
    ): Boolean {
        var bfound = false

        for (i in sninspect until ecinspect) {
            val p = stk[i]
            val minnod = stackpos[3]
            val tmplist = stackpos[1]
            var count = 0
            /* count number of disconnections to  p,
	       building up stk[tmplist+count] containing the
	       disconnected points */
            var j = scother
            while (j < ecother && count < minnod) {
                val k = stk[j]
                if (!(if (binspect1) connected[p][k] else connected[k][p])) {
                    stk[tmplist + count] = k
                    count++
                }
                j++
            }

            /* check if new minimum found, in that case update fixpoint */
            if (count < minnod) {
                stackpos[2] = i
                stackpos[3] = count

                /* save tmplist by making it the new savelist */
                /* TODO: MKE: If I find two minimums aren't I just swapping the tmp and save list back and forth? */
                val savelist = stackpos[0]
                stackpos[0] = stackpos[1]
                stackpos[1] = savelist
                bfound = true
            }
        }
        return bfound
    }


    /* generates the incidence matrix  connected from the edgelist
	   starting with edgelist[e]
	pre:  all nodes in edgelist < MAXINP1,2
	post: orignode1[0..*m) contains the original node1 numbers
	      orignode2[0..*n) contains the original node2 numbers
	      connected[][] == TRUE if edge, o/w FALSE
	 *m == number of rows
	 *n == number of columns
	 */

    private fun genincidence(
            e: Int,
            edgelist: Array<Edge>,
            orignode1: IntArray,
            orignode2: IntArray,
            connected: Array<BooleanArray>
    ): IntArray {
        var e = e
        val mn = IntArray(2)
        val newnode1 = IntArray(MAXINP1)
        val newnode2 = IntArray(MAXINP2)

        /* init newnode */
        for (i in 0 until MAXINP1) newnode1[i] = -1
        for (j in 0 until MAXINP2) newnode2[j] = -1

        mn[1] = 0
        mn[0] = mn[1]

        while (e != 0) { /* process the edge list with edge index e */
            val i = edgelist[e].node1
            val j = edgelist[e].node2
            var newi = newnode1[i]
            var newj = newnode2[j]
            var keepgoing = false
            if (newi == -1) {
                if (mn[0] >= MAXM) { /* out of bounds for connected, reject */
                    printf("Left bound %d for incidence matrix ", MAXM)
                    printf("reached, edge (%d, %d) rejected\n", i, j)
                    keepgoing = true
                } else {
                    newi = mn[0]++
                    /* init connected on the fly */
                    for (k in 0 until MAXN) connected[newi][k] = false
                    newnode1[i] = newi
                    orignode1[newi] = i
                }
            }
            if (!keepgoing && newj == -1) {
                if (mn[1] >= MAXN) { /* out of bounds for connected, reject */
                    printf("Right bound %d for incidence matrix ", MAXN)
                    printf("reached, edge (%d, %d) rejected\n", i, j)
                    keepgoing = true
                } else {
                    newj = mn[1]++
                    newnode2[j] = newj
                    orignode2[newj] = j
                }
            }
            if (!keepgoing) connected[newi][newj] = true

            e = edgelist[e].nextedge
        }
        return mn
    }


    /* reads edges of bipartite graph from input, puts them in disjoint
	   lists of edges representing its connected components
	   pre:  nodes are nonzero integers < MAXINP1,2
		 other edges are rejected, and so are edges starting
		 from the MAXEDGEth edge on and larger, each with a warning msg.
	   post: return value == numco  (largest index of a connected component)
		 where  numco < MAXCO,  and for   1 <= co <= numco:
		 edgelist[co].firstedge == 0    if  co  is not a component
			       == edgeindex  e  otherwise where  e > 0  and
	         edgelist[e].node1, .node2[e] are endpoints of edge,
		 edgelist[e].nextedge == next edgeindex of component,
		                         zero if  e  is index to the last edge
	 */
    private fun getconnco(firstedge: IntArray, edgelist: Array<Edge>, vertexPairs: Array<IntArray>): Int {
        var numco: Int
        var newedge: Int
        val co1 = IntArray(MAXINP1)
        val co2 = IntArray(MAXINP2)   /* components of node1,2  */

        /* initialize  component indices of left and right nodes */
        for (i in 0 until MAXINP1)
            co1[i] = 0
        for (j in 0 until MAXINP2)
            co2[j] = 0

        numco = 0
        newedge = 0

        for (vertexPair in vertexPairs) {
            val i = vertexPair[0]
            val j = vertexPair[1]
            if (i < 0 || i >= MAXINP1 || j < 0 || j >= MAXINP2)
                printf("Edge (%d, %d) not in admitted range (0..%d, 0..%d), rejected\n",
                        i, j, MAXINP1 - 1, MAXINP2 - 1)
            else if (newedge >= MAXEDGES - 1)
                printf("max no. %d of edges exceeded, edge (%d, %d) rejected\n",
                        MAXEDGES - 1, i, j)
            else {
                /* add edge (i,j) to current componentlist */
                newedge++
                edgelist[newedge].node1 = i
                edgelist[newedge].node2 = j

                /* current components of i,j  */
                val ico = co1[i]
                val jco = co2[j]

                if (ico == 0) {
                    /*  i  has not yet been in a component before  */
                    if (jco == 0) {
                        /*  j  has not yet been in a component before  */
                        /*  add a new component  */
                        numco++
                        co2[j] = numco
                        co1[i] = co2[j]
                        firstedge[numco] = newedge
                        edgelist[newedge].nextedge = 0
                    } else { /* j  is already in a component: add  i  to j's
		           component, adding list elements in front */
                        co1[i] = jco
                        edgelist[newedge].nextedge = firstedge[jco]
                        firstedge[jco] = newedge
                    }
                } else { /* i  is already in a component */
                    if (jco == 0) {
                        /*  j  has not yet been in a component before  */
                        /*  add  j  to  i's component  */
                        co2[j] = ico
                        edgelist[newedge].nextedge = firstedge[ico]
                        firstedge[ico] = newedge
                    } else { /* i  and  j  are already in components  */
                        if (ico == jco) {
                            /* i, j  in same component: just add the current edge */
                            edgelist[newedge].nextedge = firstedge[ico]
                            firstedge[ico] = newedge
                        } else { /*  i  and  j  in different components:
		               merge these by traversing the edgelists
			       and updating components of all incident nodes
			       (this is wasteful since only nodes need be
			       updated, not edges)  */
                            var e: Int
                            val newco: Int
                            val oldco: Int
                            if (ico < jco) {
                                newco = ico
                                oldco = jco
                            } else {
                                newco = jco
                                oldco = ico
                            }
                            /* insert the current edge */
                            edgelist[newedge].nextedge = firstedge[oldco]
                            e = newedge
                            while (true) {
                                co2[edgelist[e].node2] = newco
                                co1[edgelist[e].node1] = co2[edgelist[e].node2]
                                if (edgelist[e].nextedge == 0) break
                                e = edgelist[e].nextedge
                            }
                            /*  e  is now the last edge in the updated list  */
                            edgelist[e].nextedge = firstedge[newco]
                            firstedge[newco] = newedge
                            /* oldco is unused now: reuse it if it was the
			  last component, otherwise just leave empty */
                            if (oldco == numco) numco--
                            firstedge[oldco] = 0
                        }
                    }
                }
            }
        }
        return numco
    }

    private fun addClique(clique1: IntArray, nclique1: Int, clique2: IntArray, nclique2: Int, orignode1: IntArray, orignode2: IntArray, cliques: MutableList<BipartiteClique>) {
        val clique = BipartiteClique()
        clique.left = sortClique(clique1, nclique1, orignode1)
        clique.right = sortClique(clique2, nclique2, orignode2)
        cliques.add(clique)
    }

    private fun sortClique(clique: IntArray, nclique: Int, orignode: IntArray): IntArray {
        val sorted = IntArray(nclique)
        for (i in 0 until nclique) {
            val x = orignode[clique[i]]
            var j = i
            while (j > 0) {
                val y = sorted[j - 1]
                if (y <= x) {
                    break
                }
                sorted[j] = y
                --j
            }
            sorted[j] = x
        }
        return sorted
    }


    /* works on the edgelists as generated by  getconnco
	   it processes each component by computing its maximal cliques
		pre : firstedge[1..numco], if nonzero, points to a connected component
	      in edgelist
		post: all components are processed
	 */
    private fun workonco(numco: Int, firstedge: IntArray, edgelist: Array<Edge>): List<BipartiteClique> {
        val cliques = ArrayList<BipartiteClique>()

        val orignode1 = IntArray(MAXM)
        val orignode2 = IntArray(MAXN)
        val connected = Array(MAXM) { BooleanArray(MAXN) }

        val stk = IntArray(STKSIZE)  /* stack */
        var tos: Int  /* top of stack */
        val clique1 = IntArray(MAXM)
        val clique2 = IntArray(MAXN)
        /* CLIQUE for first and second node class  */

        for (co in 1..numco) {
            if (firstedge[co] != 0) {
                /* found a nonzero component list */

                /* graph dimensions */
                val mn = genincidence(firstedge[co], edgelist, orignode1, orignode2, connected)
                val m = mn[0]
                val n = mn[1]


                /* compute the cliques of the component via  extend;
	      		initialize stack with the full sets of nodes
	      		and empty sets CAND and NOT  */
                tos = 0
                for (i in 0 until m) {
                    stk[tos++] = i   /* CAND1 = NODES1 */
                }
                for (i in 0 until n) {
                    stk[tos++] = i   /* CAND2 = NODES2 */
                }
                extend(stk, connected, clique1, 0, clique2, 0,
                        0, 0, m, m, m, m + n, tos, orignode1, orignode2, cliques)
            }
        }
        return cliques
    }

    private fun printf(s: String, vararg args: Any) {
        s.replace("\\n".toRegex(), System.getProperty("line.separator"))
        print(String.format(s, *args))
    }

    companion object {
        private fun max(a: Int, b: Int): Int {
            return if (a > b) a else b
        }

        private fun min(a: Int, b: Int): Int {
            return if (a < b) a else b
        }

        private val MAXM = 700    /* max. no of left nodes for incidence matrix */
        private val MAXN = 700    /* max. no of right nodes for incidence matrix */

        private val MAXINP1 = 5000    /* max. no of left nodes in input */
        private val MAXINP2 = 5000    /* max. no of right nodes in input */
        private val MAXEDGES = 50000       /* max. no of edges in input */
        private val MAXCO = min(MAXINP1, MAXINP2) + 1

        /* max. no of connected components;  on the smaller side, each node could be in different component */
        private val STKSIZE = (MAXM + 1) * (MAXN + 1)
    }
}