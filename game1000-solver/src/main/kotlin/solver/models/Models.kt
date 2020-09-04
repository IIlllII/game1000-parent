package solver.models

import kotlinx.serialization.Serializable
import lcp.Rational
import utils.diceRoll
import utils.pickRandom
import java.time.Instant
import java.util.HashSet
import kotlin.random.Random


enum class Pos {
    FINAL,ONES,TENS,HUNDREDS;

    companion object {
        fun nonFinal(): Set<Pos> {
            return setOf(TENS, HUNDREDS, ONES);
        }
    }
}




/**
 *
 */
@Serializable
data class State(val current : Int,
                 val hundreds : Int,
                 val tens : Int,
                 val ones: Int,
                 val plHun : Int,
                 val plTen : Int,
                 val plOne : Int
) {
    init {

        if(current !in 0..6) throw IllegalStateException("Current can not be: " + current);
        if(hundreds < 0) throw IllegalStateException("Hundreds can not be: " + hundreds);
        if(tens < 0) throw IllegalStateException("Tens can not be: " + tens);
        if(ones < 0) throw IllegalStateException("Ones can not be: " + ones);
        if(plHun !in 0..3) throw IllegalStateException("plHun can not be: " + plHun);
        if(plTen !in 0..3) throw IllegalStateException("plTen can not be: " + plTen);
        if(plOne !in 0..3) throw IllegalStateException("plOne can not be: " + plOne);

    }

    companion object {

        private fun startState(current : Int) : State {
            return State(
                    current=current,
                    hundreds = 0,
                    tens = 0,
                    ones = 0,
                    plHun = 0,
                    plTen = 0,
                    plOne = 0
            )
        }

        fun emptyState() : State {
            return startState(0)
        }

        fun setupGameState(current : Int, vararg plays : Pair<Pos,Int>) : State {
            var v = emptyState()
            plays.forEach {
                v = v.newCurrent(it.second).addToPos(it.first)
            }
            return v.newCurrent(current)
        }

    }

    fun placements() : Int = plHun + plTen + plOne

    fun placementsLeft() : Int = 9 - placements()

    fun isFull() : Boolean = fullHundred() && fullTens() && fullOnes()

    fun stateValue() : Int = hundreds*100 + tens*10 + ones

    fun stateDistance() : Int = Math.abs(1000-stateValue())

    fun fullHundred() : Boolean = plHun >= 3

    fun fullTens() : Boolean = plTen >= 3

    fun fullOnes() : Boolean = plOne >= 3

    fun newCurrent(x : Int) : State = this.copy(current=x)




    fun addToPos(pos : Pos) : State {
        return when (pos) {
            Pos.TENS -> addTens(current);
            Pos.HUNDREDS -> addHundred(current);
            Pos.ONES -> addOnes(current);
            Pos.FINAL -> throw IllegalStateException("Illegal to have a placement");
        }
    }

    fun addHundred(x : Int) : State {
        if(x !in 1..6) throw IllegalArgumentException("Value must be between 1 and 6, was" + x );
        if(fullHundred()) throw IllegalStateException("Full position");
        return this.copy(hundreds=hundreds+x,plHun = plHun+1);
    }

    fun addTens(x : Int) : State {
        if(x !in 1..6) throw IllegalArgumentException("Value must be between 1 and 6, was" + x );
        if(fullTens()) throw IllegalStateException("Full position");
        return this.copy(tens=tens+x,plTen=plTen+1);
    }

    fun addOnes(x : Int) : State {
        if(x !in 1..6) throw IllegalArgumentException("Value must be between 1 and 6, was" + x );
        if(fullOnes()) throw IllegalStateException("Full position");
        return this.copy(ones=ones+x,plOne=plOne+1);
    }

    fun maximumResult(): Int {
        return (3-plHun)*600 + (3-plTen)*60 + (3-plOne)*6;
    }

    fun minimumResult(): Int {
        return (3-plHun)*100 + (3-plTen)*10 + (3-plOne)*1;
    }

    fun getPossiblePlacements() : Set<Pos> {
        val res = HashSet<Pos>();
        if(!fullHundred()) {res.add(Pos.HUNDREDS)};
        if(!fullTens()) {res.add(Pos.TENS)};
        if(!fullOnes()) {res.add(Pos.ONES)};
        return res;
    }

    fun getPossiblePlacementsOrdered() : Array<Pos> {
        val res = ArrayList<Pos>(3);
        if(!fullOnes()) {res.add(Pos.ONES)};
        if(!fullTens()) {res.add(Pos.TENS)};
        if(!fullHundred()) {res.add(Pos.HUNDREDS)};
        return res.toTypedArray();
    }

    fun getPositionChange(state : State) : Pos {
        val a = plHun+plOne+plTen;
        val b = state.plHun+state.plOne+state.plTen;
        if(b-a != 1) {
            throw IllegalArgumentException("This can only find the last move if one state away");
        }
        return when {
            plHun != state.plHun -> {
                Pos.HUNDREDS
            }
            plTen != state.plTen -> {
                Pos.TENS;
            }
            plOne != state.plOne -> {
                Pos.ONES;
            }
            else -> {
                throw IllegalStateException("Should not be possible");
            }
        }
    }

    fun runRandomTurns(num : Int) : State {
        val random = Random(Instant.now().toEpochMilli());
        var st = this;
        (1..num).forEach {
            st = st.newCurrent(random.diceRoll());
            st = st.addToPos(random.pickRandom(st.getPossiblePlacements()));
        }
        return st;
    }

}

/**
 *
 */
fun toState(current:Int,ls : List<Int>, pl : List<Int>) : State {
    return State(current,ls[0],ls[1],ls[2],pl[0],pl[1],pl[2]);
}

data class Comb(val a: State, val b: Placement)

data class Comb2(val state: StateAndHist, val pos : Pos)

data class StateAndHist(val game : List<Int>, val state : State) {

    companion object {
        fun empty() : StateAndHist {
            return StateAndHist(emptyList(), State(0,0,0,0,0,0,0))
        }

        fun current(num : Int) : StateAndHist {
            return StateAndHist(emptyList(), State(num,0,0,0,0,0,0))
        }

        fun setupGameState(current : Int, vararg plays : Pair<Pos,Int>) : StateAndHist {
            var v = StateAndHist(emptyList(), State(0,0,0,0,0,0,0))
            plays.forEach {
                v = v.newCurrent(it.second).addToPos(it.first)
            }
            return v.newCurrent(current)
        }
    }

    fun newCurrent(num : Int) : StateAndHist {
        return StateAndHist(this.game,this.state.newCurrent(num));
    }

    fun addToPos(pos : Pos) : StateAndHist {
        return StateAndHist(this.game+this.state.current,this.state.addToPos(pos));
    }

    fun runRandomTurns(numberOfTurns : Int) : StateAndHist {
        val random = Random(Instant.now().toEpochMilli());
        var st = this;
        (1..numberOfTurns).forEach {
            val num = random.diceRoll();
            st = st.newCurrent(num);
            st = st.addToPos(random.pickRandom(st.getPossiblePlacements()));
        }
        return st;
    }

    fun getPossiblePlacements() : Set<Pos> {
        return this.state.getPossiblePlacements();
    }

}

data class Resolver(val map : Map<StateAndHist, Pos>) {}

data class Placement(val placements : List<Pos>,
                     val winsHundred : Int,
                     val winsTen:Int,
                     val winsOne : Int,
                     val eqResolver : Resolver = Resolver(HashMap()),
                     val mixedCase : List<Pair<Rational, Pos>> = emptyList())


data class ComparisonResults(
        val gamesPlayed : Long,
        val winPlayer1 : Long,
        val winPlayer2 : Long,
        val draws : Long,
        val averageDistanceTo1000Player1 : Double,
        val averageDistanceTo1000Player2 : Double,
        val player1Strategy  : (StateAndHist) -> Pos,
        val player2Strategy :  (StateAndHist) -> Pos
)