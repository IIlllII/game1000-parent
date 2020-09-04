package utils

import lcp.Rational

/*
 * Copyright (c) Jonas Waage 03/09/2020
 */

operator fun Rational.plus(r : Rational) : Rational {
    return this.add(r)
}

operator fun Rational.times(r : Rational) : Rational {
    return this.multiply(r)
}

operator fun Rational.minus(r : Rational) : Rational {
    return this.subtract(r)
}

operator fun Rational.unaryMinus() : Rational {
    return this.negate()
}