package bot.state.oam

import bot.state.Agent
import bot.state.EnemyState
import bot.state.FramePoint
import bot.state.map.Direction
import bot.state.map.MapConstants
import nintaco.api.API
import org.jheaps.annotations.VisibleForTesting
import util.d

/**
 * reason about the sprites
 */
class OamStateReasoner(
    private val api: API
) {
    private val sprites: List<SpriteData>

    var ladderSprite: Agent? = null
    var direction: Direction = Direction.None
    var damaged: Boolean = false

    init {
        sprites = readOam()
    }

    val DEBUG = false

    val alive: List<SpriteData>
        get() {
            return sprites.filter { it != null && !it.hidden }
        }

    val loot: List<SpriteData>
        get() {
            return sprites.filter { it.isLoot }
        }

    val allDead: Boolean
        get() = alive.isEmpty()

    fun agents(): List<Agent> =
        sprites.map { it.toAgent() }

    private fun SpriteData.toAgent(): Agent =
        Agent(index = index, point = point, state = toState(), tile = tile, attribute = attribute)

    @VisibleForTesting
    fun combine(toCombine: List<SpriteData>): List<SpriteData> {
        // could be multiple at x
        val xMap = toCombine.associateBy { it.point.x }

        // can delete, because there is a sprite 8pxs to left that is the same
        val toDelete = toCombine
            // keep all the projectiles because most are just small
//            .filter { !SpriteData.projectiles.contains(it.tile) }
            .filter {
//                val matched = xMap[it.point.x - 8]
                val matched = xMap[it.point.x - 8]
                // tiles do not always match
//                val delete = matched?.tile == it.tile && matched.point.y == it.point.y
                val delete = matched?.let { ma ->
                    toCombine.any { ma.point.y == it.point.y }
                } ?: false
//                val delete = matched?.point?.y == it.point.y
                if (delete) {
                    d { "! delete, ${it.point} because matches ${matched?.point}" }
                }
                delete
        }

        val mutable = toCombine.toMutableList()
        for (spriteData in toDelete) {
            if (DEBUG) {
                d { "! remove $spriteData" }
            }
            mutable.remove(spriteData)
        }

        if (DEBUG || true) {
            d { " alive sprites AFTER delete" }
            mutable.forEachIndexed { index, sprite ->
                d { "$index: $sprite" }
            }
        }


        return mutable
    }

    private fun SpriteData.toState(): EnemyState =
        when {
            this.hidden -> EnemyState.Dead
            isLoot -> EnemyState.Loot
            isProjectile -> EnemyState.Projectile
            else -> EnemyState.Alive
        }

    // this isn't real
    // 21: SpriteData(index=21, point=(74, 23), tile=62, attribute=0, hidden=false)
    // there are always 2 sprites on for each enemy
    // one is at x, other is at x+8, same attribute
    // to translate to current coordinates
    // use the lower x value
    // then subtract 61 from the y, value

    private fun readOam(at: Int): SpriteData {
        val x = api.readOAM(at + 0x0003)
        val y = api.readOAM(at)
        val tile = api.readOAM(at + 0x0001)
        val attrib = api.readOAM(at + 0x0002)
        return SpriteData(at / 4, FramePoint(x, y - MapConstants.yAdjust), tile, attrib)
    }

    private fun readOam(): List<SpriteData> {
        val spritesRaw = (0..63).map {
            readOam(0x0001 * (it * 4))
        }

        val dirDamage = LinkDirectionFinder.direction(spritesRaw)
        direction = dirDamage.direction
        damaged = dirDamage.damaged

        val ladders = spritesRaw.filter { it.tile == ladder }
        ladderSprite = if (ladders.isNotEmpty()) {
            val sp = if (ladders.size == 1) {
                ladders.first()
            } else {
                if (ladders[0].point.x < ladders[1].point.x) {
                    ladders[0]
                } else {
                    ladders[1]
                }
            }
            sp.toAgent()
        } else {
            null
        }

        d { " sprites ** alive ** ${spritesRaw.filter { !it.hidden }.size} dir ${direction}" }
        // ahh there are twice as many sprites because each sprite is two big
        val alive = spritesRaw.filter { !it.hidden }
        if (DEBUG || true) {
            d { " alive sprites" }
            alive.forEachIndexed { index, sprite ->
                d { "$index: $sprite" }
            }
        }

        if (DEBUG) {
            d { " sprites" }
            spritesRaw.forEachIndexed { index, sprite ->
                d { "$index: $sprite ${LinkDirectionFinder.dirFor(sprite)}" }
            }
        }

        return combine(alive)
    }
}
fun Agent.isGannonTriforce(): Boolean =
    tile == triforceTile

data class SpriteData(
    val index: Int,
    val point: FramePoint,
    val tile: Int,
    val attribute: Int,
    val tileByte: String = tile.toString(16),
    val attributeByte: String = attribute.toString(16)
) {
    val tilePair = tile to attribute

    // keep
    // Debug: (Kermit) 49: SpriteData(index=49, point=(177, 128), tile=160, attribute=2) None
    val hidden: Boolean = point.y >= 248 || attribute == 32 || EnemyGroup.ignore.contains(tile) ||
            EnemyGroup.ignorePairs.contains(tilePair) // does this work?
            //|| point.y < 60  dont need that because the y coordinate is adjusted
            //|| projectiles.contains(tile) //|| loot.contains(tile) // should be separate
            || ( (tile == 248 || tile == 250) && point.y == 187) // spinny guy
            // tile 52 is a bomb
            || ( (tile == 52) && point.y == 187) // could be just any 187 point should be considered dead
            || ( (tile == 142 || tile == 144) && point.y == 187)
            || ( (tile == 164) && point.y == 187)
            || point.y >= 187 // this keeps coming up, make sense ,because we translated it 61
            || point.y < 0

    val isLoot = !hidden && EnemyGroup.loot.contains(tile)

    val isProjectile = !hidden && EnemyGroup.projectiles.contains(tile)
    // it doesn't solve the pancake problem
}


