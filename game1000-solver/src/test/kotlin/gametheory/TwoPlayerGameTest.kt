package gametheory

import bimatrix.R
import org.junit.Test
import utils.unaryMinus
import kotlin.test.assertEquals

/*
 * Copyright (c) Jonas Waage 04/09/2020
 */

class TwoPlayerGameTest {

    @Test
    fun testGetRowCol() {
        val game = TwoPlayerGame.create (
                { p(1, 2); p(0, 4) },
                { p(1, 2); p(0, 2) }
        )

        assertEquals(1.R,game.getRow(0)[0].a)
        assertEquals(2.R,game.getRow(0)[0].b)

        assertEquals(0.R,game.getColumn(1)[0].a)
        assertEquals(4.R,game.getColumn(1)[0].b)

        assertEquals(0.R,game.getColumn(1)[1].a)
        assertEquals(2.R,game.getColumn(1)[1].b)

        assertEquals(2.R,game.getColumn(0)[1].b)
        assertEquals(4.R,game.getColumn(1)[0].b)
    }


    @Test
    fun stronglyDominantTest() {
        val game = TwoPlayerGame.create (
             { p(4, 2) ; p(3, 1) },
             { p(2,2) ; p(2,2) }
        )

        assertEquals(true,game.isStronglyDominantStrategy(Player.ROW,0))
        assertEquals(false,game.isStronglyDominantStrategy(Player.ROW,1))
        assertEquals(false,game.isStronglyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,game.isStronglyDominantStrategy(Player.COLUMN,1))

        val game2 = TwoPlayerGame.create (
             { p(4, 4) ; p(3, 1) },
             { p(2,3) ; p(2,2) }
        )

        assertEquals(true,game2.isStronglyDominantStrategy(Player.ROW,0))
        assertEquals(false,game2.isStronglyDominantStrategy(Player.ROW,1))
        assertEquals(true,game2.isStronglyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,game2.isStronglyDominantStrategy(Player.COLUMN,1))
    }


    @Test
    fun weaklyDominantTest() {
        val game = TwoPlayerGame.create (
                { p(4, 2) ; p(3, 1) },
                { p(2,2) ; p(2,2) }
        )

        assertEquals(true,game.isWeaklyDominantStrategy(Player.ROW,0))
        assertEquals(false,game.isWeaklyDominantStrategy(Player.ROW,1))
        assertEquals(true,game.isWeaklyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,game.isWeaklyDominantStrategy(Player.COLUMN,1))

        val game2 = TwoPlayerGame.create (
                { p(4, 4) ; p(3, 1) },
                { p(2,3) ; p(2,2) }
        )

        assertEquals(true,game2.isWeaklyDominantStrategy(Player.ROW,0))
        assertEquals(false,game2.isWeaklyDominantStrategy(Player.ROW,1))
        assertEquals(true,game2.isWeaklyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,game2.isWeaklyDominantStrategy(Player.COLUMN,1))
    }


    @Test
    fun veryWeaklyDominantTest() {
        val game = TwoPlayerGame.create {
            row { p(4, 2); p(3, 2) }
            row { p(2, 2); p(2, 2) }
        }

        assertEquals(true,game.isVeryWeaklyDominantStrategy(Player.ROW,0))
        assertEquals(false,game.isVeryWeaklyDominantStrategy(Player.ROW,1))
        assertEquals(true,game.isVeryWeaklyDominantStrategy(Player.COLUMN,0))
        assertEquals(true,game.isVeryWeaklyDominantStrategy(Player.COLUMN,1))

        val game2 = TwoPlayerGame.create {
            row { p(4, 4); p(3, 1) }
            row { p(2, 3); p(2, 2) }
        }

        assertEquals(true,game2.isVeryWeaklyDominantStrategy(Player.ROW,0))
        assertEquals(false,game2.isVeryWeaklyDominantStrategy(Player.ROW,1))
        assertEquals(true,game2.isVeryWeaklyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,game2.isVeryWeaklyDominantStrategy(Player.COLUMN,1))
    }




    @Test
    fun nashEquilibriumTest() {

        //Prisoners dilemma
        val game = TwoPlayerGame.create (
                { p(-2, -2); p(-8, -1) },
                { p(-1, -8); p(-5, -5) }
        )

        assertEquals(false,game.isNashEquilibrium(0,0))
        assertEquals(false,game.isNashEquilibrium(0,1))
        assertEquals(false,game.isNashEquilibrium(1,0))
        assertEquals(true,game.isNashEquilibrium(1,1))

        //BOS
        val game2 = TwoPlayerGame.create (
                { p(2, 1); p(0, 0) },
                { p(0, 0); p(1, 2) }
        )

        assertEquals(true,game2.isNashEquilibrium(0,0))
        assertEquals(false,game2.isNashEquilibrium(0,1))
        assertEquals(false,game2.isNashEquilibrium(1,0))
        assertEquals(true,game2.isNashEquilibrium(1,1))
    }


    @Test
    fun `single not weakly dominant nash equilibrium`() {

        val game = TwoPlayerGame.create {
            columnLabels("A","B","C")
            row("A") { p(-1, -1); p(0, 0); p(1, 1) }
            row("B") { p(0, 0); p(-1, -1); p(-1, -1) }
            row("C") { p(1, 1); p(-1, -1); p(2, 2) }
        }

        assertEquals(1,game.pureNashEquilibriums().size)
        assertEquals(false,game.isNashEquilibrium("A","C"))
        assertEquals(false,game.isNashEquilibrium("C","A"))

        assertEquals(true,game.isNashEquilibrium("C","C"))
        assertEquals(false,game.isVeryWeaklyDominantStrategy(Player.ROW,"C"))
        assertEquals(false,game.isVeryWeaklyDominantStrategy(Player.COLUMN,"C"))

    }

    @Test
    fun `a single weakly dominant equilibrium`() {

        val game = TwoPlayerGame.create {
            columnLabels("A","B","C")
            row("A") { p(-1, -1); p(0, 0); p(1, 1) }
            row("B") { p(0, 0); p(-1, -1); p(0, 0) }
            row("C") { p(1, 1); p(0, 0); p(2, 2) }
        }

        val eq = game.pureNashEquilibriums()

        assertEquals(1,eq.size)
        assertEquals(true,game.isNashEquilibrium("C","C"))
        assertEquals(true,game.isWeaklyDominantStrategy(Player.ROW,"C"))
        assertEquals(false,game.isStronglyDominantStrategy(Player.ROW,"C"))

    }



    @Test
    fun `one single weak equilibirum, and one only nash`() {

        val game = TwoPlayerGame.create {
            columnLabels("A","B")
            row("A") { p(2, 2); p(2, 2) }
            row("B") { p(2, 2); p(3, 3) }
        }

        val eq = game.pureNashEquilibriums()

        assertEquals(2,eq.size)
        assertEquals(true,game.isNashEquilibrium("A","A"))
        assertEquals(true,game.isNashEquilibrium("B","B"))

        assertEquals(true,game.isWeaklyDominantStrategy(Player.ROW,"B"))
        assertEquals(false,game.isWeaklyDominantStrategy(Player.ROW,"A"))

    }


    @Test
    fun `maxMin and minMax of modified prisoners dilemma`() {

        val game = TwoPlayerGame.create {
            columnLabels("NC","C")
            row("NC") { p(-3, -3); p(-8, -2) }
            row("C") { p(-1, -8); p(-6, -6) }
        }

        val eq = game.pureNashEquilibriums()

        assertEquals(1,eq.size)
        assertEquals((-6).R,game.maxMin(Player.ROW))
        assertEquals((-6).R,game.maxMin(Player.COLUMN))
        assertEquals((-6).R,game.minMax(Player.ROW))
        assertEquals((-6).R,game.minMax(Player.COLUMN))
        assertEquals(listOf(1),game.maxMinStrategies(Player.ROW))
        assertEquals(listOf(1),game.maxMinStrategies(Player.COLUMN))
        assertEquals(listOf(1),game.minMaxStrategies(Player.ROW))
        assertEquals(listOf(1),game.minMaxStrategies(Player.COLUMN))

    }

}
