package matt.v1.cfg.user

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import matt.dataman.autosaveOnFXChanges
import matt.dataman.simpleObjectDatabase
import matt.hurricanefx.eye.delegate.FXB
import matt.hurricanefx.eye.delegate.FXE
import matt.hurricanefx.eye.delegate.fx
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

  override fun deserialize(jsonObject: JsonObject) = UserConfig().apply {
	saveExps = jsonObject["saveExps"]!!.bool
	loadExps = jsonObject["loadExps"]!!.bool
	scale = PerformanceMode.valueOf(jsonObject["scale"]!!.string)
	autosaveOnFXChanges(UserConfigSerializer.db)
  }

  override fun serialize(value: UserConfig) = jsonObj(
	"saveExps" to value.saveExps,
	"loadExps" to value.loadExps,
	"scale" to value.scale
  )

}

@Serializable(with = UserConfigSerializer::class)
class UserConfig {
  companion object {
	private val instance: UserConfig = db.autoSavingObservable.get()
	var loadExps
	  get() = instance.loadExps
	  set(value) {
		instance.loadExps = value
	  }
	val loadExpsProp = ::loadExps.fx
	var saveExps
	  get() = instance.saveExps
	  set(value) {
		instance.saveExps = value

	  }
	val saveExpsProp = ::saveExps.fx
	var scale
	  get() = instance.scale
	  set(value) {
		instance.scale = value
	  }
	val scaleProp = ::scale.fx
  }

  var saveExps by FXB(true)
  var loadExps by FXB(true)
  var scale by FXE(ORIG_BUT_NEEDS_GPU)

  init {
	autosaveOnFXChanges(db)
  }
}

