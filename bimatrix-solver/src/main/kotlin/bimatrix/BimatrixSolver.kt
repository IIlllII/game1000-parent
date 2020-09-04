package bimatrix

import lcp.LCP
import lcp.LemkeAlgorithm
import lcp.Rational
import lrs.Lrs
import java.io.PrintWriter
import java.util.*

import lrs.HPolygon
import org.slf4j.LoggerFactory

class BimatrixSolver {

    fun computePriorBeliefs(size: Int, prng: Random?): Array<Rational> {
        var priors: Array<Rational>? = null
        if (prng != null) {
            priors = Rational.probVector(size, prng)
        } else {
            val prob = Rational.ONE.divide(Rational.valueOf(size.toLong()))
            priors = Array<Rational>(size) {prob}
        }
        return priors
    }

    @Throws(LemkeAlgorithm.LemkeException::class)
    fun findOneEquilibrium(lemke: LemkeAlgorithm, a: Array<Array<Rational>>, b: Array<Array<Rational>>, xPriors: Array<Rational>, yPriors: Array<Rational>, out: PrintWriter): Equilibrium {
        // 1. Adjust the payoffs to be strictly negative (max = -1)
        val payCorrectA = BimatrixSolver.correctPaymentsNeg(a)
        val payCorrectB = BimatrixSolver.correctPaymentsNeg(b)

        // 2. Generate the LCP from the two payoff matrices and the priors
        val lcp = BimatrixSolver.generateLCP(a, b, xPriors, yPriors)
        //out.println("#Lemke LCP " + lcp.size());
        //out.println(lcp.toString());
        log.info(lcp.toString())

        // 3. Pass the combination of the two to the Lemke algorithm
        val z = lemke.run(lcp)

        // 4. Convert solution into a mixed strategy equilibrium
        val eq = BimatrixSolver.extractLCPSolution(z, a.size)
        val eqs = Equilibria()
        eqs.add(eq)


        // 5. Get original payoffs back and compute expected payoffs
        if (payCorrectA.compareTo(0) < 0) {
            BimatrixSolver.applyPayCorrect(a, payCorrectA.negate())
        }

        if (payCorrectB.compareTo(0) < 0) {
            BimatrixSolver.applyPayCorrect(b, payCorrectB.negate())
        }
        eq.payoff1 = BimatrixSolver.computeExpectedPayoff(a, eq.probVec1!!, eq.probVec2!!)
        eq.payoff2 = BimatrixSolver.computeExpectedPayoff(b, eq.probVec1!!, eq.probVec2!!)

        return eq
    }

    fun findAllEq(lrs: Lrs, a: Array<Array<Rational>>, b: Array<Array<Rational>>): Equilibria {
        val rows = a.size
        val columns = if (rows > 0) a[0].size else 0

        if (b.size != rows || rows > 0 && b[0].size != columns) {
            throw RuntimeException("Matrix b does not match matrix a") //TODO
        }

        // create LRS inputs matrices
        // First we adjust payoffs to make sure payoff matrices are positive
        val payCorrectB = getPayCorrectPos(b)
        val lrs1 = Array<Array<Rational>>(columns) { Array(rows + 1) {Rational.ONE} }
        for (j in lrs1.indices) {
            lrs1[j][0] = Rational.ONE
            for (i in 1 until lrs1[j].size) {
                lrs1[j][i] = b[i - 1][j].add(payCorrectB).negate()
            }
        }

        val payCorrectA = getPayCorrectPos(a)
        val lrs2 = Array<Array<Rational>>(rows) { Array<Rational>(columns + 1) { Rational.ONE} }
        for (i in lrs2.indices) {
            lrs2[i][0] = Rational.ONE
            for (j in 1 until lrs2[i].size) {
                lrs2[i][j] = a[i][j - 1].add(payCorrectA).negate()
            }
        }

        // run lrs on each input
        val lrsout1 = lrs.run(HPolygon(lrs1, true))
        val lrsout2 = lrs.run(HPolygon(lrs2, true))

        // create probability vectors by rescaling vertex coordinates
        // a vertex is a strategy, the strategy is defined by an array of probabilities
        val p1_vertex_array = convertToProbs(lrsout1!!.vertices)
        val p2_vertex_array = convertToProbs(lrsout2!!.vertices)

        val p1_vertex_labels = lrsout1.cobasis
        val p2_vertex_labels = lrsout2.cobasis

        // rearrange labels for P1
        // for i in 1...M+N  if i>N i=i-N if i<=N i=i+M
        // This is needed to coordinate the labels between p1 and p2.
        for (labels in p1_vertex_labels) {
            for (i in labels.indices) {
                if (labels[i] > columns) {
                    labels[i] -= columns
                } else {
                    labels[i] += rows
                }
            }
        }

        //create array of integers representing binding inequalities
        //represents bit string with 1s in all positions except 0
        val p1_lab_int_array = createLabelBitmapArr(p1_vertex_labels, rows, columns)
        val p2_lab_int_array = createLabelBitmapArr(p2_vertex_labels, rows, columns)

        // calculate p2_artificial integer so that we can ignore artificial equilibrium
        var p2_art_int = (1 shl rows + columns + 1) - 2
        for (i in rows + 1 until rows + columns + 1) {
            p2_art_int -= 1 shl i
        }

        // setup array (one for each player) @p1_eq_strategy initially with -1s for each index
        // it is set to the next free integer when a new  number for each equilibrium vertex
        val p1_eq_strategy = IntArray(p1_vertex_array.size)
        for (i in p1_eq_strategy.indices) {
            p1_eq_strategy[i] = -1
        }

        val p2_eq_strategy = IntArray(p2_vertex_array.size)
        for (i in p2_eq_strategy.indices) {
            p2_eq_strategy[i] = -1
        }

        // find and record equilibria by testing for complementarity
        // test for 0 as result of bit wise and on label_integers for vertices

        var p1_index = 0 //
        var p2_index = 0
        // index of vertices - used to go through p1/2_lab_int_array
        var eq_index = 1
        // indexes number of extreme equilibria
        var s1 = 1
        var s2 = 1
        val equilibria = Equilibria()

        // i, j index equilibrium strategy profiles of p1,p2 respectively
        for (p1_int in p1_lab_int_array) {
            for (p2_int in p2_lab_int_array) {
                if (p1_int and p2_int == 0 && p2_int != p2_art_int) {
                    // print eq vertex indices to IN
                    if (p1_eq_strategy[p1_index] == -1) {
                        p1_eq_strategy[p1_index] = s1
                        s1++
                    }
                    if (p2_eq_strategy[p2_index] == -1) {
                        p2_eq_strategy[p2_index] = s2
                        s2++
                    }

                    val eq = Equilibrium(p1_eq_strategy[p1_index], p2_eq_strategy[p2_index])

                    eq.probVec1 = p1_vertex_array[p1_index]
                    eq.payoff1 = computeExpectedPayoff(a, p1_vertex_array[p1_index], p2_vertex_array[p2_index])

                    eq.probVec2 = p2_vertex_array[p2_index]
                    eq.payoff2 = computeExpectedPayoff(b, p1_vertex_array[p1_index], p2_vertex_array[p2_index])

                    equilibria.add(eq)

                    eq_index++
                }
                p2_index++
            }
            p1_index++
            p2_index = 0
        }

        return equilibria
    }

    private fun getVertexSums(vertices: List<Array<Rational>>): Array<Rational> {
        val vertexSums = arrayOfNulls<Rational>(vertices.size)
        var i = 0
        for (vertex in vertices) {
            var sum = Rational.ZERO
            for (j in 1 until vertex.size)
            // skip the first (0|1)
            {
                sum = sum.add(vertex[j])
            }
            vertexSums[i] = sum
            ++i
        }
        return vertexSums.requireNoNulls()
    }

    private fun convertToProbs(lrs_vertex_array: List<Array<Rational>>): Array<Array<Rational>> {
        //calculate sums for each vertex in order to normalize
        val lrs_vertex_sum = getVertexSums(lrs_vertex_array)
        val prob_vertex_array = arrayOfNulls<Array<Rational>>(lrs_vertex_array.size)
        for (i in prob_vertex_array.indices) {
            val div = lrs_vertex_sum[i]
            prob_vertex_array[i] = Array(lrs_vertex_array[i].size - 1) { Rational.ZERO}
            if (div.compareTo(0) == 0)
            // this means the sum was zero, so every element is zero, why are we setting to -1?
            {
                for (j in 0 until prob_vertex_array[i]!!.size) {
                    prob_vertex_array[i]!![j] = Rational.ONE.negate()
                }
            } else {
                for (j in 0 until prob_vertex_array[i]!!.size) {
                    prob_vertex_array[i]!![j] = lrs_vertex_array[i][j + 1].divide(div) // skip the first (0|1)
                }
            }
        }
        return prob_vertex_array.requireNoNulls()
    }

    private fun createLabelBitmapArr(pl_label_array: List<Array<Int>>, rows: Int, columns: Int): IntArray {
        var i = 0
        val pl_lab_int_array = IntArray(pl_label_array.size)
        for (aref in pl_label_array) {
            var sum = (1 shl rows + columns + 1) - 2
            for (label in aref) {
                sum -= 1 shl label
            }
            pl_lab_int_array[i] = sum
            i++
        }
        return pl_lab_int_array
    }

    companion object {
        private val log = LoggerFactory.getLogger("BimatrixSolver")

        fun computeExpectedPayoff(payMatrix: Array<Array<Rational>>, probsA: Array<Rational>, probsB: Array<Rational>): Rational {
            var eq_payoff = Rational.ZERO
            for (i in payMatrix.indices) {
                for (j in 0 until payMatrix[i].size) {
                    eq_payoff = eq_payoff.add(payMatrix[i][j].multiply(probsA[i].multiply(probsB[j])))
                }
            }
            return eq_payoff
        }

        fun correctPaymentsNeg(matrix: Array<Array<Rational>>): Rational {
            var max = matrix[0][0]
            for (i in matrix.indices) {
                for (j in 0 until matrix[i].size) {
                    if (matrix[i][j].compareTo(max) > 0) {
                        max = matrix[i][j]
                    }
                }
            }

            var correct = Rational.ZERO
            if (max.compareTo(0) >= 0) {
                correct = max.negate().subtract(1)
                applyPayCorrect(matrix, correct)
            }
            return correct
        }

        fun applyPayCorrect(matrix: Array<Array<Rational>>, correct: Rational) {
            for (i in matrix.indices) {
                for (j in 0 until matrix[i].size) {
                    matrix[i][j] = matrix[i][j].add(correct) // -=
                }
            }
        }

        fun getPayCorrectPos(matrix: Array<Array<Rational>>): Rational {
            var min = matrix[0][0]
            for (i in matrix.indices) {
                for (j in 0 until matrix[i].size) {
                    if (matrix[i][j].compareTo(min) < 0) {
                        min = matrix[i][j]
                    }
                }
            }

            var correct = Rational.ZERO
            if (min.compareTo(0) <= 0) {
                correct = min.negate().add(1)
            }
            return correct
        }

        // this assumes pays have been normalized to -1 as the max value
        fun generateLCP(a: Array<Array<Rational>>, b: Array<Array<Rational>>, xPriors: Array<Rational>, yPriors: Array<Rational>): LCP {
            val nStratsA = a.size
            val nStratsB = if (a.size > 0) a[0].size else 0
            val size = nStratsA + 1 + nStratsB + 1
            val lcp = LCP(size)

            /* fill  M  */
            /* -A       */
            for (i in 0 until nStratsA) {
                for (j in 0 until nStratsB) {
                    lcp.setM(i, j + nStratsA + 1, a[i][j].negate())
                }
            }

            /* -E\T     */
            for (i in 0 until nStratsA) {
                lcp.setM(i, nStratsA + 1 + nStratsB, Rational.NEGONE)
            }
            /* F        */
            for (i in 0 until nStratsB) {
                lcp.setM(nStratsA, nStratsA + 1 + i, Rational.ONE)
            }
            /* -B\T     */
            lcp.payratmatcpy(b, true, true, nStratsA, nStratsB, nStratsA + 1, 0)
            for (i in 0 until nStratsA) {
                for (j in 0 until nStratsB) {
                    lcp.setM(j + nStratsA + 1, i, b[i][j].negate())
                }
            }

            /* -F\T     */
            for (i in 0 until nStratsB) {
                lcp.setM(nStratsA + 1 + i, nStratsA, Rational.NEGONE)
            }
            /* E        */
            for (i in 0 until nStratsA) {
                lcp.setM(nStratsA + 1 + nStratsB, i, Rational.ONE)
            }

            /* define RHS q     */
            lcp.setq(nStratsA, Rational.NEGONE)
            lcp.setq(nStratsA + 1 + nStratsB, Rational.NEGONE)

            generateCovVector(lcp, xPriors, yPriors)
            return lcp
        }

        private fun generateCovVector(lcp: LCP, xPriors: Array<Rational>, yPriors: Array<Rational>) {
            /* covering vector  = -rhsq */
            for (i in 0 until lcp.size()) {// i < lcpdim
                lcp.setd(i, lcp.q(i).negate())
            }

            /* first blockrow += -Aq    */
            val offset = xPriors.size + 1
            for (i in xPriors.indices) {
                for (j in yPriors.indices) {
                    lcp.setd(i, lcp.d(i).add(lcp.M(i, offset + j).multiply(yPriors[j])))
                }
            }

            /* third blockrow += -B\T p */
            for (i in offset until offset + yPriors.size) {
                for (j in xPriors.indices) {
                    lcp.setd(i, lcp.d(i).add(lcp.M(i, j).multiply(xPriors[j])))
                }
            }
        }

        fun extractLCPSolution(z: Array<Rational>, nrows: Int): Equilibrium {
            val eq = Equilibrium()
            val offset = nrows + 1

            val pl1 = arrayOfNulls<Rational>(offset - 1)
            System.arraycopy(z, 0, pl1, 0, pl1.size)

            val pl2 = arrayOfNulls<Rational>(z.size - offset - 1)
            System.arraycopy(z, offset, pl2, 0, pl2.size)

            eq.probVec1 = pl1.requireNoNulls()
            eq.probVec2 = pl2.requireNoNulls()

            return eq
        }
    }
}