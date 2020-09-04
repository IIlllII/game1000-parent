package solver.backwardsinduction

import allPermutationsCons
import bimatrix.BimatrixSolver
import bimatrix.Equilibria
import bimatrix.Equilibrium
import com.google.gson.Gson
import getFinalStates
import getPermutations
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import lcp.Rational
import lrs.LrsAlgorithm
import org.pcollections.ConsPStack
import org.slf4j.LoggerFactory
import placementPermutations
import solver.models.*
import utils.*
import java.io.Reader
import java.io.Writer
import java.lang.IllegalStateException
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicLong



class OnlyIfTimePassed(val seconds : Int) {

    var time = AtomicLong(System.currentTimeMillis())

    fun runIfTimePassed(run : () -> Unit) {

        val nuTime = System.currentTimeMillis()
        var doRun = false
        time.getAndUpdate {t ->
            if(nuTime - t > seconds*1000) {
                doRun = true
                nuTime
            }
            else {
                t
            }
        }


        if(doRun) {
            run();
        }
    }

}


private val polymorphicJson = Json {
    useArrayPolymorphism = true
    serializersModule = SerializersModule {
        polymorphic(Node::class, Strategy::class, Strategy.serializer())
        polymorphic(Node::class, DegenerateNode::class, DegenerateNode.serializer())
        polymorphic(Node::class, Final::class, Final.serializer())
    }
}

/**
 *
 * This solution is partly described in this <a href="https://www.bitbreeds.com/Blog/solving-get1000-continued/">blog post</a>
 *
 */
class BackwardsInductionSolution {

    private val logger = LoggerFactory.getLogger(BackwardsInductionSolution::class.java)
    private val solution : MutableMap<State, Node> = HashMap(250000)

    fun getNode(state : State) : Node? {
        return solution[state];
    }

    /**
     * Compute the solution and store it in the solution map
     */
    fun createSolution(depth :Int = 8) {

        getFinalStates().forEach {
            state -> solution[state] = Final(state.stateDistance())
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
                                .map { pair -> Pair(pair,findPayoff(state,pair.first,pair.second,i)) }

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

                        solution[node.first] = node.second;
                    }

                }

            }

        }
    }


    /**
     * Given a state and two choices, this will apply each players choice and then
     * play out the given node return the payoff.
     *
     * The payoff is for player 1, and the payoff for player two
     * is the same payoff with negative sign.
     */
    private fun findPayoff(state : State, choiceP1 : Pos, choiceP2 : Pos, round: Int) : Rational {
        val sRow = state.addToPos(choiceP1)
        val sCol = state.addToPos(choiceP2)

        var payoffRow = Rational(BigInteger.ZERO, BigInteger.ONE);
        var payoffCol = Rational(BigInteger.ZERO, BigInteger.ONE);

        allPermutationsCons(0..5, 8 - round) { gm ->

            val resultRow = doPlayOut(sRow, gm, Rational(BigInteger.ONE,BigInteger.ONE), emptyList())
            val resultCol = doPlayOut(sCol, gm, Rational(BigInteger.ONE,BigInteger.ONE), emptyList())

            resultRow.forEach { rwEn ->
                resultCol.forEach { clEn ->
                    when {
                        rwEn.result < clEn.result -> {
                            payoffRow += (rwEn.rate * clEn.rate)
                        }
                        rwEn.result > clEn.result -> {
                            payoffCol += (rwEn.rate * clEn.rate)
                        }
                    }
                }
            }

        }
        return payoffRow - payoffCol;
    }



    private fun simpleFromEquilibria(equilibria: Equilibria, state: State) : Pair<State, Node> {

        val best = equilibria.equilibriumWithMaxPayoff()

        val equilibrium = best[0].probVec1!!.toList();
        val x = equilibrium.reduce{a,b->a+b}

        if(x != Rational(BigInteger.ONE, BigInteger.ONE)) {
            throw IllegalStateException("Not complete, sum $x of $equilibrium")
        }

        val results = state.getPossiblePlacementsOrdered()
                .mapIndexed { idx,pos -> MixedPlacement(equilibrium[idx],pos) }
                .filter { mixed -> !mixed.rateOfPlay.isZero }
        //Store strategy
        return Pair(state, Strategy(results))
    }

    /**
     * Calculates the type of node from the Equilibria found
     */
    fun nodeFromEquilibria(equilibria: Equilibria, state: State) : Pair<State, Node> {

        val best = equilibria.equilibriumWithMaxPayoff()

        //Pick last if several equlibriums
        if(best.count() > 1) { //never use this for now
            logger.info("There is more than one equlibrium for $state and matrix}");
            for (idx in 0 until best.size) {
                logger.info("Probvecs: ${best[idx].probVec1?.toList()} ${best[idx].payoff1}");
            }
        }

        val possibilities = state.getPossiblePlacementsOrdered();

        val uniqueEquilibria = best.map { i->
            val strat = possibilities.mapIndexed { idx, pos ->  MixedPlacement(i.probVec1!![idx],pos) }
                    .filter { j -> !j.rateOfPlay.isZero }
            strat.toSet()
        }.toSet()

        return if(uniqueEquilibria.size > 1) {
            val countWithSize1 = uniqueEquilibria.map { i->i.count{ j -> j.rateOfPlay.isOne} }.sum()
            if(countWithSize1 > 1) {
                logger.info("Degenerate equilibria $best adding DegenerateNode")
                Pair(state, DegenerateNode(uniqueEquilibria.flatten()))
            }
            else {
                logger.info("Many but non degenerate equilibria $best")
                Pair(state, Strategy(uniqueEquilibria.first().toList()))
            }
        }
        else {
            val results = possibilities
                    .mapIndexed { idx,pos -> MixedPlacement(best[0].probVec1!![idx],pos) }
                    .filter { i-> !i.rateOfPlay.isZero }
            //Store strategy
            Pair(state, Strategy(results))
        }
    }

    /**
     * This is an attempt to do something when there is a state with two
     * seemingly equally good choices to pick from.
     */
    fun findPayoffIfChoices(row : List<Result>, col : List<Result>) : Pair<List<StateChoice>,Rational> {

        val groupsRow = row.groupBy { i->i.choices }
        val groupsCol = col.groupBy { i->i.choices }

        val mx = groupsRow.map { rw ->
            groupsCol.map { cl ->
                var num = Rational(BigInteger.ZERO, BigInteger.ONE);
                rw.value.forEach { rwEn ->
                    cl.value.forEach { clEn ->
                        when {
                            rwEn.result < clEn.result -> {
                                num += (rwEn.rate * clEn.rate)
                            }
                            rwEn.result > clEn.result -> {
                                num -= (rwEn.rate * clEn.rate)
                            }
                        }
                    }
                }
                Pair(rw.key,num)
            }.toTypedArray()
        }.toTypedArray()

        //Solve and find expected payoff and add that???
        //Instead of code below

        return if (mx.size == 1 && mx[0].size == 1) {
            mx[0][0]
        } else {

            val matrix = mx.map { i ->
                i.map { j -> j.second }.toTypedArray()
            }.toTypedArray()

            val eqs: Equilibria = findEquilibriaOfZeroSumGame(matrix)
            if(eqs.count() > 0) {
                val best = eqs.equilibriumWithMaxPayoff()

                val uniqueEquilibria = best.map { eq ->
                    val strat = (0..mx.lastIndex).map { idx ->
                        Pair(mx[idx][0].first,eq.probVec1!![idx]) }
                            .filter { i->i.second > Rational(BigInteger.ZERO, BigInteger.ONE) }
                    strat
                }

                uniqueEquilibria[0][0];
            }
            else {
                logger.info("Solving ${mx.map { it.toList() }} failed falling back to ${mx[0][0]}")
                mx[0][0]
            }
        }
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

    /**
     * Serialize solution
     */
    fun storeSolution(wr : Writer) {
        val gson = Gson();
        solution.forEach{(state,node)->
            if(node is Strategy) {
                val json = gson.toJson(Serialized(state, node))
                wr.write(json)
                wr.write("\n")
            }
            else {
                throw IllegalStateException("Can not store node of type ${node.javaClass}")
            }
        }
    }

    /**
     * Read solution from file
     */
    fun readSolution(rd : Reader) {
        val gson = Gson()
        rd.forEachLine {
            val strat = gson.fromJson<Serialized>(it, Serialized::class.java)
            solution[strat.state] = strat.strategy
        }
    }


    fun storeSolutionSer(wr: Writer) {
        solution.forEach { (state, node) ->
            val string = polymorphicJson.encodeToString(SerializedNu.serializer(), SerializedNu(state, node))
            wr.write(string)
            wr.write("\n")
        }
    }

    fun readSolutionSer(rd : Reader) {
        rd.forEachLine {
            val node = polymorphicJson.decodeFromString(SerializedNu.serializer(),it);
            solution[node.state] = node.strategy
        }
    }

    /**
     * Do placements for game, and return possible results and likelihoods
     */
    private fun doPlayOut(state : State, game : ConsPStack<Int>, rate : Rational, choices : List<StateChoice>) : List<Result> {

        val head = if(game.isEmpty()) 0 else game.get(0)+1; //Add + 1 Hack since something is off with game generation
        val converted = state.newCurrent(head)

        val st = solution[converted] ?: throw IllegalStateException("Missing state $converted $game");

        return when (st) {
            is Final -> {(listOf(Result(rate,st.result,choices)))}
            is Strategy -> {
                st.placements.flatMap { pl ->
                    doPlayOut(converted.addToPos(pl.pos).newCurrent(head),game.minus(0),rate.multiply(pl.rateOfPlay),choices)
                }
            }
            is DegenerateNode -> {
                val first = st.choices[0];
                doPlayOut(converted.addToPos(first.pos).newCurrent(head),game.minus(0),rate.multiply(first.rateOfPlay),choices)

                /*st.choices.flatMap { pl->
                    val choice = StateChoice(converted,pl.pos)
                    val playoutResult = doPlayOut(
                            state = converted.addToPos(pl.pos).newCurrent(head),
                            game = game.subList(1),
                            rate = rate.multiply(pl.rateOfPlay),
                            choices = choices+choice
                    );
                    playoutResult
                }*/
            }
        }
    }

    /**
     * For a given state, return the choice of this strategy
     */
    fun play(state : StateAndHist) : Pos {
        val sol = solution[state.state] ?: throw IllegalStateException("State ${state.state} not found")
        return when (sol) {
            is Strategy -> { pickStrat(sol) }
            is DegenerateNode -> { sol.choices[0].pos } //Pick first for now
            is Final -> { Pos.FINAL }
        }
    }

    /**
     * Play with degenerate node handling
     */
    fun play(state : StateAndHist, case : (nd : DegenerateNode) -> Pos) : Pos {
        val sol = solution[state.state] ?: throw IllegalStateException("State ${state.state} not found")
        return when (sol) {
            is Strategy -> {
                pickStrat(sol)
            }
            is DegenerateNode -> {
                case(sol)
            } //Pick first for now
            is Final -> { Pos.FINAL }
        }
    }
}


/**
 * Picks a strategy at random if a mixed strategy is needed, otherwise returns the
 * non mixed strategy.
 */
fun pickStrat(s : Strategy) : Pos {
    return if(s.placements.size == 1) {
        s.placements[0].pos
    }
    else {
        var dn = Rational.valueOf(Math.random());
        var current = s.placements[0].pos;
        for (i in s.placements){
            dn -= i.rateOfPlay;
            if(dn < Rational(BigInteger.ZERO, BigInteger.ONE)) {
                current = i.pos
                break;
            }
        }
        current
    }
}

/**
 * Utility function to find unique Position pairs
 */
fun findPairsToCalculate(choices : Array<Pos>) : Set<Pair<Pos, Pos>> {
    return choices
            .flatMap { c1 -> choices.map {c2 -> Pair(c1,c2) } }
            .filter { j -> j.first != j.second }
            .distinctBy { j -> setOf(j.first,j.second) }
            .toSet()
}

/**
 * Find the highest payoff
 */
fun Equilibria.maxPayoff() : Rational {
    return this.map { i -> i.payoff1!! }.reduce { a, b -> if(a > b) a else b }
}

/**
 * Find the equilibrium(s) with highest payoff
 */
fun Equilibria.equilibriumWithMaxPayoff() : List<Equilibrium> {
    val max = this.maxPayoff()
    return this.filter { i -> i.payoff1!! >= max }
}