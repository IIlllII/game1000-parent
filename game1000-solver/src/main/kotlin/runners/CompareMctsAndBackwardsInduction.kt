package runners

import converters.readSolution
import org.slf4j.LoggerFactory
import solver.StrategyPlayout
import solver.backwardsinduction.BackwardsInductionSolution
import solver.compareStrategies
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/*
 * Copyright (c) Jonas Waage 02/09/2020
 */

fun main() {
    val logger = LoggerFactory.getLogger("New-backwards-induction-vs-old-backwards-induction")

    val ultimate = BackwardsInductionSolution();
    BufferedReader(FileReader(File("game1000-solver/solutions/ultimate/ultimate.json"))).use {
        ultimate.readSolution(it);
    }

    val hunTenOne = StrategyPlayout(readSolution(
            File("game1000-solver/solutions/old/prio-hun-ten-one.json")
    ));

    logger.info("Comparison of two backwards induction based strategies, with different resolution algorithms")

    compareStrategies(
            st1 = hunTenOne::placementForState,
            st2 = ultimate::play,
            detailLog = true,
            runLog = false,
            allowHistory = false
    );
}