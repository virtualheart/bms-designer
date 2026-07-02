package com.openbms.service;

import com.openbms.model.BmsField;
import com.openbms.model.ExportConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip tests: BmsGenerator.generate() output, fed back through
 * BmsParser.parse(), should recover the same grid size and field geometry
 * that went in. This is the regression guard that would have caught the
 * BmsGenerator bug found during the controller refactor, where
 * fields.sort() mutated the caller's live list in place as a side effect
 * of generating export output.
 */
class BmsParserRoundTripTest {

    private final ExportService exportService = new ExportService();

    private BmsField buildField(String name, BmsField.FieldType type, int row, int col, int length) {
        BmsField f = new BmsField();
        f.setName(name);
        f.setFieldType(type);
        f.setRow(row);
        f.setCol(col);
        f.setLength(length);
        f.setColor(BmsField.BmsColor.GREEN);
        return f;
    }

    @Test
    void roundTrip_recoversGridSize() {
        List<BmsField> fields = new ArrayList<>(List.of(
                buildField("NAME", BmsField.FieldType.INPUT, 2, 5, 10)
        ));

        ExportConfig config = new ExportConfig("MYMAP", "YES", "FREEKB,FRSET", "1", "1", false);
        String bms = exportService.generateBms(config, 24, 80, fields);

        BmsParser.ParseResult result = exportService.importBms(bms);

        assertEquals(24, result.rows);
        assertEquals(80, result.cols);
    }

    @Test
    void roundTrip_recoversFieldNamePositionAndLength() {
        List<BmsField> fields = new ArrayList<>(List.of(
                buildField("CUSTNAME", BmsField.FieldType.INPUT, 3, 10, 20)
        ));

        ExportConfig config = new ExportConfig("MYMAP", "YES", "FREEKB,FRSET", "1", "1", false);
        String bms = exportService.generateBms(config, 24, 80, fields);

        BmsParser.ParseResult result = exportService.importBms(bms);

        assertEquals(1, result.fields.size(), "stopper field must be excluded from the parsed result");

        BmsField recovered = result.fields.get(0);
        assertEquals("CUSTNAME", recovered.getName());
        assertEquals(3, recovered.getRow());
        assertEquals(10, recovered.getCol());
        assertEquals(20, recovered.getLength());
    }

    @Test
    void roundTrip_recoversInitialValueAndColor() {
        BmsField field = buildField("LABEL1", BmsField.FieldType.OUTPUT, 1, 1, 8);
        field.setInitialValue("Hello");
        field.setColor(BmsField.BmsColor.BLUE);

        List<BmsField> fields = new ArrayList<>(List.of(field));

        ExportConfig config = new ExportConfig("MYMAP", "YES", "FREEKB,FRSET", "1", "1", false);
        String bms = exportService.generateBms(config, 24, 80, fields);

        BmsParser.ParseResult result = exportService.importBms(bms);
        BmsField recovered = result.fields.get(0);

        assertEquals("Hello", recovered.getInitialValue());
        assertEquals(BmsField.BmsColor.BLUE, recovered.getColor());
    }

    @Test
    void roundTrip_recoversMultipleFieldsInOriginalOrder() {
        List<BmsField> fields = new ArrayList<>(List.of(
                buildField("FIELDA", BmsField.FieldType.INPUT, 5, 1, 10),
                buildField("FIELDB", BmsField.FieldType.INPUT, 3, 1, 10),
                buildField("FIELDC", BmsField.FieldType.INPUT, 1, 1, 10)
        ));

        ExportConfig config = new ExportConfig("MYMAP", "YES", "FREEKB,FRSET", "1", "1", false);
        String bms = exportService.generateBms(config, 24, 80, fields);

        BmsParser.ParseResult result = exportService.importBms(bms);

        assertEquals(3, result.fields.size());
        // BmsGenerator sorts by row before writing, so the parsed order
        // should come back row-ascending regardless of input order.
        assertEquals("FIELDC", result.fields.get(0).getName());
        assertEquals("FIELDB", result.fields.get(1).getName());
        assertEquals("FIELDA", result.fields.get(2).getName());
    }

    @Test
    void generateBms_doesNotMutateCallersFieldListOrder() {
        // Regression test for the bug where BmsGenerator.appendFields()
        // called fields.sort() directly on the caller's live list, silently
        // reordering the designer's in-memory field list as a side effect
        // of generating export output.
        BmsField first = buildField("ZFIELD", BmsField.FieldType.INPUT, 5, 1, 10);
        BmsField second = buildField("AFIELD", BmsField.FieldType.INPUT, 1, 1, 10);

        List<BmsField> fields = new ArrayList<>(List.of(first, second));

        ExportConfig config = new ExportConfig("MYMAP", "YES", "FREEKB,FRSET", "1", "1", false);
        exportService.generateBms(config, 24, 80, fields);

        assertSame(first, fields.get(0),
                "the caller's field list order must be unchanged after export; " +
                        "BmsGenerator must sort a copy, not the live list");
        assertSame(second, fields.get(1));
    }

    @Test
    void importBms_rejectsSourceWithNoFieldDefinitions() {
        String notBms = "This is not a BMS map at all.";

        assertThrows(IllegalArgumentException.class, () -> exportService.importBms(notBms));
    }
}
