package matt.v1.cfg.user

import matt.dataman.SimpleObjectDatabase
import matt.dataman.autosaveOnFXChanges
import matt.hurricanefx.eye.delegate.FXB
import matt.hurricanefx.eye.delegate.FXE
import matt.json.custom.SimpleJson
import matt.reflect.NoArgConstructor
import matt.v1.V1_USER_CFG_FILE
import matt.v1.scaling.PerformanceMode
import matt.v1.scaling.PerformanceMode.ORIG_BUT_NEEDS_GPU


@NoArgConstructor()
class UserConfig: SimpleJson<UserConfig>(typekey = null) {
  companion object {
	private val db = SimpleObjectDatabase(
	  file = V1_USER_CFG_FILE,
	  type = UserConfig::class
	)
	private val instance: UserConfig = db.autoSavingObservable.get()
	var loadExps
	  get() = instance.loadExps
	  set(value) {
		instance.loadExps = value

	  }
	val loadExpsProp = instance.loadExpsProp
	var saveExps
	  get() = instance.saveExps
	  set(value) {
		instance.saveExps = value

	  }
	val saveExpsProp = instance.saveExpsProp
	var scale
	  get() = instance.scale
	  set(value) {
		instance.scale = value
	  }
	val scaleProp = instance.scaleProp
  }

  var saveExps by JsonBoolProp(optional = true, default = true)
  val saveExpsProp by FXB(bind = ::saveExps)

  var loadExps by JsonBoolProp(optional = true, default = true)
  val loadExpsProp by FXB(bind = ::loadExps)

  var scale by JsonEnumProp(PerformanceMode::class, default = ORIG_BUT_NEEDS_GPU, optional = true)
  val scaleProp by FXE<PerformanceMode>(bind = ::scale)

  init {
	autosaveOnFXChanges(db)
  }
}

