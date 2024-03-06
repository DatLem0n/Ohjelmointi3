package com.server;

import java.util.ArrayList;

public class Tour {
    private String tourName;
    private String tourDescription;
    private ArrayList<Integer> locationIDs;


    Tour(String tourName, String tourDescription, ArrayList<Integer> locationIDs){
        setTourName(tourName);
        setTourDescription(tourDescription);
        setLocationIDs(locationIDs);
    }

    public void setLocationIDs(ArrayList<Integer> locationIDs) {
        this.locationIDs = locationIDs;
    }
    public ArrayList<Integer> getLocationIDs(){
        return locationIDs;
    }

    public String getTourName() {
        return tourName;
    }
    public void setTourName(String tourName) {
        this.tourName = tourName;
    }
    public String getTourDescription() {
        return tourDescription;
    }
    public void setTourDescription(String tourDescription) {
        this.tourDescription = tourDescription;
    }

    /**
     * creates a string from the locationID arraylist, separated by commas
     * @return
     */
    public String locationIDsToString() {
        StringBuilder str = new StringBuilder();
        for (Integer id : locationIDs) {
            str.append(id);
            str.append(',');
        }
        return str.toString();
    }

}
