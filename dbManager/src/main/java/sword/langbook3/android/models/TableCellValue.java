package sword.langbook3.android.models;

public final class TableCellValue {
    public final int staticAcceptation;
    public final int dynamicAcceptation;
    public final String text;

    public TableCellValue(int staticAcceptation, int dynamicAcceptation, String text) {
        this.staticAcceptation = staticAcceptation;
        this.dynamicAcceptation = dynamicAcceptation;
        this.text = text;
    }
}
