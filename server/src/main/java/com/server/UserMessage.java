package com.server;

import org.json.JSONObject;

public class UserMessage {
    private String locationName;
    private String locationDescription;
    private String locationCity;

    private String originalPostingTime;

    UserMessage(String locationName, String locationDescription, String locationCity, String originalPostingTime){
        setLocationName(locationName);
        setLocationCity(locationCity);
        setLocationDescription(locationDescription);
        setOriginalPostingTime(originalPostingTime);
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public void setLocationCity(String locationCity) {
        this.locationCity = locationCity;
    }

    public String getOriginalPostingTime() {
        return originalPostingTime;
    }

    public void setOriginalPostingTime(String originalPostingTime) {
        this.originalPostingTime = originalPostingTime;
    }

    // checks if originalPostingTime is in correct format


    public JSONObject toJSONObject(){
        JSONObject json = new JSONObject();
        json.put("locationName", locationName);
        json.put("locationDescription", locationDescription);
        json.put("locationCity", locationCity);
        json.put("originalPostingTime", originalPostingTime);
        return json;
    }



}
