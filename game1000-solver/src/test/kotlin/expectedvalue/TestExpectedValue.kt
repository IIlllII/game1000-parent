package expectedvalue

import org.junit.Test
import org.junit.experimental.categories.Category
import solver.expectedvalue.ExpectedValue
import solver.models.Pos
import solver.models.State
import solver.models.StateAndHist
import test.ManualTests
import kotlin.test.assertEquals


@Category(ManualTests::class)
class TestExpectedValue {

    @Test
    fun simpleCaseTest() {
        val mx = ExpectedValue();

        val st = State.setupGameState(0,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.HUNDREDS,5),
                Pair(Pos.ONES,2),
                Pair(Pos.ONES,2),
                Pair(Pos.TENS,2),
                Pair(Pos.TENS,2),
                Pair(Pos.TENS,2)
        )

        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(1))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(2))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(3))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(4))), Pos.ONES)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(5))), Pos.ONES)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(6))), Pos.ONES)
    }


    @Test
    fun fullCaseTest() {
        val mx = ExpectedValue();

        val st = State.emptyState()

        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(1))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(2))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(3))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(4))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(5))), Pos.TENS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(6))), Pos.TENS)
    }


    @Test
    fun mediumCaseTest() {
        val mx = ExpectedValue();

        val st = State.setupGameState(0,
                Pair(Pos.HUNDREDS,4),
                Pair(Pos.ONES,4),
                Pair(Pos.TENS,3),
                Pair(Pos.TENS,3)
        )

        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(1))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(2))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(3))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(4))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(5))), Pos.ONES)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(6))), Pos.ONES)
    }



    @Test
    fun mediumCase2Test() {
        val mx = ExpectedValue();

        val st = State.setupGameState(0,
                Pair(Pos.HUNDREDS,2),
                Pair(Pos.HUNDREDS,2),
                Pair(Pos.TENS,6),
        )

        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(1))), Pos.ONES)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(2))), Pos.ONES)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(3))), Pos.ONES)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(4))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(5))), Pos.HUNDREDS)
        assertEquals(mx.runExpectedValueStrategy(StateAndHist(emptyList(),st.newCurrent(6))), Pos.ONES)
    }

}