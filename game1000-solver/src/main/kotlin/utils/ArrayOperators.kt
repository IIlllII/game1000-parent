package utils


operator fun Array<Double>.plus(arg:Array<Double>) : Array<Double>{
    assert(this.size == arg.size)
    return this.mapIndexed { a, b -> b + arg[a] }.toTypedArray()
}


operator fun Array<Double>.minus(arg:Array<Double>) : Array<Double>{
    assert(this.size == arg.size)
    return this.mapIndexed { a, b -> b - arg[a] }.toTypedArray()
}


operator fun Array<Double>.times(arg:Array<Double>) : Double{
    assert(this.size == arg.size)
    var dot = 0.0;
    for(i in this.indices) {
        dot+=this[i]*arg[i];
    }
    return dot;
}