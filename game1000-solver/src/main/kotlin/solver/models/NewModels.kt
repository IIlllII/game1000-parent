package solver.models

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import lcp.Rational



data class StateChoice(val st : State, val pos : Pos)

@Serializable
data class Serialized(val state: State, val strategy: Strategy)

@Serializable
data class SerializedNu(val state: State, @Polymorphic val strategy: Node)


data class Result(val rate : Rational, val result : Int, val choices : List<StateChoice>)


@Serializable
@Polymorphic
sealed class Node

@Serializable
@SerialName("solver.Final")
data class Final(val result : Int) : Node()

@Serializable
@SerialName("solver.Strategy")
data class Strategy(val placements: List<MixedPlacement>) : Node()

//New node type with different semantics... but what to do
@Serializable
@SerialName("solver.DegenerateNode")
data class DegenerateNode(val choices : List<MixedPlacement>) : Node()

@Serializable
data class MixedPlacement(
        val rateOfPlay : Rational,
        val pos : Pos
)
