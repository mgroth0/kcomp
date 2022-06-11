import json
import os

from PIL import Image
import numpy as np
import cv2

data_folder = '../data/Real Object Drawings/data/'
segmented_folder = data_folder + 'segmented/'
not_segmented_folder = data_folder + 'not_segmented/'
segment_data_folder = data_folder + 'segment_data/'
os.makedirs(segment_data_folder, exist_ok=True)

DEBUG = None

def ceildiv(a, b):
    return -(-a // b)

for category in os.listdir(segmented_folder):
    if category == '.DS_Store': continue
    cat_folder = segmented_folder + category + '/'
    for im_file in os.listdir(cat_folder):
        if DEBUG is not None and (category + '/' + im_file) != DEBUG: continue
        not_segmented_cat_fold = not_segmented_folder + category + '/'
        not_segmented_im_file = not_segmented_cat_fold + im_file
        os.makedirs(segment_data_folder + category, exist_ok=True)
        segment_data_file = segment_data_folder + category + '/' + im_file.replace("png", "json").replace("jpg", "json")

        segment_im = Image.open(cat_folder + im_file)
        segment_im = segment_im.convert('RGB')
        segment_im = np.array(segment_im)
        print('preprocessing segments for ' + category + '/' + im_file)

        segment_data = {
            "segments": []
        }

        for row in segment_im:
            for pix in row:

                v = pix[0]


                if v != 0 and v != 255:
                    if v not in segment_data["segments"]:
                        segment_data["segments"].append(int(v))

        with open(segment_data_file, 'w') as f:
            f.write(json.dumps(segment_data))


        # Image.fromarray(im).save(crop_file)

print("done")
