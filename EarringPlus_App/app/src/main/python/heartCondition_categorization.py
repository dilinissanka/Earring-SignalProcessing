import util
import preprocess as pp # my data preprocessing functions

def main():
    N_PATIENTS = 48
    SAMPLING_RATE = 360  # Hz

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

    classes = list(ALL_BEAT_CLASSES.keys())
    windowDuration = 0.44  # seconds
    windowWidthSamples = int(windowDuration * SAMPLING_RATE)
    pathSmallData = "small_test_database/"
    filename = "mitbih"

    # Here we will be generating the needed data
    X_dict, y_dict = util.generate_data(windowWidthSamples, classes)  # Process data


    # here we will be getting the y-dict
    # This is where we will be printing out the data for the aymthia data
    # print("This is the arymthia data: ", y_dict)


    # this is where we will be getting the Arrhythmias data
    includedArrhythmias = ["N",
                           "L",
                           "R",
                           "A",
                           "S",
                           "V",
                           "e",
                           "n",
                           "E",
                           "Q"]
    X, y = util.generate_numpy_from_dict(X_dict, y_dict, includedArrhythmias)
    # print(f"X shape = {X.shape}\nY shape = {y.shape}")

    class_percent = pp.class_breakdown(y,.05)
    percentages = class_percent.values()

    classes = [beat_class + ':' + ALL_BEAT_CLASSES[beat_class] for beat_class in class_percent.keys()]

    # print(class_percent)

    return class_percent