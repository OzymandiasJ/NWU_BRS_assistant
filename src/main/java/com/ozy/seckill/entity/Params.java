package com.ozy.seckill.entity;

import java.io.Serializable;

public class Params implements Serializable {
    int venuePlanDateId;
    int venueId;
    String venueName;
    int venuePlanId;
    int type;
    Integer campusInfoId;

    public int getVenuePlanDateId() {
        return venuePlanDateId;
    }

    public void setVenuePlanDateId(int venuePlanDateId) {
        this.venuePlanDateId = venuePlanDateId;
    }

    public int getVenueId() {
        return venueId;
    }

    public void setVenueId(int venueId) {
        this.venueId = venueId;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public int getVenuePlanId() {
        return venuePlanId;
    }

    public void setVenuePlanId(int venuePlanId) {
        this.venuePlanId = venuePlanId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Integer getCampusInfoId() {
        return campusInfoId;
    }

    public void setCampusInfoId(Integer campusInfoId) {
        this.campusInfoId = campusInfoId;
    }

    public Params(int venuePlanDateId, int venueId, String venueName, int venuePlanId, int type, Integer campusInfoId) {
        this.venuePlanDateId = venuePlanDateId;
        this.venueId = venueId;
        this.venueName = venueName;
        this.venuePlanId = venuePlanId;
        this.type = type;
        this.campusInfoId = campusInfoId;
    }
}
