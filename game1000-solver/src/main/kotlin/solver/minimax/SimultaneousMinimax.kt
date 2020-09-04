package solver.minimax

import org.slf4j.LoggerFactory
import solver.models.Pos
import solver.models.State
import solver.models.StateAndHist
import solver.tiebreaker.TieBreakers


fun Iterable<PayOff>.sum() : PayOff {
    return this.reduce{a,b -> a+b};
}


interface PayOff : Comparable<PayOff> {

    operator fun plus(payOff: PayOff) : PayOff;

    fun value() : Double;
}


data class Choices(val pl1 : Pos, val pl2 : Pos, val state : SimState) {}


interface SimState {

    fun nextPossibleStates() : List<Choices>;

    fun possibilitiesMatrix() : Array<Array<Choices>>;

    fun movesByNature() : List<SimState>;

    fun payoff() : Double;

    fun isEnded() : Boolean;

    fun player1State() : State;

    fun player2State() : State;

}


/**
 * Leverages expectiminimax, but uses only pure strategies
 */
class SimultaneousExpectiMinimax(val useCache : Boolean = true) {

    private val logger = LoggerFactory.getLogger(SimultaneousExpectiMinimax::class.java)
    /**
     * Cache results for nodes, so calculation is fast
     */
    var cache = HashMap<SimState,Pair<Double,Int>>(5000000);

    /**
     * Simple fair flushing. Assumes that new values are better then old ones.
     * TODO use a LRU instead
     */
    fun flushCache() {
        cache = HashMap(5000000);
    }


    private fun miniMax(st : SimState, depth: Int, placement : Boolean): Double {
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

            val runNext = next.flatten()
                    .map { ch -> Pair(ch,miniMax(ch.state, depth - 1,false)) };

            val choicesPl2 = runNext.groupBy { i->i.first.pl2 }
                    .entries
                    .map{ i -> Pair(i.key, i.value.map { res->res.second }.minOrNull() ?: 0.0)}

            val max = choicesPl2.map { i->i.second }.maxOrNull() as Double;

            //val maxList = choicesPl2.filter { i->i.second==max };

            val c2 = choicesPl2.find { i->i.second == max }!!.first;

            val result = runNext.filter { i->i.first.pl2==c2};

            val min = result.map { i->i.second }.minOrNull() as Double;

            val choice = result.find { i->i.second==min };

            if(useCache) {
                if(cache.size > 5000000) {
                    flushCache();
                }
                cache[st] = Pair(min,1);
            }
            return min;
        }
    }

    data class ExpectiminimaxResult(val pos: SimState, val result: Double);


    /**
     * Run minimax and cache results for hit nodes
     */
    fun runMiniMaxStrategy(stateAndHist : StateAndHist, priority : (List<Pos>) -> Pos = TieBreakers.PRIO_HUN_TEN_ONE) : Pos {
        return runMiniMaxStrategySim(Get1000SimState(stateAndHist.state,stateAndHist.state),priority);
    }

    /**
     * Run minimax and cache results for hit nodes
     */
    fun runMiniMaxStrategySim(state : SimState, priority : (List<Pos>) -> Pos = TieBreakers.PRIO_HUN_TEN_ONE) : Pos {
        val pl = state.possibilitiesMatrix();

        val result = pl.flatten().map { pos ->
            Pair(pos,this.miniMax(pos.state, 30,false))
        }

        val resMtx = pl.map { row ->
             row.map { col -> this.miniMax(col.state,30,false) }.toDoubleArray()
        }.toTypedArray();


        //if(resMtx.size > 1 ) {
            //println(resMtx.size)
        //}


        //val choiceMtx = state.possibilitiesMatrix();
        //choiceMtx.mapIndexed {i,oj-> oj.mapIndexed {j,choice -> result.  }}


        //val g1 = result.groupBy { i->i.first.pl1 };
        //val g2 = result.groupBy { i->i.first.pl2 };

        //val dbr = Array<DoubleArray>(g1.size,{j->DoubleArray(g2.size,{})



        //This looks for strategies needing a MIXED solution which will produce an different Expected value
        val grouped = result.groupBy { i->i.first.pl2 };
        val ex = grouped.entries.map { i -> val a = i.value.find { j->j.second < 0.0 }; a != null }
        val all = ex.fold(true) { a, b->a && b};

        val grouped2 = result.groupBy { i->i.first.pl1 };
        val ex2 = grouped2.entries.map { i -> val a = i.value.find { j->j.second > 0.0 }; a != null }
        val all2 = ex2.fold(true) { a, b->a && b};
        if(all && all2 && result.size >=4) {
            logger.info("We got a hit at toplevel");
        }



        val choicesPl2 = result.groupBy { i->i.first.pl2 }
                .entries
                .map{ i-> Pair(i.key, i.value.map { res->res.second }.minOrNull() ?: 0.0)}

        val max = choicesPl2.map { i->i.second }.maxOrNull() as Double;
        val c2 = choicesPl2.find { i->i.second == max }!!.first;
        val final = result.filter { i->i.first.pl2 == c2};
        val min = final.map { i->i.second }.min() as Double;

        val choice = final.filter { i->i.second==min }.map { i->i.first.pl1 };

        //println("Result ${result}");
        //println("Choice ${choice!!.first.pl1} based on $final");
        //println("Value ${choice.second}");
        return priority(choice)
    }
}