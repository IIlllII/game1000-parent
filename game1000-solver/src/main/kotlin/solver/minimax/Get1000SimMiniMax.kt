package solver.minimax

import solver.models.State

/**
 * Stores the minimax states of two players taking simultaneous moves
 * in get1000
 */
data class Get1000SimState(val player1 : State, val player2: State) : SimState {
    override fun player1State(): State {
        return player1;
    }

    override fun player2State(): State {
        return player2;
    }

    override fun nextPossibleStates(): List<Choices> {
        val a = player1.getPossiblePlacements();
        val b = player2.getPossiblePlacements();
        return a.flatMap { i-> b.map {j -> Choices(i,j, Get1000SimState(player1.addToPos(i),player2.addToPos(j)))  } }
    }

    override fun possibilitiesMatrix(): Array<Array<Choices>> {
        val a = player1.getPossiblePlacementsOrdered();
        val b = player2.getPossiblePlacementsOrdered();

        return Array(a.size) { i: Int ->
            Array(b.size) { j: Int ->
                val choicePl1 = a[i];
                val choicePl2 = b[j];
                val state = Get1000SimState(player1.addToPos(choicePl1), player2.addToPos(choicePl2));
                Choices(choicePl1, choicePl2, state);
            }
        };
    }


    override fun movesByNature(): List<SimState> {
        return (1..6).map { i-> Get1000SimState(player1.newCurrent(i),player2.newCurrent(i))  }
    }

    /**
     *
     * Player 2 will do well with positive moves.
     * Player 1 will do well with negative moves.
     *
     */
    override fun payoff(): Double {
        val plA = player1.stateDistance().toDouble();
        val plB = player2.stateDistance().toDouble();

        if(!isEnded()) {
            throw IllegalStateException()
        }

        return when {
            plA < plB -> -1.0
            plA > plB -> 1.0
            else -> 0.0
        }
    }


    override fun isEnded(): Boolean {
        return player1.isFull() && player2.isFull();
    }


}