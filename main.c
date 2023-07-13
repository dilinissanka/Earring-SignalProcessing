/*
 * Copyright (c) 2016 Intel Corporation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <zephyr/kernel.h>
#include <zephyr/drivers/sensor.h>
#include <stdlib.h>

#define SLEEP_TIME_MS   20
const struct device *lis2dw12 = DEVICE_DT_GET_ANY(st_lis2dw12);

void main(void)
{
	struct sensor_value acc[3];
	int err;
	if(!device_is_ready(lis2dw12)) {
		printk("lis2dw12 device is not ready\n");
		return;
	}
	while(true) {
		do{
			sensor_sample_fetch(lis2dw12);
			sensor_channel_get(lis2dw12, SENSOR_CHAN_ACCEL_XYZ, acc);
			printf("%d.%06d,%d.%06d,%d.%06d\n",
		       acc[0].val1, abs(acc[0].val2),
		       acc[1].val1, abs(acc[1].val2),
		       acc[2].val1, abs(acc[2].val2));
		} while(false);
		k_msleep(SLEEP_TIME_MS);
	}
}
