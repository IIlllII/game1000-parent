package lrs

import lcp.Rational
import java.util.ArrayList

class VPolygon {
    var vertices: ArrayList<Array<Rational>> = ArrayList() // may also contain rays
    var cobasis: ArrayList<Array<Int>> = ArrayList()
    //boolean hull = true; //when is this false?  I should try to set this as an output param as well
    //boolean voronoi = false; //(if true, then poly is false)

    /* all rows must have a one in column one */
    fun polytope(): Boolean {
        var ispoly = true
        for (row in vertices) {
            if (row.size < 1 || !row[0].isOne) {
                ispoly = false
            }
        }
        return ispoly
    }

    /* for V-rep, all zero in column 1     */
    fun homogeneous(): Boolean {
        var ishomo = true
        for (row in vertices) {
            if (row.size < 2 || !row[1].isZero) {
                ishomo = false
            }
        }
        return ishomo
    }
}
