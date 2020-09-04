package bimatrix

import lcp.Rational

class Equilibrium {

    var vertex1: Int? = null
    var vertex2: Int? = null

    var probVec1: Array<Rational>? = null //length = #rows (1 per strategy)
    var payoff1: Rational? = null

    var probVec2: Array<Rational>? = null //length = #cols (1 per strategy)
    var payoff2: Rational? = null

    constructor()

    constructor(vertex1: Int, vertex2: Int) {
        this.vertex1 = vertex1
        this.vertex2 = vertex2
    }
}
