package solver.expectedvalue

import solver.models.Pos
import solver.models.State
import solver.models.StateAndHist
import solver.tiebreaker.TieBreakers


/**
 * Minimizes the expected value of the distance to a 1000
 *
 * This strategy weirdly enough plays very different from one that
 * tries to maximize the chance to win in a 1 on 1 match.
 */
class ExpectedValue {

    /**
     * Cache results for nodes, so calculation is fast
     */
    private val cache = HashMap<State,Double>();

    private fun expectedValueStep(st: State, depth: Int, placement : Boolean ): Double {
        if (depth == 0 || st.isFull()) {
            return st.stateDistance().toDouble();
        }
        else if (!placement) {
            val expectedValue =  (1..6)
                    .map { i -> expectedValueStep(st.newCurrent(i), depth - 1, true) }
                    .sum() / 6.0;
            return expectedValue;
        } else {
            if(cache.containsKey(st)) {
                return cache[st] as Double
            }
            val min = st.getPossiblePlacements()
                    .map { pos -> expectedValueStep(st.addToPos(pos), depth - 1,false) }
                    .minOrNull() as Double
            cache[st] = min;
            return min;
        }
    }

    data class ExpectedValueResult(val pos: Pos, val result: Double);


    /**
     * Run algorithm depth first and cache results for hit nodes
     */
    fun runExpectedValueStrategy(stateAndHist : StateAndHist,
                                 priority : (List<Pos>) -> Pos = TieBreakers.PRIO_HUN_TEN_ONE) : Pos {
        val state = stateAndHist.state;
        val pl = state.getPossiblePlacements().map { pos ->
            ExpectedValueResult(pos, this.expectedValueStep(state.addToPos(pos), 30,false))
        }
        val min = pl.map { i -> i.result }.minOrNull();
        val results = pl.filter { i->i.result == min }.map { i->i.pos }
        return priority(results)
    }


}