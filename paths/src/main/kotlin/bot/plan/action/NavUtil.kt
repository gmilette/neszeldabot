package bot.plan.action

import bot.*
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapCell
import bot.state.map.MapConstants
import bot.state.map.MapRoute
import util.d
import kotlin.math.abs
import kotlin.random.Random

class MoveTowardsUtil() {
    private var directions: MutableList<GamePad> = mutableListOf()

    fun moveTowards(link: FramePoint, target: FramePoint, previousMove: PreviousMove): GamePad {
        d { " dir $directions $link to ${previousMove.to} didnt ${previousMove
            .didntMove} actual ${previousMove.actual} near " +
                "${previousMove.movedNear}"}
        return when {
            directions.isEmpty() -> {
                makeDirections(link, target)
                directions.first()
            }
            // the previous link didn't even try to move
            !previousMove.triedToMove -> directions.first()
            // next best
            previousMove.didntMove -> {
                // remove
                val removed = directions.removeFirst()
                d { " ** removed $removed"}
                if (directions.isEmpty()) {
                    makeDirections(link, target)
                }
                directions.first()
            }
            // keep going!
            previousMove.movedNear -> {
                d { " keep going "}
                directions.first()
            }
            // new path, probably got pushed far away
            else -> {
                makeDirections(link, target)
                directions.first()
            }
         }
    }

    data class CostDir(val cost: Int, val dist: Int, val dir: GamePad)

    private fun makeDirections(link: FramePoint, target: FramePoint) {
        val moveLeftVal = link.x - target.x
        val moveRightVal = target.x - link.x
        val moveUpValue = link.y - target.y
        val moveDownValue = target.y - link.y

        val distToGoal = link.distTo(target)
        val distLeft = link.left.distTo(target)
        val distR = link.right.distTo(target)
        val distU = link.up.distTo(target)
        val distD = link.down.distTo(target)

        var all = mutableListOf<CostDir>()
        if (moveLeftVal >= 0 && distLeft < distToGoal) all.add(
            CostDir(
                moveLeftVal,
                distLeft, GamePad
                    .MoveLeft
            )
        )
        if (moveRightVal >= 0 && distR < distToGoal) all.add(CostDir
            (moveRightVal, distR, GamePad.MoveRight))
        if (moveUpValue >= 0 && distU < distToGoal) all.add(CostDir
            (moveUpValue, distU, GamePad.MoveUp))
        if (moveDownValue >= 0 && distD < distToGoal) all.add(CostDir
            (moveDownValue, distD, GamePad.MoveDown))
        all = all.sortedBy { -it.cost }.toMutableList()

        // only keep positive

        // todo: now only keep if get closer to the goal

            // which every is positive
        // the positive values should be first

//        val all = mutableListOf<CostDir>(
//            CostDir(moveLeftVal, GamePad.MoveLeft),
//            CostDir(moveRightVal, GamePad.MoveRight),
//            CostDir(moveUpValue, GamePad.MoveUp),
//            CostDir(moveDownValue, GamePad.MoveDown),
//        ).sortedBy { -it.cost }

        directions = all.map { it.dir }.toMutableList()

        d {"Can move: ${all} ${directions}"}
    }
}

data class PreviousMove(
    var previous: PreviousMove? = null,
    val from: FramePoint = FramePoint(0, 0),
    val to: FramePoint = FramePoint(0, 0),
    val actual: FramePoint = FramePoint(0, 0),
    val move: GamePad = GamePad.None,
    val triedToMove: Boolean = true,
    // calculated
    val skipped: Boolean = from.distTo(actual) > 1,
    val distOff: Int = from.distTo(actual),
    val distOffx: Int = abs(from.x - actual.x),
    val distOffy: Int = abs(from.y - actual.y),
    val distAlert: Boolean = ((from.x == to.x && from.x != actual.x) || (from.y == to.y && from.y != actual.y) )
) {
    val didntMove: Boolean = from.x == actual.x && from.y == actual.y
    val movedNear: Boolean = from.distTo(actual) < 3
    val dir: Direction
        get() = from.dirTo(to)
}

object NavUtil {

    fun moveTowardsRoute(mapCell: MapCell, link: FramePoint, target:
    FramePoint
    ): GamePad {

//        val next = mapCell.path(link, target).firstOrNull() ?:
//            return ZeldaBot.GamePad.MoveRight
//
//        return directionTo(link, next)
        return GamePad.MoveLeft
    }

    fun directionToAvoidingObstacleM(mapCell: MapCell, from: FramePoint, to:
    FramePoint): GamePad {
        return manhattanPathFinder(mapCell, from, to) ?: randomDir()
    }

    fun randomDir(from: FramePoint = FramePoint(100, 100)): GamePad {
        d { " random dir "}
        val dir = Random.nextInt(4)
        return when  {
            dir == 0 && from.x > 3 -> GamePad.MoveLeft
            dir == 1 && from.x < MapConstants.MAX_X - 3 -> GamePad.MoveRight
            dir == 2 && from.y > 3 -> GamePad.MoveUp
            dir == 3 && from.y < MapConstants.MAX_Y - 3 -> GamePad.MoveDown
            else -> {
                d { " no movement "}
                GamePad.None
            }
        }
    }

    fun manhattanPathFinder(mapCell: MapCell, from: FramePoint, to:
        FramePoint): GamePad? {
        val path = mutableListOf<FramePoint>()
        val directions = mutableListOf<GamePad>()
        var current = from
        var dist = from.distTo(to)
        val limit = 1000
        var tries = 0
        while (dist > 5 && tries < limit) {
            tries++
            dist = current.distTo(to)
            val dir = directionToAvoidingObstacleZZ(mapCell, current, to)
            d { "go $dir $dist" }
            current = when {
                dir == GamePad.MoveUp && !current.isTop -> current.up
                dir == GamePad.MoveDown && !current.isBottom -> current.down
                dir == GamePad.MoveLeft && !current.isLeft -> current.left
                dir == GamePad.MoveRight && !current.isRight -> current
                    .right
                else -> current.up
            }
            directions.add(dir)
            path.add(current)
        }

        return if (tries > limit - 10 || directions.isEmpty()) {
            d { " random dir "}
            null
        } else {
            return directions.first()
        }
    }

    /// !!!! develop this one
    fun directionToAvoidingObstacleZ(mapCell: MapCell, from: FramePoint, to:
        FramePoint): GamePad {
        val canGoUpOrDown = mapCell.passable.get(from.up) ||
                mapCell.passable.get(from.down)

        val canGoRorL = mapCell.passable.get(from.left) ||
                mapCell.passable.get(from.right)

        val closerToY = (abs(from.x - to.x) > abs(from.y - to.y))
        val closeToX = abs(from.x - to.x) < 16
        val closeToY = abs(from.y - to.y) < 16
        d { " action: $closerToY upd $canGoUpOrDown rl $canGoRorL " }
        return when {
            //            dist < 18 -> {
//                whenClose()
//            }
            closerToY && canGoRorL -> {
                if (from.x < to.x) GamePad.MoveRight else GamePad.MoveLeft
            }
            canGoUpOrDown -> { // closer to y
                // if within range
                if (from.y < to.y) GamePad.MoveDown else GamePad.MoveUp
            }
            else -> {
                d { " default action " }
                GamePad.None
//                when {
//                    mapCell.passable.get(from.down) -> ZeldaBot.GamePad.MoveDown
//                    mapCell.passable.get(from.up) -> ZeldaBot.GamePad.MoveUp
//                    mapCell.passable.get(from.right) -> ZeldaBot.GamePad
//                        .MoveRight
//                    else -> ZeldaBot.GamePad.MoveLeft
//                }
            }
        }
    }

    fun directionToAvoidingObstacle(mapCell: MapCell, from: FramePoint, to: FramePoint):
            GamePad {
        return directionToAvoidingObstacleR(mapCell, from, listOf(to))
//        return directionToAvoidingObstacleM(mapCell, from, to)
    }

    fun directionToAvoidingObstacle(mapCell: MapCell, from: FramePoint, to: List<FramePoint>):
            GamePad {
        return directionToAvoidingObstacleR(mapCell, from, to)
//        return directionToAvoidingObstacleM(mapCell, from, to)
    }

    fun routeToAvoidingObstacle(mapCell: MapCell, from: FramePoint, to: List<FramePoint>, avoid: List<FramePoint> = emptyList()):
            List<FramePoint> {
        val route = mapCell.gstar.route(from, to, avoid)
        return route
    }

    fun directionToAvoidingObstacleR(mapCell: MapCell, from: FramePoint, to:
        List<FramePoint>):
            GamePad {
        val route = mapCell.gstar.route(from, to)
//        d { " loc: ${mapCell.mapLoc} route size ${route.size} "}
        if (route.size < 2) return randomDir()

//        val nextPoint = mapCell.gstar.route(from, to)[1]
        val nextPoint = route[1]
        return if (nextPoint == null) {
            randomDir()
        } else {
            directionTo(from, nextPoint)
        }.also {
            d { " next point $nextPoint dist ${from.distTo(nextPoint)} dir: $it"}
        }
    }

    // test case
    // map 103
    // link: FramePoint(x=176, y=128)

    fun directionToAvoidingObstacleZZ(mapCell: MapCell, from: FramePoint, to:
    FramePoint):
            GamePad {

        val canGoRight = mapCell.passable.get(from.rightEnd)
        val canGoLeft = mapCell.passable.get(from.left)
//        val canGoUp = mapCell.passable.get(from.upEnd)
        val canGoUp = mapCell.passable.get(from.up)
        val canGoDown = mapCell.passable.get(from.downEnd)
        d { " action: up $canGoUp d $canGoDown r $canGoRight l $canGoLeft" }

        return when {
            (from.y > to.y) && canGoUp -> GamePad.MoveUp
            (from.x > to.x) && canGoLeft -> GamePad.MoveLeft
            canGoDown -> GamePad.MoveDown
            canGoRight -> GamePad.MoveRight
            else -> randomDir()
        }

    }

    fun directionTo(from: FramePoint, to: FramePoint): GamePad {
        return directionToDist(from, to)
//        return when {
//            // TODO: Redo this
//            from.x == to.x -> {
//                if (from.y < to.y) GamePad.MoveDown else GamePad.MoveUp
//            }
//            from.y == to.y -> {
//                if (from.x < to.x) GamePad.MoveRight else GamePad.MoveLeft
//            }
//            else -> {
//                // it could be a corner
//                d { " default direction to x ${abs(from.x - to.x)} y ${abs(from.y - to.y)}" }
////                if (abs(from.x - to.x) == 2 && abs(from.y - to.y) == 1) {
////                    d { " default direction to DOWN corner " }
////                    GamePad.MoveDown
////                }
//                NavUtil.randomDir()
////                GamePad.MoveLeft
//            }
//        }
    }

    fun directionToDist(from: FramePoint, to: FramePoint): GamePad {
        val xDist = abs(from.x - to.x)
        val yDist = abs(from.y - to.y)
        return when {
            // TODO: Redo this
            xDist < yDist -> {
                if (from.y < to.y) GamePad.MoveDown else GamePad.MoveUp
            }
            else ->
                if (from.x < to.x) GamePad.MoveRight else GamePad.MoveLeft
            }
//            else -> {
//                // it could be a corner
//                d { " default direction to x ${abs(from.x - to.x)} y ${abs(from.y - to.y)}" }
////                if (abs(from.x - to.x) == 2 && abs(from.y - to.y) == 1) {
////                    d { " default direction to DOWN corner " }
////                    GamePad.MoveDown
////                }
//                NavUtil.randomDir()
////                GamePad.MoveLeft
//            }
        }

    fun directionToDir(from: FramePoint, to: FramePoint): Direction {
        return when {
            from.x == to.x -> {
                if (from.y < to.y) Direction.Down else Direction.Up
            }
            from.y == to.y -> {
                if (from.x < to.x) Direction.Right else Direction.Left
            }
            else -> Direction.Left
        }
    }

    fun MapLoc.directionToDir(to: MapLoc): Direction {
        return when {
            to - this == 1 -> Direction.Right
            to - this == -1 -> Direction.Left
            to - this == 16 -> Direction.Down
            to - this == -16 -> Direction.Up
            else -> {
                d { " default maploc direction to " }
                Direction.Left
            }
        }
    }

    fun moveTowards(link: FramePoint, target: FramePoint): GamePad {
        val dist = abs(target.x - link.x) + abs(target
            .y - link.y)
        d { " go find $target from $link distance: $dist"}
        return when {
//            dist < 18 -> {
//                whenClose()
//            }
            (link.x.closeTo(target.x, 5)) -> {
                when {
                    (link.y > target.y) -> GamePad.MoveUp
                    else -> GamePad.MoveDown
                }
            }
            else -> when {
                (link.x.closeTo(target.x, 5)) -> GamePad
                    .MoveLeft
                else -> GamePad.MoveRight
            }
        }
    }
}

fun Int.closeTo(other: Int, tolerance: Int) =
    this > (other - tolerance) && this < other + tolerance

