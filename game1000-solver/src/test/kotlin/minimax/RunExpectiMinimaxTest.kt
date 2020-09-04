package minimax

import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.categories.Category
import solver.minimax.Get1000SimState
import solver.expectedvalue.ExpectedValue
import solver.minimax.SimultaneousExpectiMinimax
import solver.models.Pos
import solver.models.State
import solver.models.StateAndHist
import test.ManualTests
import utils.diceRoll
import utils.pickRandom
import kotlin.random.Random
import kotlin.test.assertEquals


@Category(ManualTests::class)
class TestExpectiMinimax {

    @Test
    fun expectiminimaxSmall() {
        val mx = SimultaneousExpectiMinimax();

        val st = State.setupGameState(0,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.HUNDREDS,5),
                Pair(Pos.ONES,2),
                Pair(Pos.ONES,2),
                Pair(Pos.TENS,2),
                Pair(Pos.TENS,2),
                Pair(Pos.TENS,2)
        )

        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(1),st.newCurrent(1))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(2),st.newCurrent(2))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(3),st.newCurrent(3))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(4),st.newCurrent(4))), Pos.ONES)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(5),st.newCurrent(5))), Pos.ONES)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(6),st.newCurrent(6))), Pos.ONES)
    }

    @Test
    @Ignore //Too slow not ablse to solve this :(
    fun expectiminimaxFull() {
        val mx = SimultaneousExpectiMinimax();

        val st = State.emptyState();

        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(1),st.newCurrent(1))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(2),st.newCurrent(2))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(3),st.newCurrent(3))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(4),st.newCurrent(4))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(5),st.newCurrent(5))), Pos.TENS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(6),st.newCurrent(6))), Pos.TENS)
    }


    @Test
    fun miniMaxMedium() {
        val mx = SimultaneousExpectiMinimax();

        val st = State.setupGameState(0,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.ONES,4),
                Pair(Pos.TENS,1),
                Pair(Pos.TENS,5)
        )

        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(1),st.newCurrent(1))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(2),st.newCurrent(2))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(3),st.newCurrent(3))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(4),st.newCurrent(4))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(5),st.newCurrent(5))), Pos.ONES)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(6),st.newCurrent(6))), Pos.ONES)
    }




    @Test
    fun miniMaxMedium2() {
        val mx = SimultaneousExpectiMinimax(false);

        val st = State.setupGameState(0,
                Pair(Pos.HUNDREDS,2),
                Pair(Pos.HUNDREDS,2),
                Pair(Pos.TENS,6)
        )

        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(1),st.newCurrent(1))), Pos.ONES)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(2),st.newCurrent(2))), Pos.ONES)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(3),st.newCurrent(3))), Pos.ONES)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(4),st.newCurrent(4))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(5),st.newCurrent(5))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(6),st.newCurrent(6))), Pos.ONES)
    }

    @Test
    @Ignore //Slow
    fun miniMaxMedium3() {
        val mx = SimultaneousExpectiMinimax(false);

        val st = State.setupGameState(0,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.TENS,6)
        )

        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(1),st.newCurrent(1))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(2),st.newCurrent(2))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(3),st.newCurrent(3))), Pos.HUNDREDS)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(4),st.newCurrent(4))), Pos.ONES)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(5),st.newCurrent(5))), Pos.ONES)
        assertEquals(mx.runMiniMaxStrategySim(Get1000SimState(st.newCurrent(6),st.newCurrent(6))), Pos.ONES)
    }


    @Test
    fun runHundredDiffs() {
        (0..100).forEach {
            miniMaxDiff();
        }
    }


    fun miniMaxDiff() {
        val random = Random(9987897574)
        val nonSim = ExpectedValue();
        val sim = SimultaneousExpectiMinimax(true);

        var st = State(0, 0, 0, 0, 0, 0, 0);

        (0..3).forEach {
            st = st.newCurrent(random.diceRoll());
            st = st.addToPos(random.pickRandom(st.getPossiblePlacements()));
        }

        val start = st.newCurrent(random.diceRoll());

        val a = sim.runMiniMaxStrategySim(Get1000SimState(start,start));
        val b = nonSim.runExpectedValueStrategy(StateAndHist(emptyList(),start))

        println("$a $b $start");
        assertEquals(a, b);
    }


}