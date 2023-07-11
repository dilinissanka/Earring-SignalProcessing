import numpy as np
import matplotlib.pyplot as plt
import serial

serialInst = serial.Serial()
serialInst.baudrate = 115200
serialInst.port = 'COM5'

def main():
    readSerial()

def readSerial():
    file_name = input('Please enter file name:')
    file_name = 'C:\\Users\\dil-p\\Earring++\\Earring-\\PPGData\\finger\\'+ file_name + '.txt'
    serialInst.open()
    output_list = []
    try:
        while(serialInst.readable()):
            packet = serialInst.readline()
            output_list.append(packet.decode('utf').rstrip() + "\n")
    except (Exception, KeyboardInterrupt):
        pass

    serialInst.close()

    with open(file_name,"w") as file:
        file.writelines(output_list)
    
main()
