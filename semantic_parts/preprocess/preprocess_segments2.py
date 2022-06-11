import json
import os
import shutil
from dataclasses import dataclass
from typing import List
import time

from PIL import Image
import numpy as np



start = time.time()

os.chdir("/Users/matthewgroth/registered/ide/semantic_parts/preprocess")

data_folder = os.path.join('..', 'data', 'Real Object Drawings', 'data')
segments_folder = os.path.join(data_folder, 'segments')
segments_highlighted_folder = os.path.join(data_folder, 'segment_highlighted')
segments_labeled_folder = os.path.join(data_folder, 'segment_labelled')
segments_select_folder = os.path.join(data_folder, 'segment_selected')
segments_hi_labeled_folder = os.path.join(data_folder, 'segment_hi_labeled')
segments_select_labeled_folder = os.path.join(data_folder, 'segment_selected_labeled')
all_folder = os.path.join(data_folder, 'all')
segment_data2_folder = os.path.join(data_folder, 'segment_data2')

def rmtreeifdir(path):
    if os.path.isdir(path):
        shutil.rmtree(path,ignore_errors=True)
def remakedir(path):
    rmtreeifdir(path)
    os.makedirs(path, exist_ok=True)

remakedir(segment_data2_folder)
remakedir(segments_labeled_folder)
remakedir(segments_select_labeled_folder)
remakedir(segments_hi_labeled_folder)
remakedir(segments_highlighted_folder)
remakedir(segments_labeled_folder)

DEBUG = None

PREPROCESS = True
COPY_FILES = True
DUMMY_MIN = False # 240

all_with_dummies = os.path.join("..","data/Real Object Drawings/data/all_dummies/")
segments_with_dummies = os.path.join("..","data/Real Object Drawings/data/segments_dummies/")



if DUMMY_MIN is not None:


    if os.path.isdir(all_with_dummies):
        shutil.rmtree(all_with_dummies)
    if os.path.isdir(segments_with_dummies):
        shutil.rmtree(segments_with_dummies)

    shutil.copytree(all_folder,all_with_dummies)
    for d in range(DUMMY_MIN):
        shutil.copy(
            os.path.join(all_with_dummies,"Face/Lo_Jan2018_M_Face_FU1_All.png"),
            os.path.join(all_with_dummies,f"Face/Lo_Jan2018_M_Face_FU1_D{d}_All.png")
        )

    shutil.copytree(segments_folder,segments_with_dummies)
    for d in range(DUMMY_MIN):
        for l in range(1,7):
            debugFile1 = os.path.join(segments_with_dummies,f"Face/Lo_Jan2018_M_Face_FU1_L{l}.png")
            debugFile2 = os.path.join(segments_with_dummies,f"Face/Lo_Jan2018_M_Face_FU1_D{d}_L{l}.png")
            shutil.copy(debugFile1,debugFile2)
            # print(f"copied {debugFile1=},${debugFile2=}")
            # breakpoint()

    all_folder = all_with_dummies
    segments_folder = segments_with_dummies


@dataclass
class Style:
    mouseover: List[int]
    selected: List[int]
    labeled: List[int]
    pattern_labeled: bool


styleOne = Style(
    mouseover=[100, 100, 100, 255],
    selected=[0, 0, 255, 255],
    labeled=[255, 0, 0, 255],
    pattern_labeled=False
)
styleTwo = Style(
    mouseover=[75, 75, 75, 255],
    selected=[100, 100, 255, 255],
    labeled=[255, 255, 0, 255],
    pattern_labeled=True
)
STYLE = styleTwo

if PREPROCESS:

    print("preprocessing")


    def ceildiv(a, b):
        return -(-a // b)


    pictures = dict()

    for category in os.listdir(segments_folder):
        if category == '.DS_Store': continue
        cat_folder = os.path.join(segments_folder, category)
        for im_file in os.listdir(cat_folder):
            if DEBUG is not None and (os.path.join(category, im_file)) != DEBUG: continue
            full_im_file = os.path.join(cat_folder, im_file)
            all_cat_fold = os.path.join(all_folder, category)
            is_all_file = 'All' in im_file
            all_im_file = os.path.join(all_cat_fold, im_file)
            if is_all_file:
                os.rename(full_im_file, all_im_file)
            else:
                segment_data_cat_fold = os.path.join(segment_data2_folder, category)
                segment_highlight_im_fold = os.path.join(segments_highlighted_folder, category)
                segment_labeled_im_fold = os.path.join(segments_labeled_folder, category)
                segment_labeled_selected_im_fold = os.path.join(segments_select_labeled_folder, category)
                segment_labeled_hi_im_fold = os.path.join(segments_hi_labeled_folder, category)
                os.makedirs(segment_labeled_im_fold, exist_ok=True)
                os.makedirs(segment_labeled_selected_im_fold, exist_ok=True)
                os.makedirs(segment_labeled_hi_im_fold, exist_ok=True)
                os.makedirs(segment_highlight_im_fold, exist_ok=True)
                segment_select_im_fold = os.path.join(segments_select_folder, category)
                os.makedirs(segment_data_cat_fold, exist_ok=True)
                drawing_id = '_'.join(im_file.split('_')[:-1])
                highlight_im_file = os.path.join(segment_highlight_im_fold, im_file)
                labelled_im_file = os.path.join(segment_labeled_im_fold, im_file)
                select_im_file = os.path.join(segment_select_im_fold, im_file)
                select_labeled_im_file = os.path.join(segment_labeled_selected_im_fold, im_file)
                hi_labeled_im_file = os.path.join(segment_labeled_hi_im_fold, im_file)
                if drawing_id not in pictures:
                    pictures[drawing_id] = {
                        "all_im": os.path.join(all_cat_fold, drawing_id + "_All.png"),
                        "data_file": os.path.join(segment_data_cat_fold, drawing_id + '.json'),
                        "data_files": [],
                        "highlight_ims": [],
                        "labelled_ims": [],
                        "select_ims": [],
                        "select_labeled_ims": [],
                        "hi_labeled_ims": [],
                        "segments": {}
                    }
                pictures[drawing_id]["data_files"].append(full_im_file)
                pictures[drawing_id]["highlight_ims"].append(highlight_im_file)
                pictures[drawing_id]["labelled_ims"].append(labelled_im_file)
                pictures[drawing_id]["select_ims"].append(select_im_file)
                pictures[drawing_id]["select_labeled_ims"].append(select_labeled_im_file)
                pictures[drawing_id]["hi_labeled_ims"].append(hi_labeled_im_file)

    for pic in pictures.keys():
        print(f"{pic=}")
        layers = pictures[pic]["data_files"]
        hi_im_files = pictures[pic]["highlight_ims"]
        la_im_files = pictures[pic]["labelled_ims"]
        la_sel_im_files = pictures[pic]["select_labeled_ims"]
        la_hi_im_files = pictures[pic]["hi_labeled_ims"]
        json_file = pictures[pic]["data_file"]
        segment_pic_data = pictures[pic]["segments"]

        all_im = Image.open(pictures[pic]["all_im"])
        all_im = all_im.convert('RGBA')
        all_im = np.array(all_im)

        for i in range(len(hi_im_files)):
            print(f"{i=}")
            layer_pic = layers[i]
            segment_im = Image.open(layer_pic)
            segment_im = segment_im.convert('RGBA')
            segment_im = np.array(segment_im)
            print('preprocessing segments')
            segment = []
            segment_pic_data[layer_pic.split("_L")[-1].split(".")[0]] = segment

            highlight_im = np.copy(all_im)
            labelled_im = np.copy(all_im)
            select_im = np.copy(all_im)
            select_labeled_im = np.copy(all_im)
            hi_labeled_im = np.copy(all_im)

            for r, row in enumerate(segment_im):
                bool_row = []
                segment.append(bool_row)
                for c, pix in enumerate(row):
                    pix = pix.tolist()
                    # print("pix=" + json.dumps(pix))
                    white = pix[0] == 255 and pix[1] == 255 and pix[2] == 255

                    pixIsInSeg = not white

                    bool_row.append(pixIsInSeg)

                    if pixIsInSeg:
                        highlight_im[r, c] = STYLE.mouseover
                        if not STYLE.pattern_labeled:
                            select_labeled_im[r,c] = STYLE.selected
                            hi_labeled_im[r,c] = STYLE.mouseover
                            labelled_im[r, c] = STYLE.labeled
                        elif (r+c) % 16 == 0 or ((r+c)+1) % 16 == 0 or ((r+c)+2) % 16 == 0 or ((r+c)+3) % 16 == 0:
                            labelled_im[r, c] = STYLE.labeled
                            select_labeled_im[r, c] = STYLE.labeled
                            hi_labeled_im[r,c] = STYLE.labeled
                        else:
                            labelled_im[r, c] = [0, 0, 0, 255]
                            select_labeled_im[r, c] = STYLE.selected
                            hi_labeled_im[r,c] = STYLE.mouseover
                        select_im[r, c] = STYLE.selected
                    else:
                        highlight_im[r, c][3] = 0
                        labelled_im[r, c][3] = 0
                        select_im[r, c][3] = 0
                        select_labeled_im[r, c][3] = 0
                        hi_labeled_im[r, c][3] = 0

            Image.fromarray(highlight_im).save(pictures[pic]["highlight_ims"][i])
            Image.fromarray(labelled_im).save(pictures[pic]["labelled_ims"][i])
            Image.fromarray(select_im).save(pictures[pic]["select_ims"][i])
            Image.fromarray(select_labeled_im).save(pictures[pic]["select_labeled_ims"][i])
            Image.fromarray(hi_labeled_im).save(pictures[pic]["hi_labeled_ims"][i])

        with open(json_file, 'w') as f:
            f.write(json.dumps(segment_pic_data))

            # Image.fromarray(im).save(crop_file)

if (COPY_FILES):

    print("copying data to other project")

    other_project_data_folder = "/Users/matthewgroth/registered/ide/ktor-gradle-sample2/web/data"

    other_project_segments_highlighted_folder = os.path.join(other_project_data_folder, 'segment_highlighted')
    other_project_segments_labelled_folder = os.path.join(other_project_data_folder, 'segment_labelled')
    other_project_segments_select_folder = os.path.join(other_project_data_folder, 'segment_selected')
    other_project_segments_select_labeled_folder = os.path.join(other_project_data_folder, 'segment_selected_labeled')
    other_project_segments_hi_labeled_folder = os.path.join(other_project_data_folder, 'segment_hi_labeled')
    other_project_all_folder = os.path.join(other_project_data_folder, 'all')
    other_project_segment_data2_folder = os.path.join(other_project_data_folder, 'segment_data2')






    rmtreeifdir(other_project_segments_highlighted_folder)
    rmtreeifdir(other_project_segments_labelled_folder)
    rmtreeifdir(other_project_segments_select_folder)
    rmtreeifdir(other_project_segments_select_labeled_folder)
    rmtreeifdir(other_project_segments_hi_labeled_folder)
    rmtreeifdir(other_project_all_folder)
    rmtreeifdir(other_project_segment_data2_folder)

    shutil.copytree(segments_highlighted_folder, other_project_segments_highlighted_folder)
    shutil.copytree(segments_labeled_folder, other_project_segments_labelled_folder)
    shutil.copytree(segments_select_folder, other_project_segments_select_folder)
    shutil.copytree(segments_select_labeled_folder, other_project_segments_select_labeled_folder)
    shutil.copytree(segments_hi_labeled_folder, other_project_segments_hi_labeled_folder)
    shutil.copytree(all_folder, other_project_all_folder)
    shutil.copytree(segment_data2_folder, other_project_segment_data2_folder)

stop = time.time()
print(f"done in {stop - start} secs")
