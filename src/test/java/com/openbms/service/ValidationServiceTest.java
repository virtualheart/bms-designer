package com.openbms.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private final ValidationService validationService = new ValidationService();

    @Test
    void isValidMapName_acceptsSimpleAlphaName() {
        assertTrue(validationService.isValidMapName("MYMAP"));
    }

    @Test
    void isValidMapName_acceptsLowercaseByUppercasingFirst() {
        assertTrue(validationService.isValidMapName("mymap"),
                "map names are normalized to uppercase before validation");
    }

    @Test
    void isValidMapName_rejectsNull() {
        assertFalse(validationService.isValidMapName(null));
    }

    @Test
    void isValidMapName_rejectsNamesLongerThanSevenCharacters() {
        // BMS map names are limited to 8 chars total; first char + up to 7 more = 8
        assertFalse(validationService.isValidMapName("TOOLONGNAME"));
    }

    @Test
    void isValidMapName_rejectsNameStartingWithDigit() {
        assertFalse(validationService.isValidMapName("1MAP"));
    }

    @Test
    void isValidMapName_acceptsNameStartingWithSpecialChar() {
        assertTrue(validationService.isValidMapName("@MAP"));
    }

    @Test
    void isValidFieldName_acceptsAlphaNumericWithUnderscoreAndHyphen() {
        assertTrue(validationService.isValidFieldName("FIELD_NAME-1"));
    }

    @Test
    void isValidFieldName_rejectsNull() {
        assertFalse(validationService.isValidFieldName(null));
    }

    @Test
    void isValidFieldName_rejectsNameStartingWithDigit() {
        assertFalse(validationService.isValidFieldName("1FIELD"));
    }

    @Test
    void isValidFieldName_rejectsNameLongerThanThirtyChars() {
        String tooLong = "A" + "B".repeat(30); // 31 chars total
        assertFalse(validationService.isValidFieldName(tooLong));
    }

    @Test
    void fieldNameExists_returnsFalseForNullInputs() {
        assertFalse(validationService.fieldNameExists(null, null, null));
    }
}
