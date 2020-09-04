package solver.tiebreaker

import solver.models.Pos

/*
 * Copyright (c) Jonas Waage 02/09/2020
 */
object TieBreakers {

    /**
     * For equal expected value we pick hundred > ten > one as placements
     */
    val PRIO_HUN_TEN_ONE : (List<Pos>) -> Pos = {
        when {
            it.contains(Pos.HUNDREDS) -> {
                Pos.HUNDREDS
            }
            it.contains(Pos.TENS) -> {
                Pos.TENS
            }
            else -> {
                Pos.ONES
            }
        }
    }
}