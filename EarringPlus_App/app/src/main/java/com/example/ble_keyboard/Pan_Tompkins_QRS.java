package com.example.ble_keyboard;

public class Pan_Tompkins_QRS {
    public static double[] band_pass_filter(double[] signal) {
        double[] result = {};
        double[] sig = copyECGArray(signal);

        for (int index = 0; index < signal.length; index++) {
            sig[index] = signal[index];

            if (index >= 1) {
                sig[index] += 2 * sig[index - 1];
            }

            if (index >= 2) {
                sig[index] -= sig[index - 2];
            }

            if (index >= 6) {
                sig[index] -= 2 * sig[index - 6];
            }

            if (index >= 12) {
                sig[index] += sig[index - 12];
            }
        }

        result = copyECGArray(sig);

        // here we will be passing in the high pass filter into here
        for (int index = 0; index < signal.length; index++) {
            result[index] = -1 * sig[index];

            if (index >= 1) {
                result[index] -= result[index - 1];
            }

            if (index >= 16) {
                result[index] += 32 * sig[index - 16];
            }

            if (index >= 32) {
                result[index] += sig[index - 32];
            }
        }

        // Here we will be normalizing the results
        double[] minAndMax = minAndMax(result);

        // now we will want to divide all values by the max
        double maxValue = Math.max(-minAndMax[0], minAndMax[1]);

        // Here we will be diving all of the values and then we will be returning the
        // resulting array
        double[] finalArray = divideAllElements(result, maxValue);

        return finalArray;
    }


    public static double[] derivative(double[] signal, int freq) {
        double[] result = copyECGArray(signal);

        for (int index = 0; index < signal.length; index++) {
            result[index] = 0;
            if (index >= 1) {
                result[index] -= 2 * signal[index - 1];
            }

            if (index >= 2) {
                result[index] -= signal[index - 2];
            }

            if (index >= 2 && index <= signal.length - 2) {
                result[index] += 2 * signal[index + 1];
            }


            if (index >= 2 && index <= signal.length - 3){
                result[index] += signal[index + 2];
            }

            result[index] = (result[index] * freq) / 8;
        }

        return result;
    }


    public static double[] squaring(double[] signal) {
        double[] result = copyECGArray(signal);

        for (int index = 0; index < signal.length; index++) {
            result[index] = signal[index] * signal[index];
        }

        return result;
    }


    public static double[] moving_window_intergration(double[] signal, int freq) {
        double[] result = copyECGArray(signal);
        int wind_size = (int) Math.round((0.150 * freq));
        double sum = 0.0;

        for (int j = wind_size; j < signal.length; j++) {
            sum += signal[j] / wind_size;
            result[j] = sum;
        }

        for (int index = wind_size; index < signal.length; index++) {
            sum += signal[index] / wind_size;
            sum -= signal[index - wind_size] / wind_size;
            result[index] = sum;
        }

        return result;
    }


    public static double[] solve(double[] signal) {
        double[] bypass = band_pass_filter(signal);

        double[] derivative = derivative(bypass, 100);

        double[] square = squaring(derivative);

        return moving_window_intergration(square, 100);
    }







    /***********************************************************************************************
    These are the helper methods
     **********************************************************************************************/



    public static double[] divideAllElements(double[] array, double maxValue) {
        double[] dividedValues = new double[array.length];

        for (int i = 0; i < array.length; i++) {
            dividedValues[i] = array[i] / maxValue;
        }

        return dividedValues;

    }

    public static double[] minAndMax(double[] array) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            if (min > array[i]) {
                min = array[i];
            }

            if (max < array[i]) {
                max = array[i];
            }
        }


        return new double[]{min, max};
    }


    public static double[] copyECGArray(double[] signal) {
        double[] ecgArray = new double[signal.length];
        for (int i = 0; i < ecgArray.length; i++) {
            ecgArray[i] = signal[i];
        }

        return ecgArray;
    }
}
