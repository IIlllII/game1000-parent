package bimatrix

import lcp.ColumnTextWriter
import java.io.IOException
import java.io.Writer
import java.util.ArrayList
import java.util.HashMap

class Equilibria : Iterable<Equilibrium> {
    private val extremeEquilibria = ArrayList<Equilibrium>()
    private var cliques: List<BipartiteClique>? = null

    private val vertexMap1 = HashMap<Int, Equilibrium>()
    private val vertexMap2 = HashMap<Int, Equilibrium>()

    private val coclique = CliqueAlgorithm()

    private var dirty = true

    fun add(eq: Equilibrium) {
        extremeEquilibria.add(eq)

        // TODO: how can I turn these into indices
        vertexMap1[eq.vertex1!!] = eq
        vertexMap2[eq.vertex2!!] = eq
        dirty = true
    }

    fun getByVertex1(vertexId: Int): Equilibrium {
        return vertexMap1[vertexId] as Equilibrium
    }

    fun getByVertex2(vertexId: Int): Equilibrium {
        return vertexMap2[vertexId] as Equilibrium
    }

    operator fun get(idx: Int): Equilibrium {
        return extremeEquilibria[idx]
    }

    fun ncliques(): Int {
        return cliques!!.size
    }

    fun getClique(idx: Int): BipartiteClique {
        return cliques!![idx]
    }

    fun setCliques(cliques: List<BipartiteClique>) {
        this.cliques = cliques
        dirty = true
    }

    fun count(): Int {
        return extremeEquilibria.size
    }

    @Throws(IOException::class)
    fun print(output: Writer, decimal: Boolean) {
        val colpp = ColumnTextWriter()
        var idx = 1
        for (ee in extremeEquilibria) {
            colpp.writeCol("EE")

            colpp.writeCol(String.format("%s", idx++))
            colpp.alignLeft()

            colpp.writeCol("P1:")
            colpp.writeCol(String.format("(%1s)", ee.vertex1))
            for (coord in ee.probVec1!!) {
                colpp.writeCol(String.format(
                        if (decimal) "%.4f" else "%s",
                        if (decimal) coord.doubleValue() else coord.toString()))
            }
            colpp.writeCol("EP=")
            colpp.writeCol(String.format(
                    if (decimal) "%.3f" else "%s",
                    if (decimal) ee.payoff1!!.doubleValue() else ee.payoff1.toString()))

            colpp.writeCol("P2:")
            colpp.writeCol(String.format("(%1s)", ee.vertex2))
            for (coord in ee.probVec2!!) {
                colpp.writeCol(String.format(
                        if (decimal) "%.4f" else "%s",
                        if (decimal) coord.doubleValue() else coord.toString()))
            }
            colpp.writeCol("EP=")
            colpp.writeCol(String.format(
                    if (decimal) "%.3f" else "%s",
                    if (decimal) ee.payoff2!!.doubleValue() else ee.payoff2.toString()))
            colpp.endRow()
        }
        output.write(colpp.toString())
    }

    override fun iterator(): Iterator<Equilibrium> {
        return extremeEquilibria.iterator()
    }

    fun cliques(): Iterable<BipartiteClique> {
        return CliqueIterator()
    }

    inner class CliqueIterator : Iterable<BipartiteClique> {
        override fun iterator(): Iterator<BipartiteClique> {
            if (dirty) {
                coclique.run(this@Equilibria)
                dirty = false
            }
            return cliques!!.iterator()
        }
    }
}
