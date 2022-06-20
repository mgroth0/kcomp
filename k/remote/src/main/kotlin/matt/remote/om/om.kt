package matt.remote.om

import matt.klib.commons.get
import matt.klib.file.MFile

object OM {
  const val USER = "mjgroth"
  val OM5_HOME = MFile("/om5/user/$USER")
  val OM2_HOME = MFile("/om2/user/$USER")
  val OM_KCOMP = OM2_HOME["kcomp"]
  val OM_DATA_FOLD = OM2_HOME["data"]
}