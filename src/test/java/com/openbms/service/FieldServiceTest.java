package com.openbms.service;

import com.openbms.model.BmsField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldServiceTest {

    private final FieldService fieldService = new FieldService();

    private BmsField fieldAt(int row, int col, int length) {
        BmsField f = new BmsField();
        f.setRow(row);
        f.setCol(col);
        f.setLength(length);
        return f;
    }

    @Test
    void overlaps_detectsSameRowOverlappingColumns() {
        BmsField a = fieldAt(1, 1, 10);
        BmsField b = fieldAt(1, 5, 10);

        assertTrue(fieldService.overlaps(b, List.of(a)),
                "fields sharing row 1 with overlapping column ranges should be detected as overlapping");
    }

    @Test
    void overlaps_doesNotFlagAdjacentNonOverlappingFields() {
        BmsField a = fieldAt(1, 1, 5);   // occupies columns 1-5
        BmsField b = fieldAt(1, 6, 5);   // occupies columns 6-10, touching but not overlapping

        assertFalse(fieldService.overlaps(b, List.of(a)),
                "fields that are adjacent but not overlapping in column range should not be flagged");
    }

    @Test
    void overlaps_ignoresFieldsOnDifferentRows() {
        BmsField a = fieldAt(1, 1, 10);
        BmsField b = fieldAt(2, 1, 10);

        assertFalse(fieldService.overlaps(b, List.of(a)),
                "fields on different rows should never be considered overlapping regardless of columns");
    }

    @Test
    void overlaps_excludesFieldFromComparingAgainstItself() {
        BmsField a = fieldAt(1, 1, 10);
        List<BmsField> fields = new ArrayList<>(List.of(a));

        assertFalse(fieldService.overlaps(a, fields),
                "a field must not be reported as overlapping itself when checked against a list containing it");
    }

    @Test
    void cloneField_assignsUniqueNameWithSuffix() {
        BmsField original = new BmsField();
        original.setName("CUSTNAME");

        List<BmsField> fields = new ArrayList<>(List.of(original));

        BmsField clone = fieldService.cloneField(original, fields);

        assertEquals("CUSTNAME_1", clone.getName(),
                "first clone of a field should get a _1 suffix");
        assertNotSame(original, clone, "clone must be a distinct object, not the same reference");
    }

    @Test
    void cloneField_incrementsSuffixWhenFirstSuffixTaken() {
        BmsField original = new BmsField();
        original.setName("CUSTNAME");

        BmsField existingClone = new BmsField();
        existingClone.setName("CUSTNAME_1");

        List<BmsField> fields = new ArrayList<>(List.of(original, existingClone));

        BmsField secondClone = fieldService.cloneField(original, fields);

        assertEquals("CUSTNAME_2", secondClone.getName(),
                "when _1 is already taken, the next available suffix should be used");
    }

    @Test
    void cloneField_copiesAllGeometryAndAppearance() {
        BmsField original = new BmsField();
        original.setName("SRC");
        original.setRow(5);
        original.setCol(10);
        original.setLength(7);
        original.setColor(BmsField.BmsColor.RED);
        original.setBgColor(BmsField.BmsColor.BLUE);
        original.setInitialValue("HELLO");

        BmsField clone = fieldService.cloneField(original, new ArrayList<>(List.of(original)));

        assertEquals(original.getRow(), clone.getRow());
        assertEquals(original.getCol(), clone.getCol());
        assertEquals(original.getLength(), clone.getLength());
        assertEquals(original.getColor(), clone.getColor());
        assertEquals(original.getBgColor(), clone.getBgColor());
        assertEquals(original.getInitialValue(), clone.getInitialValue());
    }

    @Test
    void copyExact_preservesOriginalNameUnlikeCloneField() {
        BmsField original = new BmsField();
        original.setName("KEEPME");

        BmsField copy = fieldService.copyExact(original);

        assertEquals("KEEPME", copy.getName(),
                "copyExact must preserve the exact name, since it's used for undo/redo snapshots " +
                        "where identity must be preserved rather than de-duplicated");
    }

    @Test
    void generateAutoName_startsAtOneWhenListEmpty() {
        String name = fieldService.generateAutoName(new ArrayList<>());
        assertEquals("FIELD1", name);
    }

    @Test
    void generateAutoName_skipsExistingNames() {
        BmsField existing = new BmsField();
        existing.setName("FIELD1");

        String name = fieldService.generateAutoName(new ArrayList<>(List.of(existing)));

        assertEquals("FIELD2", name,
                "auto-naming must skip names already in use, " +
                        "regression test for a bug where a pre-constructed BmsField passed " +
                        "to the dialog caused it to think it was editing rather than adding, " +
                        "silently skipping auto-naming entirely");
    }

    @Test
    void fieldNameExists_isCaseInsensitive() {
        BmsField existing = new BmsField();
        existing.setName("CUSTNAME");

        assertTrue(fieldService.fieldNameExists("custname", null, List.of(existing)));
    }

    @Test
    void fieldNameExists_excludesCurrentFieldFromCollisionCheck() {
        BmsField field = new BmsField();
        field.setName("CUSTNAME");

        assertFalse(fieldService.fieldNameExists("CUSTNAME", field, List.of(field)),
                "a field must not collide with its own current name when editing");
    }
}
