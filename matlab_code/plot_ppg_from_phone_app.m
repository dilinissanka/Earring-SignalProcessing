fileName = "Accelerometer_y.txt";
fs = 50;
lines = readtable(fileName,'Delimiter', ',');

ppg_data_array = [];

for i=1:height(lines)
    line = lines(i,:);
    ppg_data = line.Var2;
    ppg_data_array = [ppg_data_array, ppg_data];
end


t_time = 0:(1.0/fs):((length(ppg_data_array)-1)/fs);
figure;
plot(t_time, ppg_data_array);