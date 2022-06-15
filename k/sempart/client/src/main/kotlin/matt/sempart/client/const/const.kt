package matt.sempart.client.const

import matt.kjs.Path

val WIDTH: Int = 600
val HEIGHT: Int = 700
val HALF_WIDTH = WIDTH/2


val DATA_FOLDER = Path("data")


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