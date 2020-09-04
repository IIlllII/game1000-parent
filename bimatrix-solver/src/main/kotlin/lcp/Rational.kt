package lcp

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import lcp.BigIntegerUtils.greater
import lcp.BigIntegerUtils.negative
import lcp.BigIntegerUtils.one
import lcp.BigIntegerUtils.positive
import lcp.BigIntegerUtils.zero
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*


@Serializer(forClass = BigInteger::class)
object BigIntSerializer : KSerializer<BigInteger> {
    override val descriptor =
            PrimitiveSerialDescriptor("BigIntSerializer", PrimitiveKind.STRING)


    override fun serialize(encoder: Encoder, value: BigInteger) {
        encoder.encodeString(value.toString());
    }

    override fun deserialize(decoder: Decoder): BigInteger {
        return BigInteger(decoder.decodeString())
    }

}

@Serializable
data class Rational(
        @Serializable(with = BigIntSerializer::class) var num: BigInteger,
        @Serializable(with = BigIntSerializer::class) var den: BigInteger) {

    init {
        if (zero(den)) {
            throw ArithmeticException("Divide by zero")
        } else if (!one(den)) {
            reduce()
        }
    }


    //Num and Den are only used by Tableau... I'd like to get rid of them, but can't see how
    @Transient
    val isZero: Boolean
        get() = zero(num)

    @Transient
    val isOne: Boolean
        get() = one(num) && one(den) // should be reduced at all times

    constructor(toCopy: Rational) : this(toCopy.num,toCopy.den)

    /* reduces Na Da by gcd(Na,Da) */
    private fun reduce() {
        if (!zero(num)) {
            if (negative(den)) {
                den = den.negate()
                num = num.negate()
            }
            val gcd = num.gcd(den)
            if (!one(gcd)) {
                num = num.divide(gcd)
                den = den.divide(gcd)
            }
        } else {
            den = BigInteger.ONE
        }
    }

    constructor(numerator: Long, denominator: Long) : this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator)) {}

    private fun addEq(toAdd: Rational) {
        if (den == toAdd.den) {
            num = num.add(toAdd.num)
        } else {
            num = num.multiply(toAdd.den)
            num = num.add(toAdd.num.multiply(den))
            den = den.multiply(toAdd.den)
        }
        reduce()
    }

    private constructor(value: Long) : this(value, 1) {}

    private fun mulEq(other: Rational) {
        num = num.multiply(other.num)
        den = den.multiply(other.den)
        reduce()
    }

    //Helper Methods
    private fun flip() /* aka. Reciprocate */ {
        val x = num
        num = den
        den = x
    }

    fun add(b: Rational): Rational {
        if (zero(num))
            return b
        else if (zero(b.num)) return this
        val rv = Rational(this)
        rv.addEq(b)
        return rv
    }

    fun add(b: Long): Rational {
        if (b == 0L) return this
        val rv = Rational(b)
        rv.addEq(this)
        return rv
    }

    fun subtract(b: Rational): Rational {
        if (zero(b.num)) return this
        val c = b.negate()
        if (!zero(num))
            c.addEq(this)
        return c
    }

    fun subtract(b: Long): Rational {
        if (b == 0L) return this
        val c = Rational(-b)
        c.addEq(this)
        return c
    }

    fun multiply(b: Rational): Rational {
        if (zero(num) || zero(b.num)) return ZERO
        val rv = Rational(this)
        rv.mulEq(b)
        return rv
    }

    fun divide(b: Rational): Rational {
        val rv = Rational(b)
        rv.flip()
        rv.mulEq(this)
        return rv
    }

    fun negate(): Rational {
        return if (zero(num)) this else Rational(num.negate(), den)
    }

    fun reciprocate(): Rational {
        if (zero(num)) throw ArithmeticException("Divide by zero")
        val rv = Rational(this)
        rv.flip()
        if (negative(den)) {
            rv.num = rv.num.negate()
            rv.den = rv.den.negate()
        }
        return rv
    }

    operator fun compareTo(other: Rational): Int {
        if (num == other.num && den == other.den)
            return 0

        //see if it is a num only compare...
        if (den == other.den)
            return if (greater(other.num, this.num)) -1 else 1

        //check signs...
        if ((zero(num) || negative(num)) && positive(other.num))
            return -1
        else if (positive(num) && (zero(other.num) || negative(other.num)))
            return 1

        val c = other.negate()
        c.addEq(this)
        return if (c.isZero) 0 else if (negative(c.num)) -1 else 1
    }

    operator fun compareTo(other: Long): Int {
        val othernum = BigInteger.valueOf(other)
        return if (num == othernum && one(den))
            0
        else
            compareTo(Rational(othernum, BigInteger.ONE))
    }


    fun doubleValue(): Double {
        try {
            return BigDecimal(num).divide(BigDecimal(den)).toDouble()
        } catch (e: ArithmeticException) {
            return BigDecimal(num).divide(BigDecimal(den), 32, BigDecimal.ROUND_HALF_UP).toDouble()
        }

    }

    //Basic Overrides
    override fun equals(obj: Any?): Boolean {
        if (obj == null || obj !is Rational)
            return false

        val r = obj as Rational?
        return compareTo(r!!) == 0
    }

    override fun hashCode(): Int {
        return num.multiply(BigInteger.valueOf(7)).toInt() xor den.multiply(BigInteger.valueOf(17)).toInt()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(num.toString())
        if (!one(den) && !zero(num)) {
            sb.append("/")
            sb.append(den)
        }
        return sb.toString()
    }

    companion object {
        val ZERO = Rational(BigInteger.ZERO, BigInteger.ONE)
        val ONE = Rational(BigInteger.ONE, BigInteger.ONE)
        val NEGONE = Rational(BigInteger.ONE.negate(), BigInteger.ONE)

        @Throws(NumberFormatException::class)
        fun valueOf(s: String): Rational {
            val fraction = s.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (fraction.size < 1 || fraction.size > 2)
                throw NumberFormatException("BigIntegerRational not formatted correctly")

            if (fraction.size == 2) {
                val num = BigInteger(fraction[0])
                val den = BigInteger(fraction[1])
                return Rational(num, den)
            } else {
                val dec = BigDecimal(s)
                return valueOf(dec)
            }
        }

        fun valueOf(dx: Double): Rational {
            val x = BigDecimal.valueOf(dx)
            return valueOf(x)
        }

        fun valueOf(x: BigDecimal): Rational {
            var num = x.unscaledValue()
            var den = BigInteger.ONE

            var scale = x.scale()
            while (scale > 0) {
                den = den.multiply(BigInteger.TEN)
                --scale
            }
            while (scale < 0) {
                num = num.multiply(BigInteger.TEN)
                ++scale
            }

            val rv = Rational(num, den)
            rv.reduce()
            return rv
        }

        fun valueOf(value: Long): Rational {
            return if (value == 0L)
                ZERO
            else if (value == 1L)
                ONE
            else
                Rational(value)
        }

        fun sum(list: Iterable<Rational>): Rational {
            val sum = ZERO
            for (rat in list) {
                sum.addEq(rat)
            }
            return sum
        }

        // TODO: why is this here?
        fun gcd(a: Long, b: Long): Long {
            var a = a
            var b = b
            var c: Long
            if (a < 0L) {
                a = -a
            }
            if (b < 0L) {
                b = -b
            }
            if (a < b) {
                c = a
                a = b
                b = c
            }
            while (b != 0L) {
                c = a % b
                a = b
                b = c
            }
            return a
        }

        fun probVector(length: Int, prng: Random): Array<Rational> {
            if (length == 0)
                return arrayOf()
            else if (length == 1) return arrayOf(Rational.ONE)

            val dProb = prng.nextDouble()
            val probA = Rational.valueOf(dProb)
            val probB = Rational.valueOf(1 - dProb)
            if (length == 2) {
                return arrayOf(probA, probB)
            } else {
                val a = probVector(length / 2, prng)
                val b = probVector((length + 1) / 2, prng)
                val c = arrayOfNulls<Rational>(a.size + b.size)
                for (i in a.indices) {
                    c[i] = a[i].multiply(probA)
                }
                for (i in b.indices) {
                    c[a.size + i] = b[i].multiply(probB)
                }
                return c as Array<Rational>
            }
        }

        // TODO: put this somewhere else...
        fun printRow(name: String, value: Rational, colpp: ColumnTextWriter, excludeZero: Boolean) {
            if (!value.isZero || !excludeZero) {
                colpp.writeCol(name)
                colpp.writeCol(value.toString())

                if (!BigIntegerUtils.one(value.den)) {
                    colpp.writeCol(String.format("%.3f", value.doubleValue()))
                }
                colpp.endRow()
            }
        }
    }
}