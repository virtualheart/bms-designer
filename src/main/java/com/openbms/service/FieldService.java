package com.openbms.service;

import com.openbms.model.BmsField;
import java.util.List;

public class FieldService {

    // Check if a field overlaps any field in the list 
    public boolean overlaps(BmsField field, List<BmsField> fields) {
        for (BmsField f : fields) {
            if (f == field) continue;

            if (f.getRow() == field.getRow()) {
                int start1 = f.getCol();
                int end1 = f.getCol() + f.getLength() - 1;

                int start2 = field.getCol();
                int end2 = field.getCol() + field.getLength() - 1;

                if (start1 <= end2 && start2 <= end1)
                    return true;
            }
        }
        return false;
    }

    public BmsField cloneField(BmsField f, List<BmsField> allFields) {
        BmsField copy = new BmsField();
        String baseName = f.getName();
        int suffix = 1;
        String newName = baseName + "_" + suffix;

        while (fieldNameExists(newName, null, allFields)) {
            suffix++;
            newName = baseName + "_" + suffix;
        }

        copy.setName(newName);
        copy.setFieldType(f.getFieldType());
        copy.setLength(f.getLength());
        copy.setColor(f.getColor());
        copy.setBgColor(f.getBgColor());
        copy.setProtection(f.getProtection());
        copy.setIntensity(f.getIntensity());
        copy.setInitialValue(f.getInitialValue());
        copy.setRow(f.getRow());
        copy.setCol(f.getCol());
        return copy;
    }

    // Check if a field name already exists in the list 
    public boolean fieldNameExists(String fieldName,
                                   BmsField currentField,
                                   List<BmsField> fields) {
        for (BmsField f : fields) {
            if (!f.equals(currentField) &&
                f.getName().equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }

    // Validate a field name 
    public boolean isValidFieldName(String name) {
        return name != null && name.matches("[A-Za-z@#$][A-Za-z0-9@#$-_]{0,29}");
    }

}