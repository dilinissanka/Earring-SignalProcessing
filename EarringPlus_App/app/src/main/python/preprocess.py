from scipy import signal
import numpy as np
import util

def movingAverageFilter(ECG, window_size,mode):
    """
    Smooth an ECG signal using moving average filter

    Input:
    :param ECG: ECG signal, numpy array of shape (n_samples, )
    :param window_size: size of the moving average window

    Output:
    :return: smoothed ECG signal, numpy array of shape (n_samples, )
    """

    MA_filter = np.ones(window_size)/window_size
    filtered_ECG = np.convolve(ECG, MA_filter, mode=mode)

    return filtered_ECG



def bandPassFilter(ECG, lowcut, highcut, order=5):
    fs =  util.SAMPLING_FREQUENCY

    b, a = signal.butter(order, [lowcut, highcut],fs=fs, btype='band')

    filtered = signal.lfilter(b, a, ECG)

    return filtered

def DC_block_filter(ECG):

    b = [1, -1]
    a = [1, -0.98]
    filtered = signal.lfilter(b, a, ECG)

    return filtered


def class_breakdown(y, min_percent):
    """
    Generate class breakdown

    Input:
    :param y: labels, numpy array of shape (n_samples,1)
    : min_percent: minimum percentage of a class to be included in the breakdown

    Output:
    :return: class breakdown, dictionary
    """
    labels = {}
    for i in range(0, len(y)):
        label = y[i][0]
        if label in labels:
            labels[label] += 1
        else:
            labels[label] = 1

    # generate percentage breakdown
    breakdown_percent = {}
    for key in labels:
        if labels[key]/len(y) > min_percent:
            breakdown_percent[key] = labels[key]/len(y)


    return breakdown_percent