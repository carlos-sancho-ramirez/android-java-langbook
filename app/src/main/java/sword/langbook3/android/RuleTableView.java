package sword.langbook3.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class RuleTableView extends View implements Runnable {

    private static final int TEXT_SIZE_SP = 24;

    private int _horizontalSpacing = 40;
    private int _verticalSpacing = 10;
    private int _textSize;
    private Paint _textPaint;

    private int _columnCount;
    private String[] _texts;

    private float[] _columnWidths;
    private float _desiredTableWidth;
    private float _desiredTableHeight;

    public RuleTableView(Context context) {
        super(context);
        init(context);
    }

    public RuleTableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RuleTableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        _textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP, metrics);

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
        final int yStep = _textSize + _verticalSpacing;
        final float startXPos = -_diffX;
        float yPos = (_textSize * 4) / 5 - _diffY;
        float xPos = startXPos;
        int column = 0;

        for (int i = 0; i < length; i++) {
            String text = _texts[i];
            if (text != null) {
                canvas.drawText(text, xPos, yPos, _textPaint);
            }

            xPos += _columnWidths[column++] + _horizontalSpacing;
            if (column >= _columnCount || xPos > width) {
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

        float desiredTableWidth = 0;
        for (float columnWidth : columnWidths) {
            desiredTableWidth += columnWidth;
        }
        if (columnCount > 1) {
            desiredTableWidth += (columnCount - 1) * _horizontalSpacing;
        }

        final int rowCount = (texts.length + columnCount - 1) / columnCount;

        float desiredTableHeight = rowCount * _textSize;
        if (rowCount > 1) {
            desiredTableHeight += (rowCount - 1) * _verticalSpacing;
        }

        _columnWidths = columnWidths;
        _desiredTableWidth = desiredTableWidth;
        _desiredTableHeight = desiredTableHeight;

        invalidate();
    }

    private boolean _trackingTouch;
    private float _touchDownX;
    private float _touchDownY;

    private static final int DRAG_SAMPLES = 4;
    private static final float _speedThreshold = 0.2f; // pixels/millisecond
    private static final float _speedDeceleration = 0.1f; // pixels/millisecond
    private static final long _speedUpdateTimeInterval = 5L; // milliseconds

    private final float[] _lastDragX = new float[DRAG_SAMPLES];
    private final float[] _lastDragY = new float[DRAG_SAMPLES];
    private final long[] _lastDragTime = new long[DRAG_SAMPLES];
    private int _lastDragIndex = 0;
    private boolean _lastDragBufferCompleted;

    private float _diffX;
    private float _diffY;
    private float _speedX;
    private float _speedY;
    private float _accX;
    private float _accY;

    private float getDiff(float current, float touchDown, float acc, float max) {
        float diff = acc + touchDown - current;

        if (diff < 0) {
            diff = 0;
        }

        if (diff > max) {
            diff = max;
        }

        return diff;
    }

    private void applyMove(float x, float y) {
        final float viewWidth = getWidth();
        final float viewHeight = getHeight();

        final float maxDiffX = _desiredTableWidth - viewWidth;
        boolean shouldInvalidate = false;
        if (maxDiffX > 0) {
            float diffX = getDiff(x, _touchDownX, _accX, maxDiffX);
            if (_diffX != diffX) {
                shouldInvalidate = true;
                _diffX = diffX;
            }
        }

        final float maxDiffY = _desiredTableHeight - viewHeight;
        if (maxDiffY > 0) {
            float diffY = getDiff(y, _touchDownY, _accY, maxDiffY);
            if (_diffY != diffY) {
                shouldInvalidate = true;
                _diffY = diffY;
            }
        }

        if (shouldInvalidate) {
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                _accX = _diffX;
                _accY = _diffY;

                _touchDownX = x;
                _touchDownY = y;

                _lastDragBufferCompleted = false;
                _lastDragIndex = 0;
                _lastDragX[0] = x;
                _lastDragY[0] = y;
                _lastDragTime[0] = System.currentTimeMillis();

                removeCallbacks(this);
                _trackingTouch = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (++_lastDragIndex == DRAG_SAMPLES) {
                    _lastDragBufferCompleted = true;
                    _lastDragIndex = 0;
                }

                _lastDragX[_lastDragIndex] = x;
                _lastDragY[_lastDragIndex] = y;
                _lastDragTime[_lastDragIndex] = System.currentTimeMillis();
                applyMove(x, y);
                break;

            case MotionEvent.ACTION_UP:
                if (++_lastDragIndex == DRAG_SAMPLES) {
                    _lastDragBufferCompleted = true;
                    _lastDragIndex = 0;
                }

                _lastDragX[_lastDragIndex] = x;
                _lastDragY[_lastDragIndex] = y;
                _lastDragTime[_lastDragIndex] = System.currentTimeMillis();
                applyMove(x, y);

                _speedX = getSpeed(_lastDragX);
                _speedY = getSpeed(_lastDragY);
                if (Math.abs(_speedX) > _speedThreshold || Math.abs(_speedY) > _speedThreshold) {
                    postDelayed(this, _speedUpdateTimeInterval);
                }

                _trackingTouch = false;
                break;
        }

        return true;
    }

    private float getSpeed(float[] positions) {
        if (_lastDragBufferCompleted) {
            final int firstIndex = (_lastDragIndex + 1 == DRAG_SAMPLES)? 0 : _lastDragIndex + 1;
            final float lastPosition = positions[_lastDragIndex];
            final long lastTime = _lastDragTime[_lastDragIndex];
            final float firstPosition = positions[firstIndex];
            final long firstTime = _lastDragTime[firstIndex];

            final float speed = (lastPosition - firstPosition) / (lastTime - firstTime);
            if (Math.abs(speed) > _speedThreshold) {
                return speed;
            }
        }

        return 0;
    }

    @Override
    public void run() {
        if (!_trackingTouch && (Math.abs(_speedX) > _speedThreshold || Math.abs(_speedY) > _speedThreshold)) {
            final long currentTime = System.currentTimeMillis();
            final long interval = currentTime - _lastDragTime[_lastDragIndex];
            final float x = _lastDragX[_lastDragIndex] + _speedX * interval;
            final float y = _lastDragY[_lastDragIndex] + _speedY * interval;

            if (++_lastDragIndex == DRAG_SAMPLES) {
                _lastDragIndex = 0;
            }
            _lastDragX[_lastDragIndex] = x;
            _lastDragY[_lastDragIndex] = y;
            _lastDragTime[_lastDragIndex] = currentTime;
            applyMove(x, y);

            if (_speedX > _speedThreshold) {
                if (_speedX > _speedDeceleration) {
                    _speedX -= _speedDeceleration;
                }
                else {
                    _speedX = 0;
                }
            }
            else if (_speedX < -_speedThreshold) {
                if (_speedX < _speedDeceleration) {
                    _speedX += _speedDeceleration;
                }
                else {
                    _speedX = 0;
                }
            }

            if (_speedY > _speedThreshold) {
                if (_speedY > _speedDeceleration) {
                    _speedY -= _speedDeceleration;
                }
                else {
                    _speedY = 0;
                }
            }
            else if (_speedY < -_speedThreshold) {
                if (_speedY < _speedDeceleration) {
                    _speedY += _speedDeceleration;
                }
                else {
                    _speedY = 0;
                }
            }

            if (Math.abs(_speedX) > _speedThreshold || Math.abs(_speedY) > _speedThreshold) {
                postDelayed(this, _speedUpdateTimeInterval);
            }
        }
    }
}
