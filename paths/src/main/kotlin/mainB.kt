import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import bot.GamePad
import bot.ZeldaBot
import bot.plan.runner.PlanRunner
import bot.state.*
import bot.state.map.MapConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import util.Map2d
import util.d

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(width = 800.dp, height = 500.dp),
        title = "Zelda"
    ) {
        d { " compose " }
        val model = remember { ZeldaModel() }

        val state = model.plan.value
        var count = remember { mutableStateOf(0) }
        var showMap = remember { mutableStateOf(false) }
        val act = remember { mutableStateOf(true) }

        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            Text(
                text = "Plan: ${state?.mapLoc} ${state?.state?.currentMapCell?.mapData?.name ?: ""}",
                fontSize = 20.sp,
            )
            Text(
                text = state?.planRunner?.masterPlan?.toStringCurrentPlanPhase() ?: "None",
                fontSize = 20.sp,
            )
            Text(
                text = "Action (c): ${state?.currentAction}",
                fontSize = 20.sp,
            )
            Text(
                text = "Action (n): ${state?.planRunner?.afterThis()?.name ?: ""}",
                fontSize = 20.sp,
            )
            Text(
                text = "Action (n2): ${state?.planRunner?.afterAfterThis()?.name ?: ""}",
                fontSize = 20.sp,
            )
            Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    count.value = count.value + 1
                    model.start()
                }) {
                Text("START")
            }
            Row(
                modifier = Modifier.align(Alignment.Start)
            ) {
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        model.forceDir(GamePad.A, num = 15)
                    }) {
                    Text("  A ")
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        model.forceDir(GamePad.B, num = 15)
                    }) {
                    Text("  B ")
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        model.forceDir(GamePad.Start, 2)
                    }) {
                    Text("  S  ")
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        model.updateEnemies()
                    }) {
                    Text("  Update Enemies  ")
                }

            }

            Row(
                modifier = Modifier.align(Alignment.Start)
            ) {
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        model.forceDir(GamePad.MoveRight)
                    }) {
                    Text("  R  ")
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        model.forceDir(GamePad.MoveLeft)
                    }) {
                    Text("  L  ")
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        model.forceDir(GamePad.MoveUp)
                    }) {
                    Text("  U  ")
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        model.forceDir(GamePad.MoveDown)
                    }) {
                    Text("  D  ")
                }
            }
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(
                ) {
                    Text("Ladder")
                    Checkbox(
                        checked = act.value,
                        onCheckedChange = {
                            act.value = it
                            model.ladder(it)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.Start)
            ) {
                Row {
                    Text("Act")
                    Checkbox(
                        checked = act.value,
                        onCheckedChange = {
                            act.value = it
                            model.changeAct(it)
                        }
                    )
                }

                Row {
                    Text("ShowMap")
                    Checkbox(
                        checked = showMap.value,
                        onCheckedChange = {
                            showMap.value = it
                        }
                    )
                }
            }

            Row {
                Text("Enemies", fontSize = 20.sp)
                state?.state?.frameState?.let {
                    val alive = it.enemiesClosestToLink(EnemyState.Alive).size
                    val dead = it.enemiesClosestToLink(EnemyState.Dead).size
                    val unknown = it.enemiesClosestToLink(EnemyState.Unknown).size
                    Text("Alive: ${alive} dead: $dead unknown ${unknown}", modifier = Modifier.padding(12.dp))
                    Text("Killed: ${it.killedEnemyCount} of ${state.state.numEnemiesSeen}", modifier = Modifier.padding(12.dp))
                }
            }

            Row {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Enemies", fontSize = 20.sp)
                    state?.enemiesInfo?.let { enemies ->
                        if (enemies.isNotEmpty()) {
                            enemies.filter { it.state != EnemyState.Dead } .forEachIndexed { index, enemy ->
                                Text("$index: storedIndex: ${enemy.index} ${enemy.state.name} ${enemy.point} ${enemy.point.toG} id ${enemy
                                    .droppedId}")
                            }
                        }
                    }
                }
            }

//            state?.state?.let {mState ->
//                HyruleMap(mState, state.planRunner)
//            }
            if (showMap.value) {
                state?.stateSnapshot?.let { mState ->
                    HyruleMap(mState, state.planRunner)
                }
//                showMap.value = false
            }
        }
    }
}

@Composable
private fun HyruleMap(state: MapLocationState, plan: PlanRunner) {
    val link = state.link
    val path: List<FramePoint> = emptyList()
    val enemies: List<Agent> = state.frameState.enemiesSorted.filter { it.state == EnemyState.Alive }
    val projectiles: List<Agent> = state.frameState.enemiesSorted.filter { it.state == EnemyState.Projectile }

    val v = 2
    Canvas(
        modifier = Modifier.width((MapConstants.MAX_X.toFloat()*v).dp)
            .height((MapConstants.MAX_Y.toFloat()*v).dp )
    ) {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            paint.color = Color.Black
            val linkPathPaint = Paint()
            linkPathPaint.color = Color.Blue
            val targetPaint = Paint()
            targetPaint.color = Color.Cyan
            val passablePaint = Paint()
            passablePaint.color = Color.LightGray
            val notPassable = Paint()
            notPassable.color = Color.Gray
            val enemyPaint = Paint()
            enemyPaint.color = Color.Red
            val projPaint = Paint()
            projPaint.color = Color.Magenta
//            canvas.drawRect(0f, 0f, MapConstants.MAX_X.toFloat()*(v+1), MapConstants.MAX_Y.toFloat()*(v+1), paint)

//            d { " draw map cell"}
//            //for
            val passable = state.currentMapCell.passable

////            canvas.drawRect(x.toFloat(),y.toFloat(),x+v.toFloat(),y+v.toFloat(), passablePaint)
            for (x in 0..255) {
                val xa = x*v
//                for (y in 0..167) {
                    for (y in 0..167) {
//                val pt = point.toScreenY
                    val ya = y*v
                    if (passable.get(x, y)) {
                        canvas.drawRect(xa.toFloat(),ya.toFloat(),(xa+1)*v.toFloat(),(ya+1)*v.toFloat(), passablePaint)
                    } else {
                        canvas.drawRect(xa.toFloat(),ya.toFloat(),(xa+1)*v.toFloat(),(ya+1)*v.toFloat(), notPassable)
                    }
                }
            }
            drawPoint(canvas, v, link, paint)
            // draw path: linkPathPaint
            for (enemy in enemies) {
                drawGridPoint(canvas, v, enemy.point, enemyPaint)
            }
            for (pro in projectiles) {
                drawGridPoint(canvas, v, pro.point, projPaint)
            }

            drawPoint(canvas, v, plan.target(), targetPaint)

            val path = Path()
            val linkPath = plan.path()
            if (linkPath.isNotEmpty()) {
                d { " draw path ${linkPath.size}"}
                path.moveTo(linkPath[0].x.toFloat()*v, linkPath[0].y.toFloat()*v)
                for (pt in linkPath) {
                    path.lineTo(pt.x.toFloat()*v, pt.y.toFloat()*v)
                }
                canvas.drawPath(path, linkPathPaint)
            }
            // top is les than bottom
        }
    }
}

private fun drawPoint(canvas: Canvas, v: Int, pt: FramePoint, paint: Paint) {
    canvas.drawRect(pt.x*v.toFloat(),pt.y*v.toFloat(),(pt.x+1)*v.toFloat(),(pt.y+1)*v.toFloat(), paint)
}

private fun drawGridPoint(canvas: Canvas, v: Int, pt: FramePoint, paint: Paint) {
    canvas.drawRect(pt.x*v.toFloat(),pt.y*v.toFloat(),(pt.x+16)*v.toFloat(),(pt.y+16)*v.toFloat(), paint)
}

class ZeldaModel : ZeldaBot.ZeldaMonitor {
    val scope = CoroutineScope(Dispatchers.IO)
    val plan = mutableStateOf<ShowState?>(null)
    var enemiesInfo: List<Agent> = emptyList()
    private var updateEnemiesOnNext: Boolean = false

    private var stateSnapshot: MapLocationState? = null

    private var bot: ZeldaBot? = null

    init {
        start()
    }

    override fun update(state: MapLocationState, planRunner: PlanRunner) {
        if (updateEnemiesOnNext) {
            enemiesInfo = state.frameState.enemies.toMutableList()
            stateSnapshot = state
            updateEnemiesOnNext = false
        }
        plan.value = ShowState(
            currentAction = planRunner.action.name,
            mapLoc = state.frameState.mapLoc,
            state = state,
            stateSnapshot = stateSnapshot,
            planRunner = planRunner,
            enemiesInfo = enemiesInfo
        )
    }

    fun start() {
        val monitor = this
        scope.launch {
            bot = ZeldaBot.startIt(monitor)
        }
//        plan.value = ShowState("Starting...", 0, MapLocationState())
    }

    fun skip() {
        bot?.skip()
    }

    fun unStick() {
        ZeldaBot.unstick += 100
    }

    fun forceDir(forcedDirection: GamePad, num: Int = 100) {
        ZeldaBot.unstick += num
        ZeldaBot.forcedDirection = forcedDirection
    }

    fun updateEnemies() {
        updateEnemiesOnNext = true
    }

    fun changeAct(act: Boolean) {
        ZeldaBot.doAct = act
    }

    fun ladder(act: Boolean) {
        ZeldaBot.hasLadder = act
    }

    data class ShowState(
        val currentAction: String,
        val mapLoc: MapLoc = 0,
        val state: MapLocationState,
        val stateSnapshot: MapLocationState?,
        val planRunner: PlanRunner,
        var enemiesInfo: List<Agent> = emptyList()
    )
}