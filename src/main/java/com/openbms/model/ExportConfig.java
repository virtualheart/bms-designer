package com.openbms.model;

public record ExportConfig(
        String mapName,
        String tioapfx,
        String ctrl,
        String line,
        String column,
        boolean includePreview
) {}