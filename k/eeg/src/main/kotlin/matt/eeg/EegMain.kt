package matt.eeg

import brainflow.BoardIds
import brainflow.BoardShim
import brainflow.BrainFlowError
import brainflow.BrainFlowInputParams
import brainflow.LogLevels.LEVEL_INFO
import matt.gui.app.GuiApp
import matt.kjlib.shell.execReturn
import java.util.Arrays
import kotlin.system.exitProcess


fun main(): Unit = GuiApp {



  val bt = "State: On" in execReturn("/usr/sbin/system_profiler", "SPBluetoothDataType")

  if (bt) {
	getDataFromBoard()
  } else {
	println("please turn on bluetooth")
	exitProcess(1)
  }

  /*//  println(execPython(python))
  //
  //  val testString = "${'$'}{something}"*/


}.start()

private fun getDataFromBoard() {
  BoardShim.enable_board_logger()
  BoardShim.set_log_level(0)
  val params = BrainFlowInputParams()
  params.serial_port = "/dev/cu.usbmodem11"
  params.mac_address
  val board_id = BoardIds.GANGLION_BOARD._code
  val board_shim = BoardShim(board_id, params)

  try {
	board_shim.prepare_session()
//    val instanceProp = BoardShim::class.memberProperties.first { it.name == "instance" }
//    instanceProp.isAccessible = true
//    val ec = instanceProp.get(board_shim).prepare_session(board_shim.board_id, board_shim.input_json)
//    if (ec != STATUS_OK._code) {
//      throw BrainFlowError("Error in prepare_session", ec)
//    }
  } catch (e: BrainFlowError) {
    println("e.message=${e.message}")
    e.printStackTrace()
	println("Is the Ganglion turned on?")
	exitProcess(2)
  }
  // board_shim.start_stream (); // use this for default options
  // board_shim.start_stream (); // use this for default options
  /*board_shim.start_stream(450000, "file://file_stream.csv:w")*/
  board_shim.start_stream(450000, "")
  BoardShim.log_message(LEVEL_INFO._code, "Start sleeping in the main thread")

//  board_shim.


  print("board_shim.board_id=${board_shim.board_id}")

//  board_shim.config_board()

  Thread.sleep(5000)
  board_shim.stop_stream()
  println(board_shim._board_data_count)
  val data = board_shim.get_current_board_data(30) // doesnt flush it from ring buffer

  // double[][] data = board_shim.get_board_data (); // get all data and flush
  // from ring buffer
  // double[][] data = board_shim.get_board_data (); // get all data and flush
  // from ring buffer
  for (i in data.indices) {
	println(Arrays.toString(data[i]))
  }
  board_shim.release_session()

  println("get_eeg_channels= ${BoardShim.get_eeg_channels(BoardIds.GANGLION_BOARD._code)}")
  println("get_emg_channels= ${BoardShim.get_emg_channels(BoardIds.GANGLION_BOARD._code)}")
  println("get_ecg_channels= ${BoardShim.get_ecg_channels(BoardIds.GANGLION_BOARD._code)}")
  println("get_accel_channels= ${BoardShim.get_accel_channels(BoardIds.GANGLION_BOARD._code)}")
  println("get_sampling_rate= ${BoardShim.get_sampling_rate(BoardIds.GANGLION_BOARD._code)}")
  println("get_timestamp_channel= ${BoardShim.get_timestamp_channel(BoardIds.GANGLION_BOARD._code)}")






  /*this throws an error?*/
  /*println("get_other_channels= ${BoardShim.get_other_channels(BoardIds.GANGLION_BOARD._code)}")*/

  println("data.size=${data.size}")
  println("data[0].size=${data[0].size}")

  println("\n\n")
  println("|||||||||||||||||||||||||||||||||||||||||||||||")
  println("REMEMBER TO TURN OFF THE GANGLION AND BLUETOOTH")
  println("|||||||||||||||||||||||||||||||||||||||||||||||")
  println("\n\n")
  exitProcess(0)

}


//language=Python
val python = """
  
import argparse
import time

from brainflow.board_shim import BoardIds, BoardShim, BrainFlowInputParams

import matplotlib.pyplot as plt
  
BoardShim.enable_dev_board_logger()

parser = argparse.ArgumentParser()
# use docs to check which parameters are required for specific board, e.g. for Cyton - set serial port
parser.add_argument('--timeout', type=int, help='timeout for device discovery or connection', required=False,
					default=0)
parser.add_argument('--ip-port', type=int, help='ip port', required=False, default=0)
parser.add_argument('--ip-protocol', type=int, help='ip protocol, check IpProtocolType enum', required=False,
					default=0)
parser.add_argument('--ip-address', type=str, help='ip address', required=False, default='')
parser.add_argument('--serial-port', type=str, help='serial port', required=False, default='/dev/cu.usbmodem11')
parser.add_argument('--mac-address', type=str, help='mac address', required=False, default='')
parser.add_argument('--other-info', type=str, help='other info', required=False, default='')
parser.add_argument('--streamer-params', type=str, help='streamer params', required=False, default='')
parser.add_argument('--serial-number', type=str, help='serial number', required=False, default='')
parser.add_argument('--board-id', type=int, help='board id, check docs to get a list of supported boards',
					required=False, default=BoardIds.GANGLION_BOARD)
parser.add_argument('--file', type=str, help='file', required=False, default='')
args = parser.parse_args()

params = BrainFlowInputParams()
params.ip_port = args.ip_port
params.serial_port = args.serial_port
params.mac_address = args.mac_address
params.other_info = args.other_info
params.serial_number = args.serial_number
params.ip_address = args.ip_address
params.ip_protocol = args.ip_protocol
params.timeout = args.timeout
params.file = args.file

board = BoardShim(args.board_id, params)
print("preparing session...")
board.prepare_session()
print("prepared session")
# board.start_stream () # use this for default options
print("starting stream")
board.start_stream(45000, args.streamer_params)
print("recording data...")
time.sleep(10)
# data = board.get_current_board_data (256) # get latest 256 packages or less, doesnt remove them from internal buffer
print("getting data")
data = board.get_board_data()  # get all data and remove it from internal buffer
print("stopping stream")
board.stop_stream()
print("stopped stream")
board.release_session()
print("released session")

print(f"${'$'}{cBoardShim.get_eeg_channels(BoardIds.GANGLION_BOARD)=}")
print(f"${'$'}{BoardShim.get_emg_channels(BoardIds.GANGLION_BOARD)=}")
print(f"${'$'}{BoardShim.get_ecg_channels(BoardIds.GANGLION_BOARD)=}")
print(f"${'$'}{BoardShim.get_accel_channels(BoardIds.GANGLION_BOARD)=}")
# and so on, check docs for full list
# also we have methods to get sampling rate from board id, get number of timestamp channel and others
print(f"${'$'}{BoardShim.get_sampling_rate(BoardIds.GANGLION_BOARD)=}")
print(f"${'$'}{BoardShim.get_timestamp_channel(BoardIds.GANGLION_BOARD)=}")

# print(f"${'$'}{BoardShim.get_other_channels(BoardIds.GANGLION_BOARD)=}") #throws error



print(f"${'$'}{type(data)=},${'$'}{data.shape =}")
""".trimIndent()



