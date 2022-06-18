package matt.sempart.client.const

import matt.kjs.Path

const val WIDTH: Int = 600
const val HEIGHT: Int = 700
const val HALF_WIDTH = WIDTH/2
const val HALF_HEIGHT = HEIGHT/2


val DATA_FOLDER = Path("data")


val TRAIN_IM = "Face/Bd_Jul2018_M_Face_PO1"
val ORIG_DRAWING_IMS =
  mutableListOf(
	"Face/Bd_Jul2018_M_Face_PO1",
	"Face/Ba_Jan2018_F_Face_FU3",
	"Face/Lo_Jan2018_M_Face_FU1"
  )

val LABELS = listOf(
  "eye",
  "mouth",
  "ear",
  "nose"
)

val INSTRUCTIONS_IM_WOLFRAM = "https://www.wolframcloud.com/obj/mjgroth/folder-1/Bd_Jul2018_M_Face_PO1_All.png"
val INSTRUCTIONS_IM_RELATIVE = "data/Bd_Jul2018_M_Face_PO1_All.png"
val INSTRUCTIONS_VID_WOLFRAM = "https://www.wolframcloud.com/obj/mjgroth/semantic-parts/instructions.mp4"
val INSTRUCTIONS_VID_RELATIVE = "data/instructions.mp4"
val SEND_DATA_PREFIX = "send?PROLIFIC_PID="

val TRIAL_CONFIRM_MESSAGE = "Are you sure you are ready to proceed? You cannot go back to this image."

val COMPLETION_URL = "https://app.prolific.co/submissions/complete?cc=92B81EA2"