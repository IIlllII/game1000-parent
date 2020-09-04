package mcts

import org.junit.Test
import org.slf4j.LoggerFactory
import solver.mcts.mctsForNode
import solver.models.Pos
import solver.models.State
import utils.getRandomSequence
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
 * Copyright (c) Jonas Waage 02/09/2020
 */
class MctsTest {

    private val logger = LoggerFactory.getLogger("MctsTest")

    @Test
    fun testProv() {
        val random = Random(97876985)
        val a = random.getRandomSequence(5,1..6);
        assertEquals(5,a.size);
        a.forEach { i-> assertTrue(i in 1..6) };
    }


    @Test
    fun testMTCS() {
        val random = Random(8998098098)
        val rounds = 10000;
        for(i in 1..6) {
            val r = mctsForNode(
                    random,
                    State.setupGameState(i),
                    rounds,
                    true,
                    sqrt(2.0)
            );
            logger.info("$r\n\n---------------\n");

        }

    }

    @Test
    fun testMTCSSimple() {
        val random = Random(98723090)
        val rounds = 10000;
        for(i in 1..6) {
            val pos = mctsForNode(
                    random,
                    State.setupGameState(i,
                            Pair(Pos.HUNDREDS,5),
                            Pair(Pos.HUNDREDS,4),
                            Pair(Pos.TENS,3),
                            Pair(Pos.TENS,3),
                            Pair(Pos.ONES,2),
                            Pair(Pos.ONES,2)),
                    rounds,
                    true,
                    2* sqrt(2.0)
            );
            println("$pos\n\n---------------\n");
            when {
                i <= 2 -> {
                    assertEquals(pos, Pos.HUNDREDS);
                }
                i <= 4 -> {
                    assertEquals(pos, Pos.TENS);
                }
                else -> {
                    assertEquals(pos, Pos.ONES);
                }
            }
        }

    }


    @Test
    fun testMTCSVerySimple() {
        val random = Random(8709798709)
        val rounds = 10000;
        for(i in 1..6) {
            val pos = mctsForNode(
                    random,
                    State.setupGameState(i,
                            Pair(Pos.HUNDREDS,5),
                            Pair(Pos.HUNDREDS,4),
                            Pair(Pos.TENS,2),
                            Pair(Pos.TENS,2),
                            Pair(Pos.TENS,2),
                            Pair(Pos.ONES,2),
                            Pair(Pos.ONES,2)),
                    rounds,
                    true,
                    2* sqrt(2.0)
            );
            logger.info("$pos\n\n---------------\n");
            when {
                i <= 3 -> {
                    assertEquals(pos, Pos.HUNDREDS);
                }
                else -> {
                    assertEquals(pos, Pos.ONES);
                }
            }
        }
    }


}