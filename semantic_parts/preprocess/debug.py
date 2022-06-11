import json
import os

from PIL import Image
import numpy as np
import cv2

segment_im = Image.open('../temp.png')
segment_im = segment_im.convert('RGBA')
segment_im = np.array(segment_im)

print(segment_im)