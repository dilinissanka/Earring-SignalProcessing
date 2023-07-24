
#include "remote.h"


#define DEVICE_NAME CONFIG_BT_DEVICE_NAME
#define DEVICE_NAME_LEN (sizeof(DEVICE_NAME)-1)

static K_SEM_DEFINE(bt_init_ok, 1, 1);

static int8_t acceleration_data[6] = {0};
//static uint8_t button_value = 5;

enum bt_button_notifications_enabled notifications_enabled;
static struct bt_remote_service_cb remote_callbacks;

static const struct bt_data ad[] = {
    BT_DATA_BYTES(BT_DATA_FLAGS, (BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR)),
    BT_DATA(BT_DATA_NAME_COMPLETE, DEVICE_NAME, DEVICE_NAME_LEN)
};

static const struct bt_data sd[] = {
    BT_DATA_BYTES(BT_DATA_UUID128_ALL, BT_UUID_REMOTE_SERV_VAL),
};


/* Declarations */

static ssize_t read_accel_characteristic_cb(struct bt_conn *conn, const struct bt_gatt_attr *attr, void *buf, uint16_t len, uint16_t offset);
void accel_chrc_ccc_cfg_changed(const struct bt_gatt_attr *attr, uint16_t value);



BT_GATT_SERVICE_DEFINE(remote_srv,
BT_GATT_PRIMARY_SERVICE(BT_UUID_REMOTE_SERVICE),
    BT_GATT_CHARACTERISTIC(BT_UUID_REMOTE_BUTTON_CHRC,
                    BT_GATT_CHRC_READ | BT_GATT_CHRC_NOTIFY,
                    BT_GATT_PERM_READ,
                    read_accel_characteristic_cb, NULL, NULL),
    BT_GATT_CCC(accel_chrc_ccc_cfg_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),
);


/* Callbacks */


void on_sent(struct bt_conn *conn, void *user_data)
{
    ARG_UNUSED(user_data);
    printk("Notification sent on connection %p", (void *)conn);
}

static ssize_t read_accel_characteristic_cb(struct bt_conn *conn, const struct bt_gatt_attr *attr,
			 void *buf, uint16_t len, uint16_t offset)
{
    printk(sizeof(acceleration_data));
	return bt_gatt_attr_read(conn, attr, buf, len, offset, &acceleration_data,
				 sizeof(acceleration_data));
}

void accel_chrc_ccc_cfg_changed(const struct bt_gatt_attr *attr, uint16_t value)
{
    bool notif_enabled = (value == BT_GATT_CCC_NOTIFY);
    printk("Notifications %s", notif_enabled? "enabled":"disabled");

    notifications_enabled = notif_enabled? BT_BUTTON_NOTIFICATIONS_ENABLED:BT_BUTTON_NOTIFICATIONS_DISABLED;
    if (remote_callbacks.notif_changed) {
        remote_callbacks.notif_changed(notifications_enabled);
    }
}


/* Custom functions */

int send_button_notification(struct bt_conn *conn)
{
    int err = 0;

    struct bt_gatt_notify_params params = {0};
    const struct bt_gatt_attr *attr = &remote_srv.attrs[2];

    params.attr = attr;
    params.data = &acceleration_data;
    params.len = 6;
    params.func = on_sent;

    err = bt_gatt_notify_cb(conn, &params);

    return err;
}

void set_accel_status(int32_t x_int, int32_t x_dec, int32_t y_int, int32_t y_dec, int32_t z_int, int32_t z_dec)
{
    acceleration_data[0] = x_int;
    acceleration_data[1] = x_dec;
    acceleration_data[2] = y_int;
    acceleration_data[3] = y_dec;
    acceleration_data[4] = z_int;
    acceleration_data[5] = z_dec;
}

int bluetooth_init(struct bt_conn_cb *bt_cb, struct bt_remote_service_cb *remote_cb)
{
    int err;
    printk("Initializing bluetooth...");

    if (bt_cb == NULL || remote_cb == NULL) {
        return -NRFX_ERROR_NULL;
    }

    bt_conn_cb_register(bt_cb);
    remote_callbacks.notif_changed = remote_cb->notif_changed;

    err = bt_enable(NULL);
    if (err) {
        printk("bt_enable returned %d", err);
        return err;
    }
    
    k_sem_take(&bt_init_ok, K_FOREVER);

    err = bt_le_adv_start(BT_LE_ADV_CONN, ad, ARRAY_SIZE(ad), sd, ARRAY_SIZE(sd));
    if (err) {
        printk("Couldn't start advertising (err = %d)", err);
        return err;
    }



    return err;
}