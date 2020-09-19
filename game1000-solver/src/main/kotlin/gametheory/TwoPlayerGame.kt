package gametheory

import bimatrix.R
import lcp.Rational
import org.apache.commons.math3.fraction.BigFraction
import org.apache.commons.math3.linear.Array2DRowFieldMatrix
import utils.BF
import utils.getEquationCoefficients
import utils.makeStandardForm
import utils.solve
import java.lang.IllegalArgumentException
import java.lang.RuntimeException

/*
 * Copyright (c) Jonas Waage 04/09/2020
 */
data class Payoff(val a : Rational,val b : Rational) {
    fun swap() : Payoff {
        return Payoff(b,a)
    }
}

class GameScope(private val rows : ArrayList<ArrayList<Payoff>>,
                private val rowMapping : MutableMap<String,Int>,
                private val colMapping : MutableMap<String,Int>) {

    fun columnLabels(vararg label : String) {
        label.forEachIndexed { idx,lbl->
            colMapping[lbl] = idx
        }
    }

    fun row(init : RowScope.() -> Unit) {
        val row = ArrayList<Payoff>()
        init(RowScope(row))
        rows.add(row);
    }

    fun row(id:String,init : RowScope.() -> Unit) {
        rowMapping[id] = rows.size
        val row = ArrayList<Payoff>()
        init(RowScope(row))
        rows.add(row);
    }
}


class RowScope(private val row : ArrayList<Payoff>) {

    fun p(a:Int,b:Int) {
        row.add(Payoff(a.R,b.R))
    }

    fun p(a:Int) {
        row.add(Payoff(a.R,(-a).R))
    }

}

enum class Player {ROW,COLUMN}

class GameException(msg:String) : RuntimeException(msg)

class TwoPlayerGame private constructor(
        private val rows : List<List<Payoff>>,
        private val rowMap : Map<String,Int>,
        private val colMap : Map<String,Int>) {

    private val cols : ArrayList<ArrayList<Payoff>>

    private val indexToRowId : Map<Int,String> = rowMap.entries.map { i-> Pair(i.value,i.key) }.toMap()
    private val indexToColumnId : Map<Int,String> = colMap.entries.map { i-> Pair(i.value,i.key) }.toMap()

    companion object {
        fun create(vararg init : RowScope.() -> Unit) : TwoPlayerGame {
            val rows = ArrayList<ArrayList<Payoff>>()
            init.map { i -> val r = ArrayList<Payoff>(); rows.add(r); i( RowScope(r) ) }
            return TwoPlayerGame(rows, emptyMap(), emptyMap())
        }

        fun create(init :GameScope.() -> Unit) : TwoPlayerGame {
            val rows = ArrayList<ArrayList<Payoff>>()
            val rowMap = mutableMapOf<String,Int>()
            val colMap = mutableMapOf<String,Int>()
            init(GameScope(rows,rowMap,colMap))
            return TwoPlayerGame(rows,rowMap,colMap)
        }
    }

    init {
        assert(rows.isNotEmpty()) {"Must have more then 0 rows"}
        assert(rows[0].isNotEmpty()) {"Must have more then 0 columns"}
        assert(rows.map { i -> i.size }.distinct().size == 1) { "All rows must have the same mount of values" }

        //Precalculate columns
        val columns = ArrayList<ArrayList<Payoff>>()
        (rows[0].indices).forEach { i ->
            val column = ArrayList<Payoff>()
            (rows.indices).forEach { j ->
                column.add(rows[j][i])
            }
            columns.add(column)
        }

        cols = columns

        if(rowMap.isNotEmpty()) {
            assert(rowMap.keys.size == rows.size)
            {"Row labels has wrong size ${rowMap.keys.size} should be the same as number of rows ${rows.size} "}
        }

        if(colMap.isNotEmpty()) {
            assert(colMap.keys.size == cols.size)
            {"Column labels has wrong size ${colMap.keys.size} should be the same as number of rows ${cols.size} "}

        }
    }

    override fun toString(): String {
        return "TwoPlayerGame(rows=$rows)"
    }

    fun pureNashEquilibriumIds() : List<Pair<String,String>> {
        return pureNashEquilibriums().map {
            eq -> Pair(getRowOrColumnId(Player.ROW,eq.first),getRowOrColumnId(Player.COLUMN,eq.second))
        }
    }

    fun pureNashEquilibriums() : List<Pair<Int,Int>> {
        return rows.indices.flatMap {i->
            cols.indices.flatMap { j->
                if(isNashEquilibrium(i,j)) {
                    listOf(Pair(i,j))
                }
                else {
                    emptyList()
                }
            }
        }
    }

    private fun conditionTrueForAll(player : Player, id: Int, condition : (Rational, Rational) -> Boolean ) : Boolean {
        return when(player) {
            Player.ROW -> {
                getRow(id).mapIndexed { idx, p ->
                    getColumn(idx).filterIndexed { rIdx, _ -> rIdx != id }
                            .map { i -> condition(p.a,i.a) }
                }.flatten().all {it}
            }
            Player.COLUMN -> {
                getColumn(id).mapIndexed { idx, p ->
                    getRow(idx).filterIndexed { rIdx, _ -> rIdx != id }
                            .map { i -> condition(p.b,i.b) }
                }.flatten().all {it}
            }
        }
    }

    private fun conditionTrueForOne(player : Player,id: Int,condition : (Rational,Rational) -> Boolean ) : Boolean {
        return when(player) {
            Player.ROW -> {
                getRow(id).mapIndexed { idx, p ->
                    getColumn(idx).filterIndexed { rIdx, _ -> rIdx != id }
                            .map { i -> condition(p.a,i.a) }
                }.flatten().any {it}
            }
            Player.COLUMN -> {
                getColumn(id).mapIndexed { idx, p ->
                    getRow(idx).filterIndexed { rIdx, _ -> rIdx != id }
                            .map { i -> condition(p.b,i.b) }
                }.flatten().any {it}
            }
        }
    }


    fun getRowOrColumnId(player:Player, id :Int) : String {
        return when(player) {
            Player.ROW -> indexToRowId[id] ?: throw IllegalArgumentException("No row with id $id for player $player")
            Player.COLUMN -> indexToColumnId[id] ?: throw IllegalArgumentException("No row with id $id for player $player")
        }
    }

    fun getRowOrColumn(player:Player, id :String) : Int {
        return when(player) {
            Player.ROW -> getRow(id)
            Player.COLUMN -> getColumn(id)
        }
    }

    fun getRow(id:String) : Int {
        return rowMap[id]?:throw GameException("Row label $id is not defined")
    }

    fun getColumn(id:String) : Int {
        return colMap[id]?:throw GameException("Columns label $id is not defined")
    }

    fun isNashEquilibrium(row : String,col : String) : Boolean {
        return isNashEquilibrium(getRow(row),getColumn(col))
    }

    fun isNashEquilibrium(row : Int,col : Int) : Boolean {
        val r = getRow(row)
        val colCondition = r.map { r[col].b >= it.b  }.all { it }
        val c  = getColumn(col)
        val rowCondition = c.map { c[row].a >= it.a  }.all { it }
        return colCondition && rowCondition
    }

    fun isStronglyDominantStrategy(player : Player, id : String) : Boolean {
        return isStronglyDominantStrategy(player,getRowOrColumn(player,id))
    }

    fun isStronglyDominantStrategy(player : Player, id : Int) : Boolean {
        return conditionTrueForAll(player,id) { a, b -> a > b }
    }

    fun isStronglyDominantStrategyEquilibrium(row : Int, col : Int) : Boolean {
        return isStronglyDominantStrategy(Player.ROW,row) && isStronglyDominantStrategy(Player.COLUMN,col)
    }

    fun isStronglyDominantStrategyEquilibrium(row : String, col : String) : Boolean {
        return isStronglyDominantStrategyEquilibrium(getRowOrColumn(Player.ROW,row),getRowOrColumn(Player.COLUMN,col))
    }


    fun isWeaklyDominantStrategy(player : Player, id : String) : Boolean {
        return isWeaklyDominantStrategy(player,getRowOrColumn(player,id))
    }

    fun isWeaklyDominantEquilibrium(row : Int, col : Int) : Boolean {
        return isWeaklyDominantStrategy(Player.ROW,row) && isWeaklyDominantStrategy(Player.COLUMN,col)
    }

    fun isWeaklyDominantStrategyEquilibrium(row : String, col : String) : Boolean {
        return isWeaklyDominantEquilibrium(getRowOrColumn(Player.ROW,row),getRowOrColumn(Player.COLUMN,col))
    }

    fun isVeryWeaklyDominantStrategyEquilibrium(row : Int, col : Int) : Boolean {
        return isVeryWeaklyDominantStrategy(Player.ROW,row) && isVeryWeaklyDominantStrategy(Player.COLUMN,col)
    }

    fun isVeryWeaklyDominantStrategyEquilibrium(row : String, col : String) : Boolean {
        return isVeryWeaklyDominantStrategyEquilibrium(getRowOrColumn(Player.ROW,row),getRowOrColumn(Player.COLUMN,col))
    }

    fun isWeaklyDominantStrategy(player : Player, id : Int) : Boolean {
        val all = conditionTrueForAll(player,id) { a,b -> a >= b }
        val one = conditionTrueForOne(player,id) { a,b -> a > b }
        return all && one
    }

    fun isVeryWeaklyDominantStrategy(player : Player, id : String) : Boolean {
        return isVeryWeaklyDominantStrategy(player,getRowOrColumn(player,id))
    }

    fun isVeryWeaklyDominantStrategy(player : Player, id : Int) : Boolean {
        return conditionTrueForAll(player,id) { a,b -> a>=b }
    }

    fun getRow(id : Int) : List<Payoff> {
        return rows[id]
    }

    fun getColumn(id : Int) : List<Payoff> {
        return cols[id]
    }

    fun maxMin(player: Player) : Rational {
        return when(player) {
            Player.ROW -> rows.map { i -> i.map { j -> j.a }.minOrNull() as Rational }.maxOrNull() as Rational
            Player.COLUMN -> cols.map { i -> i.map { j -> j.b }.minOrNull() as Rational }.maxOrNull() as Rational
        }
    }

    fun maxMinStrategyIds(player: Player) : List<String> {
        return maxMinStrategies(player).map { i -> getRowOrColumnId(player,i) }
    }

    fun maxMinStrategies(player: Player) : List<Int> {
        val maxMin = maxMin(player)
        return when(player) {
            Player.ROW -> {
                rows
                        .mapIndexed { idx,i -> Pair(idx,i.map { j -> j.a }.minOrNull()) }
                        .filter { i->i.second == maxMin }
                        .map { i->i.first }
            }
            Player.COLUMN -> {
                cols
                        .mapIndexed { idx,i -> Pair(idx,i.map { j -> j.b }.minOrNull()) }
                        .filter { i -> i.second == maxMin }
                        .map { i -> i.first }
            }
        }
    }

    fun isZeroSum() : Boolean {
        val bools = rows.flatMap {
            i -> i.map {
                j -> j.a + j.b == 0.R
            }
        }
        return bools.all { it }
    }

    private fun getZeroSumMatrix() : Array2DRowFieldMatrix<BigFraction> {
        if(!isZeroSum()) {
            throw GameException("Matrix is not Zero sum $rows")
        }

        val vg : Array<Array<BigFraction>> = rows.map {
            it.map { i->i.a.BF }.toTypedArray()
        }.toTypedArray()

        return Array2DRowFieldMatrix(vg,false)
    }

    private fun solveChecks() {
        if(colMap.isEmpty() || rowMap.isEmpty()) {
            throw GameException("Game $this should be labeled")
        }
        if(!isZeroSum()) {
            throw GameException("Game $this is not zero sum")
        }
    }

    fun solveZeroSumForRowPlayer() : List<Pair<String,BigFraction>>  {
        solveChecks()
        val reduced = removeAllDominatedRowsAndColumns()
        val solution = solveZeroSumForRowPlayer(reduced);
        return solution.mapIndexed { index, bigFraction -> Pair(reduced.getRowOrColumnId(Player.ROW,index),bigFraction) }
    }

    fun solveZeroSumForColumnPlayer() : List<Pair<String,BigFraction>>  {
        solveChecks()
        val swapPayoffs = cols.map { it.map { pf -> pf.swap() } };
        val transposeGame = TwoPlayerGame(swapPayoffs,colMap,rowMap)
        return transposeGame.solveZeroSumForRowPlayer()
    }

    private fun solveZeroSumForRowPlayer(reduced : TwoPlayerGame) : List<BigFraction> {
        val row =  reduced.rowMap.values.map{it+1}.toSet()
        val col =  reduced.colMap.values.map{it+1}.toSet()
        val matrix = getEquationCoefficients(reduced.getZeroSumMatrix(),row,col)
        val x = makeStandardForm(matrix.first)
        val result = solve(x).toArray();
        return result.dropLast(1)
    }

    fun playerOneSupports() : Set<Int> {
        return rowMap.values.toSet()
    }

    fun minMax(player: Player) : Rational {
        return when (player) {
            Player.ROW -> cols.map { i -> i.map { j -> j.a }.maxOrNull() as Rational }.minOrNull() as Rational
            Player.COLUMN -> rows.map { i -> i.map { j -> j.b }.maxOrNull() as Rational }.minOrNull() as Rational
        }
    }

    fun minMaxStrategyIds(player: Player) : List<String> {
        return minMaxStrategies(player).map { i -> getRowOrColumnId(player,i) }
    }

    fun minMaxStrategies(player: Player) : List<Int> {
        val minMax = minMax(player)
        return when(player) {
            Player.ROW -> {
                cols
                        .mapIndexed { idx,i -> Pair(idx,i.map { j -> j.a }.maxOrNull()) }
                        .filter { i->i.second == minMax }
                        .map { i->i.first }
            }
            Player.COLUMN -> {
                rows
                        .mapIndexed { idx,i -> Pair(idx,i.map { j -> j.b }.maxOrNull()) }
                        .filter { i -> i.second == minMax }
                        .map { i -> i.first }
            }
        }
    }



    fun rowIsDominated(a : Int) : Boolean {
        val check = rows.indices.toSet().minus(a)
        val doms = check.map { row ->
            val comp = rows[row].mapIndexed { idx, pf ->
                rows[a][idx].a <= pf.a
            }
            comp.all { it }
        }
        return doms.any { it }
    }

    fun columnIsDominated(a : Int) : Boolean {
        val check = cols.indices.toSet().minus(a)
        val doms = check.map { row ->
            val res = cols[row].mapIndexed { idx, pf ->
                cols[a][idx].b <= pf.b
            }
            res.all { it }
        }
        return doms.any { it }
    }


    /**
     * Keep removing columns and rows as long as one is dominated
     *
     * Can be done in a more functional style, but clearer like this I think.
     *
     */
    private fun removeAllDominatedRowsAndColumns() : TwoPlayerGame {
        var game = this;
        do {
            var hasDominated = false
            for(i in game.rows.indices) {
                if(game.rowIsDominated(i)) {
                    hasDominated = true
                    game = game.removeRow(i)
                    break // Indices are bad, must break
                }
            }
            for(j in game.cols.indices) {
                if(game.columnIsDominated(j)) {
                    hasDominated = true
                    game = game.removeColumn(j)
                    break  // Indices are bad, must break
                }
            }
        } while (hasDominated)
        return game
    }


    private fun removeRow(id : Int) : TwoPlayerGame {
        val nextRowMap = rowMap
                .filter { i -> i.value != id }
                .mapValues { if(it.value > id) it.value-1 else it.value }
        val rows = rows.filterIndexed { idx, _ -> idx != id }
        return TwoPlayerGame(rows,nextRowMap,colMap)
    }

    private fun removeColumn(id : Int) : TwoPlayerGame {
        val nextColMap = colMap
                .filter { i -> i.value != id }
                .mapValues { if(it.value > id) it.value -1 else it.value }
        val rows = rows.map { it.filterIndexed {idx, _ -> idx != id} }
        return TwoPlayerGame(rows,rowMap,nextColMap)
    }

}


operator fun BigFraction.div(i: Int): BigFraction {
    return this.divide(i.BF)
}

operator fun BigFraction.div(i: Long): BigFraction {
    return this.divide(i.BF)
}
