package matt.eeg.simplified

import brainflow.BoardIds
import brainflow.BoardShim
import brainflow.BrainFlowInputParams
import brainflow.LogLevels.LEVEL_INFO
import java.util.Arrays
import kotlin.system.exitProcess


fun main() {
  BoardShim.enable_board_logger()
  val params = BrainFlowInputParams()
  params.serial_port = "/dev/cu.usbmodem11"
  val board_id = BoardIds.GANGLION_BOARD._code
  val board_shim = BoardShim(board_id, params)
  board_shim.prepare_session()
  board_shim.start_stream(450000, "file://file_stream.csv:w")
  BoardShim.log_message(LEVEL_INFO._code, "Start sleeping in the main thread")
  Thread.sleep(5000)
  board_shim.stop_stream()
  println(board_shim._board_data_count)
  val data = board_shim.get_current_board_data(30) // doesnt flush it from ring buffer
  for (i in data.indices) {
	println(Arrays.toString(data[i]))
  }
  board_shim.release_session()
  exitProcess(0)
}

