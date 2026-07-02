package com.openbms.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BmsFieldTest {

    @Test
    void setFieldType_input_resolvesToUnprotected() {
        BmsField f = new BmsField();
        f.setFieldType(BmsField.FieldType.INPUT);
        assertEquals(BmsField.Protection.UNPROT, f.getProtection());
    }

    @Test
    void setFieldType_output_resolvesToProtected() {
        BmsField f = new BmsField();
        f.setFieldType(BmsField.FieldType.OUTPUT);
        assertEquals(BmsField.Protection.PROT, f.getProtection());
    }

    @Test
    void setFieldType_inout_resolvesToUnprotected() {
        BmsField f = new BmsField();
        f.setFieldType(BmsField.FieldType.INOUT);
        assertEquals(BmsField.Protection.UNPROT, f.getProtection());
    }

    @Test
    void isEntryField_trueForUnprotected() {
        BmsField f = new BmsField();
        f.setProtection(BmsField.Protection.UNPROT);
        assertTrue(f.isEntryField());
    }

    @Test
    void isEntryField_trueForNumeric() {
        BmsField f = new BmsField();
        f.setProtection(BmsField.Protection.NUM);
        assertTrue(f.isEntryField());
    }

    @Test
    void isEntryField_falseForProtected() {
        BmsField f = new BmsField();
        f.setProtection(BmsField.Protection.PROT);
        assertFalse(f.isEntryField(),
                "protected fields are display-only and must not be reachable by Tab " +
                        "navigation in the CICS emulator");
    }

    @Test
    void buildAttrbString_combinesProtectionAndIntensity() {
        BmsField f = new BmsField();
        f.setProtection(BmsField.Protection.UNPROT);
        f.setIntensity(BmsField.Intensity.BRT);

        assertEquals("UNPROT,BRT", f.buildAttrbString());
    }

    @Test
    void buildColor_returnsNullForNeutral() {
        BmsField f = new BmsField();
        f.setColor(BmsField.BmsColor.NEUTRAL);
        assertNull(f.buildColor(), "NEUTRAL is the BMS default and should be omitted from generated output");
    }

    @Test
    void buildColor_returnsNameForNonNeutralColor() {
        BmsField f = new BmsField();
        f.setColor(BmsField.BmsColor.RED);
        assertEquals("RED", f.buildColor());
    }

    @Test
    void setName_alwaysUppercases() {
        BmsField f = new BmsField();
        f.setName("lowername");
        assertEquals("LOWERNAME", f.getName());
    }

    @Test
    void setRow_clampsToMinimumOne() {
        BmsField f = new BmsField();
        f.setRow(-5);
        assertEquals(1, f.getRow());
    }

    @Test
    void setLength_ignoresNonPositiveValues() {
        BmsField f = new BmsField();
        f.setLength(10);
        f.setLength(0);
        assertEquals(10, f.getLength(), "setLength must reject non-positive values and keep the prior length");
    }
}
