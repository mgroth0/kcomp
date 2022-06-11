import os

from PIL import Image
import numpy as np
import cv2

DEBUG = None
# DEBUG = 'Face/Ba_Jan2018_F_Face_FU3.png'
# DEBUG = 'House/Aa_Blu0500_M_House_CON.png'
PRE_CROP = False
CROP = True
RECOLOR = True

os.chdir("/Users/matthewgroth/registered/ide/semantic_parts/preprocess")

data_folder = '../data/Real Object Drawings/data/'
raw_folder = data_folder + 'raw/'
crop_demo_folder = data_folder + 'crop_demo/'
crop_folder = data_folder + 'crop/'
os.makedirs(crop_demo_folder, exist_ok=True)
os.makedirs(crop_folder, exist_ok=True)

RESIZE_TO_MAX_WIDTH = True
RESIZE_TO_MAX_HEIGHT = True

widths_post_crop = []

# THRESHOLD = 255
THRESHOLD = 100

ALL_BUT_BOTTOM = True

# (largest)
# ORIG_SHAPE_MAX = (1024, 1024)
# done (max post crop width = 1298)
# ORIG_SHAPE_MAX = (1298, 1298)
# ORIG_SHAPE_MAX = (590, 590) #specifically request by sharon
# ORIG_SHAPE_MAX = (400, 400) #specifically request by sharon
ORIG_SHAPE_MAX = (600, 500)  # specifically request by sharon
# ORIG_SHAPE_MAX = (1953, 1953) # Person/Ra_Jul2018_M_Person_FU3.png
BUFFER_W = 50
BUFFER_H = 100
H_BUFFER_TOP_ONLY = True
FINAL_SHAPE = (ORIG_SHAPE_MAX[0] + BUFFER_H * 2, ORIG_SHAPE_MAX[1] + BUFFER_W * 2)
ABS_FINAL_SHAPE = True
if ABS_FINAL_SHAPE:
    FINAL_SHAPE = (700, 600)
else:
    # make square
    FINAL_SHAPE = (FINAL_SHAPE[0], FINAL_SHAPE[0])



def ceildiv(a, b):
    return -(-a // b)

for category in os.listdir(raw_folder):
    if category == '.DS_Store': continue
    cat_folder = raw_folder + category + '/'
    for im_file in os.listdir(cat_folder):
        if im_file == 'Labeled': continue
        if DEBUG is not None and (category + '/' + im_file) != DEBUG: continue
        crop_demo_cat_fold = crop_demo_folder + category + '/'
        crop_cat_fold = crop_folder + category + '/'
        os.makedirs(crop_demo_cat_fold, exist_ok=True)
        os.makedirs(crop_cat_fold, exist_ok=True)
        pre_crop_file = crop_demo_cat_fold + im_file
        crop_file = crop_cat_fold + im_file

        im = Image.open(cat_folder + im_file)

        im = im.convert('RGB')

        if DEBUG:
            im.show()

        im = np.array(im)

        # shouldnt I be running this check AFTER cropping?
        # if not RESIZE_TO_MAX_WIDTH and (im.shape[1] > ORIG_SHAPE_MAX[0] or im.shape[0] > ORIG_SHAPE_MAX[1]):
        #     raise Exception("larger image size: " + cat_folder + im_file)

        if DEBUG:
            print(im)

        print('cropping ' + category + '/' + im_file)

        first_colored_y = None
        last_colored_y = None
        first_colored_x = None
        last_colored_x = None

        for y in range(im.shape[0]):
            row = im[y]
            colored = any(any(c < THRESHOLD for c in px) for px in row)
            if colored:
                last_colored_y = y
                if first_colored_y is None:
                    first_colored_y = y

        for x in range(im.shape[1]):
            col = im[:, x]
            colored = any(any(c < THRESHOLD for c in px) for px in col)
            if colored:
                last_colored_x = x
                if first_colored_x is None:
                    first_colored_x = x

        center_colored_y = (last_colored_y - first_colored_y) // 2 + first_colored_y
        center_colored_x = (last_colored_x - first_colored_x) // 2 + first_colored_x

        widths_post_crop.append(last_colored_x - first_colored_x + 1)

        if PRE_CROP:
            im[first_colored_y, :, :] = 0
            im[last_colored_y, :, :] = 0
            im[:, first_colored_x, :] = 0
            im[:, last_colored_x, :] = 0
            Image.fromarray(im).save(pre_crop_file)

        if CROP:
            im = im[first_colored_y:last_colored_y + 1, first_colored_x:last_colored_x + 1, :]

            # was previously using this above. But shouldnt I do it here?
            if not RESIZE_TO_MAX_WIDTH and (im.shape[1] > ORIG_SHAPE_MAX[0] or im.shape[0] > ORIG_SHAPE_MAX[1]):
                raise Exception("larger image size: " + cat_folder + im_file)

            if RECOLOR:
                # breakpoint()

                temp_im = np.zeros(im.shape[0:2]) + 255
                temp_im[np.amin(im, axis=2) < THRESHOLD] = 0
                temp_im = np.expand_dims(temp_im, axis=1)
                temp_im = np.moveaxis(temp_im, 1, 2)
                im = np.repeat(temp_im, 3, axis=2)

            if RESIZE_TO_MAX_HEIGHT and im.shape[0] != ORIG_SHAPE_MAX[0]:
                ratio = ORIG_SHAPE_MAX[0] / im.shape[0]
                newWidth = round(im.shape[1] * ratio)
                # print("shape before = " + str(im.shape))
                # print("newHeight=" + str(newHeight))
                # print(f"{im.shape[1]=}")
                im = cv2.resize(im, dsize=(newWidth, ORIG_SHAPE_MAX[0]), interpolation=cv2.INTER_LINEAR)
                # print("shape after = " + str(im.shape))
                # print(imA.shape)
                # im.shape = (newHeight, im.shape[1], 3)

            if (RESIZE_TO_MAX_HEIGHT and RESIZE_TO_MAX_WIDTH and im.shape[1] > ORIG_SHAPE_MAX[1]) or (
                    (not RESIZE_TO_MAX_HEIGHT) and
                    RESIZE_TO_MAX_WIDTH and im.shape[1] != ORIG_SHAPE_MAX[1]):
                ratio = ORIG_SHAPE_MAX[1] / im.shape[1]
                newHeight = round(im.shape[0] * ratio)
                # print("shape before = " + str(im.shape))
                # print("newHeight=" + str(newHeight))
                # print(f"{im.shape[1]=}")
                im = cv2.resize(im, dsize=(ORIG_SHAPE_MAX[1], newHeight), interpolation=cv2.INTER_LINEAR)
                # print("shape after = " + str(im.shape))
                # print(imA.shape)
                # im.shape = (newHeight, im.shape[1], 3)

            if not ABS_FINAL_SHAPE:
                needed_y = BUFFER_H * 2
                needed_x = BUFFER_W * 2
            else:
                needed_y = FINAL_SHAPE[0] - im.shape[0]
                needed_x = FINAL_SHAPE[1] - im.shape[1]
            try:
                im = np.concatenate((np.zeros((needed_y // (1 if ALL_BUT_BOTTOM else 2), im.shape[1], 3)) + 255, im),
                                    axis=0)
            except:
                breakpoint()

            if not ALL_BUT_BOTTOM:
                im = np.concatenate((im, np.zeros((ceildiv(needed_y, 2), im.shape[1], 3)) + 255), axis=0)
            im = np.concatenate((np.zeros((im.shape[0], needed_x // 2, 3)) + 255, im), axis=1)
            im = np.concatenate((im, np.zeros((im.shape[0], ceildiv(needed_x, 2), 3)) + 255), axis=1)

            im = im.astype(np.uint8)

            # print(type(im))
            # print(im.shape)
            # print(im.dtype)
            # print('max'+str(np.max(im)))
            # print('min'+str(np.min(im)))
            # print(im)

            Image.fromarray(im).save(crop_file)

        if DEBUG:
            print(f'{first_colored_y=}')
            print(f'{last_colored_y=}')
            print(f'{first_colored_x=}')
            print(f'{last_colored_x=}')

print("done (max post crop width = " + str(max(widths_post_crop)) + ")")
