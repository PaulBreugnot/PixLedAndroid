package com.pixled.pixledandroid.deviceGroup.mainActivity;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;
import com.flask.colorpicker.OnColorSelectedListener;
import com.pixled.pixledandroid.R;
import com.pixled.pixledandroid.device.DeviceAdapter;
import com.pixled.pixledandroid.device.DeviceService;
import com.pixled.pixledandroid.device.DeviceViewHolder;
import com.pixled.pixledandroid.deviceGroup.editActivity.DeviceListAdapter;
import com.pixled.pixledandroid.deviceGroup.editActivity.EditGroupActivity;
import com.pixled.pixledandroid.mqtt.MqttAndroidConnectionImpl;
import com.pixled.pixledandroid.utils.DeviceGroupIdPair;
import com.pixled.pixledandroid.utils.ServerConfig;
import com.pixled.pixledandroid.welcome.WelcomeActivity;
import com.pixled.pixledserver.core.color.ColorDto;
import com.pixled.pixledserver.core.device.base.Device;
import com.pixled.pixledserver.core.device.base.DeviceDto;
import com.pixled.pixledserver.core.group.DeviceGroup;
import com.pixled.pixledserver.core.group.DeviceGroupDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class GroupSelectionActivity extends AppCompatActivity {

    private static final String LOG_TAG = "SELECT_GROUP";

    /*
    Pager setup
     */
    private ViewPager groupPager;
    private GroupPagerAdapter groupPagerAdapter;

    /*
    Tabs and toolbar
     */
    private TabLayout tabLayout;
    private Toolbar toolbar;
    private LinearLayout tabView;
    private Button newGroupButton;

    /*
    Change color setup
     */
    private View changeColor;
    private ColorPickerView colorPicker;
    private Button OkButton;
    private Device selectedDevice;
    private DeviceViewHolder selectedDeviceViewHolder;

    // The adapter of RecyclerView displayed above the color picker
    private DeviceAdapter colorChangeDeviceAdapter;

    // Groups list
    private List<DeviceGroup> deviceGroups = new ArrayList<>();
    // Groups index (used to retrieve groups from GroupViewFragments)
    private Map<Integer, DeviceGroup> deviceGroupsIndex = new HashMap<>();

    // Device index
    private Map<Integer, Device> deviceIndex = new HashMap<>();
    // Map device ids to their lightViews
    private Map<DeviceGroupIdPair, DeviceViewHolder> deviceViewsIndex = new HashMap<>();

    // Map group ids to their page fragment
    private Map<Integer, GroupViewFragment> viewFragmentIndex = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up main view
        setContentView(R.layout.group_pager);


        // Set up tool bar
        toolbar = findViewById(R.id.groupToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().show();

        // Instantiate a ViewPager and a PagerAdapter.
        groupPager = findViewById(R.id.group_pager);
        groupPagerAdapter = new GroupPagerAdapter(getSupportFragmentManager(), deviceGroups);
        groupPager.setAdapter(groupPagerAdapter);
        groupPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                groupPagerAdapter.getFragmentPositionsIndex().get(i).updateDeviceGroupState();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        // Give the TabLayout the ViewPager
        tabLayout = findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(groupPager);

        tabView = findViewById(R.id.tabsLinearLayout);

        /*
        Set up change color
         */
        changeColor = findViewById(R.id.change_color);
        colorPicker = findViewById(R.id.color_picker_view);
        colorPicker.addOnColorChangedListener(new OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                // ONLY SET HUE AND SATURATION !
                float[] newHsvColor = new float[3];
                Color.colorToHSV(color, newHsvColor);
                selectedDevice.getDeviceState().getColor().setHue(newHsvColor[0]);
                selectedDevice.getDeviceState().getColor().setSaturation(newHsvColor[1]);

                // Update LightCardView contained in the main view
                selectedDeviceViewHolder
                        .getIntensitySeekBar()
                        .getProgressDrawable()
                        .setColorFilter(
                                selectedDevice.getDeviceState().getColor().getArgb(),
                                PorterDuff.Mode.MULTIPLY);
                selectedDeviceViewHolder
                        .getIntensitySeekBar()
                        .getThumb()
                        .setColorFilter(
                                selectedDevice.getDeviceState().getColor().getArgb(),
                                PorterDuff.Mode.SRC_ATOP);
                selectedDeviceViewHolder
                        .getChangeColorButton()
                        .setBackgroundColor(color);

                // Update the LightCardView alongside the ColorPicker
                colorChangeDeviceAdapter
                        .getDeviceViews().get(0)
                        .getIntensitySeekBar()
                        .getProgressDrawable()
                        .setColorFilter(
                                selectedDevice.getDeviceState().getColor().getArgb(),
                                PorterDuff.Mode.MULTIPLY);
                colorChangeDeviceAdapter
                        .getDeviceViews().get(0)
                        .getIntensitySeekBar().getThumb()
                        .setColorFilter(
                                selectedDevice.getDeviceState().getColor().getArgb(),
                                PorterDuff.Mode.SRC_ATOP);
                colorChangeDeviceAdapter.getDeviceViews().get(0)
                        .getChangeColorButton()
                        .setBackgroundColor(color);
            }
        });
        OkButton = findViewById(R.id.validate_color);
        OkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGroups();
            }
        });

        final DeviceService deviceService = new Retrofit.Builder()
                .baseUrl(ServerConfig.ENDPOINT)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(DeviceService.class);

        colorPicker.addOnColorSelectedListener(new OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                // Publish value
                // (Value update is done by the OnColorChangeListener)
                publishColorChanged(deviceService, selectedDevice);
                for (DeviceGroup dg : selectedDevice.getDeviceGroups()) {
                    DeviceViewHolder deviceViewHolder = deviceViewsIndex.get(new DeviceGroupIdPair(selectedDevice.getId(), dg.getId()));
                    if (deviceViewHolder != null) {
                        deviceViewHolder.updateColorBox();
                    }
                }
            }
        });

        findViewById(R.id.new_group_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchNewGroup();
            }
        });


        // Set up MQTT
        ((MqttAndroidConnectionImpl) WelcomeActivity.mqttAndroidConnection).setGroupSelectionActivity(this);

        fetchGroups();
    }

    private void fetchGroups() {
        // Fetch available rooms for the given building from the server
        GroupService groupService = new Retrofit.Builder()
                .baseUrl(ServerConfig.ENDPOINT)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(GroupService.class);

        groupService.listGroups().enqueue(new Callback<List<DeviceGroupDto>>() {

            @Override
            public void onResponse(Call<List<DeviceGroupDto>> call, Response<List<DeviceGroupDto>> response) {
                List<DeviceGroupDto> listGroups = response.body();

                Log.i("RETROFIT", listGroups.size() + " groups fetched.");
                for (DeviceGroupDto r : listGroups) {
                    Log.i("RETROFIT","Group : " + r.getName());
                    DeviceGroup deviceGroup = new DeviceGroup(r);
                    deviceGroups.add(deviceGroup);
                    deviceGroupsIndex.put(deviceGroup.getId(), deviceGroup);
                }
                groupPagerAdapter.notifyDataSetChanged();
                fetchDevices();
            }

            @Override
            public void onFailure(Call<List<DeviceGroupDto>> call, Throwable t) {
                Log.e("RETROFIT","Retrofit error : " + t);
            }
        });
    }

    private void fetchDevices() {
        DeviceService deviceService = new Retrofit.Builder()
                .baseUrl(ServerConfig.ENDPOINT)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(DeviceService.class);

        deviceService.listDevices().enqueue(new Callback<List<DeviceDto>>() {

            @Override
            public void onResponse(Call<List<DeviceDto>> call, Response<List<DeviceDto>> response) {
                List<DeviceDto> list = response.body();

                for (DeviceDto d : list) {
                    Log.i("RETROFIT","Device : " + d.getId() + " " + d.getName());
                    Device device = d.generateDevice();
                    deviceIndex.put(d.getId(), device);
                    // deviceViewsIndex.put(new DeviceGroupIdPair(device.getId(), ), new ArrayList<>());

                    for (Integer i : d.getDeviceGroups()) {
                        DeviceGroup deviceGroup = deviceGroupsIndex.get(i);
                        device.getDeviceGroups().add(deviceGroup);
                        deviceGroup.getDevices().add(device);

                        GroupViewFragment fragment = viewFragmentIndex.get(deviceGroup.getId());

                        if(fragment != null) {
                            fragment.getDeviceAdapter().notifyDataSetChanged();
                            fragment.updateDeviceNumber();
                        }
                    }
                }


            }

            @Override
            public void onFailure(Call<List<DeviceDto>> call, Throwable t) {
                Log.e("RETROFIT","Retrofit error : " + t);
            }
        });
    }

    public static void publishColorChanged(DeviceService deviceService, Device selectedDevice) {
        int color = selectedDevice.getDeviceState().getColor().getArgb();
        Log.i("COLOR PICKER",
                "Color changed : " + Integer.toString(color) + "(" +
                        ((color >> 16) & 0xff) + ", " +
                        ((color >> 8) & 0xff) + ", " +
                        (color & 0xff) + ")");

        ColorDto colorDto = new ColorDto(selectedDevice.getDeviceState().getColor());
        deviceService.changeDeviceColor(selectedDevice.getId(),
                "application/json;charset=UTF-8",
                colorDto)
                .enqueue(new Callback<DeviceDto>() {
                    @Override
                    public void onResponse(Call<DeviceDto> call, Response<DeviceDto> response) {
                        Log.i("COLOR PICKER", "Change color request OK");
                    }

                    @Override
                    public void onFailure(Call<DeviceDto> call, Throwable t) {
                        Log.i("COLOR PICKER", "Change color request failed." + t);
                    }
                });
    }

    public void showChangeColor(Device device, DeviceViewHolder deviceViewHolder) {
        // Hide the pager view, show the color change view (called from "changeColor buttons" in
        // LightViewHolders.
        selectedDevice = device;
        selectedDeviceViewHolder = deviceViewHolder;
        // The recycler view (aka a list) in which lights will be displayed

        final RecyclerView recyclerView = findViewById(R.id.selected_device_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Device> deviceList = new ArrayList<>();
        deviceList.add(device);
        colorChangeDeviceAdapter = new DeviceAdapter(null, deviceList, this, false);
        recyclerView.setAdapter(colorChangeDeviceAdapter);

        // getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        groupPager.setVisibility(View.INVISIBLE);
        tabView.setVisibility(View.INVISIBLE);
        changeColor.setVisibility(View.VISIBLE);
        toolbar.setTitle("Change Device Color");

        // Only hue and saturation are taken into account by the color picker
        float[] hsv = {
                device.getDeviceState().getColor().getHue(),
                device.getDeviceState().getColor().getSaturation(),
                1};
        int fullColor = Color.HSVToColor(hsv);
        colorPicker.setInitialColor(fullColor, false);

    }

    public void showGroups() {
        // Synchronized the original light card view with the one that was displayed with the color
        // picker.
        // getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (DeviceGroup deviceGroup : selectedDeviceViewHolder.getDevice().getDeviceGroups()) {
            viewFragmentIndex.get(deviceGroup.getId()).getDeviceAdapter().notifyItemChanged(selectedDeviceViewHolder.getAdapterPosition());
        }
        // Hide the change color view, show the pager view
        changeColor.setVisibility(View.INVISIBLE);
        groupPager.setVisibility(View.VISIBLE);
        tabView.setVisibility(View.VISIBLE);
        toolbar.setTitle("Available Devices");
    }

    private void launchNewGroup() {
        Intent intent = new Intent(this, EditGroupActivity.class);
        startActivity(intent);
    }

    public PagerAdapter getGroupPagerAdapter() {
        return groupPagerAdapter;
    }

    public List<DeviceGroup> getDeviceGroups() {
        return deviceGroups;
    }

    public Map<Integer, Device> getDeviceIndex() {
        return deviceIndex;
    }

    public Map<DeviceGroupIdPair, DeviceViewHolder> getDeviceViewsIndex() {
        return deviceViewsIndex;
    }

    public Map<Integer, DeviceGroup> getDeviceGroupsIndex() {
        return deviceGroupsIndex;
    }

    public Map<Integer, GroupViewFragment> getViewFragmentIndex() {
        return viewFragmentIndex;
    }
}
