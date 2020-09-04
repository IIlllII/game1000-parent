package converters

import com.google.gson.*
import solver.models.*
import java.io.File
import java.lang.reflect.Type
import java.util.HashMap


class MapDeSerializer : JsonDeserializer<Resolver> {
    override fun deserialize(el: JsonElement?, p1: Type?, p2: JsonDeserializationContext?): Resolver? {
        val gson = Gson();
        if(el != null) {
            val a = HashMap<StateAndHist, Pos>();
            for(i in el.asJsonArray){
                val tmp = gson.fromJson(i, Comb2::class.java);
                a.put(tmp.state,tmp.pos);
            }
            return Resolver(a);
        }
        else {
            return Resolver(HashMap());
        }


    }

}

class MapSerializer : JsonSerializer<Resolver> {

    override fun serialize(p0: Resolver?, p1: Type?, p2: JsonSerializationContext?): JsonElement? {
        val gson = Gson();
        if(p0 != null) {
            val ls = JsonArray();
            for (i in p0.map.entries) {
                ls.add(gson.toJsonTree(Comb2(i.key,i.value)));
            }
            return ls;
        }
        else {
            return JsonArray();
        }
    }


}


fun toPrettyFormat(solution : Map<State, Placement>) : List<SolutionFormat> {
    return solution.entries.map {
        ent ->
        val state = ent.key;
        val placement = ent.value;

        val overrides = placement.eqResolver.map.entries.map {
            ov -> Override(ov.key.game,ov.value)
        }

        SolutionFormat(
                state=state,
                defaultPlacement = placement.placements[0],
                overrides = overrides);
    }
}

fun writePrettyFormat(solution: Map<State, Placement>, file : File) {
    val converted = toPrettyFormat(solution);
    val gson = GsonBuilder().setPrettyPrinting().create()
    file.writer().use { wr ->
        wr.write(gson.toJson(converted));
    }
}

fun readPrettyformat(file : File) : List<SolutionFormat> {
    val gson = Gson();
    file.reader().use { rd->
        val sol = gson.fromJson(rd, JsonArray::class.java);
        val solution = sol.map { i-> gson.fromJson(i, SolutionFormat::class.java)}
        println("Read " + solution.size + " from pretty json file");
        return solution;
    }
}

/**
 * Slightly lossy conversion.
 * Does not contain win data.
 */
fun prettyFormatToOld(data : List<SolutionFormat>) : Map<State, Placement> {
    val mp = HashMap<State, Placement>();
    data.forEach {
        i->

        val ovr = i.overrides
                .groupBy(
                { o-> StateAndHist(o.history,i.state) },
                { o-> o.pos })
                .mapValues(
                        { en->en.value.first() }
                );

        val eqRes = Resolver(ovr);

        val pl = Placement(eqResolver = eqRes,
                placements = listOf(i.defaultPlacement),
                winsHundred = 0,
                winsOne = 0,
                winsTen = 0);

        mp.put(i.state,pl);
    }
    return mp;
}

/**
 *  Read a solution from a file to a Map of HistState and Placement.
 */
fun readSolution(file : File) : Map<State, Placement> {
    assert(file.exists());

    var lines = 0;

    val gsonBuilder = GsonBuilder();
    gsonBuilder.registerTypeAdapter(Resolver::class.java, MapDeSerializer());
    val gson = gsonBuilder.create();

    val map = HashMap<State, Placement>();

    file.forEachLine{line ->
        val kv = gson.fromJson(line.trim(), Comb::class.java);
        lines++;
        map.put(kv.a,kv.b);
    };
    println("Read " + lines + " from json file");

    return map;
}

data class SimpleState(val current : Int, val tens: Int, val hundreds:Int, val ones :Int, val sum : Int)

data class OldResult(val placement : Pos)

data class OldSolutionModel(val state : SimpleState,  val result: OldResult)


/**
 *  Read a solution from a file to a Map of HistState and Placement.
 */
fun readOldSolution(file : File) : Map<SimpleState, Pos> {
    assert(file.exists());

    var lines = 0;

    val gsonBuilder = GsonBuilder();
    gsonBuilder.registerTypeAdapter(Resolver::class.java, MapDeSerializer());
    val gson = gsonBuilder.create();

    val map = HashMap<SimpleState, Pos>();

    file.forEachLine{line ->
        val kv = gson.fromJson(line.trim(), OldSolutionModel::class.java);
        lines++;
        map[kv.state] = kv.result.placement;
    };
    println("Read $lines from json file");

    return map;
}



/**
 *  Write a map of HistState and Placements to JSON format
 */
fun writeSolution(inputMap : Map<State, Placement>, file : File) {

    if(file.exists()) {
        file.delete();
    }

    var lines = 0;

    val gsonBuilder = GsonBuilder();
    gsonBuilder.registerTypeAdapter(Resolver::class.java, MapSerializer());
    val gson = gsonBuilder.create();

    inputMap.forEach({kv ->
        file.appendText(gson.toJson(Comb(kv.key,kv.value))+"\n");
        lines++;
    });

    println("Wrote " + lines + " to json file");
}