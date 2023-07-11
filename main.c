/*
 * Copyright (c) 2017, NXP
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <zephyr/kernel.h>
#include <zephyr/drivers/sensor.h>
#include <stdio.h>

void main(void)
{
	struct sensor_value ir;
	const struct device *const dev = DEVICE_DT_GET_ANY(maxim_max30101);

	if (dev == NULL) {
		printf("Could not get max30101 device\n");
		return;
	}
	if (!device_is_ready(dev)) {
		printf("max30101 device %s is not ready\n", dev->name);
		return;
	}

	while (1) {
		sensor_sample_fetch(dev);
		sensor_channel_get(dev, SENSOR_CHAN_IR, &ir);
		printk("%d\n", ir.val1);
		k_sleep(K_MSEC(20));
	}
}
