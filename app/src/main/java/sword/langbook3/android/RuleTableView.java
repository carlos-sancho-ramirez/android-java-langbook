package sword.langbook3.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class RuleTableView extends View {

    private int _horizontalSpacing = 40;
    private int _textSize = 30;
    private Paint _textPaint;

    private int _columnCount;
    private String[] _texts;

    private float[] _columnWidths;

    public RuleTableView(Context context) {
        super(context);
        init();
    }

    public RuleTableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RuleTableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(_textSize);
        paint.setColor(0xFFCC3333);
        _textPaint = paint;
    }

    @Override
    public void onDraw(Canvas canvas) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        final int length = _texts.length;
        final int yStep = _textSize;
        final int startXPos = 0;
        int yPos = _textSize;
        int xPos = startXPos;
        int column = 0;

        for (int i = 0; i < length; i++) {
            String text = _texts[i];
            if (text != null) {
                canvas.drawText(text, xPos, yPos, _textPaint);
            }

            xPos += _columnWidths[column++] + _horizontalSpacing;
            if (column >= _columnCount && xPos > width) {
                i += _columnCount - column;
                xPos = startXPos;
                yPos += yStep;
                column = 0;

                if (yPos > height + yStep) {
                    break;
                }
            }
        }
    }

    public void setValues(int columnCount, String[] texts) {
        _columnCount = columnCount;
        _texts = texts;

        final float[] columnWidths = new float[columnCount];
        final int length = texts.length;
        for (int i = 0; i < length; i++) {
            String text = texts[i];
            if (text != null) {
                float width = _textPaint.measureText(text);
                final int column = i % columnCount;
                if (width > columnWidths[column]) {
                    columnWidths[column] = width;
                }
            }
        }

        _columnWidths = columnWidths;

        invalidate();
    }
}
