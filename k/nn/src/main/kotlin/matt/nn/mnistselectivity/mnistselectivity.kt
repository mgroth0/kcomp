package matt.nn.mnistselectivity

import matt.async.date.tic
import matt.file.commons.DATA_FOLDER
import matt.kjlib.shell.shell

/*ORIG: https://colab.research.google.com/drive/1n1yBIF3rSA5KPqP8VYk97yLZ7VSK37qe#scrollTo=y5a20t84B01h*/
/*MINE (just added download line): https://colab.research.google.com/drive/1vGnaYrjaNdGTiPZ2msZDJ-WEzyNdlBWb*/
private val PRIV_DATA_FOLD = DATA_FOLDER + "MNIST_selectivity"


fun mnistSelectivityDemo() {
  val t = tic(prefix = "MNIST SELECTIVITY")
  t.toc("starting MNIST selectivity demo")
  val sampleDataFold = PRIV_DATA_FOLD + "sample_data"
  val robustnessFold = PRIV_DATA_FOLD + "Robustness"
  sampleDataFold.mkdirs()
  if (PRIV_DATA_FOLD.doesNotExist) {
	t.toc("cloning robustness")
	shell("git", "clone", "https://github.com/kimvc7/Robustness.git", workingDir = PRIV_DATA_FOLD, debug = true)
	t.toc("finished cloning robustness")
  }
  t.toc("generating datasets")
  shell("/usr/bin/python3", "main.py", "--run=config", "--config=generate_datasets", workingDir = robustnessFold)
  t.toc("finished generating datasets")
  t.toc("finished MNIST selectivity demo")
}