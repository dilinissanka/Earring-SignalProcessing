import numpy as np
import matplotlib.pyplot as plt
import serial
import time
import os.path

serialInst = serial.Serial()
serialInst.baudrate = 115200
serialInst.port = 'COM5'

save_path = 'C:/Users/dil-p/Earring++/Earring-/PPG Data/earlobe_IR_data/'

def main():
    readSerial()

def readSerial():
    file_name = input('Please enter file name:')
    file_name = file_name + '.txt'
    completeName = os.path.join(save_path, file_name)
    serialInst.open()
    output_list = []
    try:
        while(serialInst.readable()):
            packet = serialInst.readline()
            output_list.append(str(time.time()) + " " +  packet.decode('utf').rstrip() + "\n")
    except (Exception, KeyboardInterrupt):
        pass

    serialInst.close()

    with open(completeName, 'w') as file:
        file.writelines(output_list)
    
main()
