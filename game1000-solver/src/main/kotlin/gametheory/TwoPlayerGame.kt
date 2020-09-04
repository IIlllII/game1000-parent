package gametheory

import bimatrix.R
import lcp.Rational

/*
 * Copyright (c) Jonas Waage 04/09/2020
 */
data class Payoff(val a : Rational,val b : Rational)

class GameScope(val col : ArrayList<ArrayList<Payoff>>) {

    fun id(vararg label : String) {

    }

    fun row(init : RowScope.() -> Unit) {
        val row = ArrayList<Payoff>()
        init(RowScope(row))
        col.add(row);
    }

    fun row(id:String,init : RowScope.() -> Unit) {

    }
}


class RowScope(val row : ArrayList<Payoff>) {

    fun p(a:Int,b:Int) {
        row.add(Payoff(a.R,b.R))
    }
}

enum class Player {ROW,COLUMN}

class TwoPlayerGame(init : GameScope.() -> Unit) {
    val rows = ArrayList<ArrayList<Payoff>>()
    val cols : ArrayList<ArrayList<Payoff>>

    init {
        init(GameScope(rows))
        assert(rows.size > 0) {"Must have more then 0 rows"}
        assert(rows[0].size > 0) {"Must have more then 0 columns"}
        assert(rows.map { i -> i.size }.distinct().size==1) { "All rows must have the same mount of values" }

        //Precalculate columns
        val columns = ArrayList<ArrayList<Payoff>>()
        (0 until rows[0].size).forEach { i ->
            val column = ArrayList<Payoff>()
            (0 until rows.size).forEach { j ->
                column.add(rows[j][i])
            }
            columns.add(column)
        }
        cols = columns
    }

    override fun toString(): String {
        return "TwoPlayerGame(rows=$rows)"
    }

    fun hasStronglyDominantStrategy(player : Player) {

    }

    fun hasWeaklyDominantStrategy(player : Player) {

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
                }.flatten().any()
            }
            Player.COLUMN -> {
                getColumn(id).mapIndexed { idx, p ->
                    getRow(idx).filterIndexed { rIdx, _ -> rIdx != id }
                            .map { i -> condition(p.b,i.b) }
                }.flatten().any()
            }
        }
    }

    fun isNashEquilibrium(row : Int,col : Int) : Boolean {
        val r = getRow(row)
        val colCondition = r.map { r[col].b >= it.b  }.all { it }
        val c  = getColumn(col)
        val rowCondition = c.map { c[row].a >= it.a  }.all { it }
        return colCondition && rowCondition
    }

    fun isStronglyDominantStrategy(player : Player, id : Int) : Boolean {
        return conditionTrueForAll(player,id) { a, b -> a > b }
    }

    fun isWeaklyDominantStrategy(player : Player, id : Int) : Boolean {
        return conditionTrueForAll(player,id) { a,b -> a >= b } && conditionTrueForOne(player,id) { a,b -> a > b }
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




