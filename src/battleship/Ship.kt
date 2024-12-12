package battleship
/** This class is necessary to keep track of the specific ship that could be hit by a player.*/
class Ship (private val coordinates: Array<Pair<Int,Int>>, length:Int, val name:String, isHorizontal: Boolean){

    /** The board cells locations are saved for efficient querying */
    val boardLocations: Array<Int> = if (isHorizontal) {
        Array<Int>(length){coordinates[0].first + it + coordinates[0].second * 10 }
    } else {
        Array<Int>(length){coordinates[0].first + (coordinates[0].second + it) * 10 }
    }

    /** Also, relative location of the hits respecting the board cells locations are tracked for filtering.*/
    private val locationHits : Array<Boolean> = Array(length) {false}

    fun setHit( target : Int): Int = boardLocations
        .indexOf(target)
        .let {
           locationHits[it] = true
           it
       }

    //Here, for instance, we can filter the non-hit ship cells
    fun getPristines() = locationHits.mapIndexedNotNull{i,l-> if(!l) i else null}

    var isSunk : Boolean = false

}