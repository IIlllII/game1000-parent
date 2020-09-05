package gametheory

import bimatrix.R
import lcp.Rational
import java.lang.IllegalArgumentException
import java.lang.RuntimeException

/*
 * Copyright (c) Jonas Waage 04/09/2020
 */
data class Payoff(val a : Rational,val b : Rational)

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


class RowScope(val row : ArrayList<Payoff>) {

    fun p(a:Int,b:Int) {
        row.add(Payoff(a.R,b.R))
    }

}

enum class Player {ROW,COLUMN}

class GameException(msg:String) : RuntimeException(msg)

class TwoPlayerGame private constructor(
        private val rows : List<List<Payoff>>,
        private val rowMap : Map<String,Int>,
        private val colMap : Map<String,Int>) {

    private val cols : ArrayList<ArrayList<Payoff>>

    val indexToRowId : Map<Int,String> = rowMap.entries.map { i-> Pair(i.value,i.key) }.toMap()
    val indexToColumnId : Map<Int,String> = colMap.entries.map { i-> Pair(i.value,i.key) }.toMap()

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

    fun isWeaklyDominantStrategy(player : Player, id : String) : Boolean {
        return isWeaklyDominantStrategy(player,getRowOrColumn(player,id))
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


}




