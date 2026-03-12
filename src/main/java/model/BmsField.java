package model;

// import javafx.scene.paint.Color;

public class BmsField {

    public enum FieldType {
        INPUT, OUTPUT, INOUT
    }

    public enum Protection {
        ASKIP, PROT, UNPROT, NUM
    }

    public enum Intensity {
        BRT, NORM, DRK
    }

    public enum Highlight {
        NONE, BLINK, REVERSE, UNDERLINE
    }

    public enum BmsColor {
        BLUE,
        GREEN,
        NEUTRAL,
        PINK,
        RED,
        TURQUOISE,
        YELLOW
    }

    private String name;
    private FieldType fieldType;
    private int row;
    private int col;
    private int length;
    // private Color color;
    private BmsColor color;
    private BmsColor bgColor;
    private Protection protection;
    private Intensity intensity;
    private String initialValue = "";
    private Highlight highlight = Highlight.NONE;


    private String attrb;  // e.g., "UNPROT,NORM" or "PROT,NORM"
    public String getAttrb() { return attrb; }

    public BmsField() {
        this.name = "FIELD";
        this.fieldType = FieldType.INPUT;
        this.row = 1;
        this.col = 1;
        this.length = 10;
        this.color = BmsColor.GREEN;
        this.bgColor = BmsColor.NEUTRAL;
        this.protection = Protection.UNPROT;
        this.intensity = Intensity.NORM;
    }

    // ===== Getters & Setters =====

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            this.name = name.toUpperCase();
        }
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
        autoResolveProtection();
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = Math.max(1, row);
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = Math.max(1, col);
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        if (length > 0) {
            this.length = length;
        }
    }

    public BmsColor getColor() {
        return color;
    }

    public void setColor(BmsColor color) {
        this.color = color;
    }

    public BmsColor getBgColor() {
        return bgColor;
    }

    public void setBgColor(BmsColor bgColor) {
        this.bgColor = bgColor;
    }

    public Protection getProtection() {
        return protection;
    }

    public void setProtection(Protection protection) {
        this.protection = protection;
    }

    public Intensity getIntensity() {
        return intensity;
    }

    public void setIntensity(Intensity intensity) {
        this.intensity = intensity;
    }

    public String getInitialValue() { 
        return initialValue; 
    }

    public void setInitialValue(String value) {
        this.initialValue = value; 
    }

    public Highlight getHighlight() {
        return highlight;
    }

    public void setHighlight(Highlight highlight) {
        this.highlight = highlight;
    }

    // ===== Business Logic =====
    
    private void autoResolveProtection() {
        if (fieldType == null) return;

        switch (fieldType) {
            case INPUT:
                this.protection = Protection.UNPROT;
                break;
            // case OUTPUT:
            //     this.protection = Protection.ASKIP;
            //     break;
            case OUTPUT:
                this.protection = Protection.PROT;
                break;
            case INOUT:
                this.protection = Protection.UNPROT;
                break;
        }
    }

    public String buildAttrbString() {
        StringBuilder sb = new StringBuilder();

        if (protection != null) {
            sb.append(protection.name());
        }

        if (intensity != null) {
            sb.append(",").append(intensity.name());
        }

        return sb.toString();
    }

    public String buildColor() {
        if (color == null || color == BmsColor.NEUTRAL)
            return null;
        return color.name();
    }

    public String buildHighlight() {
        if (highlight == null || highlight == Highlight.NONE)
            return null;
        return highlight.name();
    }
    public String getBmsColor() {
        return color != null ? color.name() : "NEUTRAL";
    }

    public boolean isLikelyLabel() {

        if (fieldType != FieldType.OUTPUT)
            return false;

        if (initialValue == null || initialValue.isBlank())
            return false;

        return true;
    }
}