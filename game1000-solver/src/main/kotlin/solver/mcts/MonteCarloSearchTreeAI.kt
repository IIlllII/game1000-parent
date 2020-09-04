package solver.mcts

import org.slf4j.LoggerFactory
import solver.models.Pos
import solver.models.State
import utils.diceRoll
import utils.pickRandom
import kotlin.random.Random

private val logger = LoggerFactory.getLogger("MCTS")

class SelectionResult<T>(val rollsLeft : List<Int> ,val node : MCTSTree<T>)

/**
 * Select leaf node
 */
fun <T> doSelection(random: Random,current : SelectionResult<T> ,simulations : Long,constant : Double) : SelectionResult<T> {
    if(current.node.isLeaf()) {
        return current;
    }
    if(!current.node.decision) { //If the node has no decision
        val first = current.rollsLeft.first()-1;
        val next = SelectionResult(current.rollsLeft.drop(1),current.node.children[first]);
        return doSelection(random,next,simulations,constant);
    }
    val nodes = current.node.selectChildren(constant, simulations);
    val node = random.pickRandom(nodes); //Select random node if several
    return doSelection(random,SelectionResult(current.rollsLeft,node),simulations,constant); //Continue selecting
}

/**
 * Expand and pick a random node for simulation
 */
fun expansion(random: Random,current : SelectionResult<State>) : SelectionResult<State> {
    if(current.node.value.isFull()) {
        return current;
    }
    else {
        for (j in current.node.value.getPossiblePlacementsOrdered()) {
            val nextState = current.node.value.addToPos(j);
            val node = MCTSTree(nextState, current.node, mutableListOf());
            node.decision = false;
            for (roll in 1..6) {
                val st = nextState.newCurrent(roll);
                val plNode = MCTSTree(st, node, mutableListOf());
                node.addLeaf(plNode);
            }
            current.node.addLeaf(node);
        }
        val selectChoice = random.pickRandom(current.node.children);
        val selectNextGame = current.rollsLeft.first()-1;
        val rollsLeft = current.rollsLeft.drop(1);
        return SelectionResult(rollsLeft,selectChoice.children[selectNextGame]);
    }
}


data class ActionResult<T>(val placement: Pos, val startState : T, val result :T)


/**
 * Play out the game using a function to select the best move
 */
fun mctsPlayoutGame(
        st : State,
        playoutStrategy: (List<ActionResult<State>>) -> ActionResult<State>,
        moves : List<Int>) : Int {
    if(st.isFull()){
        return st.stateDistance();
    }
    else {

        val minPl = st.getPossiblePlacements()
                .map{ i -> ActionResult(placement = i,
                        startState = st,
                        result = st.addToPos(i)) }

        val pick = playoutStrategy(minPl);

        val first = moves.first();
        val next = moves.drop(1);
        return mctsPlayoutGame(pick.result.newCurrent(first),playoutStrategy,next);
    }
}


/**
 * Modify all nodes on the path with the result
 */
fun backPropagation(node : MCTSTree<State>, result : Double) {
    if(node.parent == null) {
        node.addResult(result);
    }
    else {
        node.addResult(result);
        backPropagation(node.parent,result);
    }
}

/**
 * Runs montecarlo tree search the given amount of rounds
 */
fun mctsForNode(random : Random,
                root: State,
                rounds: Int,
                log: Boolean,
                exploreVSExploit: Double,
                earlyEnd: (MCTSTree<State>) -> Boolean = { false },
                strat: (List<ActionResult<State>>) -> ActionResult<State> = { i -> random.pickRandom(i) }): Pos {

    val treeRoot1 = MCTSTree(root, null, mutableListOf());
    val treeRoot2 = MCTSTree(root, null, mutableListOf());

    val strat1 = Strat(treeRoot1, strat, exploreVSExploit);
    val strat2 = Strat(treeRoot2, strat, exploreVSExploit);

    val node = mctsForRoot(
            random,
            listOf(strat1, strat2),
            rounds,
            earlyEnd);

    if (log) {
        logger.info("Root $treeRoot1");
    }
    return root.getPositionChange(node.value);
}


class Strat(val root : MCTSTree<State>,
            val strat : (List<ActionResult<State>>) -> ActionResult<State>,
            val constant: Double) {

    fun runSimulation(random: Random,rolls:List<Int>) : Pair<MCTSTree<State>,Int> {
        val node = doSelection(random,SelectionResult(rolls,root),root.simulations,constant);
        val expandedNode = expansion(random,node); //Expand and return list with expansion
        val result = mctsPlayoutGame(expandedNode.node.value,strat,rolls);
        return Pair(expandedNode.node,result);
    }

}

/**
 * Run MCTS for the given amount of rounds, with the given
 * strategy for playout.
 *
 * The strategy for playout determines how the nodes from
 * the playout are chosen.
 */
fun mctsForRoot(
        random : Random,
        roots : List<Strat>,
        rounds:Int,
        earlyEnd : (MCTSTree<State>) -> Boolean) : MCTSTree<State> {

    assert(roots.size > 1);
    val set = roots.map { i->i.root.value.placementsLeft() }.toSet()
    assert(set.size == 1);
    val left = set.first();

    for(i in 1..rounds) {
        val rolls = (1..left).map { random.diceRoll() }
        val results = roots.map { ro -> ro.runSimulation(random,rolls) };
        val min = results.map { res -> res.second }.minOrNull();

        results.forEach { res ->
            val win = if (res.second == min) 1.0 else 0.0;
            backPropagation(res.first, win);
        }

        val end = roots.all { rt -> earlyEnd(rt.root) };
        if(end) {
            break;
        }
    }

    val max = roots.first().root.children.map { i -> i.simulations }.reduce(Math::max);
    val states = roots.first().root.children.filter { i -> i.simulations == max };
    return random.pickRandom(states);
}



/**
 * Terminate early for nodes where there is a no doubt for the best action after X runs
 * according to the MCTS.
 */
fun simpleEarlyEnd(tree: MCTSTree<State>): Boolean {
    if (tree.children.size < 2){
        return true; //No choices, no point in going on
    }
    return if (tree.simulations < 750) {
        false; //Simulation min limit
    }
    else {
        val a = tree.children.map { child -> child.simulations }.sorted().reversed();
        val diff = (a[0]/tree.simulations.toDouble()) - (a[1]/tree.simulations.toDouble());
        diff > 0.5
    }
}
