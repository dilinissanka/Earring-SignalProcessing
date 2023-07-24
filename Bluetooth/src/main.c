/*
 * Copyright (c) 2012-2014 Wind River Systems, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <zephyr/kernel.h>
#include <dk_buttons_and_leds.h>
#include <zephyr/drivers/sensor.h>
#include <stdlib.h>

#include "remote.h"

#define RUN_STATUS_LED DK_LED1
#define CONN_STATUS_LED DK_LED2
#define RUN_LED_BLINK_INTERVAL 20


static struct bt_conn *current_conn;
struct device *lis2dw12 = DEVICE_DT_GET_ANY(st_lis2dw12);

/* Declarations */
void on_connected(struct bt_conn *conn, uint8_t err);
void on_disconnected(struct bt_conn *conn, uint8_t reason);
void on_notif_changed(enum bt_button_notifications_enabled status);
void on_data_received(struct bt_conn *conn, const uint8_t *const data, uint16_t len);

struct bt_conn_cb bluetooth_callbacks = {
	.connected 		= on_connected,
	.disconnected 	= on_disconnected,
};
struct bt_remote_service_cb remote_callbacks = {
	.notif_changed = on_notif_changed,
    .data_received = on_data_received,
};

/* Callbacks */

void on_connected(struct bt_conn *conn, uint8_t err)
{
	if(err) {
		printk("connection err: %d", err);
		return;
	}
	printk("Connected.");
	current_conn = bt_conn_ref(conn);
	dk_set_led_on(CONN_STATUS_LED);
}

void on_disconnected(struct bt_conn *conn, uint8_t reason)
{
	printk("Disconnected (reason: %d)", reason);
	dk_set_led_off(CONN_STATUS_LED);
	if(current_conn) {
		bt_conn_unref(current_conn);
		current_conn = NULL;
	}
}

void on_notif_changed(enum bt_button_notifications_enabled status)
{
	if (status == BT_BUTTON_NOTIFICATIONS_ENABLED) {
		printk("Notifications enabled");
	}
	else {
		printk("Notificatons disabled");
	}
}

void on_data_received(struct bt_conn *conn, const uint8_t *const data, uint16_t len)
{
	uint8_t temp_str[len+1];
	memcpy(temp_str, data, len);
	temp_str[len] = 0x00;

	printk("Received data on conn %p. Len: %d", (void *)conn, len);
}


void accel_update()
{
	// save it to a buffer, when reach certain length send, 256 max
	// measure current & put in shared slides
	// change sleep function to app timer, so entire thing goes to sleep --> reduce power consumption
	// on chip step counting
	// modify android studio app
	static int8_t acceleration_data[6] = {0};
	struct sensor_value acc[3];
	sensor_sample_fetch(lis2dw12);
	sensor_channel_get(lis2dw12, SENSOR_CHAN_ACCEL_XYZ, acc);
	printf("%d.%06d,%d.%06d,%d.%06d\n",
	acc[0].val1, acc[0].val2,
	acc[1].val1, acc[1].val2,
	acc[2].val1, acc[2].val2);
	set_accel_status((int8_t)acc[0].val1,(int8_t)acc[0].val2, (int8_t)acc[1].val1, (int8_t)acc[1].val2, (int8_t)acc[2].val1, (int8_t)acc[2].val2);
	send_button_notification(current_conn);
}

/* Configurations */

/* main */

void main(void)
{
	int err;
	int blink_status = 0;
	printk("Hello World! %s\n", CONFIG_BOARD);

	if(!device_is_ready(lis2dw12)) {
		printk("lis2dw12 device is not ready\n");
		return;
	}

	err = bluetooth_init(&bluetooth_callbacks, &remote_callbacks);
	if (err) {
		printk("bt_enable returned %d", err);
	}

	printk("Running...");
	for (;;) {
		accel_update();
		dk_set_led(RUN_STATUS_LED, (blink_status++%2));
		k_sleep(K_MSEC(RUN_LED_BLINK_INTERVAL));
	}
}
