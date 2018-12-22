package com.example.paulbreugnot.lightroom.building;
import com.example.paulbreugnot.lightroom.room.Room;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
public interface BuildingService {

    //String ENDPOINT = "http://192.168.12.1:8080/api/";
    // String ENDPOINT = "http://10.0.2.2:8080/api/";
    // String ENDPOINT = "http://10.103.1.211:8080/api/";
    String ENDPOINT = "http://192.168.1.124:8080/api/";

    @GET("buildings")
    Call<List<Building>> listBuildings();

    @GET("buildings/{buildingId}/rooms")
    Call<List<Room>> listBuildingRooms(@Path("buildingId") long id);

}