package runners

import org.slf4j.LoggerFactory
import solver.backwardsinduction.BackwardsInductionSolution
import solver.compareStrategies
import solver.expectedvalue.ExpectedValue
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/*
 * Copyright (c) Jonas Waage 02/09/2020
 */

fun main() {
    val logger = LoggerFactory.getLogger("ExpectedValue-vs-backwardsinduction")

    val ultimate = BackwardsInductionSolution();
    BufferedReader(FileReader(File("game1000-solver/solutions/ultimate/ultimate.json"))).use {
        ultimate.readSolution(it);
    }

    val expectedValue = ExpectedValue();

    logger.info("Running comparison of expected value based strategy and backwards induction based strategy: ")
    compareStrategies(
            st1 = expectedValue::runExpectedValueStrategy,
            st2 = ultimate::play,
            detailLog = true,
            runLog = false,
            allowHistory = false
    );
}