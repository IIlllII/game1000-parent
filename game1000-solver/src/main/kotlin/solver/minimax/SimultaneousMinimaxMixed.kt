package solver.minimax

import bimatrix.BimatrixSolver
import lcp.Rational
import lrs.LrsAlgorithm
import org.slf4j.LoggerFactory
import solver.models.Pos
import solver.models.StateAndHist
import solver.tiebreaker.TieBreakers
import java.lang.IllegalStateException




/**
 *
 * An attempt at an expectiminimax solution that leverages mixed strategies
 * FIXME, this is in development and does not work properly at the moment.
 *
 */
class SimultaneousExpectiMinimaxMixed(val useCache : Boolean = true) {

    val logger = LoggerFactory.getLogger(SimultaneousExpectiMinimaxMixed::class.java)

    /**
     * Cache results for nodes, so calculation is a bit faster
     */
    var cache = HashMap<SimState,Pair<Double,Int>>(5000000);

    /**
     * Simple fair flushing. Assumes that current values are better then old ones.
     * TODO use a LRU instead
     */
    fun flushCache() {
        cache = HashMap(5000000);
    }


    fun miniMax(st : SimState, depth: Int, placement : Boolean): Double {
        if (depth == 0 || st.isEnded()) {
            return st.payoff();
        }
        else if (!placement) {
            val expectedValue = st.movesByNature()
                    .map { i -> miniMax(i, depth - 1,true) }
                    .sum();
            return expectedValue;
        } else {
            if(useCache && cache.containsKey(st)) {
                val hit = cache[st];
                val min = hit!!.first
                cache[st] = Pair(min,hit.second+1);
                return min;
            }

            val next = st.possibilitiesMatrix();

            if(cache.size > 0 && cache.size%100000 == 0) {
                logger.debug("Cached: ${cache.size}")
            }

            val payoff = next.map { row ->
                row.map { col -> this.miniMax(col.state,30,false) }.toDoubleArray()
            }.toTypedArray();

            val program = BimatrixSolver()

            val lrs = LrsAlgorithm()

            val a = convertToRationalArray(payoff,true)
            val b = convertToRationalArray(payoff,false)

            val eqs = program.findAllEq(lrs, a, b)

            //Must negate it to invert flip above due to me using negative payoffs as good for player 1
            val calculatedPayoff = -eqs[0].payoff1!!.doubleValue()

            if(useCache) {
                if(cache.size > 5000000) {
                    flushCache();
                }
                cache[st] = Pair(calculatedPayoff,1);
            }

            val choices = st.player1State().getPossiblePlacementsOrdered();
            val mapped = choices.mapIndexed { idx, i -> Pair(eqs[0].probVec1!![idx],i) }

            val obj = mapped.find { i -> Rational.ONE == i.first }
            if(obj != null) return calculatedPayoff;

            //if(st.player1State() == st.player2State()) {
            logger.debug("Mixed state found: $st")
            //}

            return calculatedPayoff;
        }
    }

    data class ExpectiminimaxResult(val pos: SimState, val result: Double);

    /**
     * Run minimax and cache results for hit nodes
     */
    fun runMiniMaxStrategyMixed(stateAndHist : StateAndHist, priority : (List<Pos>) -> Pos = TieBreakers.PRIO_HUN_TEN_ONE) : Pos {
        return runMiniMaxStrategyMixedSim(Get1000SimState(stateAndHist.state,stateAndHist.state),priority);
    }

    /**
     * Run minimax and cache results for hit nodes
     */
    private fun runMiniMaxStrategyMixedSim(state : SimState, priority : (List<Pos>) -> Pos = TieBreakers.PRIO_HUN_TEN_ONE) : Pos {
        val pl = state.possibilitiesMatrix();

        val resMtx = pl.map { row ->
             row.map { col -> this.miniMax(col.state,30,false) }.toDoubleArray()
        }.toTypedArray();

        val s = resMtx.joinToString(separator = "\n")
        { i ->
            i.map { j ->
                "$j "
            }
            i.joinToString()
        }

        val program = BimatrixSolver()

        val lrs = LrsAlgorithm()

        val a = convertToRationalArray(resMtx,true)

        val b = convertToRationalArray(resMtx,false)

        val eqs = program.findAllEq(lrs, a, b)

        val choices = state.player1State().getPossiblePlacementsOrdered();

        val mapped = choices.mapIndexed { idx, i -> Pair(eqs[0].probVec1!![idx],i) }

        val total = mapped.map { i -> i.first }.fold(Rational.ZERO,Rational::add)

        if(total != Rational.ONE) {
            throw IllegalStateException("$mapped");
        }

        val obj = mapped.find { i -> Rational.ONE == i.first }
        if(obj != null) return obj.second;
        //TODO apply tie breaker here?

        var r = Rational.valueOf(Math.random());
        for(i in mapped) {
            r = r.subtract(i.first)
            if(r < 0) {
                logger.debug("$state")
                logger.debug("\n$s");
                logger.debug("Result ${i.second} with prob ${i.first} of $mapped");
                return i.second;
            }
        }

        throw IllegalStateException("No choice :o");
    }
}


fun convertToRationalArray(arr: Array<DoubleArray>, invert: Boolean): Array<Array<Rational>> {
    return arr
            .map { inner ->
                inner
                        .map { num -> Rational.valueOf(if (invert) -num else num) }
                        .toTypedArray()
            }
            .toTypedArray()
}