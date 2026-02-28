package com.l2.statictools;

import javafx.scene.image.Image;

import java.util.Objects;

public class ImageResources {
    public static final Image GSLOGO16;
    public static final Image GSLOGO20;
    public static final Image GSLOGO24;
    public static final Image GSLOGO30;
    public static final Image GSLOGO32;
    public static final Image GSLOGO36;
    public static final Image GSLOGO48;
    public static final Image GSLOGO64;
    public static final Image GSLOGO80;
    public static final Image GSLOGO96;
    public static final Image GSLOGO128;
    public static final Image GSLOGO256;
    public static final Image YES;
    public static final Image NO;


    static {
        try {
            GSLOGO16 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-16.png"),
                    "Failed to load resource: /images/GSRipper-16.png"
            ));
            GSLOGO20 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-20.png"),
                    "Failed to load resource: /images/GSRipper-16.png"
            ));
            GSLOGO24 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-24.png"),
                    "Failed to load resource: /images/GSRipper-24.png"
            ));
            GSLOGO30 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-30.png"),
                    "Failed to load resource: /images/GSRipper-30.png"
            ));
            GSLOGO32 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-32.png"),
                    "Failed to load resource: /images/GSRipper-32.png"
            ));
            GSLOGO36 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-36.png"),
                    "Failed to load resource: /images/GSRipper-36.png"
            ));
            GSLOGO48 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-48.png"),
                    "Failed to load resource: /images/GSRipper-48.png"
            ));
            GSLOGO64 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-64.png"),
                    "Failed to load resource: /images/GSRipper-64.png"
            ));
            GSLOGO80 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-80.png"),
                    "Failed to load resource: /images/GSRipper-80.png"
            ));
            GSLOGO96 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-96.png"),
                    "Failed to load resource: /images/GSRipper-96.png"
            ));
            GSLOGO128 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-128.png"),
                    "Failed to load resource: /images/GSRipper-128.png"
            ));
            GSLOGO256 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-256.png"),
                    "Failed to load resource: /images/GSRipper-256.png"
            ));
            YES = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/yes-16.png"),
                    "Failed to load resource: /images/yes-16.png"
            ));
            NO = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/no-16.png"),
                    "Failed to load resource: /images/no-16.png"
            ));

        } catch (NullPointerException e) {
            throw new IllegalStateException("Failed to initialize ImageResources due to missing resource", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize ImageResources", e);
        }
    }

}
