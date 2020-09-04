package runners

import org.slf4j.LoggerFactory
import solver.backwardsinduction.BackwardsInductionSolution
import solver.compareStrategies
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/*
 * Copyright (c) Jonas Waage 02/09/2020
 */

fun main() {
    val logger = LoggerFactory.getLogger("Backwards-induction-versions-comparison")

    val ultimate = BackwardsInductionSolution();
    BufferedReader(FileReader(File("solutions/ultimate/ultimate.json"))).use {
        ultimate.readSolution(it);
    }

    val ultimateOtherEq = BackwardsInductionSolution();
    BufferedReader(FileReader(File("solutions/ultimate/ultimate-latest-with-degen-nodes-2.json"))).use {
        ultimateOtherEq.readSolutionSer(it);
    }

    /*
    * TODO This has mixed strategies, so need to run lots of runs to get a proper comparison
    */
    logger.info("Comparing backwards induction versions")
    (1..5).forEach { _ ->
        compareStrategies(
                st1 = ultimate::play,
                st2 = ultimateOtherEq::play,
                detailLog = true,
                runLog = false,
                allowHistory = true
        );
    }
}