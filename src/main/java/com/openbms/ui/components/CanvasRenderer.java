package com.openbms.ui.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.openbms.model.BmsField;

import java.util.List;

public class CanvasRenderer {

    private final int cellWidth;
    private final int cellHeight;

    public CanvasRenderer(int cellWidth, int cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    public int getCellWidth()  { return cellWidth; }
    public int getCellHeight() { return cellHeight; }

    // Grid
    public void drawGrid(Canvas canvas, int rows, int cols) {

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.web("#0C0C0C"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.web("#003300"));

        for (int r = 0; r <= rows; r++) {
            gc.strokeLine(0, r * cellHeight,
                    cols * cellWidth,
                    r * cellHeight);
        }

        for (int c = 0; c <= cols; c++) {
            gc.strokeLine(c * cellWidth, 0,
                    c * cellWidth,
                    rows * cellHeight);
        }
    }

    // Fields
    public void drawFields(Canvas canvas,
                           List<BmsField> fields,
                           int rows,
                           int cols,
                           BmsField selectedField) {

        drawGrid(canvas, rows, cols);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));

        for (BmsField f : fields) {

            int x = (f.getCol() - 1) * cellWidth;
            int y = (f.getRow() - 1) * cellHeight;
            int w = f.getLength() * cellWidth;

            gc.setFill(toFxColor(f.getBgColor()));
            gc.fillRect(x, y, w, cellHeight);

            gc.setFill(toFxColor(f.getColor()));
            gc.fillText(f.getInitialValue(), x + 2, y + cellHeight - 5);

            // Resize handle
            gc.setFill(Color.RED);
            gc.fillRect(x + w - 4, y, 4, cellHeight);

            if (f == selectedField) {
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(1);
                gc.strokeRect(x, y, w, cellHeight);

                gc.setFill(Color.rgb(255, 255, 0, 0.25));
                gc.fillRect(x, y, w, cellHeight);
            }
        }
    }

    // Hit-test
    public boolean isInside(BmsField f, double mouseX, double mouseY) {
        int x = (f.getCol() - 1) * cellWidth;
        int y = (f.getRow() - 1) * cellHeight;
        int w = f.getLength() * cellWidth;
        return mouseX >= x && mouseX <= x + w
                && mouseY >= y && mouseY <= y + cellHeight;
    }

    // Overlap
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

    // Color Mapping
    private Color toFxColor(BmsField.BmsColor c) {
        return switch (c) {
            case RED      -> Color.RED;
            case GREEN    -> Color.LIME;
            case BLUE     -> Color.BLUE;
            case YELLOW   -> Color.YELLOW;
            case TURQUOISE -> Color.CYAN;
            case PINK     -> Color.HOTPINK;
            case NEUTRAL  -> Color.WHITE;
        };
    }
}
