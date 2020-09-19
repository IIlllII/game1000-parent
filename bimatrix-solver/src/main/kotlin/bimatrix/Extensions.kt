package bimatrix

import lcp.Rational


val Int.R
    get() = Rational.valueOf(this.toLong())