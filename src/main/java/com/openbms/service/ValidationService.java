package com.openbms.service;

import com.openbms.model.BmsField;
import java.util.List;

public class ValidationService {

    public boolean isValidMapName(String name) {

        if (name == null) return false;

        name = name.trim().toUpperCase();

        return name.matches("^[A-Z@#$][A-Z0-9@#$]{0,6}$");
    }

    public boolean isValidFieldName(String name) {

        if (name == null) return false;

        return name.matches("[A-Za-z@#$][A-Za-z0-9@#$-_]{0,29}");
    }

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