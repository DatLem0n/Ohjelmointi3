package com.server;

import org.json.JSONObject;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class Message {
    private String locationName;
    private String locationDescription;
    private String locationCity;

    private String originalPostingTime;

    Message(String locationName, String locationDescription, String locationCity, String originalPostingTime){
        setLocationName(locationName);
        setLocationCity(locationCity);
        setLocationDescription(locationDescription);
        setOriginalPostingTime(originalPostingTime);
    }

    Message(String locationName, String locationDescription, String locationCity, Long unixTime){
        setLocationName(locationName);
        setLocationCity(locationCity);
        setLocationDescription(locationDescription);
        setOriginalPostingTime(unixToDate(unixTime));
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

    public long dateToUnix(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        long unix = 0;
        try {
            LocalDateTime time = LocalDateTime.parse(originalPostingTime, formatter);
            unix = time.toInstant(ZoneOffset.UTC).toEpochMilli();
        }catch (DateTimeException e){
            System.out.println("Error parsing date");
            e.printStackTrace();
        }catch (ArithmeticException e){
            System.out.println("Why are we still using this >:(");
            e.printStackTrace();
        }
        return unix;
    }

    private String unixToDate(Long unixTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.systemDefault());
        Instant instant = Instant.ofEpochMilli(unixTime);
        return formatter.format(instant);
    }


    public JSONObject toJSONObject(){
        JSONObject json = new JSONObject();
        json.put("locationName", locationName);
        json.put("locationDescription", locationDescription);
        json.put("locationCity", locationCity);
        json.put("originalPostingTime", originalPostingTime);
        return json;
    }



}
