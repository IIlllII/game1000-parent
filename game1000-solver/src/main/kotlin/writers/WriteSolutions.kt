package writers

import solver.backwardsinduction.BackwardsInductionSolution
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/*
 * Copyright (c) Jonas Waage 03/09/2020
 */
fun main() {
    val ul = BackwardsInductionSolution();
    ul.createSolution();

    BufferedWriter(FileWriter(File("solutions/ultimate/backwards-induction-1.json")))
            .use { writer ->
                ul.storeSolution(writer)
            }
}