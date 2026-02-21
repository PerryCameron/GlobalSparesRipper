package com.l2.statictools;

import javafx.scene.image.Image;

import java.util.Objects;

public class ImageResources {
    public static final Image GSLOGO16;
    public static final Image GSLOGO24;
    public static final Image GSLOGO64;


    static {
        try {
            GSLOGO16 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-16.png"),
                    "Failed to load resource: /images/GSRipper-16.png"
            ));
            GSLOGO24 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-24.png"),
                    "Failed to load resource: /images/GSRipper-24.png"
            ));
            GSLOGO64 = new Image(Objects.requireNonNull(
                    ImageResources.class.getResourceAsStream("/images/GSRipper-64.png"),
                    "Failed to load resource: /images/GSRipper-64.png"
            ));

        } catch (NullPointerException e) {
            throw new IllegalStateException("Failed to initialize ImageResources due to missing resource", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize ImageResources", e);
        }
    }

}
