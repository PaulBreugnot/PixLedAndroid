package com.pixled.pixledandroid.device;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.pixled.pixledandroid.R;
import com.pixled.pixledandroid.deviceGroup.GroupSelectionActivity;
import com.pixled.pixledandroid.deviceGroup.GroupViewFragment;
import com.pixled.pixledandroid.utils.ServerConfig;
import com.pixled.pixledserver.core.ToggleState;
import com.pixled.pixledserver.core.device.base.Device;
import com.pixled.pixledserver.core.device.base.DeviceDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class DeviceViewHolder extends RecyclerView.ViewHolder {

    /*
    Controller for lights views (views that are displayed for each light in the RecyclerView)
     */
    private Device device;

    private TextView deviceId;
    private Switch deviceSwitch;
    private Button changeColorButton;
    private TextView connectedTextView;
    private SeekBar intensitySeekBar;

    // Card view associated to this view
    private CardView rootCardView;

    private GroupViewFragment groupViewFragment;

    public DeviceViewHolder(final View itemView,
                            final GroupViewFragment roomViewFragment,
                            boolean enableColorButton) {
        super(itemView);
        rootCardView = itemView.findViewById(R.id.rootCardView);

        this.groupViewFragment = roomViewFragment;

        // Light id view
        deviceId = itemView.findViewById(R.id.deviceId);

        // Connected TextView
        connectedTextView = itemView.findViewById(R.id.connected);

        // Button used to switch light
        deviceSwitch = itemView.findViewById(R.id.deviceSwitch);
        deviceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceService lightService = new Retrofit.Builder()
                        .baseUrl(ServerConfig.ENDPOINT)
                        .addConverterFactory(JacksonConverterFactory.create())
                        .build()
                        .create(DeviceService.class);

                lightService.switchDevice(device.getId()).enqueue(new Callback<DeviceDto>() {
                    @Override
                    public void onResponse(Call<DeviceDto> call, Response<DeviceDto> response) {
                        device.switchDevice();
                        // Update room status according to the current lights setup
                        roomViewFragment.updateGroupStatus();
                    }

                    @Override
                    public void onFailure(Call<DeviceDto> call, Throwable t){
                        Log.i("RETROFIT", "Error on switchLight " + device.getId());
                        deviceSwitch.setChecked(!deviceSwitch.isChecked());
                    }
                }
                );
            }
        });

        // Initiate the color selection view
        changeColorButton = itemView.findViewById(R.id.change_color_button);
        final DeviceViewHolder thisLightViewHolder = this;
        if (enableColorButton) {
            changeColorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((GroupSelectionActivity) roomViewFragment.getActivity())
                            .showChangeColor(device, thisLightViewHolder);
                }
            });
        }

        // Instanciating a light service
        final DeviceService deviceService = new Retrofit.Builder()
                .baseUrl(ServerConfig.ENDPOINT)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(DeviceService.class);

        // Initiate the intensity slider
        intensitySeekBar = itemView.findViewById(R.id.intensitySeekBar);
        intensitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // set seek bar color
                device.getDeviceState().getColor().setValue(((float) progress) / 100);
                intensitySeekBar.getProgressDrawable().setColorFilter(device.getDeviceState().getColor().getArgb(), PorterDuff.Mode.MULTIPLY);
                intensitySeekBar.getThumb().setColorFilter(device.getDeviceState().getColor().getArgb(), PorterDuff.Mode.SRC_ATOP);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                GroupSelectionActivity.publishColorChanged(deviceService, device);
            }
        });
    }

    public void bind(Device device){
        // Used to synchronized the view with informations contained in the corresponding
        // Light instance.
        this.device = device;
        deviceId.setText((Long.valueOf(device.getId())).toString());
        deviceSwitch.setChecked(device.getDeviceState().getToggleState() == ToggleState.ON);
        connectedTextView.setText(device.getDeviceState().isConnected() ? "connected" : "disconnected");

        connectedTextView.setTextColor(device.getDeviceState().isConnected() ?
                groupViewFragment.getActivity().getResources().getColor(R.color.device_connected) :
                groupViewFragment.getActivity().getResources().getColor(R.color.device_disconnected));

        rootCardView.setCardBackgroundColor(device.getDeviceState().isConnected() ?
                groupViewFragment.getActivity().getResources().getColor(R.color.card_view_background) :
                groupViewFragment.getActivity().getResources().getColor(R.color.disconnected_background));


        float[] hsv = {
                device.getDeviceState().getColor().getHue(),
                device.getDeviceState().getColor().getSaturation(),
                1};
        int fullColor = Color.HSVToColor(hsv);
        changeColorButton.setBackgroundColor(fullColor);
        int initialColor = device.getDeviceState().getColor().getArgb();
        intensitySeekBar.getProgressDrawable().setColorFilter(initialColor, PorterDuff.Mode.MULTIPLY);
        intensitySeekBar.getThumb().setColorFilter(initialColor, PorterDuff.Mode.SRC_ATOP);
        intensitySeekBar.setProgress((int) (device.getDeviceState().getColor().getValue() * 100));
    }

    public Button getChangeColorButton() {
        return changeColorButton;
    }

    public SeekBar getIntensitySeekBar() {
        return intensitySeekBar;
    }

    public Device getDevice() {
        return device;
    }

    public GroupViewFragment getGroupViewFragment() {
        return groupViewFragment;
    }
}