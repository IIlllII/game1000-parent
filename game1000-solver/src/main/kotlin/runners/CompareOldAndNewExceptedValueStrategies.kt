package runners

import converters.SimpleState
import converters.readOldSolution
import converters.readSolution
import org.slf4j.LoggerFactory
import solver.StrategyPlayout
import solver.backwardsinduction.BackwardsInductionSolution
import solver.compareStrategies
import solver.expectedvalue.ExpectedValue
import solver.models.StateAndHist
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/*
 * Copyright (c) Jonas Waage 02/09/2020
 */

fun main() {
    val logger = LoggerFactory.getLogger("New-expected-value-vs-old-expected-value")

    val exOld = readOldSolution(
            File("game1000-solver/solutions/old/ai_states_EX.json")
    );

    val oldStored = { s: StateAndHist ->
        val simple = SimpleState(
                current = s.state.current,
                hundreds = s.state.plHun,
                tens = s.state.plTen,
                ones = s.state.plOne,
                sum = s.state.stateValue())

        exOld[simple] ?: throw IllegalStateException("No action")
    }

    val expectedValue = ExpectedValue();

    logger.info("Compare old and new expected value strategies")
    compareStrategies(
            st1 = oldStored,
            st2 = expectedValue::runExpectedValueStrategy,
            detailLog = true,
            runLog = false,
            allowHistory = false
    );
}