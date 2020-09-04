package utils

import org.pcollections.ConsPStack
import org.pcollections.PStack
import kotlin.random.Random


fun <T> Random.pickRandom(possibilities: Set<T>): T {
    if(possibilities.isEmpty()) {
        throw IllegalArgumentException("Can not get a value from an empty set");
    }
    return possibilities.elementAt(this.nextInt(possibilities.size));
}

fun Random.getRandomSequence(size : Int,range: IntRange) : PStack<Int> {
    var a = ConsPStack.empty<Int>();
    for(i in 0 until size) {
        a = a.plus(getRandomValue(range));
    }
    return a;
}

fun Random.getRandomValue(range: IntRange): Int {
    return this.pickRandom(range.toList());
}

fun <T> Random.pickRandom(inp : List<T>) :T {
    val num = this.nextInt(inp.size)
    return inp[num];
}

fun Random.diceRoll() : Int {
    return this.nextInt(6)+1
}