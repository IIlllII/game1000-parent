package runners

import converters.prettyFormatToOld
import converters.readPrettyformat
import org.slf4j.LoggerFactory
import solver.StrategyPlayout
import solver.compareStrategies
import solver.expectedvalue.ExpectedValue
import solver.mcts.ActionResult
import solver.mcts.mctsForNode
import solver.mcts.simpleEarlyEnd
import solver.models.Placement
import solver.models.Resolver
import solver.models.State
import utils.pickRandom
import java.io.File
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Copyright (c) Jonas Waage 02/09/2020
 */

fun main() {
    val logger = LoggerFactory.getLogger("ExpectedValue-vs-MCTS")

    val expectedValue = ExpectedValue();

    val format = readPrettyformat(
            File("game1000-solver/solutions/history/best-solution-nice-format.json")
    );
    val converted = prettyFormatToOld(format);
    val playout = StrategyPlayout(converted);


    val random = Random(214324242342) //Make deterministic
    val randomPlayout = {i:List<ActionResult<State>> -> random.pickRandom(i)}

    //Can be used as a different playout for the MCTS
    val strategyPlayout = {i:List<ActionResult<State>> -> playout.strategyPlayout(i)}

    val outPut = HashMap<State, Placement>()
    var nr = 0;

    /*
    This is sort of a hack, I run 1000 rounds of MCTS for each node, and store the result
    in a map, and then use
    TODO run a round with lots of MCTS rounds (maybe scaled to depth) and store the result
    I have a feeling something is wrong with the MCTS stuff though, as it gets stomped!
     */
    converted.keys.forEach { state ->
        if (!state.isFull()) {
            val pos = mctsForNode(
                    random = random,
                    root = state,
                    rounds = 1000,
                    log = false,
                    exploreVSExploit = 0.25* sqrt(2.0),
                    earlyEnd = ::simpleEarlyEnd,
                    strat = randomPlayout);

            outPut[state] = Placement(listOf(pos), 0, 0, 0, Resolver(emptyMap()));

            nr++
            if (nr % 1000 == 0) {
                logger.info("Progress, at nr: $nr")
            }
        }
    }

    logger.info("Running comparison of expected value based strategy and MCTS based strategy: ")
    compareStrategies(
            st1 = expectedValue::runExpectedValueStrategy,
            st2 = {i->outPut[i.state]!!.placements.first()},
            detailLog = true,
            runLog = false,
            allowHistory = false
    );
}