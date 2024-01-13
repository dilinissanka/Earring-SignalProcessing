import numpy as np

ALL_BEAT_CLASSES = {"N": "Normal beat",
                    "L": "Left bundle branch block beat",
                    "R": "Right bundle branch block beat",
                    "A": "Atrial premature beat",
                    "S": "Premature or ectopic supraventricular beat",
                    "V": "Premature ventricular contraction",
                    "e": "Atrial escape beat",
                    "n": "Supraventricular escape beat",
                    "E": "Ventricular escape beat",
                    "Q": "Unclassifiable beat"}

SAMPLING_FREQUENCY = (360) # Hz
N_PATIENTS = 48

def generate_data(window_size, classes, path="mitbih_database/"):
    """
    Generate X,y data for each patient ID from the MIT-BIH Arrhythmia Database in mitbih_database/ directory.

    Input:
    :param window_size: size of the sequence to be used for classification
    :param classes: list of classes to select for classification
    :param path: path to the mitbih_database/ directory

    Output:
    :return: X, y
    X: ECG dictionary, key: patient ID, value: numpy array of shape (nBeats, ecgWindowSize)
    y: labels, numpy array of shape (nPatients, )
    """

    # set path
    path = path
    window_size = window_size

    classes = classes
    n_classes = len(classes)
    classCount = [0]*n_classes

    X = dict() # key: patientID, value: list of patient's beats
    y = dict() # key: patientID, value: list of patient's beat labels

    # print("These are the filenames: ", filenames)

    # Split and save .csv , .txt
    records = list()
    patientIDs = list()
    annotations = list()

    # for f in filenames:
    filename = "200"
    file_extension = ".csv"

    print(filename)
    print(file_extension)

    # *.csv; ECG data are the .csv files
    if (file_extension == '.csv'):
        records.append("/storage/emulated/0/henlo/mitbih_database/200.csv")
        patientIDs.append(int(filename))

    # *.txt; annotations are the .txt files
    elif (file_extension == '.txt'):
        annotations.append(path + filename + file_extension)

    filename = "200annotations"
    file_extension = ".txt"

    # *.csv; ECG data are the .csv files
    if (file_extension == '.csv'):
        records.append("/storage/emulated/0/henlo/mitbih_database/200annotations.txt")
        patientIDs.append(int(filename))

    # *.txt; annotations are the .txt files
    elif (file_extension == '.txt'):
        annotations.append("/storage/emulated/0/henlo/mitbih_database/200annotations.txt")



    # Records
    print(f"Processing patient ID: {str(100)}")
    signals = []

    with open(records[0], 'rt') as csvfile:
        row_index = -1
        for line in csvfile:
            # print(line)
            # Remove leading/trailing whitespaces and split by commas
            values = line.strip().split(",")
            if(row_index >= 0): # skip first row
                MLII_lead = int(values[1]) # Modified Limb Lead (MLII)
                V5 = int(values[2]) # V5 lead
                signals.insert(row_index, MLII_lead)
            row_index += 1

    # Read anotations: R-wave position and Arrhythmia class
    with open(annotations[0], 'r') as annotationFile:
        data = annotationFile.readlines() # 650001 lines
        beat = list()

        for d in range(1, len(data)): # skip first row
            splitted = data[d].split(' ')
            splitted = filter(None, splitted)
            next(splitted)                   # first get the sample Time
            pos = int(next(splitted))        # then get the Sample ID
            arrhythmia_type = next(splitted) # lastly get the Arrhythmia type
            arrhythmia_type = np.array(arrhythmia_type).reshape(1,1)
            if(arrhythmia_type in classes):
                arrhythmia_index = classes.index(arrhythmia_type)
                if(window_size < pos and pos < (len(signals) - window_size)):
                    beat  = signals[pos-window_size+1:pos+window_size]
                    beat = np.array(beat).reshape(1,len(beat))
                    if X.get(id) is None:
                        X[id] = beat
                        y[id] = arrhythmia_type
                    X[id] = np.concatenate((X[id],beat))
                    y[id] = np.concatenate((y[id],arrhythmia_type))

    return X, y

def get_patient_beat(X_dict, y_dict, patientID, beat_index):
    """
    Get patientID's single ECG beat at beat_index

    Input:
    :param X: ECG dictionary, key: patient ID, value: numpy array of shape (nBeats, ecgWindowSize)
    :param y: labels, numpy array of shape (nPatients, )
    :param patientID: patient ID
    :param beat_index: beat index

    Output:
    :return: beat, label
    beat: numpy array of shape (1, ecg_window_size)
    label: char
    """

    X_beat = np.array(X_dict[patientID][beat_index])
    y_label = y_dict[patientID][beat_index]

    return X_beat, y_label


def get_arrhythmia(X, y, arrhythmia_type):
    """
    Get arrhythmia_type beats for all patients

    Input:
    :param X_dict: ECG dictionary, key: patient ID, value: numpy array of shape (n_beats, ecg_window_size)
    :param y_dict: key: patient ID, value: labels, numpy array of shape (n_patients, )
    :param arrhythmia: arrhythmia class

    Output:
    :return: X, y
    X: numpy array of shape (n_beats, ecg_window_size)
    y: numpy array of shape (n_beats, )
    """

    # if X and y are dictonary, convert to numpy array
    if isinstance(X, dict):
        X,y = generate_numpy_from_dict(X, y)

    arrhythmia_indices = np.where(y == arrhythmia_type)[0]

    if arrhythmia_indices.size > 0: # if arrhythmia_type exists
        arrhythmia = X[arrhythmia_indices,:]
    else:
        arrhythmia = np.array([])

    return arrhythmia


def generate_numpy_from_dict(X_dict, y_dict, include_arrhythmia=[]):
    """
    Generate ALL patient data in numpy array from X_dict.

    Input:
    :param X_dict: ECG dictionary, key: patient ID, value: numpy array of shape (n_beats, ecg_window_size)
    :param y_dict: key: patient ID, value: labels, numpy array of shape (n_patients, )
    :param inlude_arrhythmia: list of arrhythmia classes to include

    Output:
    :return: X, y
    X: numpy array of shape (n_beats, ecg_window_size)
    y: numpy array of shape (n_beats, )
    """

    X = np.array([])
    y = np.array([])

    for key in X_dict.keys():
        if(X.size == 0):
            if include_arrhythmia:
                arrhythmia_indices = np.where(y_dict[key] == include_arrhythmia)[0]
                X = X_dict[key][arrhythmia_indices,:]
                y = y_dict[key][arrhythmia_indices]
        else:
            if include_arrhythmia: # if arrhythmias to include
                arrhythmia_indices = np.where(y_dict[key] == include_arrhythmia)[0]
                X = np.concatenate( (X,X_dict[key][arrhythmia_indices,:]), axis=0)
                y = np.concatenate( (y,y_dict[key][arrhythmia_indices]), axis=0)
            else:
                X = np.concatenate((X, np.array(X_dict[key])), axis=0)
                y = np.concatenate((y, np.array(y_dict[key])), axis=0)

    return X, y




def getSingleSample(x,y,beat_index,class_index):
    beat_index = beat_index
    class_index = class_index # 0 = L; 1 = N; 2 = R; 3 = V
    beat_num = np.where(y == class_index)[0][beat_index]
    sample = x[beat_num]
    return sample




