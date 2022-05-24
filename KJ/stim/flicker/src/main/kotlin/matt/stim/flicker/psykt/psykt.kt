@file:Suppress("UNREACHABLE_CODE", "unused")

package matt.stim.flicker.psykt

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.RadioButton
import javafx.scene.control.TextField
import javafx.scene.control.TextInputDialog
import javafx.scene.control.ToggleGroup
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import javafx.scene.shape.Shape
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import javafx.util.Duration
import matt.hurricanefx.eye.prop.div
import matt.hurricanefx.eye.prop.getValue
import matt.hurricanefx.eye.prop.setValue
import matt.hurricanefx.tornadofx.async.runLater
import matt.hurricanefx.tornadofx.nodes.add
import matt.hurricanefx.tornadofx.nodes.clear
import matt.json.custom.SimpleJson
import matt.kjlib.log.err
import matt.stim.flicker.psykt.ECEO.Phase.EC
import matt.stim.flicker.psykt.ECEO.Phase.EO
import matt.stim.flicker.psykt.Presenter.tasksParamControl
import java.io.File
import java.lang.Math.ceil
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.random.Random.Default.nextDouble
import kotlin.random.Random.Default.nextInt
import kotlin.reflect.KClass


class Subject(val name: String)
interface Stimulus {
  fun generate()
  fun show()
  fun hide()
}

@Suppress("unused")
class Response

@Suppress("unused")
class Feedback

@Suppress("unused")
class Experiment

class Logger {
  var headers = mutableListOf(
	"trial",
	"time"
  )

  val data = LinkedHashMap<String, Any>()
  fun writeHeaders() {
	logFile.parentFile.mkdirs()
	val gitignore = File(logFile.parentFile.absolutePath + File.separator + ".gitignore")
	gitignore.createNewFile()
	gitignore.writeText(
	  "# Ignore everything in this directory\n" +
		  "*\n" +
		  "# Except this file\n" +
		  "!.gitignore"
	)
	logFile.writeText("")
	headers = mutableListOf("trial", "time").apply { addAll(currentTask!!.logHeaders) }
	var i = 0
	headers.forEach {
	  if (i > 0) {
		logFile.appendText(",${it}")
	  } else logFile.appendText(it)
	  data[it] = ""
	  i++
	}
	logFile.appendText("\n")
  }

  fun dateString() = SimpleDateFormat("yy-MM-dd-HH-mm").format(Calendar.getInstance().time)
  fun timestamp() = currentTimeMillis()
  private fun update() {
	data["trial"] = currentTrial
	data["time"] = timestamp()
  }

  fun startTrial() {
	update()
	writeLine()
  }

  fun endTrial(lastTrial: Boolean) {
	data.keys.forEach {
	  if (it == "phase") {
		if (lastTrial) data[it] = "done"
		else data[it] = ""
	  }
	}
	update()
	writeLine()

	//        data["time"] = ""
	//        data.keys.forEach {
	//
	//        }
	//        writeLine()
  }

  fun log(vararg d: Pair<String, Any>) {
	data.keys.forEach {
	  data[it] = ""
	}
	update()
	d.forEach {
	  data[it.first] = it.second.toString()
	}
	writeLine()
  }

  private fun writeLine() {
	var i = 0
	data.forEach { _, v ->
	  if (i > 0) {
		logFile.appendText(",")
	  }
	  logFile.appendText(v.toString())
	  i++
	}
	logFile.appendText("\n")
  }

  fun resetFile() {
	//        logFile = File("data" +
	//                File.separator +
	//                "Psykt-" +
	//                "${currentTask!!::class.simpleName}${File.separator}${dateString()}--${currentSubject!!.name}${currentSubjectIteration}" + ".log")
	logFile =
	  File(System.getProperty("user.home") + "${File.separator}Desktop${File.separator}${dateString()}--${currentSubject!!.name}${currentSubjectIteration}" + ".log")
  }

  val dataFolder = File("data")
  var currentTask: Task? = null
  var currentSubject: Subject? = null
  var currentTrial = 0
  var currentSubjectIteration = 1
  var logFile = File("data/nulllog.txt")
}

abstract class Task() {
  var cancelTrials = false
  var ended = false

  //    init{
  //        workingParams.clear()
  //    }
  val logger = Logger()
  abstract var instructions: String
  abstract fun stimulus(): Stimulus
  abstract fun run()
  abstract val logHeaders: Array<String>

  val trialsProp = SimpleIntegerProperty(5)
  var trials by trialsProp
  val timerProp = SimpleIntegerProperty(0)
  var timer by timerProp
  val subjectProp = SimpleStringProperty("TEST")
  var subject by subjectProp
  val subjectIterationProp = SimpleIntegerProperty(0)
  var subjectIteration by subjectIterationProp
  val stimDurProp = SimpleLongProperty(1000)
  var stimDur by stimDurProp // ms
  val isiProp = SimpleLongProperty(200)
  var isi by isiProp // ms

  val paramProps = mutableListOf(
	"trials" to trialsProp,
	"timer" to timerProp,
	"subject" to subjectProp,
	"subect iteration" to subjectIterationProp,
	"stimDur" to stimDurProp,
	"isi" to isiProp
	//            ,
	//            "showFreq" to show
  )
}

val taskList = FXCollections.observableArrayList<KClass<out Task>>(
  DigitSpan::class,
  NBack::class,
  SpeedReading::class,
  Stroop::class,
  Video::class,
  VPVT::class,
  APVT::class,
  CVT::class,
  ECEO::class,
  ScreenFlicker1::class,
  ScreenFlicker2::class,
  SSVEPverify::class,
  ScreenFlicker3::class
)

class APVT: Task() {
  init {
	stimDur = 0L // ms
	isi = 1000L // ms
  }

  var canFinishTrial = false
  var respondToSpace = false
  val spaceResponseHandler = EventHandler<KeyEvent> {
	if (respondToSpace) {
	  thread {
		when (it.code) {
		  KeyCode.SPACE -> {
			logger.data["responseTime"] = currentTimeMillis()
			canFinishTrial = true
		  }
		  else          -> Unit
		}
	  }
	}
  }

  override fun stimulus() = object: Stimulus {
	override fun generate() = Unit

	override fun show() {
	  stimulate(soundClip)
	  canFinishTrial = false
	  respondToSpace = true
	}

	override fun hide() {

	}
  }

  override val logHeaders = arrayOf(
	"responseTime"
  )

  override fun run() {
	prepareExperiment()

	runTrials {
	  err("")
	  ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, spaceResponseHandler)
	  while (!canFinishTrial) {
		sleep(1)
	  }
	  removeAllStim()
	  respondToSpace = false
	}
	endExperiment()
  }


  val soundProp = SimpleStringProperty("chinese-gong-daniel_simon.mp3").apply {
	paramProps += "sound" to this
  }

  val soundClip by lazy {
	val sound = Media(File("sounds" + File.separator + soundProp.get()).toURI().toString())
	MediaPlayer(sound)
  }

  override var instructions =
	"A green circle will appear at a set interval. As soon as it appears each time, press the space bar."
}

class CVT: Task() {
//  lateinit var thread: Thread
  var stimImage: Shape? = null
  var shouldYes: Boolean = false
  var canFinishTrial = false
  var responseYes: Boolean? = null
  var respondToSpace = false
  override fun stimulus() = object: Stimulus {
	override fun generate() {
	  val yes = nextDouble() < yesChance.get()
	  stimImage = if (yes) mainTarget else mainDistractor
	  shouldYes = yes
	}

	override fun show() {
	  stimulate(stimImage!!)
	  responseYes = null
	  canFinishTrial = false
	  respondToSpace = true
	}

	override fun hide() {
	  //            removeAllStim()
	  //            respondToSpace = false
	}
  }

  override val logHeaders = arrayOf(
	"stim",
	"responseYes",
	"correct"
  )

  override fun run() {
	prepareExperiment()
	stimulate(instructions2)
	blockUntilUserHitsSpace()
	removeAllStim()
	var correct = false
	val checkImage = defaultImage("images/check.png")
	val xImage = defaultImage("images/x.png")
	var givingFeedback = false
	fun feedback() {
	  givingFeedback = true
	  if (correct) {
		stimulate(checkImage)
	  } else {
		stimulate(xImage)
	  }
	  sleep(1000)
	  removeAllStim()
	  givingFeedback = false
	}

	val cvtSpaceHandler = EventHandler<KeyEvent> {
	  if (respondToSpace) {
		responseYes = when (it.code) {
		  KeyCode.LEFT  -> {
			true
		  }
		  KeyCode.RIGHT -> {
			false
		  }
		  else          -> null
		}
		if (responseYes != null) {
		  //                    thread.interrupt()
		  canFinishTrial = true
		}
	  }
	}
	ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, cvtSpaceHandler)

	runTrials {
	  logger.data["stim"] = stimNames[stimImage].toString()
	  logger.data["responseYes"] = responseYes.toString()
	  correct = (shouldYes == responseYes)
	  logger.data["correct"] = correct
	  while (!canFinishTrial) {
		sleep(1)
	  }
	  removeAllStim()
	  respondToSpace = false
	  feedback()
	  while (givingFeedback) {
		sleep(10)
	  }
	  removeAllStim()
	}
	ExperimentStage.removeEventFilter(KeyEvent.KEY_PRESSED, cvtSpaceHandler)
	endExperiment()
  }

  override var instructions = "You will soon begin a task after some instructions and practice.\n" +
	  "\n" +
	  "NOTE: Please try to minimize eye blinking, excessive blinking can interfere with the data collection.\n" +
	  "\n" +
	  "(PRESS SPACE KEY TO CONTINUE)\n"

  val instructions2 = TextFlow(
	instructionsText("You will be presented with 2 different symbols one at a time:"),
	distractor(),
	target(),
	instructionsText(
	  "\n" +
		  "\n" +
		  "\n" +
		  "\n" +
		  "Use the LEFT and RIGHT keys to respond YES and NO.\n" +
		  "\n" +
		  "When "
	),
	target(),
	instructionsText(
	  "\n" +
		  "is presented, respond YES with LEFT.\n" +
		  "\n" +
		  "Press SPACE to continue."
	)
  )


  fun distractor() = Rectangle(50.0, 50.0, Color.YELLOW)
  val mainDistractor = distractor()

  fun target() = Rectangle(50.0, 50.0, Color.YELLOW).apply {
	rotate = 45.0
  }

  val mainTarget = target()

  val stimNames = mapOf(
	mainDistractor to "Distractor",
	mainTarget to "Target"
  )

  var n = SimpleIntegerProperty(2).apply {
	paramProps += "n" to this
  }
  var yesChance = SimpleDoubleProperty(0.5).apply {
	paramProps += "yesChance" to this
  }
}


const val DEMO_FORWARD_MIN = 3
const val DEMO_FORWARD_MAX = 4
const val DEMO_REVERSE_MIN = 2
const val DEMO_REVERSE_MAX = 3
const val FORWARD_MIN = 3
const val FORWARD_MAX = 9
const val REVERSE_MIN = 2
const val REVERSE_MAX = 8

const val N_SPANS = 3
const val DEMO_SPANS = 1
var demo = true
const val PASSING_N_CORRECT = 2

const val instructions_forward_demo =
  "A sequence of n numbers appears on the screen, one at a time. At the end of the sequence, type the digits back in the same order. Every trial, n increases by 1. First we practice with n = $DEMO_FORWARD_MIN up to n = $DEMO_FORWARD_MAX. Press the spacebar to start."


const val instructions_forward =
  "Now we will start the real task, which goes up to n = $FORWARD_MAX. Every three trials, n increases by 1. If less than $PASSING_N_CORRECT out of $N_SPANS trials are correct, we advance to the next phase."

const val instructions_reverse_demo =
  "Now practice the backwards digit span. Enter the digits back in the reverse order that they are presented. We will practice with n = $DEMO_REVERSE_MIN up to n = $DEMO_REVERSE_MAX, 1 trial each."

const val instructions_reverse =
  "Now we will start the actual reverse digit span at n = $REVERSE_MIN up to $REVERSE_MAX. Remember to enter the digits back in the reverse order that they are presented. Every three trials, n increases by 1. If less than $PASSING_N_CORRECT out of $N_SPANS trials are correct, we advance to the task is over."


class DigitSpan: Task() {
  val stims = mutableListOf<Label>()
  val digits = mutableListOf<Int>()
  var correct = false
  var sameResultTwice = false
  override fun stimulus() = object: Stimulus {

	override fun generate() {
	  stims.clear()
	  digits.clear()
	  if (adaptive.get() && sameResultTwice) {
		if (correct) n.set(n.get() + 1)
		else n.set(n.get() - 1)
	  }
	  for (i in 1..n.get()) {
		fun tryRand() = nextInt(10)
		var next = tryRand()
		while (next == 0) {
		  next = tryRand()
		}
		digits.add(next)
	  }

	  for (digit in digits) {
		stims += Label(digit.toString()).apply {
		  font = Font(100.0)
		}
	  }
	}

	override fun show() {
	  logger.data["stim"] = digits

	  val l = Label("${digits.size} Digits").apply {
		textFill = Color.GREEN
		font = Font(100.0)
	  }

	  stimulate(l)

	  sleep(2500)

	  removeAllStim()

	  sleep(1000)

	  for ((i, _) in digits.withIndex()) {
		stimulate(stims[i])
		if (i < digits.size - 1) {
		  sleep(1000)
		  removeAllStim()
		  sleep(20)
		}

	  }
	}

	override fun hide() {
	  removeAllStim()
	  sleep(20)
	}
  }

  override val logHeaders = arrayOf(
	"stim",
	"n",
	"response",
	"correct"
  )

  var letInResponse = true

  var nCorrect = mutableListOf<Boolean?>()
  var n_current = 1


  class DigitRemover {
	var cancel = false
	fun run() {
	  sleep(SHOW_DIGIT_T)
	  if (!cancel) {
		removeAllStim()
	  }
	}
  }

  var lastDigitRemover: DigitRemover? = null

  fun digitSpanTrial() {
	val responses = mutableListOf<Int>()
	var canFinishTrial = false
	val digitResponseHandler = EventHandler<KeyEvent> {
	  if (!letInResponse) return@EventHandler
	  letInResponse = false
	  if (lastDigitRemover != null) {
		lastDigitRemover!!.cancel = true
		removeAllStim()
	  }
	  thread {
		val response = when (it.code) {
		  KeyCode.DIGIT1 -> 1
		  KeyCode.DIGIT2 -> 2
		  KeyCode.DIGIT3 -> 3
		  KeyCode.DIGIT4 -> 4
		  KeyCode.DIGIT5 -> 5
		  KeyCode.DIGIT6 -> 6
		  KeyCode.DIGIT7 -> 7
		  KeyCode.DIGIT8 -> 8
		  KeyCode.DIGIT9 -> 9
		  else           -> null
		}
		if (response != null) {
		  val resp = Label(response.toString()).apply {
			textFill = Color.BLUE
			font = Font(100.0)
		  }
		  stimulate(resp)
		  val dr = DigitRemover()
		  lastDigitRemover = dr
		  thread { dr.run() }
		  responses.add(response)
		  println("responses size = ${responses.size}")
		  if (responses.size >= n.get()) {
			canFinishTrial = true
			//                        removeAllStim()
		  }

		}
		letInResponse = true

	  }

	}
	logger.data["n"] = n.get()
	ExperimentRoot.background = Background(BackgroundFill(Color.GRAY, null, null))
	ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, digitResponseHandler)
	while (!canFinishTrial) {
	  sleep(10)
	}
	sleep(SHOW_DIGIT_T)
	ExperimentRoot.background = Background(BackgroundFill(Color.WHITE, null, null))
	ExperimentStage.removeEventFilter(KeyEvent.KEY_PRESSED, digitResponseHandler)
	println("raw responses:")
	responses.forEach {
	  println("\t${it}")
	}
	val nResponses = mutableListOf<Int>()
	for (i in 0 until n.get()) {
	  nResponses += responses[i]
	}
	responses.clear()
	nResponses.forEach { responses.add(it) }
	logger.data["response"] = responses

	var newCorrect = true
	if (!forward.get()) responses.reverse()
	for ((index, response) in responses.withIndex()) {
	  if (!newCorrect) break
	  newCorrect = (digits[index] == response)
	}
	sameResultTwice = (newCorrect == correct)
	correct = newCorrect
	logger.data["correct"] = correct
	//            val checkImage = defaultImage("images/check.png")
	//            val checkImage = Circle(25.0,Color.GREEN)
	val checkImage = Label("✓").apply {
	  font = Font.font(250.0)
	  textFill = Color.GREEN
	}
	//            val xImage = defaultImage("images/x.png")
	//            val xImage = Rectangle(25.0,25.0,Color.RED)
	val xImage = Label("X").apply {
	  font = Font.font(250.0)
	  textFill = Color.RED
	}
	val feedback = if (correct) checkImage else xImage
	stimulate(feedback)
	val spans = if (demo) DEMO_SPANS else N_SPANS
	val passing = if (demo) 0 else PASSING_N_CORRECT
	if (nCorrect.isEmpty()) {
	  for (i in 0 until spans) {
		nCorrect.add(null)
	  }
	}
	nCorrect[n_current - 1] = correct
	if (!demo && n_current in passing..(spans - 1)) {
	  var corrects = 0
	  for (b in nCorrect) {
		if (b != null && b) {
		  corrects += 1
		}
	  }
	  if (corrects >= passing) {
		if (n.get() < nMax) {
		  n.set(n.get() + 1)
		} else {
		  cancelTrials = true
		}
		n_current = 1
		nCorrect.clear()
	  } else if (corrects < nCorrect.size - passing) {
		cancelTrials = true
		n_current = 1
		nCorrect.clear()
	  } else {
		n_current += 1
		//                nCorrect.clear()
	  }
	} else if (n_current == spans) {

	  var corrects = 0
	  for (b in nCorrect) {
		if (b != null && b) {
		  corrects += 1
		}
	  }
	  if (corrects < passing) {
		cancelTrials = true
	  }
	  if (n.get() < nMax) {
		n.set(n.get() + 1)
	  } else {
		cancelTrials = true
	  }
	  n_current = 1
	  nCorrect.clear()
	} else {
	  n_current += 1
	  //            nCorrect.clear()
	}
	sleep(1000)
	removeAllStim()
  }

  var nMax = 9

  override fun run() {


	prepareExperiment(instructions_forward_demo)
	n.set(DEMO_FORWARD_MIN)
	nMax = DEMO_FORWARD_MAX
	forward.set(true)
	n_current = 1
	demo = true
	cancelTrials = false
	trials = nMax*2
	runTrials {
	  digitSpanTrial()
	}

	instruct(instructions_forward)
	n.set(FORWARD_MIN)
	nMax = FORWARD_MAX
	forward.set(true)
	n_current = 1
	demo = false
	cancelTrials = false
	trials = nMax*2
	runTrials {
	  digitSpanTrial()
	}

	instruct(instructions_reverse_demo)
	n.set(DEMO_REVERSE_MIN)
	nMax = DEMO_REVERSE_MAX
	forward.set(false)
	n_current = 1
	demo = true
	cancelTrials = false
	trials = nMax*2
	runTrials {
	  digitSpanTrial()
	}


	instruct(instructions_reverse)
	n.set(REVERSE_MIN)
	nMax = REVERSE_MAX
	forward.set(false)
	n_current = 1
	demo = false
	cancelTrials = false
	trials = nMax*2
	runTrials {
	  digitSpanTrial()
	}

	removeAllStim()

	stimulate(Label("Thank you for completing the experiment").apply {
	  font = Font(50.0)
	})

	sleep(5000)

	endExperiment()


  }

  fun instruct(s: String) {
	val instructionsLabel = instructionsLabel(s)
	Platform.runLater {
	  ExperimentRoot.apply {
		children.clear()
		children.add(instructionsLabel)
	  }

	  ExperimentStage.show()
	  ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, spaceHandler)
	}

	blockUntilUserHitsSpace()
	Platform.runLater {
	  ExperimentRoot.apply {
		children.remove(instructionsLabel)
	  }
	}

  }


  var n = SimpleIntegerProperty(4).apply {
	paramProps += "n" to this
  }

  var forward = SimpleBooleanProperty(true).apply {
	paramProps += "forward" to this
  }
  var adaptive = SimpleBooleanProperty(false).apply {
	paramProps += "adaptive" to this
  }

  override var instructions = ""
}

val SHOW_DIGIT_T = 200L


class ECEO: Task() {
  init {
	stimDur = 0L // ms
	isi = 0L // ms
  }

  var respondToSpace = false
  val spaceResponseHandler: (KeyEvent)->Unit = {
	println("GOT KEY")
	if (respondToSpace) {
	  println("RESPOND SPACE")
	  thread {
		println("KEY THREAD")
		when (it.code) {
		  KeyCode.SPACE -> {
			println("SPACE KEY... LOGGING BLINK")
			logger.log("blink" to currentTimeMillis())
		  }
		  else          -> Unit
		}
	  }
	}
  }

  override fun stimulus() = object: Stimulus {
	override fun generate() = Unit

	override fun show() {
	  stimulate(currentSoundClip!!)
	  respondToSpace = true
	}

	override fun hide()  = Unit
  }

  override val logHeaders = arrayOf(
	"phase",
	"blink"
  )

  //    val phases = arrayOf(
  //            Phase.EC to 10,
  //            Phase.EO to 10,
  //            Phase.EC to 10,
  //            Phase.EO to 10,
  //            Phase.EC to 10,
  //            Phase.EO to 10,
  //            Phase.EC to 20,
  //            Phase.EO to 20,
  //            Phase.EC to 20,
  //            Phase.EO to 30,
  //            Phase.EC to 30,
  //            Phase.EO to 20,
  //            Phase.EC to 20,
  //            Phase.EO to 20,
  //            Phase.EC to 10,
  //            Phase.EO to 10,
  //            Phase.EC to 10,
  //            Phase.EO to 10,
  //            Phase.EC to 10,
  //            Phase.EO to 10
  //    )

  val phases = arrayOf(
	EC to 300,
	EO to 300
  )

  init {
	trials = phases.size
  }

  override fun run() {
	prepareExperiment()
	experimentKeyFilter(spaceResponseHandler)

	currentSoundClip = clips[phases.first().first.ordinal]
	runTrials {
	  val phaseN = logger.currentTrial - 1
	  val phase = phases[phaseN]
	  logger.log("phase" to phase.first.name)
	  sleep(phase.second*1000L)
	  removeAllStim()
	  respondToSpace = false
	  if (phaseN != phases.size - 1) {
		currentSoundClip = clips[phases[phaseN + 1].first.ordinal]
	  } else {
		stimulate(thankYouClip)
	  }
	}
	endExperiment()
  }

  val closeFile = "close.m4a"
  val openFile = "open.m4a"
  val thankYou = "thank you.m4a"

  val closeSoundClip by lazy {
	val sound = Media(File("sounds" + File.separator + closeFile).toURI().toString())
	MediaPlayer(sound)
  }
  val openSoundClip by lazy {
	val sound = Media(File("sounds" + File.separator + openFile).toURI().toString())
	MediaPlayer(sound)
  }
  val thankYouClip by lazy {
	val sound = Media(File("sounds" + File.separator + thankYou).toURI().toString())
	MediaPlayer(sound)
  }

//  var currentClip: MediaPlayer? = null
  val clips by lazy {
	arrayOf(closeSoundClip, openSoundClip)
  }

  enum class Phase {
	EC,
	EO
  }

  var currentSoundClip: MediaPlayer? = null

  override var instructions =
	"If you hear the word \"close\", close your eyes. If you hear the word \"open\", open your eyes. This will repeat over 5 minutes.\n\n" +
		"" +
		"While your eyes are open, try not to blink and to keep your focus fixated on one object to reduce signal noise. For the duration of the experiment please keep your finger on the space bar. If you do blink, just hit the space bar. Because I'm trying to detect when eyes are open and closed, it's important I remove all blink data.\n\n" +
		"" +
		"If for any reason your timing of opening and closing your eyes is off by more than 2 seconds, just stop and let me know and we'll restart.\n\n Press space to continue."
}


class NBack: Task() {
  lateinit var thread: Thread
  var stimImageView: ImageView? = null
  val stimHistory = mutableListOf<File>()
  val stimuli = File("images").resolve("stimuli").listFiles()
  val numStimuli = stimuli.size
  var stimImage: File? = null
  var shouldPress: Boolean = false
  var didPress = false
  var respondToSpace = false
  override fun stimulus() = object: Stimulus {
	override fun generate() {
	  val match = if (logger.currentTrial >= n.get()) (nextDouble() < matchChance.get()) else false
	  stimImage = if (match) {
		stimHistory[logger.currentTrial - n.get()]
	  } else {
		var rStimIdx = nextInt(numStimuli)
		val copy = ArrayList<File>().apply {
		  addAll(stimuli)
		}
		if (logger.currentTrial >= n.get()) {
		  copy.remove(stimHistory[logger.currentTrial - n.get()])
		  rStimIdx = nextInt(numStimuli - 1)
		}
		copy[rStimIdx]
	  }
	  stimHistory += stimImage!!
	  stimImageView = defaultImage(stimImage!!)

	  val i = stimHistory.size - 1
	  shouldPress = if (i >= n.get()) {
		println("this image = ${stimImage}")
		println("possible match = ${stimHistory[i - n.get()]}")
		stimHistory[i] == stimHistory[i - n.get()]
	  } else {
		false
	  }

	  println("shouldPress = ${shouldPress}")
	}

	override fun show() {
	  stimulate(stimImageView!!)
	  didPress = false
	  println("didPress = FALSE")
	  respondToSpace = true
	}

	override fun hide() {
	  //            removeAllStim()
	  respondToSpace = false
	}
  }

  override val logHeaders = arrayOf(
	"image",
	"shouldPress",
	"didPress",
	"correct"
  )

  override fun run() {
	prepareExperiment()
	var correct = false
	val checkImage = smallImage("images/check.png")
	val xImage = smallImage("images/x.png")
	var givingFeedback = false
	fun feedback() {
	  givingFeedback = true
	  if (correct) {
		stimulate(checkImage)
	  } else {
		stimulate(xImage)
	  }
	  sleep(1000)
	  removeAllStim()
	  givingFeedback = false
	}

	val nBackSpaceHandler = EventHandler<KeyEvent> {

	  if (it.code == KeyCode.SPACE && respondToSpace) {
		didPress = true
		println("didPress!")
		thread.interrupt()
	  }
	}
	ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, nBackSpaceHandler)

	runTrials {
	  logger.data["image"] = stimImage!!.name
	  logger.data["shouldPress"] = shouldPress
	  logger.data["didPress"] = didPress
	  correct = (shouldPress == didPress)
	  logger.data["correct"] = correct
	  println("correct=${correct}")
	  if (logger.currentTrial > n.get()) feedback()
	  else removeAllStim()
	  while (givingFeedback) {
		sleep(10)
	  }
	}
	ExperimentStage.removeEventFilter(KeyEvent.KEY_PRESSED, nBackSpaceHandler)
	endExperiment()
  }

  override var instructions = "For this task, images will be displayed in a random sequence.\n" +
	  "Keep in memory the most recent 2 images.\n" +
	  "When the currently displayed image is the same as the one displayed 2 times ago, press the space bar.\n" +
	  "If it is different, do nothing.\n" +
	  "\n" +
	  "Press Space to begin.\n"

  var n = SimpleIntegerProperty(2).apply {
	paramProps += "n" to this
  }
  var matchChance = SimpleDoubleProperty(0.5).apply {
	paramProps += "matchChance" to this
  }
}


class ScreenFlicker1: Task() {
  init {
	stimDur = 0L // ms
	isi = 0L // ms
  }

  override fun stimulus() = object: Stimulus {
	override fun generate() = Unit

	override fun show() {
	  if (currentSoundClip != lastSoundClip) {
		stimulate(currentSoundClip!!)
	  }
	  runLater {
		stimulate(currentStim!!)
	  }

	}

	override fun hide() = Unit
  }

  override val logHeaders = arrayOf(
	"phase"
  )

  /*val FREQ_MIN = 1
  val FREQ_MAX = 40
  val SEIZURE_RANGE = 15..25*/

  val phases = mutableListOf<Pair<Phase, Int>>().apply {
	//        for (p in FREQ_MIN..FREQ_MAX) {
	////            if (p !in SEIZURE_RANGE) {
	////            add(LookAtWallPhase() to 10)
	//            add(SolidScreenPhase() to 10)
	//            add(BlinkingScreenPhase(p) to 10)
	////            }
	//        }
	add(LookAtWallPhase() to 300)
	add(BlinkingScreenPhase(12) to 300)
  }
  //            LookAtWallPhase() to 10,
  ////            SolidScreenPhase() to 5,
  ////            BlinkingScreenPhase(2) to 5,
  ////            BlinkingScreenPhase(4) to 5,
  ////            BlinkingScreenPhase(8) to 5,
  //            BlinkingScreenPhase(12) to 10
  ////            BlinkingScreenPhase(16) to 5,
  ////            BlinkingScreenPhase(32) to 5,
  ////            BlinkingScreenPhase(64) to 5,
  ////            BlinkingScreenPhase(128) to 5,
  ////            BlinkingScreenPhase(256) to 5
  //    )

  init {
	trials = phases.size
  }

  var lastSoundClip: MediaPlayer? = null
  override fun run() {
	prepareExperiment()

	currentSoundClip = phases.first().first.clip()
	runLater {
	  currentStim = phases.first().first.stim()
	}

	runTrials {
	  val phaseN = logger.currentTrial - 1
	  val phase = phases[phaseN].first
	  val t = phases[phaseN].second
	  when (phase) {
		is LookAtWallPhase  -> logger.log("phase" to "wall")
		is SolidScreenPhase -> logger.log("phase" to "screen")
		else                -> {
		  phase as BlinkingScreenPhase
		  logger.log("phase" to phase.freqHz.toString() + "Hz")
		}
	  }
	  sleep(t*1000L)
	  removeAllStim()
	  stimulate(dot)
	  if (phaseN != phases.size - 1) {
		lastSoundClip = currentSoundClip
		currentSoundClip = phases[phaseN + 1].first.clip()
		runLater {
		  currentStim = phases[phaseN + 1].first.stim()
		}
	  } else {
		stimulate(thankYouClip)
	  }
	}
	endExperiment()
  }

  var currentClip: MediaPlayer? = null

  abstract class Phase {
	abstract fun stim(): Node
	abstract fun clip(): MediaPlayer
  }

  class LookAtWallPhase: Phase() {
	override fun stim() = Label("please look at the wall")
	override fun clip() = lookAtWallClip
  }

  abstract class LookAtScreenPhase: Phase()
  class SolidScreenPhase: LookAtScreenPhase() {
	override fun stim() = Rectangle(0.0, 0.0)
	override fun clip() = lookAtScreenClip
  }

  class BlinkingScreenPhase(val freqHz: Int): LookAtScreenPhase() {
	override fun clip() = lookAtScreenClip
	override fun stim() = StackPane(Rectangle(0.0, 0.0, Color.BLACK).apply {
	  runLater {
		widthProperty().bind(ExperimentRoot.widthProperty())
		heightProperty().bind(ExperimentRoot.heightProperty())
	  }

	}, Rectangle(0.0, 0.0).apply {
	  //            widthProperty().bind(ExperimentRoot.widthProperty() / 2)
	  //            heightProperty().bind(ExperimentRoot.heightProperty() / 2)
	  runLater {
		widthProperty().bind(ExperimentRoot.widthProperty())
		heightProperty().bind(ExperimentRoot.heightProperty())
	  }


	  val timeline = Timeline(
		KeyFrame(Duration.ZERO, EventHandler {
		  //                        fill = Color.DARKGREEN
		  fill = Color.BLACK
		}),
		KeyFrame(Duration.seconds(1/freqHz.toDouble()/2), EventHandler {
		  //                        fill = Color.LIGHTBLUE
		  fill = Color.WHITE
		}),
		KeyFrame(Duration.seconds(1/freqHz.toDouble()), EventHandler {
		  //                        fill = Color.DARKGREEN
		  fill = Color.BLACK
		})
	  )

	  timeline.cycleCount = Timeline.INDEFINITE
	  timeline.play()

	  //            Color.BLACK
	}, dot.apply {
	  toFront()
	})
  }

  var currentSoundClip: MediaPlayer? = null
  var currentStim: Node? = null

  override var instructions =
	"For the next 10 minutes, you will look at different stimuli. Try to move as little as possible and follow the verbal instructions. You will be informed when the test is complete.\n" +
		"Press space to continue."
}

val lookAtWall = "look_at_wall.m4a"
val lookAtScreen = "look_at_screen.m4a"
val thankYou = "thank you.m4a"

val lookAtWallClip by lazy {
  val sound = Media(File("sounds" + File.separator + lookAtWall).toURI().toString())
  MediaPlayer(sound)
}
val lookAtScreenClip by lazy {
  val sound = Media(File("sounds" + File.separator + lookAtScreen).toURI().toString())
  MediaPlayer(sound)
}
val thankYouClip by lazy {
  val sound = Media(File("sounds" + File.separator + thankYou).toURI().toString())
  MediaPlayer(sound)
}


val dot = Circle(2.5, Color.BLUE).apply {
  StackPane.setAlignment(this, Pos.CENTER)
}

class ScreenFlicker2: Task() {
  init {
	stimDur = 0L // ms
	isi = 0L // ms
  }

  override fun stimulus() = object: Stimulus {
	override fun generate() {
	}

	override fun show() {
	  if (currentSoundClip != lastSoundClip) {
		//                stimulate(currentSoundClip!!)
	  }
	  runLater {
		stimulate(currentStim!!)
	  }
	}

	override fun hide() {
	}
  }

  override val logHeaders = arrayOf(
	"phase"
  )

  //    val FREQ_MIN = 1
  val FREQ_MIN = 10
  val FREQ_MAX = 20

  //    val FREQ_MAX = 40
  val SEIZURE_RANGE = 15..25

  val phases = mutableListOf<Pair<Phase, Int>>().apply {
	add(SolidScreenPhase() to 10)
	add(BlinkingScreenPhase(15) to 10)
	add(SolidScreenPhase() to 10)
	add(BlinkingScreenPhase(30) to 10)
  }

  //    val phases = mutableListOf<Pair<Phase, Int>>().apply {
  //        for (p in FREQ_MIN..FREQ_MAX) {
  //////            if (p !in SEIZURE_RANGE) {
  ////            add(LookAtWallPhase() to 10)
  //            add(SolidScreenPhase() to 10)
  //            add(BlinkingScreenPhase(p) to 10)
  //////            }
  //        }
  //
  //
  //
  //
  //
  ////        add(LookAtWallPhase() to 300)
  ////        add(BlinkingScreenPhase(12) to 300)
  //    }

  //            LookAtWallPhase() to 10,
  ////            SolidScreenPhase() to 5,
  ////            BlinkingScreenPhase(2) to 5,
  ////            BlinkingScreenPhase(4) to 5,
  ////            BlinkingScreenPhase(8) to 5,
  //            BlinkingScreenPhase(12) to 10
  ////            BlinkingScreenPhase(16) to 5,
  ////            BlinkingScreenPhase(32) to 5,
  ////            BlinkingScreenPhase(64) to 5,
  ////            BlinkingScreenPhase(128) to 5,
  ////            BlinkingScreenPhase(256) to 5
  //    )

  init {
	trials = phases.size
  }

  var lastSoundClip: MediaPlayer? = null
  override fun run() {
	prepareExperiment()

	ExperimentRoot.background = Background(BackgroundFill(Color.BLACK, null, null))

	currentSoundClip = phases.first().first.clip()
	runLater {
	  currentStim = phases.first().first.stim()
	}

	runTrials {
	  val phaseN = logger.currentTrial - 1
	  val phase = phases[phaseN].first
	  val t = phases[phaseN].second
	  when (phase) {
		is LookAtWallPhase  -> logger.log("phase" to "wall")
		is SolidScreenPhase -> logger.log("phase" to "screen")
		else                -> {
		  phase as BlinkingScreenPhase
		  logger.log("phase" to phase.freqHz.toString() + "Hz")
		}
	  }
	  sleep(t*1000L)
	  removeAllStim()
	  stimulate(dot)
	  if (phaseN != phases.size - 1) {
		lastSoundClip = currentSoundClip
		currentSoundClip = phases[phaseN + 1].first.clip()
		runLater {
		  currentStim = phases[phaseN + 1].first.stim()
		}
	  } else {
		stimulate(thankYouClip)
	  }
	}
	endExperiment()
  }

  var currentClip: MediaPlayer? = null

  abstract class Phase {
	abstract fun stim(): Node
	abstract fun clip(): MediaPlayer
  }

  class LookAtWallPhase: Phase() {
	override fun stim() = Label("please look at the wall")
	override fun clip() = lookAtWallClip
  }

  abstract class LookAtScreenPhase: Phase()
  class SolidScreenPhase: LookAtScreenPhase() {
	override fun stim() = Rectangle(0.0, 0.0)
	override fun clip() = lookAtScreenClip
  }

  class BlinkingScreenPhase(val freqHz: Int): LookAtScreenPhase() {
	override fun clip() = lookAtScreenClip
	val rect1 = Rectangle(0.0, 0.0, Color.BLACK).apply {
	  //            runLater{
	  widthProperty().bind(ExperimentRoot.widthProperty())
	  heightProperty().bind(ExperimentRoot.heightProperty())
	  //            }

	}
	val rect2 = Rectangle(0.0, 0.0).apply {
	  //            runLater{
	  widthProperty().bind(ExperimentRoot.widthProperty()/2)
	  heightProperty().bind(ExperimentRoot.heightProperty()/2)
	}
	var lastTimeLine: Timeline? = null
	override fun stim() = StackPane(rect1, rect2.apply {

	  lastTimeLine?.stop()
	  lastTimeLine = Timeline(
		KeyFrame(Duration.ZERO, EventHandler {
		  fill = Color.DARKGREEN
		  //                        fill = Color.BLACK
		}),
		KeyFrame(Duration.seconds(1/freqHz.toDouble()/2), EventHandler {
		  fill = Color.LIGHTBLUE
		  //                        fill = Color.WHITE
		}),
		KeyFrame(Duration.seconds(1/freqHz.toDouble()), EventHandler {
		  fill = Color.DARKGREEN
		  //                        fill = Color.BLACK
		})
	  )

	  lastTimeLine?.cycleCount = Timeline.INDEFINITE
	  lastTimeLine?.play()

	  //            Color.BLACK
	}, dot.apply {
	  toFront()
	})
  }

  var currentSoundClip: MediaPlayer? = null
  var currentStim: Node? = null

  override var instructions =
	"For the next 10 minutes, you will look at different stimuli. Try to move as little as possible and follow the verbal instructions. You will be informed when the test is complete.\n" +
		"During the blinking phase, please focus on the dot on screen.\n" +
		"Press space to continue."
}


class ScreenFlicker3: Task() {

  val showFreqProp = SimpleBooleanProperty(false)
  var showFreq by showFreqProp

  init {
	stimDur = 0L // ms
	isi = 0L // ms
	paramProps.add(
	  "showFreqs" to showFreqProp
	)
  }

  override fun stimulus() = object: Stimulus {
	override fun generate() {
	}

	override fun show() {
	  if (currentSoundClip != lastSoundClip) {
		//                stimulate(currentSoundClip!!)
	  }
	  runLater {
		updateFreqLabel(currentFreq)
		stimulate(currentStim!!)
	  }
	}

	override fun hide() {
	}
  }

  override val logHeaders = arrayOf(
	"phase"
  )

  //    val FREQ_MIN = 1
  val FREQ_MIN = 10
  val FREQ_MAX = 20

  //    val FREQ_MAX = 40
  val SEIZURE_RANGE = 15..25


  //    8.57... so it works well with screen refresh rate
  val eigthFreq = 60.0/7.0

  val freqs = arrayOf<Number>(
	eigthFreq, 30, 10, 28, 12, 26, 14, 24, 15, 22, 16, 20, 18, 8
	//            1 // ,2,3
  )

  val phases = mutableListOf<Pair<Phase, Int>>().apply {
	freqs.forEach {
	  add(SolidScreenPhase() to 5)
	  add(BlinkingScreenPhase(it, 5, showFreq) to 5)
	}
	freqs.forEach {
	  add(SolidScreenPhase() to 5)
	  add(BlinkingScreenPhase(it, 5, showFreq) to 5)
	}
  }

  //    val phases = mutableListOf<Pair<Phase, Int>>().apply {
  //        for (p in FREQ_MIN..FREQ_MAX) {
  //////            if (p !in SEIZURE_RANGE) {
  ////            add(LookAtWallPhase() to 10)
  //            add(SolidScreenPhase() to 10)
  //            add(BlinkingScreenPhase(p) to 10)
  //////            }
  //        }
  //

  init {
	trials = phases.size
  }

  var lastSoundClip: MediaPlayer? = null
  override fun run() {
	prepareExperiment()

	ExperimentRoot.background = Background(BackgroundFill(Color.BLACK, null, null))

	currentSoundClip = phases.first().first.clip()
	runLater {
	  val p = phases.first().first
	  currentStim = p.stim()
	  currentFreq = if (p is BlinkingScreenPhase) {
		p.freqHz.toDouble()
	  } else 0.0

	}

	runTrials {
	  val phaseN = logger.currentTrial - 1
	  val phase = phases[phaseN].first
	  val t = phases[phaseN].second
	  when (phase) {
		is LookAtWallPhase  -> logger.log("phase" to "wall")
		is SolidScreenPhase -> logger.log("phase" to "screen")
		else                -> {
		  phase as BlinkingScreenPhase
		  logger.log("phase" to phase.freqHz.toString() + "Hz")
		}
	  }
	  sleep(t*1000L)
	  removeAllStim()
	  stimulate(dot)
	  if (phaseN != phases.size - 1) {
		lastSoundClip = currentSoundClip
		currentSoundClip = phases[phaseN + 1].first.clip()
		runLater {
		  val p = phases[phaseN + 1].first
		  currentStim = p.stim()
		  currentFreq = if (p is BlinkingScreenPhase) {
			p.freqHz.toDouble()
		  } else 0.0

		}
	  } else {
		stimulate(thankYouClip)
	  }
	}
	endExperiment()
  }

  var currentClip: MediaPlayer? = null

  abstract class Phase {
	abstract fun stim(): Node
	abstract fun clip(): MediaPlayer
  }

  class LookAtWallPhase: Phase() {
	override fun stim() = Label("please look at the wall")
	override fun clip() = lookAtWallClip
  }

  abstract class LookAtScreenPhase: Phase()
  class SolidScreenPhase: LookAtScreenPhase() {
	override fun stim() = Rectangle(0.0, 0.0)
	override fun clip() = lookAtScreenClip
  }

  class BlinkingScreenPhase(freqHz: Number, durSec: Number, val showFreq: Boolean): LookAtScreenPhase() {

	val freqHz = freqHz.toDouble()
	val durSec = durSec.toDouble()

	override fun clip() = lookAtScreenClip
	val rect1 = Rectangle(0.0, 0.0, Color.BLACK).apply {
	  //            runLater{
	  widthProperty().bind(ExperimentRoot.widthProperty())
	  heightProperty().bind(ExperimentRoot.heightProperty())
	  //            }

	}
	val rect2 = Rectangle(0.0, 0.0).apply {
	  //            runLater{
	  widthProperty().bind(ExperimentRoot.widthProperty()/2)
	  heightProperty().bind(ExperimentRoot.heightProperty()/2)
	}
	var lastTimeLine: Timeline? = null
	override fun stim() = StackPane(rect1, StackPane(
	  rect2.apply {

		lastTimeLine?.stop()
		lastTimeLine = Timeline()
		val cycles = ceil(freqHz*durSec).toInt()

		val cycleLifespan = 1/freqHz
		for (c in 1..cycles) {

		  val start = (c - 1)*cycleLifespan
		  //                val end = start + cycleLifespan
		  val mid = start + (cycleLifespan/2)

		  lastTimeLine!!.keyFrames.addAll(
			KeyFrame(Duration.seconds(start), EventHandler {
			  fill = Color.DARKGREEN
			  //                            StackPane.setAlignment(this, Pos.CENTER_RIGHT)
			  //                        fill = Color.BLACK
			}),
			KeyFrame(Duration.seconds(mid), EventHandler {
			  fill = Color.LIGHTBLUE
			  //                            StackPane.setAlignment(this, Pos.CENTER_LEFT)
			  //                        fill = Color.WHITE

			})
			//                        KeyFrame(Duration.seconds(end.toDouble()), EventHandler {
			//                            fill = Color.DARKGREEN
			//                            StackPane.setAlignment(this, Pos.CENTER_RIGHT)
			////                        fill = Color.BLACK
			//                        })
		  )
		}


		//            lastTimeLine?.cycleCount = Timeline.INDEFINITE
		lastTimeLine?.play()

		//            Color.BLACK
	  }, dot
	).apply {
	  prefWidthProperty().bind(ExperimentRoot.widthProperty()/2)
	  prefHeightProperty().bind(ExperimentRoot.heightProperty()/2)
	  if (showFreq) {
		add(freqLabel.apply {
		  translateY = 600.0
		})
	  }
	})
  }


  var currentSoundClip: MediaPlayer? = null
  var currentStim: Node? = null
  var currentFreq = 0.0

  override var instructions =
	"For the next 10 minutes, you will look at different stimuli. Try to move as little as possible and follow the verbal instructions. You will be informed when the test is complete.\n" +
		"During the blinking phase, please focus on the dot on screen.\n" +
		"Press space to continue."
}


class SpeedReading: Task() {
  var nextLabel: Label? = null
  override fun stimulus() = object: Stimulus {

	fun askQuestion() {
	  val thread = Thread.currentThread()
	  currentQuestion = currentBlock.questions[questionIndex]
	  val stim = VBox().apply {
		add(Label(currentQuestion.question))
		val answerButtons = ToggleGroup()
		val answers = currentQuestion.wrongAnswers.toMutableList().apply {
		  add(currentQuestion.rightAnswer)
		  shuffle()
		}
		answers.forEach {
		  add(RadioButton(it).apply {
			answerButtons.toggles.add(this)
		  })
		}
		add(Button("Continue").apply {

		  setOnAction {
			if (answerButtons.selectedToggle != null) {
			  val correct = ((answerButtons.selectedToggle as RadioButton).text == currentQuestion.rightAnswer)
			  stimDur += if (correct) {
				-200
			  } else {
				200
			  }

			  thread.interrupt()
			}

		  }

		})
	  }

	  runLater {
		stimulate(stim)
	  }
	  try {
		while (true) {
		  sleep(100)
		}
	  } catch (e: InterruptedException) {

	  }
	}

	override fun generate() {
	  if (i == words().size) {
		while (questionIndex < currentBlock.questions.size) {
		  askQuestion()
		  questionIndex++
		}
		blockIndex++
		if (blockIndex >= adaptiveReading.blocks.size) {
		  endExperiment()
		} else {
		  currentBlock = adaptiveReading.blocks[blockIndex]
		  i = 0
		}
	  }
	  nextLabel = Label(words()[i++]).apply {
		font = Font(40.0)
	  }
	}

	override fun show() {
	  stimulate(nextLabel!!)
	}

	override fun hide() {
	  removeAllStim()
	}
  }

  override val logHeaders = arrayOf(
	"word"
  )

  override fun run() {
	prepareExperiment()
	runTrials {
	  logger.data["word"] = nextLabel!!.text
	}
	endExperiment()
  }

  override var instructions =
	"For this task, words from a text reading will be displayed one at a time in sequence.\n" +
		"All you have to do is keep paying attention and read each word" +
		"Press Space to begin.\n"

  //    var reading by param("DefaultSpeedReading.txt")
  var adaptive = SimpleBooleanProperty(true).apply {
	paramProps += "adaptive" to this
  }
  var reading = SimpleStringProperty("AdaptiveReadingExample.json").apply {
	paramProps += "reading" to this
  }

  @Suppress("UNREACHABLE_CODE")
  val adaptiveReading by lazy {
	err("""Klaxon().converter(AdaptiveReadingConverter).parse<AdaptiveReading>(File("readings" + File.separator + reading).readText())!!""")
	AdaptiveReading(arrayOf())
  }
  var blockIndex = 0
  var questionIndex = 0
  lateinit var currentBlock: AdaptiveReadingTextBlock
  lateinit var currentQuestion: TextBlockQuestion
  fun text() =
	if (reading.get().contains(".txt")) {
	  adaptive.set(false)
	  File("readings" + File.separator + reading).readText()
	} else {
	  currentBlock = adaptiveReading.blocks[blockIndex]
	  currentBlock.text
	}

  fun words() = text().split(" ")
  var i = 0
}

class AdaptiveReading(val blocks: Array<AdaptiveReadingTextBlock>): SimpleJson<AdaptiveReading>(typekey = null) {
  init {
	val readName = "ERR"
	err(
	  """
		  
		  
		  
	object AdaptiveReadingConverter : matt.json.klaxon.Converter {
  override fun canConvert(cls: Class<*>): Boolean {
	return (cls == AdaptiveReading::class.java)
  }

  override fun fromJson(jv: JsonValue): Any {
	var blocks = mutableListOf<AdaptiveReadingTextBlock>()
	println("HERE0")
	JsonReader(StringReader(jv.obj!!.toJsonString())).use { reader ->
	  println("BEGIN ARRAY 1")
	  reader.beginObject {
		reader.nextName()
		reader.beginArray {
		  var block: AdaptiveReadingTextBlock
		  val questions = mutableListOf<TextBlockQuestion>()
		  fun getQuestions() {
			println("BEGIN ARRAY 2")
			reader.beginArray {
			  while (reader.hasNext()) {
				var question: String = ""
				var wrongAnswers = mutableListOf<String>()
				var rightAnswer: String = ""
				println("BEGIN OBJ 1")
				reader.beginObject {

				  while (reader.hasNext()) {
					val readName = reader.nextName()
					when (readName) {
					  "question" -> question = reader.nextString()
					  "wrong answers" -> {
						println("BEGIN ARRAY 3")
						reader.beginArray {
						  while (reader.hasNext()) {
							wrongAnswers.add(reader.nextString())
						  }
						}
					  }
					  "right answer" -> rightAnswer = reader.nextString()
					  else -> throw RuntimeException("Unexpected name: $readName")
					}
				  }
				}
				questions.add(TextBlockQuestion(question, wrongAnswers.toTypedArray(), rightAnswer))
			  }
			}
		  }
		  while (reader.hasNext()) {
			var text: String = ""
			println("BEGIN OBJ 2")
			reader.beginObject {
			  while (reader.hasNext()) {
				val readName = reader.nextName()
				println("READNAME 1: $readName")
				when (readName) {
				  "text" -> text = reader.nextString()
				  "questions" -> getQuestions()
				  else -> throw RuntimeException("Unexpected name: $readName")
				}
			  }
			}
			block = AdaptiveReadingTextBlock(text, questions.toTypedArray())
			blocks.add(block)
			questions.clear()
		  }

		}
	  }
	}
	return AdaptiveReading(blocks.toTypedArray())
  }

  override fun toJson(value: Any): String {
	throw RuntimeException("no need for this")
  }
}	  
		  
		  
		""".trimIndent()
	)
  }
}

class AdaptiveReadingTextBlock(val text: String, val questions: Array<TextBlockQuestion>)
class TextBlockQuestion(val question: String, val wrongAnswers: Array<String>, val rightAnswer: String)


class SSVEPverify: Task() {
  init {
	stimDur = 0L // ms
	isi = 0L // ms
  }

  override fun stimulus() = object: Stimulus {
	override fun generate() {
	}

	override fun show() {
	  if (currentSoundClip != lastSoundClip) {
		//                stimulate(currentSoundClip!!)
	  }
	  runLater {
		updateFreqLabel(currentFreq!!)
		stimulate(currentStim!!)
	  }
	}

	override fun hide() {
	}
  }

  override val logHeaders = arrayOf(
	"phase"
  )

  //    val FREQ_MIN = 1
  val FREQ_MIN = 10
  val FREQ_MAX = 20

  //    val FREQ_MAX = 40
  val SEIZURE_RANGE = 15..25

  //    val phases = mutableListOf<Pair<Phase, Int>>().apply {
  //        add(SolidScreenPhase() to 3)
  //        add(BlinkingScreenPhase(15, 10) to 10)
  //        add(SolidScreenPhase() to 3)
  //        add(BlinkingScreenPhase(17, 10) to 10)
  //        add(SolidScreenPhase() to 3)
  //        add(BlinkingScreenPhase(30, 10) to 10)
  //        add(SolidScreenPhase() to 3)
  //        add(BlinkingScreenPhase(35, 10) to 10)
  //    }

  val phases = mutableListOf<Pair<Phase, Int>>().apply {
	add(SolidScreenPhase() to 3)
	add(BlinkingScreenPhase(20, 10) to 10)
	add(SolidScreenPhase() to 3)
	add(BlinkingScreenPhase(22, 10) to 10)
	add(SolidScreenPhase() to 3)
	add(BlinkingScreenPhase(25, 10) to 10)
	add(SolidScreenPhase() to 3)
	add(BlinkingScreenPhase(29, 10) to 10)
  }

  //    val phases = mutableListOf<Pair<Phase, Int>>().apply {
  //        for (p in FREQ_MIN..FREQ_MAX) {
  //////            if (p !in SEIZURE_RANGE) {
  ////            add(LookAtWallPhase() to 10)
  //            add(SolidScreenPhase() to 10)
  //            add(BlinkingScreenPhase(p) to 10)
  //////            }
  //        }
  //
  //
  //
  //
  //
  ////        add(LookAtWallPhase() to 300)
  ////        add(BlinkingScreenPhase(12) to 300)
  //    }

  //            LookAtWallPhase() to 10,
  ////            SolidScreenPhase() to 5,
  ////            BlinkingScreenPhase(2) to 5,
  ////            BlinkingScreenPhase(4) to 5,
  ////            BlinkingScreenPhase(8) to 5,
  //            BlinkingScreenPhase(12) to 10
  ////            BlinkingScreenPhase(16) to 5,
  ////            BlinkingScreenPhase(32) to 5,
  ////            BlinkingScreenPhase(64) to 5,
  ////            BlinkingScreenPhase(128) to 5,
  ////            BlinkingScreenPhase(256) to 5
  //    )

  init {
	trials = phases.size
  }

  var lastSoundClip: MediaPlayer? = null
  override fun run() {
	prepareExperiment()

	ExperimentRoot.background = Background(BackgroundFill(Color.BLACK, null, null))

	currentSoundClip = phases.first().first.clip()
	runLater {
	  val p = phases.first().first
	  currentStim = p.stim()
	  currentFreq = if (p is BlinkingScreenPhase) {
		p.freqHz.toDouble()
	  } else 0.0

	}

	runTrials {
	  val phaseN = logger.currentTrial - 1
	  val phase = phases[phaseN].first
	  val t = phases[phaseN].second
	  when (phase) {
		is LookAtWallPhase  -> logger.log("phase" to "wall")
		is SolidScreenPhase -> logger.log("phase" to "screen")
		else                -> {
		  phase as BlinkingScreenPhase
		  logger.log("phase" to phase.freqHz.toString() + "Hz")
		}
	  }
	  sleep(t*1000L)
	  removeAllStim()
	  stimulate(dot)
	  if (phaseN != phases.size - 1) {
		lastSoundClip = currentSoundClip
		currentSoundClip = phases[phaseN + 1].first.clip()
		runLater {
		  val p = phases[phaseN + 1].first
		  currentStim = p.stim()
		  currentFreq = if (p is BlinkingScreenPhase) {
			p.freqHz.toDouble()
		  } else 0.0

		}
	  } else {
		stimulate(thankYouClip)
	  }
	}
	endExperiment()
  }

  var currentClip: MediaPlayer? = null

  abstract class Phase {
	abstract fun stim(): Node
	abstract fun clip(): MediaPlayer
  }

  class LookAtWallPhase: Phase() {
	override fun stim() = Label("please look at the wall")
	override fun clip() = lookAtWallClip
  }

  abstract class LookAtScreenPhase: Phase()
  class SolidScreenPhase: LookAtScreenPhase() {
	override fun stim() = Rectangle(0.0, 0.0)
	override fun clip() = lookAtScreenClip
  }

  class BlinkingScreenPhase(freqHz: Number, durSec: Number): LookAtScreenPhase() {

	val freqHz = freqHz.toDouble()
	val durSec = durSec.toDouble()

	override fun clip() = lookAtScreenClip
	val rect1 = Rectangle(0.0, 0.0, Color.BLACK).apply {
	  //            runLater{
	  widthProperty().bind(ExperimentRoot.widthProperty())
	  heightProperty().bind(ExperimentRoot.heightProperty())
	  //            }

	}
	val rect2 = Rectangle(0.0, 0.0).apply {
	  //            runLater{
	  widthProperty().bind(ExperimentRoot.widthProperty()/2)
	  heightProperty().bind(ExperimentRoot.heightProperty()/2)
	}
	var lastTimeLine: Timeline? = null
	override fun stim() = StackPane(rect1, StackPane(
	  rect2.apply {

		lastTimeLine?.stop()
		lastTimeLine = Timeline()
		val cycles = ceil(freqHz*durSec).toInt()

		val cycleLifespan = 1/freqHz
		for (c in 1..cycles) {

		  val start = (c - 1)*cycleLifespan
		  //                val end = start + cycleLifespan
		  val mid = start + (cycleLifespan/2)

		  lastTimeLine!!.keyFrames.addAll(
			KeyFrame(Duration.seconds(start), EventHandler {
			  fill = Color.DARKGREEN
			  //                            StackPane.setAlignment(this, Pos.CENTER_RIGHT)
			  //                        fill = Color.BLACK
			}),
			KeyFrame(Duration.seconds(mid), EventHandler {
			  fill = Color.LIGHTBLUE
			  //                            StackPane.setAlignment(this, Pos.CENTER_LEFT)
			  //                        fill = Color.WHITE

			})
			//                        KeyFrame(Duration.seconds(end.toDouble()), EventHandler {
			//                            fill = Color.DARKGREEN
			//                            StackPane.setAlignment(this, Pos.CENTER_RIGHT)
			////                        fill = Color.BLACK
			//                        })
		  )
		}


		//            lastTimeLine?.cycleCount = Timeline.INDEFINITE
		lastTimeLine?.play()

		//            Color.BLACK
	  }, freqLabel
	  //                dot.apply {
	  //            toFront()
	  //        }
	).apply {
	  prefWidthProperty().bind(ExperimentRoot.widthProperty()/2)
	  prefHeightProperty().bind(ExperimentRoot.heightProperty()/2)
	})
  }

  var currentSoundClip: MediaPlayer? = null
  var currentStim: Node? = null
  var currentFreq = 0.0

  override var instructions =
	"For the next 10 minutes, you will look at different stimuli. Try to move as little as possible and follow the verbal instructions. You will be informed when the test is complete.\n" +
		"During the blinking phase, please focus on the dot on screen.\n" +
		"Press space to continue."
}

val freqLabel = Label("").apply {
  textFill = Color.WHITE
  font = Font(40.0)
}

fun updateFreqLabel(f: Double) {
  freqLabel.text = "${f}Hz"
}


class Stroop: Task() {

  fun Color.colorName() = if (this == Color.RED) "RED" else if (this == Color.BLUE) "BLUE" else "???"
  val colors = arrayOf(Color.RED, Color.BLUE)
  lateinit var word: String
  lateinit var color: Color
  lateinit var left: Color
  lateinit var right: Color
  lateinit var stim: Label
  override fun stimulus() = object: Stimulus {
	override fun generate() {
	  word = colors[nextInt(2)].colorName()
	  color = colors[nextInt(2)]
	  left = colors[nextInt(2)]
	  right = colors.first { it != left }
	  stim = Label(word).apply {
		font = Font(40.0)
		textFill = color
	  }
	}

	override fun show() {
	  stimulate(stim)
	  stimulate(
		Label(
		  left.colorName()
		).apply {
		  font = Font(30.0)
		  textFill = left
		  translateX = -100.0
		  translateY = 100.0
		})
	  stimulate(
		Label(right.colorName()).apply {
		  font = Font(30.0)
		  textFill = right
		  translateX = 100.0
		  translateY = 100.0
		})
	}

	override fun hide() {
	}
  }

  override val logHeaders = arrayOf(
	"word",
	"color",
	"left",
	"right",
	"response",
	"correct"
  )

  override fun run() {
	prepareExperiment()

	runTrials {
	  logger.data["word"] = word
	  logger.data["color"] = color
	  logger.data["left"] = left
	  logger.data["right"] = right
	  var canFinishTrial = false
	  var response: Boolean? = null
	  val digitResponseHandler = EventHandler<KeyEvent> {
		thread {
		  response = when (it.code) {
			KeyCode.LEFT  -> false
			KeyCode.RIGHT -> true
			else          -> null
		  }
		  if (response != null) {
			canFinishTrial = true
		  }
		}
	  }
	  ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, digitResponseHandler)
	  while (!canFinishTrial) {
		sleep(10)
	  }
	  logger.data["response"] = if (response!!) "RIGHT" else "LEFT"
	  val choice = if (response!!) right else left
	  val correct = (choice == color)
	  logger.data["correct"] = correct
	  val checkImage = defaultImage("images/check.png")
	  val xImage = defaultImage("images/x.png")
	  val feedback = if (correct) checkImage else xImage
	  stimulate(feedback)
	  sleep(1000)
	  removeAllStim()
	  //            Platform.runLater {
	  //                ExperimentRoot.children.remove(feedback)
	  //            }
	}
	endExperiment()
  }

  override var instructions =
	"A color word will appear, but the color of the text may not be the same as the word itself. Press the left or right arrow key as quickly as possible to match the color of the text of the center word to one of the colors on the left or right. Pay no attention to the text in the central word."
}


class Video: Task() {
  override fun stimulus() = object: Stimulus {

	override fun generate() {
	}

	override fun show() {
	  if (!ExperimentRoot.children.contains(vid)) {
		stimulate(vid)
	  }
	}

	override fun hide() {
	}
  }

  override val logHeaders = arrayOf(
	"video"
  )

  override fun run() {
	prepareExperiment()
	vid.mediaPlayer.onEndOfMedia = Runnable {
	  removeAllStim()
	  endExperiment()
	}
	runTrials {
	  logger.data["video"] = video
	}
  }

  override var instructions = "For this task all you have to do is watch a video."


  var video = SimpleStringProperty("SampleVideo_1280x720_1mb.mp4").apply {
	paramProps += "video" to this
  }
  val vid by lazy {
	val actualFile = File("videos" + File.separator + video)
	//        val emptyfile = File("videos" + File.separator + "blank.mp4")
	val media = Media(actualFile.toURI().toString())
	//        copyData(media, actualFile)
	val mediaPlayer: MediaPlayer? = MediaPlayer(media)
	mediaPlayer!!.isAutoPlay = true
	MediaView(mediaPlayer)
  }
}
//
//private fun copyData(media: Media, f: File) {
//    val locatorField = media.javaClass.getDeclaredField("jfxLocator")
//    // Inside block credits:
//    // http://stackoverflow.com/questions/3301635/change-private-static-final-field-using-java-reflection
//    run {
//        val modifiersField = Field::class.java.getDeclaredField("modifiers")
//        modifiersField.isAccessible = true
//        modifiersField.setInt(locatorField, locatorField.modifiers and Modifier.FINAL.inv())
//        locatorField.isAccessible = true
//    }
//    val customLocator = CustomLocator(f.toURI())
//    customLocator.init()
//    customLocator.hack("video/mp4", 100000, f.toURI())
//    locatorField.set(media, customLocator)
//}
//
//internal class CustomLocator(uri: URI) : Locator(uri) {
//    fun hack(type: String, length: Long, uri: URI) {
//        contentType = type
//        contentLength = length
//        this.uri = uri
//        cacheMedia()
//    }
//}


class VPVT: Task() {
  init {
	var stimDur = 0L // ms
	var isi = 1000L // ms
  }

  val stim = Circle(100.0, Color.GREEN)
  var canFinishTrial = false
  var respondToSpace = false
  val spaceResponseHandler = EventHandler<KeyEvent> {
	if (respondToSpace) {
	  thread {
		when (it.code) {
		  KeyCode.SPACE -> {
			logger.data["responseTime"] = currentTimeMillis()
			canFinishTrial = true
		  }
		  else          -> {
		  }
		}
	  }
	}
  }

  override fun stimulus() = object: Stimulus {
	override fun generate() {
	}

	override fun show() {
	  stimulate(stim)
	  canFinishTrial = false
	  respondToSpace = true
	}

	override fun hide() {

	}
  }

  override val logHeaders = arrayOf(
	"responseTime"
  )

  override fun run() {
	prepareExperiment()

	runTrials {
	  ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, spaceResponseHandler)
	  while (!canFinishTrial) {
		sleep(1)
	  }
	  removeAllStim()
	  respondToSpace = false
	}
	endExperiment()
  }

  override var instructions =
	"A green circle will appear at a set interval. As soon as it appears each time, press the space bar."
}


fun instructionsLabel(text: String) = Label(text).apply {
  isWrapText = true
  textAlignment = TextAlignment.CENTER
  font = Font(40.0)
}

fun instructionsText(text: String) = Text(text).apply {
  textAlignment = TextAlignment.CENTER
  font = Font(40.0)
}

fun Task.prepareExperiment(i: String? = null) {
  val instructionsLabel = instructionsLabel(i ?: instructions)
  runLater {
	ExperimentRoot.apply {
	  children.clear()
	  children.add(instructionsLabel)
	}
	ExperimentStage.show()
	ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, spaceHandler)
  }
  sem.drainPermits()
  logger.currentTask = this
  logger.currentSubject = Subject(subject)
  logger.currentSubjectIteration = (subjectIteration)
  println("subj itr = $subjectIteration")
  println("current subj itr = ${logger.currentSubjectIteration}")
  logger.currentTrial = 0
  logger.resetFile()
  logger.writeHeaders()
  blockUntilUserHitsSpace()
  runLater {
	ExperimentRoot.apply {
	  children.remove(instructionsLabel)
	}
  }
}

fun Task.stimulate(node: Node) {
  runLater {
	ExperimentRoot.add(node)
  }
}

fun Task.stimulate(mp: MediaPlayer) {
  //    mp.seek()
  mp.stop()
  mp.play()
}

fun removeAllStim() {
  Platform.runLater {
	ExperimentRoot.clear()
  }
}

fun Task.runTrials(block: ()->Unit) {

  ExperimentScene.cursor = Cursor.NONE

  val startTime = Instant.now().epochSecond
  for (t in 1..trials) {
	if (timer > 0) {
	  if (Instant.now().epochSecond - startTime > timer) {
		break
	  }
	}
	logger.currentTrial = t
	//        logger.startTrial()
	stimulus().apply {
	  generate()
	}
	if (cancelTrials) {
	  break
	}
	if (ended) {
	  resetTaskConfigPane(this::class)
	  break
	}
	stimulus().apply {
	  (this@runTrials as? NBack)?.thread = Thread.currentThread()
	  show()
	  try {
		sleep(stimDur)
	  } catch (e: InterruptedException) {
		println("thread interrupt")
	  }
	  hide()
	}
	block()
	logger.endTrial((t == trials))
	sleep(isi)
  }

  ExperimentScene.cursor = Cursor.DEFAULT
}

fun blockUntilUserHitsSpace() {
  sem.acquire()
}

fun Task.endExperiment() {

  ended = true
  runLater {
	ExperimentStage.removeEventFilter(KeyEvent.KEY_PRESSED, spaceHandler)
	ExperimentStage.hide()
  }
}

private val sem = Semaphore(0)
val spaceHandler = EventHandler<KeyEvent> {
  if (it.code == KeyCode.SPACE) {
	if (sem.availablePermits() == 0) sem.release()
  }
}


object ExperimentStage: Stage() {
  init {
	scene = ExperimentScene
	isFullScreen = true
  }
}

fun experimentKeyFilter(h: (KeyEvent)->Unit) {
  ExperimentStage.addEventFilter(KeyEvent.KEY_PRESSED, h)
}

object ExperimentScene: Scene(ExperimentRoot)

object ExperimentRoot: StackPane() {
  init {
	alignment = Pos.CENTER
  }
}


fun defaultImage(imageFile: File) = ImageView(imageFile.toURI().toURL().toString()).apply {
  isPreserveRatio = true
  //    prefWidth(150.0)
  fitWidth = 150.0
  fitHeight = 150.0

}

fun smallImage(imageFile: File) = ImageView(imageFile.toURI().toURL().toString()).apply {
  isPreserveRatio = true
  //    prefWidth(150.0)
  fitWidth = 30.0
  fitHeight = 30.0
  translateX = 150.0

}

fun defaultImage(imageFile: String) = defaultImage(File(imageFile))
fun smallImage(imageFile: String) = smallImage(File(imageFile))


object TaskConfigPane: VBox()


fun resetTaskConfigPane(taskClass: KClass<out Task>) {
  //        taskConfig.clear()
  val theTask = taskClass.constructors.first().call(
	// taskConfig
  )
  TaskConfigPane.apply {
	children.clear()
	theTask.paramProps.forEach { prop ->
	  children.add(Label(prop.first, tasksParamControl(prop.second)).apply {
		contentDisplay = ContentDisplay.RIGHT
	  })
	  Unit
	}



	children.addAll(

	  Button("Run").apply {
		setOnAction {
		  thread {
			println("running task whose subj itr is ${theTask.subjectIteration}")
			theTask.run()
		  }
		}
	  }

	)
  }
}

object Presenter {
  val boolList = FXCollections.observableArrayList(true, false)
  fun tasksParamControl(taskParam: Property<*>): Node {
	return when (taskParam) {


	  is IntegerProperty -> TextField(taskParam.get().toString()).apply {
		try {
		  textProperty().addListener { _, _, n ->
			try {
			  taskParam.set(n.toInt())
			} catch (e: NumberFormatException) {
			  taskParam.set(0)
			}
		  }
		} catch (e: NumberFormatException) {
		  taskParam.set(0)
		}
	  }

	  is DoubleProperty  -> TextField(taskParam.get().toString()).apply {
		try {
		  taskParam.set(text.toDouble())
		  textProperty().addListener { _, _, n ->
			try {

			  taskParam.set(n.toDouble())
			} catch (e: NumberFormatException) {
			  taskParam.set(0.0)
			}
		  }
		} catch (e: NumberFormatException) {
		  taskParam.set(0.0)
		}
	  }

	  is LongProperty    -> TextField(taskParam.get().toString()).apply {
		try {
		  taskParam.set(text.toLong())
		  textProperty().addListener { _, _, n ->
			try {

			  taskParam.set(n.toLong())
			} catch (e: NumberFormatException) {
			  taskParam.set(0)
			  textProperty().addListener { _, _, _ ->
				taskParam.set(0)
			  }
			}
		  }
		} catch (e: NumberFormatException) {
		  taskParam.set(0)
		  textProperty().addListener { _, _, n ->
			taskParam.set(0)
		  }
		}
	  }

	  is BooleanProperty -> ChoiceBox<Boolean>(boolList).apply {
		selectionModel.select(boolList.first { it == taskParam.get() })
		//                taskParam._value = true as T
		selectionModel.selectedItemProperty().addListener { _, _, n ->
		  taskParam.set(n)
		}
	  }

	  is StringProperty  -> TextField(taskParam.get()).apply {
		taskParam.set(text)
		textProperty().addListener { _, _, n ->
		  taskParam.set(n)
		  println("string _value change to ${n}!!")
		}
	  }
	  else               -> Label("??? ${taskParam::class.qualifiedName} ???")
	}
  }
}


fun oldPsyktMain(args: Array<String>) {
  //    println("args=")
  //    args.forEach {
  //        println("\t${it}")
  //    }
  //    _args = args
  //    if (args.isEmpty()) {
  //        Application.launch(PsyktApp::class.java, *args)
  //    } else {
  //        Application.launch(PsyktTask::class.java, *args)
  //    }
  //    System.exit(0)
	println("btw, there used to be a python script to execute this with various params this but I'm deleting it now because its nothing spectacular and I could always recreate it if I need to and would probably create it better")
  Application.launch(DigitSpanApp::class.java,*args)
  System.exit(0)

}

lateinit var _args: Array<String>

class PsyktApp : Application() {
  override fun start(primaryStage: Stage?) {


	MainStage.show()
	MainStage.centerOnScreen()
	TaskListView.selectionModel.select(0)
  }
}

class DigitSpanApp: Application() {
  override fun start(primaryStage: Stage?) {

	ExperimentStage.show()

	val task = DigitSpan()
	val id = TextInputDialog().apply{
	  this.contentText = "subject id"
	  this.headerText = "DigitSpan Task"
	  this.title = "task parameters"
	  this.initOwner(ExperimentStage)
	}.showAndWait()
	if (id.isPresent) {
	  task.subject = id.get()
	  thread{
		task.run()
	  }
	}
  }
}

class PsyktTask : Application() {
  override fun start(primaryStage: Stage?) {
	thread {
	  taskList.forEach {
		if (_args[0] == it.simpleName) {
		  val task = it.constructors.first().call()
		  task.apply {
			var param: Property<*>? = null
			for ((i, v) in _args.withIndex()) {
			  if (i == 0) continue
			  param = if (param != null) {
				when (param) {
				  is StringProperty -> param.set(v)
				  is DoubleProperty -> param.set(v.toDouble())
				  is IntegerProperty -> param.set(v.toInt())
				}
				null
			  } else {
				task.paramProps.firstOrNull {
				  it.first == v
				}?.second
			  }
			}
		  }
		  task.run()
		}
	  }
	}
  }
}


object MainStage: Stage() {
  init {
	title = "Psykt"
	scene = MainScene
  }
}

object TaskListView: ListView<KClass<out Task>>(taskList) {
  init {
	setCellFactory {
	  object: ListCell<KClass<out Task>>() {
		override fun updateItem(item: KClass<out Task>?, empty: Boolean) {
		  super.updateItem(item, empty)
		  if (item != null) {
			text = item.simpleName
		  }
		}
	  }
	}
	selectionModel.selectedItemProperty().addListener { _,_,n ->
	  resetTaskConfigPane(n)
	}
  }
}

object MainScene: Scene(MainRoot,1000.0,1000.0)


object MainRoot: HBox(){
  init {
	children.addAll(
	  Label("Tasks", TaskListView).apply{
		contentDisplay = ContentDisplay.BOTTOM
		HBox.setHgrow(this, Priority.SOMETIMES)
	  },
	  Label("Task Configuration", TaskConfigPane).apply {
		contentDisplay = ContentDisplay.BOTTOM
		HBox.setHgrow(this, Priority.ALWAYS)
		HBox.setHgrow(TaskConfigPane, Priority.ALWAYS)
	  }
	)
  }
}