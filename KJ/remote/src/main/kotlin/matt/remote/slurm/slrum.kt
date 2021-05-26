package matt.remote.slurm

data class SRun(
  val numCPUs: Int = 1,
  val ramGB: Int = 4,
  val numGPUs: Int = 0,
  val timeMin: Int = 5,
  val verbose: Boolean = true,
  /*val disableOnPolestar: Boolean = true*/
) {
  val command =
	  "srun ${if (verbose) "-v " else ""}-c $numCPUs --mem=${ramGB}G --gres=gpu:${numGPUs} --constraint=any-gpu -t $timeMin --pty bash"
}