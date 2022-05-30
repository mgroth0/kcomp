package matt.v1.cfg.user

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import matt.dataman.autosaveOnFXChanges
import matt.dataman.simpleObjectDatabase
import matt.hurricanefx.eye.delegate.FXB
import matt.hurricanefx.eye.delegate.FXE
import matt.hurricanefx.eye.ser.JsonObjectSerializer
import matt.json.custom.bool
import matt.json.custom.jsonObj
import matt.json.custom.string
import matt.v1.V1_USER_CFG_FILE
import matt.v1.cfg.user.UserConfigSerializer.db
import matt.v1.scaling.PerformanceMode
import matt.v1.scaling.PerformanceMode.ORIG_BUT_NEEDS_GPU

object UserConfigSerializer: JsonObjectSerializer<UserConfig>("UserConfig") {

  internal val db = simpleObjectDatabase(
	file = V1_USER_CFG_FILE,
	type = UserConfig::class
  )
  val instance: UserConfig = db.autoSavingObservable.get()

  override fun deserialize(jsonObject: JsonObject) = UserConfig().apply {
	saveExps = jsonObject["saveExps"]!!.bool
	loadExps = jsonObject["loadExps"]!!.bool
	scale = PerformanceMode.valueOf(jsonObject["scale"]!!.string)
	autosaveOnFXChanges(db)
  }

  override fun serialize(value: UserConfig) = jsonObj(
	"saveExps" to value.saveExps,
	"loadExps" to value.loadExps,
	"scale" to value.scale
  )

}

@Serializable(with = UserConfigSerializer::class)
class UserConfig {
  var saveExps by FXB(true)
  var loadExps by FXB(true)
  var scale by FXE(ORIG_BUT_NEEDS_GPU)

  init {
	autosaveOnFXChanges(db)
  }
}

val CFG = UserConfigSerializer.instance
