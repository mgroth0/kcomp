import os
from pngtosvg import rgba_image_to_svg_pixels
from PIL import Image
import numpy as np

data_folder = '../data/Real Object Drawings/data/'
# raw_folder = data_folder + 'raw/'
# crop_demo_folder = data_folder + 'crop_demo/'
crop_folder = data_folder + 'crop/'
svg_json_file = crop_folder + 'data.json'

for category in os.listdir(crop_folder):
    if category == '.DS_Store': continue
    cat_folder = crop_folder + category + '/'
    for im_file in os.listdir(cat_folder):
        im = Image.open(cat_folder + im_file)
        im = im.convert('RGBA')
        im = np.array(im)
        svg = rgba_image_to_svg_pixels(im)


