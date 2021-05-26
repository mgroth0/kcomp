package matt.remote.om

import matt.kjlib.file.get
import java.io.File

object OM {
  const val USER = "mjgroth"
  val OM5_HOME = File("/om5/user/$USER")
  val OM2_HOME = File("/om2/user/$USER")
  val OM_KCOMP = OM2_HOME["kcomp"]
  val OM_DATA_FOLD = OM2_HOME["data"]
}