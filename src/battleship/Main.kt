package battleship

import kotlin.math.absoluteValue

/** This game is a terminal based version of the popular game Battleship. Two players will have to place five types of
 * warships in a ten by ten matrix that represents the map. Each player will have their own map and will have to guess
 * the positions of the ships in the opponent map in order to shoot them and sink them all to win.*/
fun main() {

    //The game starts with the initialization process for each player
    initialization(true)
    clearScreen()
    initialization(false)
    clearScreen()

    /** This map will serve as a scoreboard for the game, taking the binary values of player one and player two as
     * true/false respectively.*/
    val shipsRemaining  = mutableMapOf<Boolean, Int>(true to 5, false to 5)

    /** The round variable will mark the corresponding player's turn.*/
    var round = true

    //Finally, the game enters the shooting rounds phase
    do{
        shipsRemaining[round] = shooting(round,shipsRemaining[round]!!)
        round = !round
    }while(!shipsRemaining.values.contains(0))
}

/** This function is responsible for the process of obtaining the five ships for the player specified on the isPlayer1
 * parameter. It will handle any errors in the process before proceeding with the next game phase. */
fun initialization(isPlayer1:Boolean) {
    println("Player ${ if(isPlayer1) "1" else "2" }, place your ships to the game field")
    var shipsPlaced = 0
    var currentShip = ""
    var coordinates: Array<Pair<Int, Int>>
    ships = mapOf(
        "Aircraft Carrier" to 5,
        "Battleship" to 4,
        "Submarine" to 3,
        "Cruiser" to 3,
        "Destroyer" to 2
    )
    /** This map will associate the placement order of the ship with its cells length number. */
    shipsOrder = (0..4).toList().associateWith { ships.keys.toList()[it] }
    val cells = if(isPlayer1) Board.p1cells else Board.p2cells
    Board.printCells(false,isPlayer1)
    do {
        try {
            currentShip = shipsOrder[shipsPlaced]!!
            println("Enter the coordinates of the $currentShip (${ships[currentShip]} cells):\n")

            /** The coordinates will be read following a similar regex format to /[A-J]1?\d\s[A-J]1?\d/. That is, it
             * retrieves a pair of coordinates consisting in an uppercase letter and a digit for the y and x
             * cartesian values of the map. For the corresponding numeric value of the letter given, Board.rows is
             * called with it.*/
            coordinates = readln().split(" ").toTypedArray<String>().let {
                arrayOf<Pair<Int, Int>>(
                    it[0].substring(1).toInt() - 1 to Board.rows[it[0].first()]!!,
                    it[1].substring(1).toInt() - 1 to Board.rows[it[1].first()]!!
                )
            }
            //Coordinates are adjusted to increasing order (x1 -> x2, y1 -> y2)
            if (coordinates[0].first > coordinates[1].first || coordinates[0].second > coordinates[1].second) {
                val newCoords = arrayOf<Pair<Int, Int>>(coordinates[1], coordinates[0])
                coordinates = newCoords
            }

            //Cell span is calculated
            val delta: Pair<Int, Int> = Pair(
                (coordinates[1].first + 1 - coordinates[0].first).absoluteValue,
                (coordinates[1].second + 1 - coordinates[0].second).absoluteValue
            )

            when {
                //The direction of the ship must be orthogonal
                coordinates[1].first - coordinates[0].first != 0 &&
                        coordinates[1].second - coordinates[0].second != 0 -> {
                    throw IllegalArgumentException("Error! Wrong ship location! Try again:")
                }

                //The length must be the same as delta
                delta.first != ships[currentShip]!! && delta.second != ships[currentShip]!! -> {
                    throw IllegalArgumentException("Error! Wrong length of the $currentShip! Try again:")
                }

                else -> {
                    val  isHorizontal = checkAvailability(coordinates,isPlayer1)

                    if (isHorizontal) {
                        for (i in coordinates[0].first..coordinates[1].first)
                            cells[i + coordinates[0].second * 10] = "O"
                    } else {
                        for (i in coordinates[0].second..coordinates[1].second)
                            cells[coordinates[0].first + i * 10] = "O"
                    }
                    if(isPlayer1)
                        Board.player1Ships[shipsPlaced] = Ship(
                            coordinates, ships[currentShip]!!, currentShip, isHorizontal)
                    else
                        Board.player2Ships[shipsPlaced] = Ship(
                            coordinates, ships[currentShip]!!, currentShip, isHorizontal)
                }
            }
            Board.printCells(false,isPlayer1)
            shipsPlaced++
        } catch (e: IllegalArgumentException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println("Error! Wrong coordinates format! Try again: ${e.message}")
        } catch (e: IndexOutOfBoundsException) {
            println("Error! Wrong coordinates format! Try again: ${e.message}")
        } catch (e: TypeCastException) {
            println("Error! Wrong coordinates format! Try again: ${e.message}")
        } catch (e: Exception) {
            println("Error! something went wrong, Try again: ${e.message}")
        }
    } while (shipsPlaced < 5)
}

/** This is the main handler of the game course, the process where the shooting begins. This function will control all
 * aspects of the current round in which the corresponding player will enter the coordinates of the targeted cell. */
fun shooting(isPlayer1: Boolean, shipsRemaining:Int) : Int{
    Board.printCells(true,isPlayer1)
    println("---------------------")
    Board.printCells(false,isPlayer1)

    var shipsRemaining = shipsRemaining
    var ship: Ship?
    val ships = if(isPlayer1) Board.player2Ships else Board.player1Ships
    var message = ""
    val enemy = if (isPlayer1) Board.p2cells else Board.p1cells
    val fog = if (isPlayer1) Board.p1fog else Board.p2fog
    var shot = false

    println("Player ${if (isPlayer1) "1" else "2"}, it's your turn:")

    /**The shooting attempt won't be submitted until the process receives an acceptable input for the shot coordinates.*/
    do {
        try {
            //Input must be in the regex form /[A-J]\1?\d/
            val input = readln().trim().let {
                Pair<Int, Int>(it.substring(1).toInt() - 1, Board.rows[it.first()]!!)
            }

            //The range of the coordinates is checked
            if (input.first !in 0..9 || input.second !in 0..9)
                throw IllegalArgumentException("Error! You entered the wrong coordinates! Try again:")

            /** From the given target, we retrieve the corresponding ship cell location or null. */
            val target = input.second * 10 + input.first
            ship = ships.firstOrNull{ s -> target in s!!.boardLocations}

            when{
                ship == null ->{
                    enemy[target] = "M"
                    fog[target] = "M"
                    message = "You missed!"
                }
                !ship.isSunk -> {
                    enemy[target] = "X"
                    fog[target] = "X"
                    ship.setHit(target)
                    if (ship.getPristines().isEmpty()) {
                        ship.isSunk = true
                        if (--shipsRemaining > 0)
                            message = "You sank a ship!"
                    } else {
                        message = "You hit a ship!"
                    }
                }
                else -> {
                    message = "You hit a ship!"
                }
            }

            Board.printCells(true,isPlayer1)
            println("---------------------")
            Board.printCells(false,isPlayer1)
            if (shipsRemaining == 0) message = "You sank the last ship.\nYou won. Congratulations!"
            println(message)
            clearScreen()
            shot = true

        } catch (e: IllegalArgumentException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println("Error! Wrong coordinates format! Try again: ${e.message}")
        } catch (e: IndexOutOfBoundsException) {
            println("Error! Wrong coordinates format! Try again: ${e.message}")
        } catch (e: TypeCastException) {
            println("Error! Wrong coordinates format! Try again: ${e.message}")
        }
    } while (!shot)

    return  shipsRemaining
}

/** The board singleton is the carrier of the whole game information, it has the data and the methods to keep the
 * game state in track.*/
object Board {

    /** The p1 and p2 cells define player one and player two cells respectively */
    val p1cells : Array < String > = Array( 100 ) { "~" }
    val p2cells : Array < String > = Array( 100 ) { "~" }
    /** The p1 and p2 fogs define player one and player two enemy map views respectively */
    val p1fog : Array < String > = Array( 100 ) { "~" }
    val p2fog : Array < String > = Array( 100 ) { "~" }

    // This map associates the letter coordinates to their corresponding numeric value
    val rows : Map < Char, Int > = ( 'A'..'J' ).toList().associateWith { ('A'.code - it.code).absoluteValue }

    val player1Ships : Array< Ship? > = Array( 5 ){ null }
    val player2Ships : Array< Ship? > = Array( 5 ){ null }

    //This is the function that will print either the fog or the cells for the given player.
    fun printCells(fog:Boolean, isPlayer1: Boolean) {
        val fields = if (fog) { if(isPlayer1) p1fog else p2fog } else if(isPlayer1)
            p1cells
        else p2cells
        print( "\n  1 2 3 4 5 6 7 8 9 10\n" )
        for ( c in rows ) {
            print("${ c.key } ${
                fields.filterIndexed { i, s -> i in ( c.value * 10 ) until ( 10 + ( c.value * 10 ) ) }
                    .joinToString(" ") }\n"
            )
        }
        print("\n")
    }
}

/** This is the most important function of the program, as it is responsible for the availability of the ships
 * placement. It ensures that ships coordinates are inside the board limits, that there's a gap of one cell between
 * ships as minimum and that there are no other ships in the range of cells described by the coordinates given.*/
fun checkAvailability(coordinates: Array<Pair<Int, Int>>, isPlayer1: Boolean) : Boolean {
    val cells = if (isPlayer1) Board.p1cells else Board.p2cells
    val (start, end) = coordinates
    val isHorizontal = start.first != end.first

    //The range of cells to be traversed along the ships length is expressed as a Pair<start,end>
    val (mainStart, mainEnd) = if (isHorizontal) start.first to end.first else start.second to end.second

    //In consequence, the other axis will represent the cross width of the gap's border rectangle
    val crossCoord = if (isHorizontal) start.second else start.first

    //This ensures that the surrounding gap's border rectangle of each ship doesn't surpass the board's limits.
    val rangeStart = (mainStart - 1).coerceAtLeast(0)
    val rangeEnd = (mainEnd + 1).coerceAtMost(9)
    val crossStart = (crossCoord - 1).coerceAtLeast(0)
    val crossEnd = (crossCoord + 1).coerceAtMost(9)

    //Indices of each cell of the cross axis will be evaluated for the coordinates of the length axis
    val indices = if (isHorizontal) {
        (rangeStart..rangeEnd).flatMap { x -> (crossStart..crossEnd).map { y -> x + y * 10 } }
    } else {
        (rangeStart..rangeEnd).flatMap { y -> (crossStart..crossEnd).map { x -> x + y * 10 } }
    }

    //Finally, it is verified if the cells are occupied for the given indices.
    indices.forEach { index ->
        if (cells[index] == "O") {
            throw IllegalArgumentException("Error! You placed it too close to another one. Try again:")
        }
    }
    return  isHorizontal
}

/** This function will attempt to clear the screen every time the game prompts for switching players.
 * Nevertheless, this approach may not work in some platforms or execution environments.*/
fun clearScreen() {
    println("Press Enter and pass the move to another player\n" +
            "...")
    readln()
    print("\u001b[H\u001b[2J")
    System.out.flush()
}

//Ships and their length
lateinit var ships : Map < String, Int >

//Order of placement and ship name
lateinit var shipsOrder: Map < Int, String >