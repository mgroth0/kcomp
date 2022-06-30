package matt.remote.om

import matt.file.mFile

object OM {
  const val USER = "mjgroth"
  val OM5_HOME = mFile("/om5/user/$USER")
  val OM2_HOME = mFile("/om2/user/$USER")
  val OM_KCOMP = OM2_HOME["kcomp"]
  val OM_DATA_FOLD = OM2_HOME["data"]
}