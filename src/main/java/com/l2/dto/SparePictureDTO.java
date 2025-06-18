package com.l2.dto;

public class SparePictureDTO {

    private Long id;
    private Long spareId;
    private String spareName;
    private byte[] picture;

    // No-args constructor
    public SparePictureDTO() {
    }

    // Constructor with required fields
    public SparePictureDTO(Long spareId, String spareName, byte[] picture) {
        this.spareId = spareId;
        this.spareName = spareName;
        this.picture = picture;
    }

    public SparePictureDTO(Long id, Long spareId, String spareName, byte[] picture) {
        this.id = id;
        this.spareId = spareId;
        this.spareName = spareName;
        this.picture = picture;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSpareId() {
        return spareId;
    }

    public void setSpareId(Long spareId) {
        this.spareId = spareId;
    }

    public byte[] getPicture() {
        return picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }

    public String getSpareName() {
        return spareName;
    }

    public void setSpareName(String spareName) {
        this.spareName = spareName;
    }
}