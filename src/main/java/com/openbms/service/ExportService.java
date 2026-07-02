package com.openbms.service;

import com.openbms.model.BmsField;
import com.openbms.model.ExportConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportService {

    private final BmsParser bmsParser = new BmsParser();

    public String generateBms(ExportConfig config,
                              int rows,
                              int cols,
                              List<BmsField> fields) {

        return BmsGenerator.generate(
                config.mapName(),
                rows,
                cols,
                fields,
                config.tioapfx(),
                config.ctrl(),
                config.line(),
                config.column(),
                config.includePreview()
        );
    }

    public String generateCopybook(String mapName, List<BmsField> fields) {
        return BmsGenerator.generateCopybook(mapName, fields, true);
    }

    public void exportToFile(String content, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    /** Reads the raw text content of a .bms (or .cpy) file from disk. */
    public String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Parses a previously exported BMS map source back into a grid size and
     * list of fields, ready to load into the designer canvas.
     *
     * @throws IllegalArgumentException if the file doesn't look like a valid
     *                                   BMS map (e.g. no DFHMDF fields found)
     */
    public BmsParser.ParseResult importBms(String bmsSource) {
        return bmsParser.parse(bmsSource);
    }
}
