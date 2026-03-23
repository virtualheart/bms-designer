package com.openbms.service;

import com.openbms.model.BmsField;
import com.openbms.model.ExportConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportService {

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
}