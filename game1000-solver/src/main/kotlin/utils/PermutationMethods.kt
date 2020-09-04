import org.pcollections.ConsPStack
import solver.models.State
import solver.models.toState
import java.util.*
import kotlin.collections.HashSet

/**
 *
 * Ways to create a winMetric of X in Z integers where each integer can be in [0..Y].
 *
 * A valid solution to (1,3,3) would be (0,1,0)
 *
 * Very efficient solution, only enters valid permutations. Could possibly be made tailrecursive.
 *
 *
 * space left = p*y , x = val
 *
 *
 */
fun getPermutations(x:Int,y:Int,z:Int) : List<List<Int>> {

    fun checkValidity(x:Int,y:Int,z:Int) {
        assert(x >= 0 && y > 0 && z > 0);
        assert(x <= z*y);
    }

    fun getXinYinZSteps(sum : Int,x:Int,y:Int,z:Int,step : ArrayList<Int>) : List<ArrayList<Int>> {
        val stp = step.size;
        if(stp >= z) {
            return Arrays.asList(step);
        }
        val a = ArrayList<ArrayList<Int>>();

        val space = ((z-(stp+1))*y); //Max space left in next steps.
        var start = Math.max(x-space,0); //x-space is minimum value to remove to fill buckets
        if(z-stp == 1) {
            start = x; //Final chance, so must place value
        }
        val end = Math.min(x,y);

        for(i in start..end) {
            val next = ArrayList(step);
            next.add(i);
            a.addAll(getXinYinZSteps(sum,x - i, y, z, next));
        }
        return a;
    }

    checkValidity(x,y,z);
    return getXinYinZSteps(x,x,y,z, ArrayList());
}



fun formableValues(count : Int,values : List<Int>) : List<Int> {
    val min = count * (values.minOrNull() as Int);
    val max = count * (values.maxOrNull() as Int);
    return (min..max).toMutableList();
}


fun <T> joinInto(a : List<List<T>>, b : List<T>) : List<List<T>> {
    val v = ArrayList<List<T>>();
    a.forEach { t: List<T> -> for(i in b) { v.add( t.plus(i) ) } }
    return v;
}



fun <T> join(a : List<T>,b : List<T>) : List<List<T>> {
    val v = ArrayList<List<T>>();
    if(a.isEmpty() && b.isNotEmpty()) {
        for (j in b) {
            val k = ArrayList<T>();
            k.add(j);
            v.add(k);
        }
    }
    else if(b.isEmpty() && a.isNotEmpty()) {
        for (i in a) {
            val k = ArrayList<T>();
            k.add(i);
            v.add(k);
        }
    }
    else {
        for(i in a) {
            for (j in b) {
                val k = ArrayList<T>();
                k.add(i);
                k.add(j);
                v.add(k);
            }
        }
    }
    return v;
}

/*
 * All possibilities of placements.
 */
fun placementPermutations(data : List<Int>,vals : List<Int>) : List<List<Int>> {
    assert(data.isNotEmpty());
    var a = join(ArrayList(),formableValues(data.firstOrNull() as Int,vals));
    for(i in data.drop(1)) {
        a = joinInto(a,formableValues(i,vals));
    }
    return a;
}


fun getPlacementPossibilities() : ArrayList<List<Int>> {
    return placementSequences(ConsPStack.empty(), ArrayList());
}

fun placementSequences(dat : ConsPStack<Int>, result : ArrayList<List<Int>>) : ArrayList<List<Int>> {
    if(dat.size >= 9) {
        result.add(dat);
    }
    else {
        if(dat.count { i-> i==0 } < 3){
            placementSequences(dat.plus(0),result);
        }
        if(dat.count { i-> i==1 } < 3){
            placementSequences(dat.plus(1),result);
        }
        if(dat.count { i-> i==2 } < 3){
            placementSequences(dat.plus(2),result);
        }
    }
    return result;
}


/**
 * Get 1000 final states.
 */
fun getFinalStates() : Set<State> {
    val placements = getPermutations(9,3,3);
    val set = HashSet<State>();
    for(i in placements) {
        val states = placementPermutations(i, (1..6).toList());
        for(j in states) {
            set.add(toState(0,j,i));
        }
    }
    return set;
}


fun allPermutations(preList : List<Int>,range: IntRange,loops : Int,body : (i:List<Int>) -> Unit){
    assert(loops >= 0);
    permLoop(range,0,loops-preList.size,preList,body);
}


fun allPermutations(range: IntRange,loops : Int,body : (i:List<Int>) -> Unit){
    assert(loops >= 0);
    permLoop(range,0,loops,ArrayList(),body);
}

fun permLoop(range : IntRange,current:Int,perms: Int,data:List<Int>, body : (i:List<Int>) -> Unit) {
    if(current < perms) {
        for(i in range) {
            permLoop(range,current + 1, perms, data.plus(i),body);
        }
    } else {
        body(data);
    }
}



fun allPermutationsCons(range: IntRange,loops : Int,body : (i:ConsPStack<Int>) -> Unit){
    assert(loops >= 0);
    permLoopCons(range,0,loops,ConsPStack.empty(),body);
}

fun permLoopCons(range : IntRange,current:Int,perms: Int,data:ConsPStack<Int>, body : (i:ConsPStack<Int>) -> Unit) {
    if(current < perms) {
        for(i in range) {
            permLoopCons(range,current + 1, perms, data.plus(i),body);
        }
    } else {
        body(data);
    }
}