package solver

import allPermutations
import org.slf4j.LoggerFactory
import solver.models.ComparisonResults
import solver.models.Pos
import solver.models.State
import solver.models.StateAndHist
import kotlin.math.abs

private val logger = LoggerFactory.getLogger("ComparisonLogger")

/**
 *
 * Clean comparison of two strategies
 *
 * @param st1 strategy 1
 * @param st2 strategy 2
 *
 * @param preList starting random numbers
 * @param detailLog log more or less
 * @param specialStart start with a special state and history
 * @param loops how many game loops should be run
 * @param allowHistory allow history to be used in strategies
 *
 * @param winCallBackS1 called when player 1 wins a game, no impl by default
 * useful for creating statistics
 * @param winCallBackS2 called when player 2 wins a game, no impl by default
 * useful for creating statistics
 *
 * @param resultCallbackS1 callback with result for player 1, no impl by default
 * useful for creating statistics
 * @param resultCallbackS2 callback with result for player 2, no impl by default
 * useful for creating statistics
 */
fun compareStrategies(st1 : (StateAndHist) -> Pos,
                      st2 : (StateAndHist) -> Pos,
                      preList : List<Int> = emptyList(),
                      detailLog : Boolean = false,
                      specialStart : StateAndHist? = null,
                      loops : Int = 9,
                      allowHistory: Boolean = true,
                      runLog : Boolean = false,
                      winCallBackS1 : (game: List<Int>,distance : Int) -> Unit = {_,_ -> },
                      winCallBackS2 : (game: List<Int>,distance : Int) -> Unit = {_,_ -> },
                      resultCallbackS1 : (game: List<Int>,distance : Int) -> Unit = {_,_->},
                      resultCallbackS2 : (game: List<Int>,distance : Int) -> Unit = {_,_->}) : ComparisonResults {

    val startState = specialStart ?: StateAndHist.empty();

    /**
     * Start at a specific node with set current value
     */
    if(specialStart != null) {
        val num = specialStart.game.size + loops;
        if(num != 9) {
            throw IllegalArgumentException("Must have 9 rounds got $num");
        }
    }

    var draw = 0L;
    var win1 = 0L;
    var win2 = 0L;
    var games = 0L;
    var averageDistance :Long = 0;
    var averageDistance2 :Long = 0;


    val modLoop = if(specialStart != null) loops-1 else loops;

    allPermutations(preList,1..6,modLoop) { fullGame ->
        val thegame = if(specialStart != null) listOf(specialStart.state.current)+fullGame else fullGame;

        val r1 = playOutGame(startState,st1,thegame,allowHistory,runLog);
        val r2 = playOutGame(startState,st2,thegame,allowHistory,runLog);

        resultCallbackS1(thegame,r1)
        resultCallbackS2(thegame,r2)

        averageDistance += r1;
        averageDistance2 += r2;

        when {
            r1 < r2 -> { win1++;winCallBackS1(thegame,r1) }
            r2 < r1 -> { win2++;winCallBackS2(thegame,r2) }
            else -> draw++
        }

        games++;
    };

    if(detailLog) {
        logger.info("---------------------------------------------")
        logger.info("Difference: ${win1 - win2}  ${(abs(win1.toDouble() - win2.toDouble()) * 100) / games.toDouble()} %");
        logger.info("W1:  $win1 ${(win1.toDouble() * 100) / games.toDouble()} %");
        logger.info("W2:  $win2 ${(win2.toDouble() * 100) / games.toDouble()} %");
        logger.info("Average distance p1: ${averageDistance.toDouble()/games.toDouble()} %");
        logger.info("Average distance p2: ${averageDistance2.toDouble()/games.toDouble()} %");
        logger.info("Draw: $draw ${(draw.toDouble() * 100) / games.toDouble()} %");
    }

    val result = ComparisonResults(
            gamesPlayed = games,
            winPlayer1 = win1,
            winPlayer2 = win2,
            draws = draw,
            averageDistanceTo1000Player1 = averageDistance.toDouble()/games.toDouble(),
            averageDistanceTo1000Player2 = averageDistance2.toDouble()/games.toDouble(),
            player1Strategy = st1,
            player2Strategy = st2
    );

    when {
        win1 > win2 -> {
            logger.info("Strategy 1 is the better one");
        }
        win2 > win1 -> {
            logger.info("Strategy 2 is the better one")
        }
        else -> {
            logger.info("Equally good strategies")
        }
    }
    return result;
}

/**
 * Play out the game using the given playout strategy to select the moves
 *
 * @param state current state with history
 * @param playoutStrategy go from state to an action
 * @param game rest of the game to play
 * @param allowHistory is it allowed to look into the game history
 * @param runLog enable logging, for debugging
 * @param stateHook inspection of the current state, for debugging
 */
fun playOutGame(
        state : StateAndHist,
        playoutStrategy: (StateAndHist) -> Pos,
        game: List<Int>,
        allowHistory: Boolean,
        runLog : Boolean = false,
        stateHook : (state : State) -> Unit = {}) : Int {
    if(state.state.isFull()){
        if(runLog) {
            logger.debug("--------- Result ---------")
            logger.debug("Rd $game");
            logger.debug("State $state")
            logger.debug("Result ${state.state.stateDistance()}")
            logger.debug("Game ${state.game}")
            logger.debug("--------------------------")
        }
        return state.state.stateDistance();
    }
    else {
        val first = game.first();
        val hist = state.game+first;
        val pl = state.state.newCurrent(first);
        stateHook.invoke(pl)
        val pick = playoutStrategy(StateAndHist(if(allowHistory) hist else emptyList() ,pl));
        val nextState = pl.addToPos(pick)
        val next = game.drop(1);
        return playOutGame(StateAndHist(hist,nextState),playoutStrategy,next,allowHistory,runLog,stateHook);
    }
}