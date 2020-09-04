package solver.backwardsinduction

import lcp.Rational
import solver.models.MixedPlacement


sealed class SubGameRes

data class SubGameResult(val payoff : Rational, val choices : List<MixedPlacement>) : SubGameRes()

data class Fin(val result : Rational) : SubGameRes()