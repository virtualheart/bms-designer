package com.openbms.service;

import com.openbms.model.BmsField;
import java.util.List;

/**
 * Centralized validation rules for map names and field names. This is the
 * single source of truth for these rules; other services (e.g. FieldService)
 * delegate here instead of re-implementing the same regexes.
 */
public class ValidationService {

    private static final String MAP_NAME_PATTERN = "^[A-Z@#$][A-Z0-9@#$]{0,6}$";
    private static final String FIELD_NAME_PATTERN = "[A-Za-z@#$][A-Za-z0-9@#$_-]{0,29}";

    public boolean isValidMapName(String name) {
        if (name == null) return false;
        return name.trim().toUpperCase().matches(MAP_NAME_PATTERN);
    }

    public boolean isValidFieldName(String name) {
        if (name == null) return false;
        return name.matches(FIELD_NAME_PATTERN);
    }

    /**
     * Checks whether fieldName is already used by a field other than
     * currentField (pass null for currentField when validating a brand new
     * field with no prior identity).
     */
    public boolean fieldNameExists(String fieldName,
                                   BmsField currentField,
                                   List<BmsField> fields) {

        if (fieldName == null || fields == null) return false;

        for (BmsField f : fields) {
            if (!f.equals(currentField)
                    && f.getName().equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }
}
