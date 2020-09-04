package bimatrix

import lcp.Rational


operator fun Rational.unaryMinus(): Rational {
    return this.negate()
}

val Int.R
    get() = Rational.valueOf(this.toLong())