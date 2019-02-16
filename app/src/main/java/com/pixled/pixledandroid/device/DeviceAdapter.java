package com.pixled.pixledandroid.device;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pixled.pixledandroid.R;
import com.pixled.pixledandroid.deviceGroup.mainActivity.GroupSelectionActivity;
import com.pixled.pixledandroid.deviceGroup.mainActivity.GroupViewFragment;
import com.pixled.pixledserver.core.device.base.Device;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

    /*
    This class is used to generate views corresponding to each device in the RecyclerView, from a
    list of devices.
     */

    private List<Device> deviceList;
    private List<DeviceViewHolder> deviceViews;

    private boolean enableColorButton;

    private GroupSelectionActivity groupSelectionActivity;

    public DeviceAdapter(List<Device> deviceList, GroupSelectionActivity groupSelectionActivity, boolean enableColorButton) {
        this.deviceList = deviceList;
        deviceViews = new ArrayList<>();
        this.groupSelectionActivity = groupSelectionActivity;
        this.enableColorButton = enableColorButton;
    }

    /*
    Automatically called to generated each device view from the device list.
     */
    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup viewGroup, int itemType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.device_card_view,viewGroup,false);
        DeviceViewHolder newdeviceView = new DeviceViewHolder(view, groupSelectionActivity, enableColorButton);
        deviceViews.add(newdeviceView);
        return newdeviceView;
    }

    /*
    Bind each list item with the corresponding view.
     */
    @Override
    public void onBindViewHolder(DeviceViewHolder deviceViewHolder, int position) {
        Device device = deviceList.get(position);
        groupSelectionActivity.getDeviceViewsIndex().put(device.getId(), deviceViewHolder);
        deviceViewHolder.bind(device);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public List<DeviceViewHolder> getDeviceViews() {
        return deviceViews;
    }
}
