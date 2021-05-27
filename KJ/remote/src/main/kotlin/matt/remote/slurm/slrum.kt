package matt.remote.slurm

data class SRun(
  val numCPUs: Int = 1,
  val ramGB: Int = 4,
  val includeAGPU: Boolean = false,
  val timeMin: Int = 5,
  val verbose: Boolean = true,
  /*val disableOnPolestar: Boolean = true*/
) {
  val command =
	  "srun ${if (verbose) "-v " else ""}-c $numCPUs --mem=${ramGB}G ${if (includeAGPU) "--gres=gpu:0 --constraint=any-gpu" else ""} -t $timeMin --pty bash"
}