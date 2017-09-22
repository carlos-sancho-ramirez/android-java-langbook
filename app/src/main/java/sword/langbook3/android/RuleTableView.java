package sword.langbook3.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class RuleTableView extends View {

    private int _textSize = 30;
    private Paint _textPaint;

    private int _columnCount;
    private String[] _texts;

    private float _maxTextWidth;
    private int _visibleColumns;

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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RuleTableView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (_maxTextWidth > 0.01f) {
            final int width = right - left;
            int visibleColumns = (int) (width / _maxTextWidth);

            if (visibleColumns == 0) {
                visibleColumns = 1;
            }

            if (visibleColumns > _columnCount) {
                visibleColumns = _columnCount;
            }

            _visibleColumns = visibleColumns;
        }
        else {
            _visibleColumns = (_columnCount == 0)? 0 : 1;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();
        final int columnWidth = width / _visibleColumns;

        final int length = _texts.length;
        final int yStep = _textSize;
        final int startXPos = 0;
        int yPos = _textSize;
        int xPos = startXPos;
        int column = 0;

        for (int i = 0; i < length; i++) {
            canvas.drawText(_texts[i], xPos, yPos, _textPaint);
            if (++column >= _columnCount) {
                column = 0;
                xPos = startXPos;
                yPos += yStep;

                if (yPos > height + yStep) {
                    break;
                }
            }
            else {
                xPos += columnWidth;
            }
        }
    }

    public void setValues(int columnCount, String[] texts) {
        _columnCount = columnCount;
        _texts = texts;

        float maxTextWidth = 0;
        final int length = texts.length;
        for (int i = 0; i < length; i++) {
            String text = texts[i];
            if (text != null) {
                float width = _textPaint.measureText(text);
                if (width > maxTextWidth) {
                    maxTextWidth = width;
                }
            }
        }
        _maxTextWidth = maxTextWidth;

        invalidate();
    }
}
