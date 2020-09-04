package solver.mcts

import kotlin.math.ln
import kotlin.math.sqrt


/**
 * Mutable tree
 */
class MCTSTree<T>(val value:T,
                  val parent : MCTSTree<T>?,
                  val children: MutableList<MCTSTree<T>>) {

    var winMetric: Double = 0.0;
    var simulations : Long = 0L
    var decision : Boolean = true;

    fun addResult(result:Double) {
        winMetric += result;
        simulations++;
    }

    fun addLeaf(tree : MCTSTree<T>)  {
        children.add(tree);
    }

    /**
     * The factor that determines which node is worth exploring
     */
    fun selectFactor(constant: Double, totalSimulations: Long): Double {
        return if (simulations == 0L) {
            Double.MAX_VALUE;
        } else {
            val right = constant* sqrt(ln(totalSimulations.toDouble()) /simulations.toDouble());
            val left = winMetric/simulations.toDouble();
            right+left;
        }
    }

    private fun ratio() : Double {
        return if(simulations == 0L) 0.0 else winMetric/simulations.toDouble();
    }

    /**
     * Get the list of children with the highest select factor
     */
    fun selectChildren(constant: Double, totalSimulations : Long) : List<MCTSTree<T>> {
        val factors = children
                .map { i -> Pair(i,i.selectFactor(constant,totalSimulations)) };
        val max = factors.map { i->i.second }.maxOrNull() ?: Double.MIN_VALUE;
        return factors.filter { i->i.second == max }.map { i->i.first };
    }

    fun isLeaf() : Boolean {
        return children.isEmpty();
    }

    override fun toString() : String {
        var a = "Data: $value\n";
        for (i in children) {
            a = a + "Average: " + i.ratio() +"   Simulations: "+ i.simulations + "   Wins:"+i.winMetric+"    HistState" + i.value +"\n";
        }
        return a;
    }

}