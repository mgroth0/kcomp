import argparse
import time
import numpy as np

import brainflow
from brainflow.board_shim import BoardShim, BrainFlowInputParams, BoardIds, BoardControllerDLL, BrainflowExitCodes
from brainflow.data_filter import DataFilter, FilterTypes, AggOperations
from brainflow.ml_model import MLModel


def main():
    print('BoardShim version: ' + BoardShim.get_version())
    print('DataFilter version: ' + DataFilter.get_version())
    print('MLModel version: ' + MLModel.get_version())
    BoardShim.enable_dev_board_logger()
    BoardShim.set_log_level(0)

    params = BrainFlowInputParams()
    params.serial_port = '/dev/cu.usbmodem11'
    board = BoardShim(BoardIds.GANGLION_BOARD.value, params)

    # for i in range(2):

    print("preparing session")
    # board.prepare_session()
    res = BoardControllerDLL.get_instance().prepare_session(board.board_id, board.input_json)
    if res != BrainflowExitCodes.STATUS_OK.value:
        print(f"{res=}")
        raise BrainFlowError('unable to prepare streaming session', res)
    print("prepared session")

    board.start_stream ()
    time.sleep(10)
    board.stop_stream()
    data = board.get_board_data()
    print(DataFilter.calc_stddev(data[2]))
    data = board.get_board_data()
    board.release_session()


if __name__ == "__main__":
    main()