package solver.backwardsinduction



import bimatrix.BimatrixSolver
import bimatrix.Equilibria
import getFinalStates
import getPermutations
import lcp.Rational
import lrs.LrsAlgorithm
import org.slf4j.LoggerFactory
import placementPermutations
import solver.models.*
import utils.plus
import utils.swap
import utils.unaryMinus
import java.math.BigInteger


/**
 * A different attempt at the solution described in this <a href="https://www.bitbreeds.com/Blog/solving-get1000-continued/">blog post</a>
 */
class PseudoSubGamePerfectAi {

    private val logger = LoggerFactory.getLogger(PseudoSubGamePerfectAi::class.java)
    private val solution : MutableMap<Pair<State, State>, SubGameRes> = HashMap(250000)

    private fun num(s1: State, s2: State) : Rational {
        return when {
            s1.stateDistance() < s2.stateDistance() -> Rational(BigInteger.ONE, BigInteger.ONE)
            s1.stateDistance() > s2.stateDistance() -> - Rational(BigInteger.ONE, BigInteger.ONE)
            else -> Rational(BigInteger.ZERO, BigInteger.ONE)
        }
    }

    fun createSolution(depth :Int = 8) {

        val states = getFinalStates();

        states.forEach { state1->
            states.forEach { state2 ->
                solution[Pair(state1,state2)] = Fin(num(state1,state2))
            }
        }

        for (i in ((8 - depth)..8).reversed()) {

            logger.info("----- Solution depth $i -----")

            //Calculates possible placements that can exist at this depth (0,2,3) at depth 5 for example
            getPermutations(i, 3, 3).forEach {placement ->

                logger.info("Calculating placements $placement")

                //Calculates possible score permutations for the given placements
                placementPermutations(placement, (1..6).toList()).forEach { resPerm ->

                    //Possible current values at this state
                    (1..6).forEach { current ->
                        //The state to solve now.
                        val state = toState(current, resPerm, placement)

                        val possibilities = state.getPossiblePlacementsOrdered()

                        val pairs = findPairsToCalculate(possibilities).toList()

                        val results = pairs
                                .map { pair -> Pair(pair,findPayoff(state,pair.first,pair.second)) }

                        val combined = (results.map { rational -> Pair(rational.first.swap(),-rational.second) } + results)
                                .toMap();

                        val matrix = possibilities.map { rowPlayer ->
                            possibilities.map { colPLayer ->
                                val toReturn =
                                        if (rowPlayer == colPLayer) {
                                            Rational(BigInteger.ZERO, BigInteger.ONE);
                                        } else {
                                            combined.getValue(Pair(rowPlayer, colPLayer));
                                        }
                                toReturn;
                            }.toTypedArray()
                        }.toTypedArray()

                        if(i <= 3) {
                            logger.info("RawMatrix ${matrix.map { i->i.toList() }.toList()}")
                        }

                        val eqs : Equilibria = findEquilibriaOfZeroSumGame(matrix)

                        val node = nodeFromEquilibria(eqs,state);

                        if(i <= 3) {
                            eqs.forEach {
                                logger.info(" Vec ${it.probVec1?.toList()} payoff ${it.payoff1}")
                            }
                            logger.info("Calculated perm $resPerm and state ${node.first} as ${node.second} ${matrix.map { i->i.toList() }.toList()}")
                        }

                        solution[Pair(node.first,node.first)] = node.second;
                    }

                }

            }

        }
    }



    /**
     * Look through next states for payoffs
     */
    private fun findPayoff(state : State, choiceP1 : Pos, choiceP2 : Pos) : Rational {
        val sRow = state.addToPos(choiceP1)
        val sCol = state.addToPos(choiceP2)

        return (1..6).map {
            val stRow = sRow.newCurrent(it);
            val stCol = sCol.newCurrent(it);

            val dt = solution[Pair(stRow,stCol)]!!
            when(dt) {
                is Fin -> {dt.result}
                is SubGameResult -> {dt.payoff}
            }
        }.reduce {a,b -> a+b}
    }

    fun nodeFromEquilibria(equilibria: Equilibria, state: State) : Pair<State, SubGameResult> {

        val best = equilibria.equilibriumWithMaxPayoff()

        //Pick last if several equlibriums
        if(best.count() > 1) { //never use this for now
            logger.info("There is more than one equlibrium for $state and matrix}");
            for (idx in 0 until best.size) {
                logger.info("Probvecs: ${best[idx].probVec1?.toList()} ${best[idx].payoff1}");
            }
        }

        val possibilities = state.getPossiblePlacementsOrdered();
        if(possibilities.size == 1) {
            
        }

        val uniqueEquilibria = best.map { i->
            val strat = possibilities.mapIndexed { idx, pos ->  MixedPlacement(i.probVec1!![idx],pos) }
                    .filter { j -> !j.rateOfPlay.isZero }
            strat.toSet()
        }.toSet()

        return Pair(state, SubGameResult(equilibria.maxPayoff(),uniqueEquilibria.first().toList()));
    }

    /**
     * Create second matrix and solve
     */
    private fun findEquilibriaOfZeroSumGame(mx : Array<Array<Rational>>) : Equilibria {
        val program = BimatrixSolver()

        val lrs = LrsAlgorithm()

        val b = mx.map { col ->
            col.map {
                -it
            }.toTypedArray()
        }.toTypedArray()

        return program.findAllEq(lrs, mx, b)
    }



}
