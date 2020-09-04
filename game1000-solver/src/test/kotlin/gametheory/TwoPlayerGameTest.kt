package gametheory

import bimatrix.R
import org.junit.Test
import kotlin.test.assertEquals

/*
 * Copyright (c) Jonas Waage 04/09/2020
 */

class TwoPlayerGameTest {

    @Test
    fun testGetRowCol() {
        val vl = TwoPlayerGame {
            row { p(1, 2) ; p(0, 4) }
            row { p(1,2) ; p(0,2) }
        }

        assertEquals(1.R,vl.getRow(0)[0].a)
        assertEquals(2.R,vl.getRow(0)[0].b)

        assertEquals(0.R,vl.getColumn(1)[0].a)
        assertEquals(4.R,vl.getColumn(1)[0].b)

        assertEquals(0.R,vl.getColumn(1)[1].a)
        assertEquals(2.R,vl.getColumn(1)[1].b)

        assertEquals(2.R,vl.getColumn(0)[1].b)
        assertEquals(4.R,vl.getColumn(1)[0].b)
    }


    @Test
    fun stronglyDominantTest() {
        val vl = TwoPlayerGame {
            row { p(4, 2) ; p(3, 1) }
            row { p(2,2) ; p(2,2) }
        }

        assertEquals(true,vl.isStronglyDominantStrategy(Player.ROW,0))
        assertEquals(false,vl.isStronglyDominantStrategy(Player.ROW,1))
        assertEquals(false,vl.isStronglyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,vl.isStronglyDominantStrategy(Player.COLUMN,1))

        val vl2 = TwoPlayerGame {
            row { p(4, 4) ; p(3, 1) }
            row { p(2,3) ; p(2,2) }
        }

        assertEquals(true,vl2.isStronglyDominantStrategy(Player.ROW,0))
        assertEquals(false,vl2.isStronglyDominantStrategy(Player.ROW,1))
        assertEquals(true,vl2.isStronglyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,vl2.isStronglyDominantStrategy(Player.COLUMN,1))
    }


    @Test
    fun weaklyDominantTest() {
        val vl = TwoPlayerGame {
            row { p(4, 2) ; p(3, 1) }
            row { p(2,2) ; p(2,2) }
        }

        assertEquals(true,vl.isWeaklyDominantStrategy(Player.ROW,0))
        assertEquals(false,vl.isWeaklyDominantStrategy(Player.ROW,1))
        assertEquals(true,vl.isWeaklyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,vl.isWeaklyDominantStrategy(Player.COLUMN,1))

        val vl2 = TwoPlayerGame {
            row { p(4, 4) ; p(3, 1) }
            row { p(2,3) ; p(2,2) }
        }

        assertEquals(true,vl2.isWeaklyDominantStrategy(Player.ROW,0))
        assertEquals(false,vl2.isWeaklyDominantStrategy(Player.ROW,1))
        assertEquals(true,vl2.isWeaklyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,vl2.isWeaklyDominantStrategy(Player.COLUMN,1))
    }


    @Test
    fun veryWeaklyDominantTest() {
        val vl = TwoPlayerGame {
            row { p(4, 2) ; p(3, 2) }
            row { p(2,2) ; p(2,2) }
        }

        assertEquals(true,vl.isVeryWeaklyDominantStrategy(Player.ROW,0))
        assertEquals(false,vl.isVeryWeaklyDominantStrategy(Player.ROW,1))
        assertEquals(true,vl.isVeryWeaklyDominantStrategy(Player.COLUMN,0))
        assertEquals(true,vl.isVeryWeaklyDominantStrategy(Player.COLUMN,1))

        val vl2 = TwoPlayerGame {
            row { p(4, 4) ; p(3, 1) }
            row { p(2,3) ; p(2,2) }
        }

        assertEquals(true,vl2.isVeryWeaklyDominantStrategy(Player.ROW,0))
        assertEquals(false,vl2.isVeryWeaklyDominantStrategy(Player.ROW,1))
        assertEquals(true,vl2.isVeryWeaklyDominantStrategy(Player.COLUMN,0))
        assertEquals(false,vl2.isVeryWeaklyDominantStrategy(Player.COLUMN,1))
    }




    @Test
    fun nashEquilibriumTest() {

        //Prisoners dilemma
        val vl = TwoPlayerGame {
            row { p(-2, -2) ; p(-8, -1) }
            row { p(-1,-8) ; p(-5,-5) }
        }

        assertEquals(false,vl.isNashEquilibrium(0,0))
        assertEquals(false,vl.isNashEquilibrium(0,1))
        assertEquals(false,vl.isNashEquilibrium(1,0))
        assertEquals(true,vl.isNashEquilibrium(1,1))

        //BOS
        val vl2 = TwoPlayerGame {
            row { p(2, 1) ; p(0, 0) }
            row { p(0,0) ; p(1,2) }
        }

        assertEquals(true,vl2.isNashEquilibrium(0,0))
        assertEquals(false,vl2.isNashEquilibrium(0,1))
        assertEquals(false,vl2.isNashEquilibrium(1,0))
        assertEquals(true,vl2.isNashEquilibrium(1,1))
    }


}
