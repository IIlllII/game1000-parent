package solver

import solver.mcts.ActionResult
import solver.models.Placement
import solver.models.Pos
import solver.models.State
import solver.models.StateAndHist
import kotlin.math.abs


/**
 * The heuristic which we play out the game using.
 */
fun simpleReduction(aR:ActionResult<State>, bR:ActionResult<State>) : ActionResult<State> {

    val a = aR.result;
    val b = bR.result;

    val da = a.maximumResult()-a.minimumResult();
    val db = b.maximumResult()-b.minimumResult();

    val exa = abs(1000-(a.minimumResult() + da/2));
    val exb = abs(1000-(b.minimumResult() + db/2));

    val oa = exa + da/(10-a.placements());
    val ob = exb + db/(10-b.placements());

    return if(oa < ob) {
        aR;
    } else {
        bR;
    }
}


/**
 * Play out the game using a predefined strategy
 */
class StrategyPlayout(val strat: Map<State, Placement>) {

    fun strategyPlayout(states : List<ActionResult<State>>) : ActionResult<State> {
        val res = strat[states[0].startState];
        return states.first { i -> res!!.placements.contains(i.placement) }
    }

    fun placementForState(st : StateAndHist) : Pos {
        val vs = strat[st.state];
        if(vs != null) {
            val hist = vs.eqResolver.map[st];
            if(hist != null) {
                return hist;
            }

            return vs.placements.first();
        }
        else {
            throw IllegalStateException("No placement for $st");
        }
    }
}
