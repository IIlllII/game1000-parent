package comparison

import converters.*
import org.junit.Test
import org.junit.experimental.categories.Category
import org.slf4j.LoggerFactory
import solver.*
import solver.minimax.SimultaneousExpectiMinimax
import solver.minimax.SimultaneousExpectiMinimaxMixed
import solver.models.Placement
import solver.models.Pos
import solver.models.State
import solver.models.StateAndHist
import test.ManualTests
import java.io.*
import kotlin.test.assertEquals


@Category(ManualTests::class)
class SpecificScenarioTest {

    private val logger = LoggerFactory.getLogger(SpecificScenarioTest::class.java)

    @Test
    fun testSpecific6MovesPlayed() {
        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val simpleOld = readSolution(
                File("solutions/old/prio-hun-ten-one.json")
        );

        val playoutOld = StrategyPlayout(simpleOld);

        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val expecti = SimultaneousExpectiMinimax();

        val start = StateAndHist.setupGameState(3,
                Pair(Pos.HUNDREDS,3),
                Pair(Pos.HUNDREDS,2),
                Pair(Pos.ONES,6),
                Pair(Pos.TENS,4),
                Pair(Pos.TENS,4),
                Pair(Pos.TENS,3)
        )

        val pl = simpleOld[start.state];

        logger.debug("Strategy Init" +pl);

        logger.debug("StartState: $start");

        compareStrategies(
                st1=playout::placementForState,
                st2=expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 3,
                specialStart = start,
                runLog = true,
                allowHistory = true
        );

        compareStrategies(
                st1=playoutOld::placementForState,
                st2=expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 3,
                specialStart = start,
                runLog = true,
                allowHistory = true
        );

    }


    @Test
    fun testSpecific5MovesPlayed() {
        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );


        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val expecti = SimultaneousExpectiMinimax();
        val expectiMixed = SimultaneousExpectiMinimaxMixed();

        val start = StateAndHist.setupGameState(3,
                Pair(Pos.HUNDREDS,3),
                Pair(Pos.HUNDREDS,2),
                Pair(Pos.ONES,6),
                Pair(Pos.TENS,4),
                Pair(Pos.TENS,5)
        )

        logger.debug("StartState: $start");

        compareStrategies(
                st1=expectiMixed::runMiniMaxStrategyMixed,
                st2=expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 4,
                specialStart = start,
                runLog = true,
                allowHistory = true
        );

    }

    @Test
    fun testMixedfail() {
        val expecti = SimultaneousExpectiMinimax(useCache = true);

        val expectiMixed = SimultaneousExpectiMinimaxMixed(useCache = true);

        val start = StateAndHist.setupGameState(4,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.ONES,6),
                Pair(Pos.TENS,5),
                Pair(Pos.TENS,4)
        )

        for(i in 1..1) {
            compareStrategies(
                    st1 = expectiMixed::runMiniMaxStrategyMixed,
                    st2 = expecti::runMiniMaxStrategy,
                    detailLog = true,
                    loops = 5,
                    specialStart = start,
                    runLog = false,
                    allowHistory = true
            );
        }
    }



    //Big test
    @Test
    fun testSpecificThreeLevelsUp() {

        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val simpleOld = readSolution(
                File("solutions/old/prio-hun-ten-one.json")
        );

        val playoutOld = StrategyPlayout(simpleOld);

        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val expecti = SimultaneousExpectiMinimax(useCache = true);

        val expectiMixed = SimultaneousExpectiMinimaxMixed(useCache = true);

        val start = StateAndHist.setupGameState(4,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.ONES,6),
                Pair(Pos.TENS,5)
        )

        val pl = simpleOld[start.state];

        logger.info("Strategy Init$pl");

        logger.info("StartState: $start");

        for(i in 1..1) {
            compareStrategies(
                    st1 = expectiMixed::runMiniMaxStrategyMixed,
                    st2 = expecti::runMiniMaxStrategy,
                    detailLog = true,
                    loops = 6,
                    specialStart = start,
                    runLog = false,
                    allowHistory = true
            );
        }

        compareStrategies(
                st1 = playoutOld::placementForState,
                st2 = expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 6,
                specialStart = start,
                runLog = false,
                allowHistory = true
        );


        compareStrategies(
                st1 = playoutOld::placementForState,
                st2 = playout::placementForState,
                detailLog = true,
                loops = 6,
                specialStart = start,
                runLog = false,
                allowHistory = true
        );
    }






    @Test
    fun compareNash() {


        val simpleWin = readSolution(
                File("solutions/old/mod-50-one-ten-hun.json")
        );

        val playoutWin = StrategyPlayout(simpleWin);

        val state1 = State.setupGameState(2,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.HUNDREDS,3),
                Pair(Pos.ONES,6),
                Pair(Pos.ONES,4),
                Pair(Pos.TENS,5),
                Pair(Pos.TENS,2)
        )

        val state2 = State.setupGameState(2,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.HUNDREDS,3),
                Pair(Pos.ONES,6),
                Pair(Pos.ONES,4),
                Pair(Pos.TENS,5),
                Pair(Pos.ONES,2)
        )


        val possible1 = state1.getPossiblePlacementsOrdered()
        val possible2 = state2.getPossiblePlacementsOrdered()

        val pairs = possible1.flatMap { i-> possible2.map { j -> Pair(i,j) } }

        val out = pairs.map { origPair ->

            logger.info("Pair -> $origPair")
            val n1 = state1.addToPos(origPair.first)
            val n2 = state2.addToPos(origPair.second)

            val rolls = (1..6).flatMap { i -> (1..6).map { j -> Pair(i, j) } }

            val res = rolls.map { rollPair ->

                val m1 = n1.newCurrent(rollPair.first)
                val m2 = n2.newCurrent(rollPair.first)

                logger.info("$rollPair --- ${m1.stateValue()} : ${m2.stateValue()}")

                val pos = playoutWin.strat[m1] as Placement
                val pos2 = playoutWin.strat[m2] as Placement

                val l1 = m1.addToPos(pos.placements.first())
                val l2 = m2.addToPos(pos2.placements.first())

                logger.info("$rollPair --- ${l1.stateValue()} : ${l1.stateValue()}")

                val k1 = l1.newCurrent(rollPair.second)
                val k2 = l2.newCurrent(rollPair.second)

                val nPos = playoutWin.strat[k1] as Placement
                val nPos2 = playoutWin.strat[k2] as Placement

                val j1 = k1.addToPos(nPos.placements.first())
                val j2 = k2.addToPos(nPos2.placements.first())

                if(!j1.isFull() || !j2.isFull()) {
                    throw IllegalStateException("They should be finished")
                }

                val num = if(j1.stateDistance() < j2.stateDistance()) {
                    1
                }
                else if(j1.stateDistance() > j2.stateDistance()) {
                    -1
                }
                else {
                    0
                }

                logger.info("$rollPair --- ${j1.stateValue()} : ${j2.stateValue()} $num")
                num
            }

            logger.info("Pair $origPair, sum ${res.sum()}")

            Pair(origPair,res.sum())
        }

        out.forEach {
            logger.info("${it.first.first}:${it.first.second} = ${it.second}")
        }

    }

    @Test
    fun levelAbove() {
        val simpleWin = readSolution(
                File("solutions/old/mod-50-one-ten-hun.json")
        );

        val playoutWin = StrategyPlayout(simpleWin);

        val state1 = State.setupGameState(2,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.HUNDREDS,3),
                Pair(Pos.ONES,6),
                Pair(Pos.ONES,4),
                Pair(Pos.TENS,5)
        )

        val vl = playoutWin.strat[state1]

        assertEquals(Pos.HUNDREDS,vl!!.placements.first())

        logger.info("$vl")
    }

    @Test
    fun testSpecificSmall() {

        /*val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );*/

        //val converted = prettyFormatToOld(format);
        //val playout = StrategyPlayout(converted);

        val expecti = SimultaneousExpectiMinimax();
        val expectiMixed = SimultaneousExpectiMinimaxMixed();

        val start = StateAndHist.setupGameState(2,
                Pair(Pos.HUNDREDS,5),
                Pair(Pos.ONES,4),
                Pair(Pos.ONES,4),
                Pair(Pos.TENS,1),
                Pair(Pos.TENS,5),
                Pair(Pos.TENS,5)
        )

        logger.info("StartState: $start");

        compareStrategies(
                st1 = expectiMixed::runMiniMaxStrategyMixed,
                st2 = expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 2,
                specialStart = start,
                runLog = true,
                allowHistory = true
        );

    }



    @Test
    fun testSpecificSmallMinMaxNu() {
        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val nonMixed = SimultaneousExpectiMinimax()
        val expecti = SimultaneousExpectiMinimaxMixed();

        val start = StateAndHist.setupGameState(1,
                Pair(Pos.HUNDREDS,3),
                Pair(Pos.ONES,5),
                Pair(Pos.TENS,5),
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.TENS,1)
        )

        logger.info("StartState: $start");

        compareStrategies(
                st1=nonMixed::runMiniMaxStrategy,
                st2=expecti::runMiniMaxStrategyMixed,
                detailLog = true,
                loops = 4,
                specialStart = start,
                runLog = true,
                allowHistory = true
        );

    }



}