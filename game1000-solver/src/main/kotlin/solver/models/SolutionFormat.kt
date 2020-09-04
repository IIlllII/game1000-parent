package solver.models


data class Override(val history : List<Int>, val pos : Pos)

data class SolutionFormat(
        val state : State,
        val defaultPlacement: Pos,
        val overrides : List<Override>
)