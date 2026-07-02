package com.openbms.service;

import com.openbms.model.BmsField;
import java.util.List;

/**
 * Field geometry and identity helpers: overlap detection, cloning with a
 * unique name, and name validation delegated to ValidationService (so the
 * validation rules live in exactly one place).
 */
public class FieldService {

    private final ValidationService validationService;

    public FieldService() {
        this(new ValidationService());
    }

    public FieldService(ValidationService validationService) {
        this.validationService = validationService;
    }

    /** Whether field overlaps any other field in the list (same row, overlapping columns). */
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

    /** Creates a copy of f with a guaranteed-unique name (suffix _1, _2, ...). */
    public BmsField cloneField(BmsField f, List<BmsField> allFields) {
        BmsField copy = new BmsField();
        String baseName = f.getName();
        int suffix = 1;
        String newName = baseName + "_" + suffix;

        while (validationService.fieldNameExists(newName, null, allFields)) {
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

    /**
     * Creates an exact copy of f, preserving its name. Used for undo/redo
     * snapshots where identity must be preserved rather than de-duplicated.
     */
    public BmsField copyExact(BmsField f) {
        BmsField copy = new BmsField();
        copy.setName(f.getName());
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

    public boolean fieldNameExists(String fieldName, BmsField currentField, List<BmsField> fields) {
        return validationService.fieldNameExists(fieldName, currentField, fields);
    }

    public boolean isValidFieldName(String name) {
        return validationService.isValidFieldName(name);
    }

    /** Generates the next available auto-name like FIELD1, FIELD2, ... */
    public String generateAutoName(List<BmsField> fields) {
        int index = 1;
        String base = "FIELD";
        while (fieldNameExists(base + index, null, fields)) {
            index++;
        }
        return base + index;
    }
}
