package comparison

import allPermutations
import converters.SimpleState
import converters.readOldSolution
import converters.readSolution
import org.junit.Test
import org.junit.experimental.categories.Category
import org.slf4j.LoggerFactory
import solver.*
import solver.backwardsinduction.BackwardsInductionSolution
import solver.backwardsinduction.PseudoSubGamePerfectAi
import solver.expectedvalue.ExpectedValue
import solver.models.*
import test.ManualTests
import java.io.*



@Category(ManualTests::class)
class FullTest {

    val logger = LoggerFactory.getLogger(FullTest::class.java)

    enum class Type { MIX, DEGENERATE };
    data class StateEntry(val state: State, val type: Type)

    @Test
    fun runTest() {

        val ultimate = BackwardsInductionSolution();
        BufferedReader(FileReader(File("solutions/ultimate/ultimate-latest-with-degen-nodes-2.json"))).use {
            ultimate.readSolutionSer(it);
        }


        val states = mutableSetOf<Set<StateEntry>>();
        val games = mutableSetOf<Pair<Set<StateEntry>, List<Int>>>()

        val start = StateAndHist.empty()
        allPermutations(emptyList(), 1..6, 9) { fullGame ->

            findGames(
                    state = start,
                    ai = ultimate,
                    game = fullGame,
                    hitDegenNodes = emptySet(),
                    store = {
                        if (it.isNotEmpty()) {
                            if (it.map { i -> i.type }.contains(Type.DEGENERATE)) {
                                games.add(Pair(it, fullGame));
                                states.add(it);
                            }
                        }
                    }
            )
        }

        states.forEach {
            if (it.size > 1) {
                val sol = it.map { st -> Pair(st, ultimate.getNode(st.state)) }
                println("Many states: $sol ");
            }
        }

        val mixed = states.map { i -> i.filter { j -> j.type == Type.MIX } }.distinct()

        mixed.forEach {
            println("Unique mixed $it")
        }


        println("With mixed ${states.count { it.size > 1 }}")

        println("Size ${states.size}")

        println("Game ${games.size}")

        val stuff = games.groupBy({ i -> i.first }, { i -> i.second });

        val singleStateGames = stuff[states.first()];

        singleStateGames!!.forEach {
            println("Single ${states.first()} $it")
        }

        var wats = 0;
        states.filter { i -> i.size > 1 && i.map { j -> j.type }.contains(Type.DEGENERATE) }.forEach { st ->

            val gamesForState = stuff[st];

            var winV1 = 0;
            var winV2 = 0;
            var draws = 0;

            gamesForState!!.forEach { game ->
                val a = playOutGame(
                        state = start,
                        game = game,
                        playoutStrategy = { i ->
                            ultimate.play(i) { nd ->
                                nd.choices[0].pos
                            }
                        },
                        allowHistory = false
                )
                ;

                val b = playOutGame(
                        state = start,
                        game = game,
                        playoutStrategy = { i -> ultimate.play(i) { nd -> nd.choices[1].pos } },
                        allowHistory = false
                )

                when {
                    a < b -> {
                        winV1++; }
                    a > b -> {
                        winV2++; }
                    else -> {
                        draws++
                    }
                }
            }

            val first = st.first()
            val left = Math.pow(6.0, first.state.placementsLeft().toDouble());
            println("Win1 $winV1 Win2 $winV2 Draws $draws fromState $left");

            if (winV1 != winV2) {
                println("WAT for ${st.map { j -> j.type }} !!!")
                wats++;
            }
        }

        println("Wats: $wats")

    }


    /**
     * Play out the game using a function to select the best move
     */
    fun findGames(
            state: StateAndHist,
            ai: BackwardsInductionSolution,
            game: List<Int>,
            hitDegenNodes: Set<StateEntry>,
            store: (nodes: Set<StateEntry>) -> Unit): List<Int> {
        if (state.state.isFull()) {
            store(hitDegenNodes)
            return listOf(state.state.stateDistance());
        } else {
            val first = game.first();
            val hist = state.game + first;
            val pl = state.state.newCurrent(first);
            val node = ai.getNode(pl) ?: throw IllegalStateException("No state");

            var nuNodes = hitDegenNodes;

            val picks = when (node) {
                is Final -> {
                    listOf(Pos.FINAL)
                }
                is Strategy -> {
                    if (node.placements.size > 1) {
                        nuNodes = nuNodes + StateEntry(pl, Type.MIX)
                    }
                    node.placements.map { i ->
                        i.pos
                    }
                }
                is DegenerateNode -> {
                    nuNodes = nuNodes + StateEntry(pl, Type.DEGENERATE)
                    node.choices.map { i ->
                        i.pos
                    }
                }
            }

            return picks.flatMap { pick ->
                val nextState = pl.addToPos(pick)
                val next = game.drop(1);
                findGames(StateAndHist(hist, nextState), ai, next, nuNodes, store);
            }
        }
    }


    @Test
    fun compareLatest() {

        val ultimate = BackwardsInductionSolution();
        BufferedReader(FileReader(File("solutions/ultimate/ultimate.json"))).use {
            ultimate.readSolution(it);
        }

        val ultimateOtherEq = BackwardsInductionSolution();
        BufferedReader(FileReader(File("solutions/ultimate/ultimate-latest-with-degen-nodes-2.json"))).use {
            ultimateOtherEq.readSolutionSer(it);
        }


        logger.info("\n\n\nComparing history override based and ultimate")
        (1..5).forEach {
            compareStrategies(
                    st1 = ultimate::play,
                    st2 = ultimateOtherEq::play,
                    detailLog = true,
                    runLog = false,
                    allowHistory = true
            );
        }

    }


    @Test
    @Category(ManualTests::class)
    fun compareBest() {

        val ultimate = BackwardsInductionSolution();
        BufferedReader(FileReader(File("solutions/ultimate/ultimate.json"))).use {
            ultimate.readSolution(it);
        }

        val ultimateOtherEq = BackwardsInductionSolution();
        BufferedReader(FileReader(File("solutions/ultimate/ultimate-latest-5.json"))).use {
            ultimateOtherEq.readSolution(it);
        }


        logger.info("\n\n\nComparing history override based and ultimate")
        (1..5).forEach {
            compareStrategies(
                    st1 = ultimate::play,
                    st2 = ultimateOtherEq::play,
                    detailLog = true,
                    runLog = false,
                    allowHistory = true
            );
        }

    }


    @Test
    fun testSubGamePerfectPayoffPreserv() {
        val ai = PseudoSubGamePerfectAi();
        ai.createSolution()
    }

    @Test
    fun compareOld() {

        val ultimate = BackwardsInductionSolution();
        BufferedReader(FileReader(File("solutions/ultimate/ultimate.json"))).use {
            ultimate.readSolution(it);
        }

        val ultimateOtherEq = BackwardsInductionSolution();
        BufferedReader(FileReader(File("solutions/ultimate/ultimate-latest-4.json"))).use {
            ultimateOtherEq.readSolution(it);
        }

        val hunTenOne = StrategyPlayout(readSolution(
                File("solutions/old/prio-hun-ten-one.json")
        ));

        /*
        val oneTenHun = StrategyPlayout(readSolution(
                File("solutions/old/prio-one-ten-hun.json")
        ));

        val tenHunOne = StrategyPlayout(readSolution(
                File("solutions/old/prio-ten-hun-one.json")
        ));

        val simpleWin = StrategyPlayout(readSolution(
                File("solutions/old/mod-50-one-ten-hun.json")
        ));
        */

        val exOld = readOldSolution(
                File("solutions/old/ai_states_EX.json")
        );


        val oldStored = { s: StateAndHist ->
            val simple = SimpleState(
                    current = s.state.current,
                    hundreds = s.state.plHun,
                    tens = s.state.plTen,
                    ones = s.state.plOne,
                    sum = s.state.stateValue())

            exOld[simple] ?: throw IllegalStateException("No action")
        }

        val expectedValue = ExpectedValue();

        logger.info("\n\n\n Comparing ex and backwards induction")
        compareStrategies(
                st1 = expectedValue::runExpectedValueStrategy,
                st2 = ultimate::play,
                detailLog = true,
                runLog = false,
                allowHistory = false
        );

        logger.info("\n\n\n Comparing ex and stored ex based")
        compareStrategies(
                st1 = expectedValue::runExpectedValueStrategy,
                st2 = oldStored,
                detailLog = true,
                runLog = false,
                allowHistory = false
        );

        logger.info("\n\n\n Comparing stored expected value based and backwards induction")
        compareStrategies(
                st1 = oldStored,
                st2 = ultimate::play,
                detailLog = true,
                runLog = false,
                allowHistory = false
        );



        logger.info("\n\n\nComparing history override based and ultimate")
        (1..5).forEach {
            //val writer = BufferedWriter(FileWriter(File("solutions/extras/u1-vs-u2-s1.txt")))
            //val writer2 = BufferedWriter(FileWriter(File("solutions/extras/e1-vs-u2-s2.txt")))
            //writer.use { wr1 ->
            // writer2.use { wr2 ->
            compareStrategies(
                    st1 = ultimate::play,
                    st2 = ultimateOtherEq::play,
                    detailLog = true,
                    runLog = false,
                    allowHistory = true
                    /*resultCallbackS1 = { _, v -> wr1.write("$v\n") },
                    resultCallbackS2 = { _, v -> wr2.write("$v\n") }*/
            );
            // }
            //}
        }

        /*
        val winWriter = BufferedWriter(FileWriter(File("solutions/extras/ex-vs-ul-s1.txt")))
        val winWriter2 = BufferedWriter(FileWriter(File("solutions/extras/ex-vs-ul-s2.txt")))

        logger.info("\n\n\n Comparing history override based and ultimate")
        winWriter.use { wr1 ->
            winWriter2.use { wr2 ->
                compareStrategies(
                        st1 = oldStored,
                        st2 = ultimate::play,
                        detailLog = true,
                        runLog = false,
                        allowHistory = true,
                        winCallBackS1 = { _, v -> wr1.write("$v\n") },
                        winCallBackS2 = { _, v -> wr2.write("$v\n") }
                );
            }
        }
        */

    }
}