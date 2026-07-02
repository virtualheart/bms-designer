package com.openbms.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * User-configurable defaults that apply across the designer: the
 * color/intensity new fields start with, the grid snap increment used while
 * dragging, and which color palette the CICS emulator renders with. Backed
 * by JavaFX properties so controls (ChoiceBox, Spinner, ToggleGroup) can
 * bind directly without extra glue code, and so changes are immediately
 * visible to anything already listening (e.g. the emulator re-rendering
 * with a new palette without needing to be reopened).
 */
public class AppSettings {

    public enum EmulatorPalette {
        GREEN_ON_BLACK,
        AMBER_ON_BLACK,
        WHITE_ON_BLACK,
        DARK_ON_LIGHT
    }

    private final ObjectProperty<BmsField.BmsColor> defaultColor =
            new SimpleObjectProperty<>(BmsField.BmsColor.GREEN);

    private final ObjectProperty<BmsField.Intensity> defaultIntensity =
            new SimpleObjectProperty<>(BmsField.Intensity.NORM);

    private final IntegerProperty snapSize = new SimpleIntegerProperty(1);

    private final ObjectProperty<EmulatorPalette> emulatorPalette =
            new SimpleObjectProperty<>(EmulatorPalette.GREEN_ON_BLACK);

    public ObjectProperty<BmsField.BmsColor> defaultColorProperty() {
        return defaultColor;
    }

    public BmsField.BmsColor getDefaultColor() {
        return defaultColor.get();
    }

    public void setDefaultColor(BmsField.BmsColor color) {
        defaultColor.set(color != null ? color : BmsField.BmsColor.GREEN);
    }

    public ObjectProperty<BmsField.Intensity> defaultIntensityProperty() {
        return defaultIntensity;
    }

    public BmsField.Intensity getDefaultIntensity() {
        return defaultIntensity.get();
    }

    public void setDefaultIntensity(BmsField.Intensity intensity) {
        defaultIntensity.set(intensity != null ? intensity : BmsField.Intensity.NORM);
    }

    public IntegerProperty snapSizeProperty() {
        return snapSize;
    }

    public int getSnapSize() {
        return snapSize.get();
    }

    public void setSnapSize(int size) {
        snapSize.set(Math.max(1, size));
    }

    public ObjectProperty<EmulatorPalette> emulatorPaletteProperty() {
        return emulatorPalette;
    }

    public EmulatorPalette getEmulatorPalette() {
        return emulatorPalette.get();
    }

    public void setEmulatorPalette(EmulatorPalette palette) {
        emulatorPalette.set(palette != null ? palette : EmulatorPalette.GREEN_ON_BLACK);
    }
}
