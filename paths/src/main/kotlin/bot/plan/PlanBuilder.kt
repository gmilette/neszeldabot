package bot.plan

import bot.GamePad
import bot.plan.runner.MasterPlan
import bot.state.FramePoint
import bot.state.MapLoc
import bot.state.map.*
import bot.state.map.level.LevelMapCellsLookup
import bot.state.map.level.LevelSpecBuilder
import bot.state.map.level.LevelStartMapLoc
import bot.state.up
import sequence.*

// master plan
// - plan phase (main thing doing, get to lev 1, gather stuff for lev 3, just
// a name
//  - plan segment (sub route
//  -- plan objective (per screen)


object InLocations {
    val topMiddleBombSpot = FramePoint(120, 32)
    val bombRight = FramePoint(13.grid - 5, 5.grid)
    val bombLeft = FramePoint(2.grid + 5, 5.grid)
    val diamondLeftTopPush = FramePoint(96, 64 )
    val diamondLeftBottomPush = FramePoint(96, 96)
    val middleStair = FramePoint(128, 80) //8x5
    val getItem = FramePoint(135, 80) // right side
    val getOutRight = FramePoint(12.grid, 2.grid) // right side
    val getOutLeft = FramePoint(3.grid, 1.grid) // right side
    val rightStair = FramePoint(209, 81)
    val rightStairGrid = FramePoint(13.grid, 5.grid)
    val rightTop = FramePoint(208, 32) //todo

    object Overworld {
        val shopRightItem = FramePoint(152, 96) // 97 failed to get heart
//        val selectHeart = FramePoint(152, 90) // still at location 44
        val centerItem = FramePoint(118, 88) // not 96
        val centerItemLetter = FramePoint(120, 88)
        val shopHeartItem = FramePoint(152, 96)
        val shopLeftItem = FramePoint(88, 88)

        val start: MapLoc = 119
    }
    object Level1 {
        val key114 = FramePoint(160, 128)
        val key83 = FramePoint(128, 50)

        // hands level1, prob same as key114
        val key69 = FramePoint(164, 128)
        val boomerang68 = FramePoint(128, 56)
    }

    object Level2 {
        val heartMid = FramePoint(128, 88) //boss heart
        val keyMid = FramePoint(128, 88)
        val keyMidDown = FramePoint(128, 81)
        val bombItemRight = FramePoint(208, 43)
        val triforce = FramePoint(120, 88) // get the middle of the triangle at the top
    }

    object Level3 {
        val heartMid = FramePoint(128, 88)
    }

    object Level4 {
        val batKey = FramePoint(144, 88)
        val squishyKey = FramePoint(135, 64)
        val moveLeftOfTwo = FramePoint(96, 64)
        val triforceHeart = FramePoint(208, 123)
    }

    object Level5 {
        val mapLocGetItem: MapLoc = 4
        val moveLeft = FramePoint(7.grid, 5.grid)
        val cornerStairs = FramePoint(13.grid, 2.grid)
        val cornerStairsBefore = FramePoint(13.grid-5, 2.grid)
        val triforceHeart = FramePoint(8.grid, 3.grid)
    }

    object Level6 {
        val moveUp = FramePoint(6.grid, 5.grid)
        val moveUpSingle = FramePoint(7.grid, 5.grid)
        val triforceHeart = FramePoint(8.grid, 5.grid)
    }

    object Level7 {
        val pushRight = FramePoint(12.grid, 5.grid)
        val triforceHeart = FramePoint(8.grid, 5.grid)
    }

    object Level8 {
        val triforceHeart = FramePoint(2.grid, 8.grid)
        val keySpot = FramePoint(8.grid, 5.grid)
    }

    object Level9 {
        val moveUpBlock = FramePoint(6.grid, 5.grid)
        val centerGannonAttack = FramePoint(7.grid, 5.grid)
    }

    object BombDirection {
        val right = FramePoint(200, 92) //?
        val left = FramePoint(2.grid + 2, 5.grid) //?
    }

}

object PlanBuilder {
    fun makeMasterPlan(hyrule: Hyrule, mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val plan = AnalysisPlanBuilder.MasterPlanOptimizer(hyrule)

        val factory = SequenceFactory(mapData, levelData, plan)

//        return levelTour(factory)
//        return real(factory)
        return part(factory)
    }

    private fun levelTour(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("tour of levels")
        return builder {
            startAt(InLocations.Overworld.start)
            seg("level1")
            for (i in 1..9) {
                phase("level $i")
                obj(Dest.level(i))
            }
        }
    }

    private fun part(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("begin!")
        return builder {
            startAt(InLocations.Overworld.start)
//            obj(Dest.level(3))
//            includeLevelPlan(levelPlan3(factory))
//            obj(Dest.level(4))
//            includeLevelPlan(levelPlan4(factory))
            //obj(Dest.level(5))
//            includeLevelPlan(levelPlan5(factory))

//            includeLevelPlan(levelPlan6(factory))
//            includeLevelPlan(levelPlan7(factory))
//            includeLevelPlan(levelPlan8(factory))
            includeLevelPlan(levelPlan9(factory))
        }
    }

    private fun real(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("begin!")
        return builder {
            startAt(InLocations.Overworld.start)
            phase("Opening sequence")
//            bombThenGoIn(FramePoint(64, 17))
            obj(Dest.item(ZeldaItem.WoodenSword))
            obj(Dest.level(2))
            includeLevelPlan(levelPlan2(factory))
            // 14
            // position point for statues!
//            goAbout(FramePoint(7.grid, 2.grid), 4, 2, true)
            // skip for now
            // position link to get the secret
            val sec:MapLoc = 61
            routeTo(sec.up)
            obj(Dest.Secrets.secretForest30NorthEast)
            obj(Dest.Secrets.bombSecret30North)
            obj(ZeldaItem.Letter)
            obj(Dest.Secrets.walk100)
            obj(Dest.Secrets.bombHeartNorth)
            obj(ZeldaItem.WhiteSword)
            obj(Dest.level(1))
            includeLevelPlan(levelPlan1(factory))
            obj(Dest.Shop.candleShopMid)
            obj(Dest.level(3))
            includeLevelPlan(levelPlan3(factory))
            obj(Dest.Secrets.fire100SouthBrown)
            obj(Dest.Shop.blueRing, position = true)
//            obj(Dest.level(4))
//            includeLevelPlan(levelPlan4(factory))

            phase("grab hearts")
            obj(Dest.Secrets.fire30GreenSouth)
            obj(Dest.Heart.fireHeart)
            obj(Dest.Secrets.bombHeartSouth)
            obj(Dest.Secrets.forest100South)
            obj(Dest.Shop.arrowShop)
            obj(Dest.Heart.ladderHeart)
            obj(Dest.Heart.raftHeart, itemLoc = Objective.ItemLoc.Right)

//            phase("level 5 and 6")
//            obj(Dest.level(5))
//            includeLevelPlan(levelPlan5(factory))
//
            obj(ZeldaItem.PowerBracelet, itemLoc = Objective.ItemLoc.None)
//            // make up and objective to walk to high up
////            routeTo(32)
            goToAtPoint(33, FramePoint(11.grid, 3.grid))
//            // hard to get into position when its passable, maybe position it
            obj(ZeldaItem.MagicSword)
//
            obj(Dest.level(6))
//
//            phase("level 7")
            obj(Dest.Shop.blueRing, itemLoc = Dest.Shop.ItemLocs.bait, position = true)
            obj(Dest.level(7))
            phase("level 8 and grab shield")
            // TODO: also get a potion
            obj(Dest.Secrets.level2secret10)
            // bait
            obj(Dest.Shop.eastTreeShop, itemLoc = Dest.Shop.ItemLocs.magicShield)
            obj(Dest.level(8))
            phase("level 9")
            obj(Dest.level(9))
            // junk
            left
            right
            right
            end
        }
    }

    fun levelPlanDebug(
        factory: SequenceFactory, optimizer: AnalysisPlanBuilder.MasterPlanOptimizer, level:
        Int
    ): MasterPlan {
        // shared prob
        val downPoint = FramePoint(120, 90)
        val pushPoint = FramePoint()
        return if (level == 1) {
            val builder = factory.make("Destroy level 1")
//            builder.inLevel.startAt(34)
//                .push(InLocations.diamondLeftBottomPush, InLocations.diamondLeftTopPush)
//                .startAt(127)
//                .go(InLocations.getItem)
//                .upTo(34) // eh
//                .end // more junk
//                .build()

            builder.inLevel.startAt(127)
                .go(InLocations.getItem)
                .upTo(34) // eh
                .end // more junk
                .build()

        } else {
            MasterPlan(emptyList())
        }
    }

    private fun levelPlan(factory: SequenceFactory, level: Int): MasterPlan {
        return when (level) {
            1 -> {
                levelPlan1(factory)
            }

            2 -> {
                levelPlan2(factory)
            }

            3 -> {
                levelPlan3(factory)
            }

            4 -> {
                levelPlan4(factory)
            }

            5 -> {
                levelPlan5(factory)
            }

            6 -> {
                levelPlan6(factory)
            }

            7 -> {
                levelPlan7(factory)
            }

            8 -> {
                levelPlan8(factory)
            }

            9 -> {
                levelPlan9(factory)
            }

            else -> {
                MasterPlan(emptyList())
            }
        }
    }

    private fun levelPlan1(factory: SequenceFactory): MasterPlan {
        val downPoint = FramePoint(120, 90)
        val builder = factory.make("Destroy level 1")
        return builder.inLevel.startAt(LevelStartMapLoc.lev(1))
            .seg("grab key")
            .left
            .goIn(GamePad.MoveLeft, 30)
            .kill
            .go(InLocations.Level1.key114)
            .right
            .seg("grab from skeleton")
            .right
            .goIn(GamePad.MoveRight, 20)
            .pickupDeadItem
            .seg("move to arrow")
            .left // first rooms
            .up //99
            .up //83
            .goIn(GamePad.MoveUp, 30)
            .seg("get key from skeletons")
            .kill // these skeletons provide a key
            .goAbout(InLocations.Level1.key83, 4, 2, true)
            .seg("Bomb and move")
            .bomb(InLocations.topMiddleBombSpot)
            .seg("go up")
            .up // 67
            .up // 51
            .seg("grab key from zig")
            .pickupDeadItem
            .seg("get key from boomerang guys")
            .up //35
            .goIn(GamePad.MoveUp, 30)
            .kill
            .goAbout(InLocations.Level1.key83, 4, 2, true)
            .seg("get arrow")
            .left
            .pushThenGoTo(InLocations.diamondLeftBottomPush, InLocations.diamondLeftTopPush)
            .startAt(127)
            .go(InLocations.getItem)
            .upTo(34) // eh
            .seg("snag boomerang")
            .rightm // don't attack
            .down.down // at 67 now
            .right // boomerang
            .goIn(GamePad.MoveRight, 30)
            .kill
            .goAbout(InLocations.Level1.boomerang68, 4, 2, true)
            .seg("destroy dragon")
            .right //69 hand grabby
            // should do but too risky for now
//                .go(InLocations.Level1.key114)
            .up
            .kill
            .right // triforce
            .goIn(GamePad.MoveRight, 20)
            .seg("get the triforce")
            .goAbout(downPoint, 4)
            .build()
    }


    private fun levelPlan2(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("Destroy level 2")

        return builder.lev(2).startAt(LevelStartMapLoc.lev(2))
            .seg("gather 3 keys")
            .right
            .kill
            .goTo(InLocations.Level2.keyMid)
            .up // nothing here
            .seg("gather key 2")
            .left
            .kill
            .seg("gather key 3")
            .left
            // opportunity kill
            .goTo(InLocations.Level2.keyMid)
            .right
            .right // grid room
            .seg("sprint up from grid")
            // right is nothing
            .up // need to be able to kill
            .seg("go get blue boomerang")
            .upm // which upm
            .right
            .kill
            .goTo(InLocations.Level2.keyMid)
            .left
            .seg("resume sprint")
            .up // skip squishy snake
            // skip getting key from squishy guy
//            .kill
//            .goTo(InLocations.Level2.keyMid)
            .upm
            .kill
            .seg("bomb room")
            .goTo(InLocations.Level2.keyMid)
            .up
            .kill // blocked before going // allow bombs
            .goTo(InLocations.Level2.bombItemRight)
            .up
            .seg("kill boss")
            .killR // need special strategy for thse guys
            .wait
            .goTo(InLocations.Level2.heartMid)
            .seg("get the trigorce")
            .left
            .goIn(GamePad.MoveLeft, 30)
            .goTo(InLocations.Level2.triforce)
            .build()
    }

    private fun levelPlan3(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("Destroy level 3")
        // open questions: fighting the trap guys
        return builder.lev(3).startAt(LevelStartMapLoc.lev(3))
            .seg("grab key")
            .leftm
            .goTo(InLocations.Level2.keyMid) //confirm
            .seg("walk round corner")
            .up // skip key
            .up
            .left
            .seg("past the compasS")
            .left
            .goIn(GamePad.MoveLeft, 30)
            .seg("fight swords")
            .kill
            .down
            .seg("get raft")
            .goTo(InLocations.rightStair)
            .startAt(15)
            .go(InLocations.getItem)
            .upTo(105) // ??
            .seg("get to boss")
            .upm
            .right
            .rightm // option to get key up, but skip
            .bomb(InLocations.BombDirection.right) // right bomb
            .right
            .upm
            .bomb(InLocations.BombDirection.right) // right bomb
            .right
            .seg("kill boss")
            .kill // need special strategy for the 4monster
            .up
            .goTo(InLocations.Level2.triforce)
            .go(InLocations.Level3.heartMid)
            .build()

    }

    private fun levelPlan4(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("Destroy level 4")
        // problems:
        // sometimes when move to push, zelda gets shifted to right
        // getting stuck on the ladder
        // get stuck in middle of maze

        return builder.lev(4).startAt(113)
            .seg("go go go")
            //key to left but dont bother
            .up
            .up // no get it because it is in the middle of the room
            .goTo(InLocations.Level4.batKey)
            .leftm
            .up
            .goTo(InLocations.Level4.squishyKey)
            .up // get key? kill squishies?
            .seg("go get ladder")
            .rightm
            .kill
            .right
            .kill2 // there will be 2 suns still running around
            .seg("push")
            .pushWait(InLocations.Level4.moveLeftOfTwo)
            .goTo(InLocations.rightTop)
            .startAt(96)
            .go(InLocations.getItem)
            .upTo(50)
            .leftm
            .leftm
            .seg("get past 4 monster")
            .up
            .up
            .bomb(InLocations.BombDirection.right) // right bomb
            .seg("get to the dragon")
            .right
            //skip key that is up
            .bomb(InLocations.BombDirection.right) // right bomb
            .right
            .killb
            .pushWait(InLocations.Level4.moveLeftOfTwo)
            .right
            .seg("fight dragon")
            .kill // dragon
            //get heart
            .goTo(InLocations.Level4.triforceHeart)
            .up
            .goTo(InLocations.Level2.triforce)
            .build()
    }

    private fun levelPlan5(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("Get to level 1")
        return builder {
            lev(5)
            startAt(LevelStartMapLoc.lev(5))
            seg("move to level 5")
            up
            bomb(InLocations.BombDirection.left)
            left
            bomb(InLocations.BombDirection.left)
            left
            seg("kill before going in")
            kill
            seg("go in")
            // not robust
            // move to bottom left first
            pushInLevelMiddleStair(88, upTo = 6, outLocation = InLocations.getOutRight)
            left
//            startAt(5)
            seg("kill before getting item")
            kill
            // it is a center push
            pushInLevelAnyBlock(inMapLoc = InLocations.Level5.mapLocGetItem,
                pushTarget = InLocations.Level5.moveLeft,
                stairsTarget = InLocations.Level5.cornerStairs
            )
            seg("backtrack out")
            right
//            startAt(6)
            pushInLevelMiddleStair(88, upTo = 100, outLocation = InLocations.getOutLeft)
            seg("get back")
            right
            right
            seg("kill all zombie to open")
//            startAt(102)
            kill //get key 102 ??
            upm // 86, rhinos
            rightm //85
//            startAt(87)
            seg("no head up to victory")
            upm // impossible maze
            upm
            upm
            seg("go left to victory")
            leftm
            leftm
            leftm
//            startAt(36)
            seg("Use Whistle")
            goIn(GamePad.MoveLeft, 10) // more in a bit before whistlin'
            switchToWhistle()
            goIn(GamePad.None, 50) // more in a bit before whistlin'
            goIn(GamePad.MoveLeft, 20) // move more in
            useItem()
//            wait(100) // wait for whistle to happen
            seg("Now destroy him")
            kill // problem the projectiles are considered enemies
            seg("Get 5 triforce")
            goTo(InLocations.Level5.triforceHeart)
            up
            goTo(InLocations.Level2.triforce)
        }
    }

    private fun levelPlan6(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("Get to level 6")
        return builder {
            lev(6)
            startAt(LevelStartMapLoc.lev(6))
            seg("move to level 6")
//            .right //key
            leftm
            seg("first ghost")
            upm // skip
            seg("squishies")
            up // todo grab key in center
            seg("past traps")
            // have to kill them to go up
            kill // can't there too many fireballs, just move on
            // get key after kill
            goTo(FramePoint(8.grid, 5.grid))
            up
            seg("kill and push to continue")
            upm
            // failed, ghosts do not move therefore, we dont know
            // when they are killed
            // ghosts, have their id's cycle
            killUntil(1) // there is the suns so
            pushJust(InLocations.Level6.moveUp)
            upm // 38
            startAt(40) // state9
            kill // just to make sure the bomb is successful
            bomb(InLocations.bombRight)
            seg("go up to get want")
            right // 39
            up
            up
            seg("get want")
            startAt(9) //save8
            kill
//            pushJust(InLocations.Level6.moveUp)
            // 117
//            goTo(InLocations.Level5.cornerStairs)
            pushInLevelAnyBlock(inMapLoc = LevelSpecBuilder.getItemLoc6,
                pushTarget = InLocations.Level6.moveUp,
                stairsTarget = InLocations.Level5.cornerStairs
            )
            seg("go down to other stair")
            down //25
            down //41
            down //57 save7
            startAt(57)
            kill
            right
            startAt(58)//save6
            seg("center move stair")
            kill1
//            pushInLevelAnyBlock(inMapLoc = LevelSpecBuilder.getItemMove6,
//                pushTarget = InLocations.Level6.moveUpSingle,
//                stairsTarget = InLocations.Level5.cornerStairs
//            )
            pushInLevelAnyBlock(inMapLoc = LevelSpecBuilder.getItemMove6,
                pushTarget = InLocations.Level6.moveUpSingle,
                stairsTarget = InLocations.Level5.cornerStairs,
                directionFrom = Direction.Left,
                outLocation = InLocations.getOutRight,
                position = FramePoint(3.grid, 5.grid),
                upTo = 29,
                thenGo = GamePad.MoveRight
            )
            down
//            // get key
            left
//             44
//             todo; ignore the iD2
            startAt(44) //save5
            upm
            startAt(28) //save4
            killArrowSpider
            goTo(InLocations.Level6.triforceHeart)
            up
            goTo(InLocations.Level2.triforce)
        }
    }

    private fun levelPlan7(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("Get to level 7")
        return builder {
            lev(7)
            startAt(LevelStartMapLoc.lev(7))
            upm
            bomb(InLocations.topMiddleBombSpot)
            upm
            upm
            seg("past water")
            kill2
            up
            leftm
            up
//            startAt(40)
            seg("bait spot")
            goIn(GamePad.MoveUp, 20)
            switchToBait()
            goIn(GamePad.None, 100)
            goIn(GamePad.MoveUp, 20) // move more in
            goTo(FramePoint(8.grid, 7.grid))
            useItem()
            upm
            rightm
//            startAt(25)
            bomb(InLocations.bombRight)
            right
            seg("red candle")
            // can't kill the guy inside
            // so skip
            killAllInCenter
////            kill
            pushInLevelMiddleStair(88, upTo = 26)
//            //https://cdn.wikimg.net/en/strategywiki/images/9/90/LOZ_Dungeon_7.png
            // sometimes is start the menu too soon
            // didn't work, maybe just assume already holding on to bombs
            bomb(InLocations.bombRight, switch = false)
            rightm
            rightm
            seg("kill whistle")
            goIn(GamePad.MoveRight, 20) // more in a bit before whistlin'
            switchToWhistle()
            goIn(GamePad.None, 50) // more in a bit before whistlin'
            goIn(GamePad.MoveRight, 20) // move more in
            useItem()
//            // should do kill all but the projectiles
////            startAt(28)
            kill
            upm
            bomb(InLocations.bombRight)
            right
            seg("Kill hands")
            startAt(13)
            killHandsInLevel7
//            kill3
            // hard, can't kill those hands
            // position
            // bottom right
            // mid
            goTo(FramePoint(2.grid, 8.grid))
            pushInLevelAnyBlock(inMapLoc = InLocations.Level5.mapLocGetItem,
                pushTarget = InLocations.Level7.pushRight,
                stairsTarget = InLocations.Level5.cornerStairsBefore,
                directionFrom = Direction.Left,
                outLocation = InLocations.getOutLeft,
                position = FramePoint(3.grid, 5.grid),
                upTo = 41,
                thenGo = GamePad.MoveRight
            )
//            goTo(InLocations.rightTop)
            seg("near dragon")
            // just wait until switching
            goIn(GamePad.None, 100)
            bomb(InLocations.bombRight)
            seg("dragon")
            rightm
            kill
            goTo(InLocations.Level7.triforceHeart)
            right
            goTo(InLocations.Level2.triforce)
        }
    }

    private fun levelPlan8(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("Get to level 8")
        return builder {
            lev(8)
            startAt(LevelStartMapLoc.lev(8))
            seg("run past")
            left
            kill // 2 projectiles that are alive for brief moments
            left
            startAt(124) //state1
            seg("go get book")
            kill2
            pushInLevelMiddleStair(LevelSpecBuilder.getItemLoc8, upTo = 124)
            seg("get back to start")
            rightm
            rightm // at start
            seg("get to crossroads")
            up
            "bomb".seg()
            bomb(InLocations.topMiddleBombSpot)
            up
            startAt(94) // save3
            kill
            goTo(InLocations.Level8.keySpot)
            upm
//            startAt(62)
            upm // master battle
            bomb(InLocations.topMiddleBombSpot)
            upm
            upm
            killArrowSpider // kill arrow guy
            rightm
            seg("get key")
            startAt(31) // save4
            killAllInCenter
            pushInLevelMiddleStair(LevelSpecBuilder.getItemLoc8Key, upTo = 31)
            seg("get back to master battle")
            left
            down
            down
            kill // master battle
            seg("take stair to end")
            rightm

            startAt(63)
            pushInLevelAnyBlock(inMapLoc = 62, //fake
                pushTarget = null,
                stairsTarget = InLocations.rightStairGrid,
                outLocation = InLocations.getOutLeft,
                upTo = 76,
                thenGo = GamePad.MoveRight
            )
//            startAt(76) //save4
            // give time to enter so that switching to bomb works
            goTo(FramePoint(11.grid, 2.grid))
            bomb(InLocations.topMiddleBombSpot)
            "kill dragon".seg()
            upm
            kill
            goTo(InLocations.Level8.triforceHeart)
            up
            goTo(InLocations.Level2.triforce)
        }
    }

    private fun LocationSequenceBuilder.levelPlan9PhaseRedRing() {
        this.add {
            lev(9)
            startAt(LevelStartMapLoc.lev(9))
            upm
            leftm
            bomb(InLocations.topMiddleBombSpot)
            upm // or else it will chase the suns
//            left
//             it's not 14
            startAt(85) // save8
            kill
            pushInLevelMiddleStair(LevelSpecBuilder.Companion.Nine.travel1, upTo = 20, outLocation = InLocations.getOutLeft)

            startAt(20) //save7
            "spiral".seg()
            //kill
            rightp // kill the pancakes, getting quite stuck
            rightm
            "go down to ring".seg()
            downm
            startAt(38) //save6
            // what if the bomb missed
            bomb(InLocations.bombRight)
            rightm
            bomb(InLocations.topMiddleBombSpot)
            // p? // need move but also have the kill
//            startHere
            upmp
            bomb(InLocations.topMiddleBombSpot)
            upm
            "ring spot".seg()
            kill // kill2
            startAt(7) //save5
            kill // kill3 // stuck here on the disappear ghost
            pushInLevelMiddleStair(LevelSpecBuilder.getItemLoc8)
        }
    }

    private fun LocationSequenceBuilder.levelPlan9PhaseSilverArrow() {
        add {
            downp // kill pancake
            downm
            leftm
//            startAt(38) //save6
            upm
            "go in next room".seg()
            upm
            bomb(InLocations.bombLeft)
            leftm
            "kill travel 1".seg()
            kill
            startAt(5) //save3
            pushInLevelAnyBlock(
                inMapLoc = LevelSpecBuilder.Companion.Nine.travel2,
                pushTarget = InLocations.Level9.moveUpBlock,
                stairsTarget = InLocations.Level5.cornerStairs,
                outLocation = InLocations.getOutLeft,
                upTo = 99,
                thenGo = GamePad.MoveRight
            )

            "travel to arrow".seg()
            left
            "past bats".seg()
            leftm  //bats
            "circle monster kill".seg()
            kill
            pushInLevelMiddleStair(LevelSpecBuilder.Companion.Nine.travel3, upTo = 32)
            bomb(InLocations.topMiddleBombSpot)
            upm
            // save9
            "acquire arrow".seg()
            kill
            "set the arrow".seg()
            pushInLevelAnyBlock(
                inMapLoc = LevelSpecBuilder.Companion.Nine.silverArrow,
                pushTarget = InLocations.Level7.pushRight,
                stairsTarget = InLocations.Level5.cornerStairsBefore,
                directionFrom = Direction.Left,
                position = FramePoint(3.grid, 5.grid),
                thenGo = GamePad.MoveRight
            )
        }
    }

    private fun LocationSequenceBuilder.levelPlan9PhaseGannon() {
        add {
            "return to center".seg()
            down
            kill
            "take stair back".seg()
            pushInLevelMiddleStair(88, upTo = 97, outLocation = InLocations.getOutRight)
            // save3
            "get to push spot".seg()
            "past first pancake".seg()
            upmp // walk past
            "past second pancake".seg()
            startAt(65)
            up
//            upmp //5
//            startHere
            "bomb left ok".seg()
            bomb(InLocations.bombLeft)
            leftmp
            kill //save6
            "push to inbetween travel".seg()
            pushInLevelAnyBlock(inMapLoc = LevelSpecBuilder.Companion.Nine.travel4,
                pushTarget = InLocations.Level6.moveUp,
                stairsTarget = InLocations.Level5.cornerStairs,
                outLocation = InLocations.getOutRight,
                upTo = 4
            )
            "get to final stair".seg() // save7
            bomb(InLocations.bombLeft)
            leftm
            killp
            pushInLevelMiddleStair(119, upTo = 82, outLocation = InLocations.getOutLeft)
            "doorstep of gannon".seg()
            kill
            up
            "seg kill gannon".seg()
            switchToArrow()
            goTo(InLocations.Level9.centerGannonAttack)
            killG
            lootInside
            upm
            "seg get princess".seg()
            rescuePrincess()
            peaceReturnsToHyrule()
            // display some summary stats before ending
//            booya
            end
        }
    }

    private fun levelPlan9(factory: SequenceFactory): MasterPlan {
        val builder = factory.make("Get to level 9")
        return builder {
            lev(9)
            levelPlan9PhaseRedRing()
            levelPlan9PhaseSilverArrow()
            levelPlan9PhaseGannon()
            end
        }
    }
}