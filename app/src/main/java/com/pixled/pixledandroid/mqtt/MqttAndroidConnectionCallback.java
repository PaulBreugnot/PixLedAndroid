package com.pixled.pixledandroid.mqtt;

import android.util.Log;

import com.pixled.pixledandroid.device.DeviceAdapter;
import com.pixled.pixledandroid.device.DeviceViewHolder;
import com.pixled.pixledandroid.deviceGroup.mainActivity.GroupSelectionActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttAndroidConnectionCallback implements MqttCallbackExtended {

    private GroupSelectionActivity groupSelectionActivity;
    private MqttAndroidClient mqttAndroidClient;

    public MqttAndroidConnectionCallback(GroupSelectionActivity groupSelectionActivity, MqttAndroidClient mqttAndroidClient) {
        this.groupSelectionActivity = groupSelectionActivity;
        this.mqttAndroidClient = mqttAndroidClient;
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Long id = Long.valueOf(message.toString());
        Log.i("MQTT","Light " + id + " : " + topic);
        if (groupSelectionActivity != null) {
            DeviceViewHolder deviceView = groupSelectionActivity.getDeviceViewsIndex().get(id);
        /*
        TODO: Handle multiple groups
         */
            DeviceAdapter deviceAdapter = groupSelectionActivity.getViewFragmentIndex().get(deviceView.getDevice().getDeviceGroups().get(0).getId()).getDeviceAdapter();
            if (deviceView != null) {
                if (topic.equals(MqttAndroidConnection.connected_topic)) {
                    deviceView.getDevice().getDeviceState().setConnected(true);

                } else if (topic.equals(MqttAndroidConnection.disconnected_topic)) {
                    deviceView.getDevice().getDeviceState().setConnected(false);
                }
                deviceAdapter.notifyItemChanged(deviceView.getAdapterPosition());
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Log.i("MQTT", "Connection complete!");
        try {
            mqttAndroidClient.subscribe(MqttAndroidConnection.connected_topic, 1);
            mqttAndroidClient.subscribe(MqttAndroidConnection.disconnected_topic, 1);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
