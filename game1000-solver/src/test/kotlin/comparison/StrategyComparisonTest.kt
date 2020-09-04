package comparison

import allPermutations
import converters.prettyFormatToOld
import converters.readPrettyformat
import converters.readSolution
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.categories.Category
import org.slf4j.LoggerFactory
import solver.*
import solver.mcts.ActionResult
import solver.mcts.mctsForNode
import solver.expectedvalue.ExpectedValue
import solver.mcts.simpleEarlyEnd
import solver.minimax.SimultaneousExpectiMinimax
import solver.models.*
import test.ManualTests
import utils.pickRandom

import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt
import kotlin.random.Random


@Category(ManualTests::class)
class StrategyComparisonTest {

    private val logger = LoggerFactory.getLogger(StrategyComparisonTest::class.java)

    /**
     * A strategy which places stuff randomly.
     * Used to compare other strategies for badness.
     * If you cannot beat this HARD something is wrong…
     */
    fun createRandomStrat(data : Set<State>) : Map<State, Placement> {
        val rd = Random(Instant.now().toEpochMilli());
        return data
                .filter { i->!i.isFull() }
                .map{ i->
                    val free = i.getPossiblePlacements();
                    Pair(i, Placement(listOf(rd.pickRandom(free)),0,0,0, Resolver(emptyMap())))}
                .toMap()
    }

    /**
     *
     */
    @Test
    @Ignore
    fun testMCTSWithStrat() {

        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val random = Random(98987066)
        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val miniMax = ExpectedValue();

        val randomPlayout = {i:List<ActionResult<State>> -> random.pickRandom(i)}
        val strategyPlayout = {i:List<ActionResult<State>> -> playout.strategyPlayout(i)}

        val outPut = HashMap<State, Placement>()
        var nr = 0;

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
                    logger.info("Run nr: $nr")
                }
            }
        }

        compareStrategies(
                st1=playout::placementForState,
                st2=miniMax::runExpectedValueStrategy,
                detailLog = true
        );

        compareStrategies(
                st1=playout::placementForState,
                st2={i->outPut[i.state]!!.placements.first()},
                detailLog = true);

        compareStrategies(
                st1=miniMax::runExpectedValueStrategy,
                st2={i->outPut[i.state]!!.placements.first()},
                detailLog = true
        );




        compareStrategies(
                st1=playout::placementForState,
                st2={ st -> random.pickRandom(st.state.getPossiblePlacements()) },
                detailLog = true);

        compareStrategies(
                st1={i->outPut[i.state]!!.placements.first()},
                st2={i-> random.pickRandom(i.state.getPossiblePlacements())},
                detailLog = true);

        compareStrategies(
                st1=miniMax::runExpectedValueStrategy,
                st2={i-> random.pickRandom(i.state.getPossiblePlacements())},
                detailLog = true);
    }








    /**
     *
     */
    @Test
    fun testOtherStrats() {

        val random = Random(87087857445)

        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val simpleOld = readSolution(
                File("solutions/old/prio-hun-ten-one.json")
        );

        val playoutOld = StrategyPlayout(simpleOld);

        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val miniMax = ExpectedValue();

        val expecti = SimultaneousExpectiMinimax();

        val start = StateAndHist(
                emptyList(),
                State.emptyState())
                .runRandomTurns(3)

        println("StartState: $start");

        compareStrategies(
                st1=expecti::runMiniMaxStrategy,
                st2=miniMax::runExpectedValueStrategy,
                detailLog = true,
                loops = 5,
                specialStart = start
        );


        compareStrategies(
                st1=playout::placementForState,
                st2=playoutOld::placementForState,
                detailLog = true,
                loops = 5,
                specialStart = start
        );


        compareStrategies(
                st1=playoutOld::placementForState,
                st2=expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 5,
                specialStart = start
        );


        compareStrategies(
                st1=playout::placementForState,
                st2=expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 5,
                specialStart = start
        );


        compareStrategies(
                st1=expecti::runMiniMaxStrategy,
                st2={i-> random.pickRandom(i.state.getPossiblePlacements())},
                loops = 5,
                detailLog = true,
                specialStart = start);
    }





    /**
     *
     */
    @Test
    fun testSpecific() {

        val random = Random(2156436)

        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val simpleOld = readSolution(
                File("solutions/old/prio-hun-ten-one.json")
        );

        val playoutOld = StrategyPlayout(simpleOld);

        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val expectedValue = ExpectedValue();

        val expecti = SimultaneousExpectiMinimax();

        val start = StateAndHist.setupGameState(3,
                Pair(Pos.HUNDREDS,2),
                Pair(Pos.HUNDREDS,3),
                Pair(Pos.ONES,6),
                Pair(Pos.TENS,3),
                Pair(Pos.TENS,4),
                Pair(Pos.TENS,4)
        );

        println("StartState: $start");

        compareStrategies(
                st1=expecti::runMiniMaxStrategy,
                st2=expectedValue::runExpectedValueStrategy,
                detailLog = true,
                loops = 3,
                specialStart = start
        );


        compareStrategies(
                st1=playout::placementForState,
                st2=playoutOld::placementForState,
                detailLog = true,
                loops = 3,
                specialStart = start
        );


        compareStrategies(
                st1=playoutOld::placementForState,
                st2=expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 3,
                specialStart = start
        );


        compareStrategies(
                st1=playout::placementForState,
                st2=expecti::runMiniMaxStrategy,
                detailLog = true,
                loops = 3,
                specialStart = start
        );


        compareStrategies(
                st1=expecti::runMiniMaxStrategy,
                st2={ i -> random.pickRandom(i.state.getPossiblePlacements())},
                loops = 3,
                detailLog = true,
                specialStart = start);
    }



    /**
     *
     */
    @Test
    fun testSpecificHistorySolution() {

        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val simpleOld = readSolution(
                File("solutions/old/prio-one-ten-hun.json")
        );

        val playoutOld = StrategyPlayout(simpleOld);

        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val start = StateAndHist.setupGameState(3,
                Pair(Pos.ONES,6),
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.TENS,5),
                Pair(Pos.TENS,4),
                Pair(Pos.HUNDREDS,1),
                Pair(Pos.TENS,2),
                )
        println("StartState: $start");

        compareStrategies(
                st1=playout::placementForState,
                st2=playoutOld::placementForState,
                detailLog = true,
                loops = 3,
                allowHistory = true,
                specialStart = start
        );

        compareStrategies(
                st1=playout::placementForState,
                st2=playoutOld::placementForState,
                detailLog = true,
                loops = 3,
                allowHistory = false,
                specialStart = start
        );

    }



    /**
     * This is crazy, the history thing seems to win more matches even though it makes
     * little sense further down in the hierarchy.
     */
    @Test
    fun testFullSolutions() {

        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val simpleOld = readSolution(
                File("solutions/old/prio-one-ten-hun.json")
        );

        val playoutOld = StrategyPlayout(simpleOld);

        val converted = prettyFormatToOld(format) as HashMap;
        val playout = StrategyPlayout(converted);

        compareStrategies(
                st1=playout::placementForState,
                st2=playoutOld::placementForState,
                detailLog = true,
                loops = 9,
                allowHistory = true
        );

        val p = converted[State(3,5,11,6,2,3,1)] as Placement;
        val nup = Placement(p.placements,p.winsHundred,p.winsTen,p.winsOne, Resolver(emptyMap()));
        converted[State(3,5,11,6,2,3,1)] = nup;
        val playout2 = StrategyPlayout(converted);

        compareStrategies(
                st1=playout2::placementForState,
                st2=playoutOld::placementForState,
                detailLog = true,
                loops = 9,
                allowHistory = true
        );

    }


    @Test
    fun findStuff() {

        val format = readPrettyformat(
                File("solutions/history/best-solution-nice-format.json")
        );

        val converted = prettyFormatToOld(format);
        val playout = StrategyPlayout(converted);

        val state = State(3,5,11,6,2,3,1);

        /**
         * Play out the game using a given function to select the best move
         */
        fun specialPlay(
                st : StateAndHist,
                playoutStrategy: (StateAndHist) -> Pos,
                random: List<Int>,
                allowHistory: Boolean,
                histories : MutableList<Pair<StateAndHist, StateAndHist>>,
                previous : StateAndHist) : List<Pair<StateAndHist, StateAndHist>> {
            if(st.state.isFull()){
                return histories;
            }
            else {
                val first = random.first();
                val hist = st.game+first;
                val pl = st.state.newCurrent(first);
                if(pl == state) {
                    println("$hist $pl");
                    histories.add(Pair(st,previous));
                }
                val pick = playoutStrategy(StateAndHist(if(allowHistory) hist else emptyList() ,pl));
                val nextState = pl.addToPos(pick)
                val next = random.drop(1);
                return specialPlay(StateAndHist(hist,nextState),playoutStrategy,next,allowHistory,histories,st);
            }
        }

        val histories = HashSet<Pair<StateAndHist, StateAndHist>>();

        allPermutations(emptyList(), 1..6, 9) { fullGame ->
            val res = specialPlay(StateAndHist.empty(), playout::placementForState, fullGame, true, mutableListOf(), StateAndHist.empty());
            histories.addAll(res);
        };

        println(histories);
    }
}