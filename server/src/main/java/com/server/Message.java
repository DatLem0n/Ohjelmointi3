package com.server;

import org.json.JSONObject;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class Message {
    private Integer id;
    private String locationName;
    private String locationDescription;
    private String locationCity;
    private String locationCountry;
    private String locationStreetAddress;
    private String originalPoster;
    private String originalPostingTime;
    private Double latitude;
    private Double longitude;
    private Double weather;
    private Integer timesVisited;

    Message(String locationName, String locationDescription, String locationCity, String locationCountry, String locationStreetAddress,
            String originalPostingTime, String originalPoster, Double latitude, Double longitude, Double weather, Integer timesVisited){
        setLocationName(locationName);
        setLocationCity(locationCity);
        setLocationCountry(locationCountry);
        setLocationStreetAddress(locationStreetAddress);
        setLocationDescription(locationDescription);
        setOriginalPostingTime(originalPostingTime);
        setOriginalPoster(originalPoster);
        setLatitude(latitude);
        setLongitude(longitude);
        setWeather(weather);
        setTimesVisited(timesVisited);
    }

    Message(Integer id, String locationName, String locationDescription, String locationCity, String locationCountry, String locationStreetAddress,
            Long unixTime, String originalPoster, Double latitude, Double longitude, Double weather, Integer timesVisited){
        setId(id);
        setLocationName(locationName);
        setLocationCity(locationCity);
        setLocationCountry(locationCountry);
        setLocationStreetAddress(locationStreetAddress);
        setLocationDescription(locationDescription);
        setOriginalPostingTime(unixToDate(unixTime));
        setOriginalPoster(originalPoster);
        setLatitude(latitude);
        setLongitude(longitude);
        setWeather(weather);
        setTimesVisited(timesVisited);
    }
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    public String getLocationStreetAddress() {
        return locationStreetAddress;
    }

    public void setLocationStreetAddress(String locationStreetAddress) {
        this.locationStreetAddress = locationStreetAddress;
    }

    public String getOriginalPostingTime() {
        return originalPostingTime;
    }

    public void setOriginalPostingTime(String originalPostingTime) {
        this.originalPostingTime = originalPostingTime;
    }

    public String getOriginalPoster() {
        return originalPoster;
    }

    public void setOriginalPoster(String originalPoster) {
        this.originalPoster = originalPoster;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getWeather() {
        return weather;
    }

    public void setWeather(Double weather) {
        this.weather = weather;
    }

    public Integer getTimesVisited() {
        return timesVisited;
    }

    public void setTimesVisited(Integer timesVisited) {
        this.timesVisited = timesVisited;
    }


    /**
     * gets posting time and formats it to unix and returns it
     * @return
     */
    public long getUnixDate(){
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

    /**
     * formats a unix date back to the correct format.
     * @param unixTime
     * @return
     */
    private String unixToDate(Long unixTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));
        Instant instant = Instant.ofEpochMilli(unixTime);
        return formatter.format(instant);
    }

    /**
     * creates a jsonObject from the message.
     * @return
     */
    public JSONObject toJSONObject(){
        JSONObject json = new JSONObject();
        json.put("locationID", id);
        json.put("locationName", locationName);
        json.put("locationDescription", locationDescription);
        json.put("locationCity", locationCity);
        json.put("locationCountry", locationCountry);
        json.put("locationStreetAddress", locationStreetAddress);
        json.put("originalPoster", originalPoster);
        json.put("originalPostingTime", originalPostingTime);
        json.put("timesVisited", timesVisited);

        if (latitude != null){
            json.put("latitude", latitude);
            json.put("longitude", longitude);
        }
        if (weather != null){
            json.put("weather", weather);
        }
        return json;
    }



}
