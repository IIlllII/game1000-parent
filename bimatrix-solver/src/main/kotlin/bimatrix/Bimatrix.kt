package bimatrix

import lcp.ColumnTextWriter
import lcp.Rational
import java.util.*

class Bimatrix(private val names: Array<String>?, private val a: Array<Array<Rational>>?, private val b: Array<Array<Rational>>, private val rowNames: Array<String>, private val colNames: Array<String>) {

    fun nrows(): Int {
        return a?.size ?: 0
    }

    fun ncols(): Int {
        return if (nrows() > 0) a!![0].size else 0
    }

    fun row(idx: Int): String {
        return strat(rowNames, idx)
    }

    fun col(idx: Int): String {
        return strat(colNames, idx)
    }

    fun firstPlayer(): String {
        return name(0)
    }

    fun secondPlayer(): String {
        return name(1)
    }

    private fun name(idx: Int): String {
        return if (names != null && names.size > 0 && names[idx] != null) names[idx] else (idx + 1).toString()
    }


    private fun strat(arr: Array<String>?, idx: Int): String {
        return if (arr != null && idx < arr.size && arr[idx] != null) arr[idx] else (idx + 1).toString()
    }

    override fun toString(): String {
        val colpp = ColumnTextWriter()
        addMatrix(0, colpp)
        colpp.endRow()
        addMatrix(1, colpp)
        return colpp.toString()
    }


    private fun addMatrix(idx: Int, colpp: ColumnTextWriter) {
        colpp.writeCol(name(idx))
        colpp.alignLeft()
        for (colName in colNames) {
            colpp.writeCol(colName)
        }
        colpp.endRow()

        val mat = if (idx == 0) a else b
        for (i in mat!!.indices) {
            colpp.writeCol(rowNames[i])
            val row = mat[i]
            for (entry in row) {
                colpp.writeCol(entry.toString())
            }
            colpp.endRow()
        }
    }


    fun printFormat(): String {
        var s = ""
        s += a!!.size.toString() + " " + a[0].size + lineSeparator
        s += lineSeparator
        for (i in a.indices) {
            for (j in 0 until a[i].size) {
                s += " " + a[i][j]
            }
            s += lineSeparator
        }

        s += lineSeparator
        s += lineSeparator
        for (i in b.indices) {
            for (j in 0 until b[i].size) {
                s += " " + b[i][j]
            }
            s += lineSeparator
        }

        return s
    }


    fun printFormatHTML(): String {
        var s = ""
        s += this.nrows().toString() + " x " + this.ncols() + " Payoff player 1" + lineSeparator

        s += lineSeparator
        s += buildMatrixString(buildString(a))

        /*
		for (int i=0;i<a.length;i++){
			for (int j=0;j<a[i].length;j++){
				s+=" "+a[i][j];
			}
			s+=lineSeparator;
		}*/

        s += lineSeparator
        s += lineSeparator
        s += this.nrows().toString() + " x " + this.ncols() + " Payoff player 2" + lineSeparator
        s += lineSeparator
        s += buildMatrixString(buildString(b))
        /*
		for (int i=0;i<b.length;i++){
			for (int j=0;j<b[i].length;j++){
				s+=" "+b[i][j];
			}
			s+=lineSeparator;
		}*/
        s += lineSeparator
        return s
    }


    /**
     * Create a String from the array of payoffs
     * @param pm:Array - 2-dim Array of payoffs
     * @return String - an return seperated string with all payoffs.
     * @author Martin
     */
    private fun buildString(pm: Array<Array<Rational>>?): Array<Array<String>>? {

        if (pm == null)
            return null
        if (pm[0] == null)
            return null

        val pm_out = Array<Array<String>>(pm.size + 1) { Array(pm[0].size + 1) {""} }

        for (i in 0 until pm_out[0].size) {
            if (i == 0) {
                pm_out[0][0] = ""
            } else {
                pm_out[0][i] = this.colNames[i - 1]
            }
        }

        for (i in pm_out.indices) {
            if (i == 0) {
                pm_out[0][0] = ""
            } else {
                pm_out[i][0] = this.rowNames[i - 1]
            }
        }

        for (i in 1 until pm_out.size) {
            for (j in 1 until pm_out[i].size) {
                pm_out[i][j] = pm[i - 1][j - 1].toString()
            }
        }

        return pm_out
    }


    /**
     * Create a String from the array of payoffs
     * @param pm:Array - 2-dim Array of payoffs
     * @return String - an return seperated string with all payoffs.
     * @author Martin
     */
    private fun buildMatrixString(pm: Array<Array<String>>?): String {


        val delimeter = " "
        val maxLength = LinkedList<Int>()
        var i = 0
        var j = 0

        if (pm == null)
            return ""
        if (pm[0] == null)
            return ""

        j = 0
        while (j < pm[0].size) {
            var maxLen = 0
            i = 0
            while (i < pm.size) {
                if (pm[i][j] != null) {
                    if (pm[i][j].length > maxLen) {
                        maxLen = pm[i][j].length
                    }

                }
                i++
            }

            maxLength.add(Integer.valueOf(maxLen))
            j++
        }

        var matrixString = ""
        i = 0
        while (i < pm.size) {
            j = 0
            while (j < pm[i].size) {

                for (w in 0 until maxLength[j] - pm[i][j].length) {
                    matrixString += " "
                }
                matrixString += pm[i][j]
                if (j < pm[i].size - 1) {
                    matrixString += delimeter
                }
                j++

            }
            if (i < pm.size - 1) {
                matrixString += lineSeparator
            }
            i++
        }
        return matrixString

    }

    companion object {

        internal val lineSeparator = System.getProperty("line.separator")
    }


}
