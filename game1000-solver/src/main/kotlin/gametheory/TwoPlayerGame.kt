package gametheory

import bimatrix.R
import lcp.Rational
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


    fun nashEquilibriums() : List<Pair<Int,Int>> {
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


}




