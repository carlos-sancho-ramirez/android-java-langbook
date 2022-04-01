package sword.langbook3.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

import static sword.langbook3.android.CharacterCompositionDefinitionDrawable.FIRST_COLOR;
import static sword.langbook3.android.CharacterCompositionDefinitionDrawable.SECOND_COLOR;
import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;

public final class CharacterCompositionDefinitionEditorView extends View {

    private static final int LEGEND_HORIZONTAL_PADDING_DP = 16;
    private static final int LEGEND_VERTICAL_PADDING_DP = 16;
    private static final int LEGEND_RADIO_BUTTON_WIDTH_DP = 32;
    private static final int LEGEND_RADIO_BUTTON_HEIGHT_DP = 32;
    private static final int LEGEND_RADIO_BUTTON_VERTICAL_SPACING_DP = 16;
    private static final int LEGEND_SELECTOR_BORDER_WIDTH = 4;
    private static final int LEGEND_SELECTOR_BORDER_MARGIN = 8;
    private static final int LEGEND_SELECTOR_COLOR = 0xCC000000;

    private static final int DRAG_AREA_PADDING_DP = 32;
    private static final int DRAG_AREA_MINIMUM_WIDTH_DP = 120;
    private static final int DRAG_AREA_MINIMUM_HEIGHT_DP = 120;
    private static final int DRAGGABLE_PADDING_DP = 4;
    private static final int DRAGGABLE_SIDE_DP = 24;

    private static final int DRAGGABLE_COLOR = 0xFFEE9900;

    private int _measuredLegendAreaWidth;
    private int _measuredLegendAreaHeight;
    private int _measuredDragAreaWidth;
    private int _measuredDragAreaHeight;

    private final Rect _rect = new Rect();
    private final Rect _legendFirstRect = new Rect();
    private final Rect _legendSecondRect = new Rect();

    private final Paint _firstRadioButtonPaint = new Paint();
    private final Paint _secondRadioButtonPaint = new Paint();
    private final Paint _legendSelectionPaint = new Paint();
    private final Paint _draggablePaint = new Paint();
    private final Paint _firstPaint = new Paint();
    private final Paint _secondPaint = new Paint();
    private final DisplayMetrics _displayMetrics;

    private int _legendLeft;
    private int _legendTop;
    private int _dragLeft;
    private int _dragTop;

    private boolean _secondSelected;
    private Area _first;
    private Area _second;

    private boolean _legendFirstDownDetected;
    private boolean _legendSecondDownDetected;

    public CharacterCompositionDefinitionEditorView(Context context) {
        super(context);
        _displayMetrics = context.getResources().getDisplayMetrics();
        init();
    }

    public CharacterCompositionDefinitionEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        _displayMetrics = context.getResources().getDisplayMetrics();
        init();
    }

    public CharacterCompositionDefinitionEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _displayMetrics = context.getResources().getDisplayMetrics();
        init();
    }

    @TargetApi(21)
    public CharacterCompositionDefinitionEditorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        _displayMetrics = context.getResources().getDisplayMetrics();
        init();
    }

    private void setRegisterArea(CharacterCompositionDefinitionArea givenArea, Area area) {
        area.x = givenArea.x;
        area.y = givenArea.y;
        area.width = givenArea.width;
        area.height = givenArea.height;
    }

    public void setRegister(CharacterCompositionDefinitionRegister register) {
        if (_first == null) {
            _first = new Area();
            _second = new Area();
        }

        setRegisterArea(register.first, _first);
        setRegisterArea(register.second, _second);
    }

    private float dipToPx(float dipMeasure) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipMeasure, _displayMetrics);
    }

    private void init() {
        _measuredLegendAreaWidth = (int) dipToPx(LEGEND_HORIZONTAL_PADDING_DP * 2 + LEGEND_RADIO_BUTTON_WIDTH_DP);
        _measuredLegendAreaHeight = (int) dipToPx(LEGEND_VERTICAL_PADDING_DP * 2 + LEGEND_RADIO_BUTTON_WIDTH_DP * 2 + LEGEND_RADIO_BUTTON_VERTICAL_SPACING_DP);
        _measuredDragAreaWidth = (int) dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP + DRAG_AREA_PADDING_DP * 2);
        _measuredDragAreaHeight = (int) dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP + DRAG_AREA_PADDING_DP * 2);

        _firstRadioButtonPaint.setColor(FIRST_COLOR);
        _firstRadioButtonPaint.setStyle(Paint.Style.FILL);

        _secondRadioButtonPaint.setColor(SECOND_COLOR);
        _secondRadioButtonPaint.setStyle(Paint.Style.FILL);

        _legendSelectionPaint.setColor(LEGEND_SELECTOR_COLOR);
        _legendSelectionPaint.setStyle(Paint.Style.STROKE);
        _legendSelectionPaint.setStrokeWidth(LEGEND_SELECTOR_BORDER_WIDTH);

        _firstPaint.setColor(FIRST_COLOR);
        _firstPaint.setStyle(Paint.Style.FILL);

        _secondPaint.setColor(SECOND_COLOR);
        _secondPaint.setStyle(Paint.Style.FILL);

        _draggablePaint.setColor(DRAGGABLE_COLOR);
        _draggablePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int measuredWidth = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        int measuredHeight = 0;
        switch (heightSpecMode) {
            case MeasureSpec.UNSPECIFIED:
                measuredHeight = (int) Math.max(_measuredLegendAreaHeight, _measuredDragAreaHeight);
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                measuredHeight = heightSpecSize;
                break;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            final int width = right - left;
            final int height = bottom - top;

            final int requiredWidth = _measuredLegendAreaWidth + _measuredDragAreaWidth;
            _legendLeft = (requiredWidth >= width)? 0 : (width - requiredWidth) / 2;
            _legendTop = (_measuredLegendAreaHeight >= height)? 0 : (height - _measuredLegendAreaHeight) / 2;

            _dragLeft = _legendLeft + _measuredLegendAreaWidth;
            _dragTop = (_measuredDragAreaHeight >= height)? 0 : (height - _measuredDragAreaHeight) / 2;

            final int legendBothLeft = _legendLeft + (int) dipToPx(LEGEND_HORIZONTAL_PADDING_DP);
            final int legendBothRight = legendBothLeft + (int) dipToPx(LEGEND_RADIO_BUTTON_WIDTH_DP);
            final int legendFirstTop = _legendTop + (int) dipToPx(LEGEND_VERTICAL_PADDING_DP);
            final int legendFirstBottom = legendFirstTop + (int) dipToPx(LEGEND_RADIO_BUTTON_HEIGHT_DP);
            _legendFirstRect.set(legendBothLeft, legendFirstTop, legendBothRight, legendFirstBottom);

            final int legendSecondTop = legendFirstBottom + (int) dipToPx(LEGEND_RADIO_BUTTON_VERTICAL_SPACING_DP);
            final int legendSecondBottom = legendSecondTop + (int) dipToPx(LEGEND_RADIO_BUTTON_HEIGHT_DP);
            _legendSecondRect.set(legendBothLeft, legendSecondTop, legendBothRight, legendSecondBottom);
        }
    }

    private void drawLegend(Canvas canvas) {
        canvas.drawRect(_legendFirstRect, _firstRadioButtonPaint);
        canvas.drawRect(_legendSecondRect, _secondRadioButtonPaint);

        final Rect refRect = _secondSelected? _legendSecondRect : _legendFirstRect;
        _rect.set(refRect.left - LEGEND_SELECTOR_BORDER_MARGIN, refRect.top - LEGEND_SELECTOR_BORDER_MARGIN,
                    refRect.right + LEGEND_SELECTOR_BORDER_MARGIN, refRect.bottom + LEGEND_SELECTOR_BORDER_MARGIN);
        canvas.drawRect(_rect, _legendSelectionPaint);
    }

    private void drawArea(Area area, Canvas canvas, Paint paint) {
        final int padding = (int) dipToPx(DRAG_AREA_PADDING_DP);
        final int width = (int) dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP);
        final int height = (int) dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP);

        final int x = _dragLeft + padding + (area.x * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int y = _dragTop + padding + (area.y * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int right = _dragLeft + padding + ((area.x + area.width) * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int bottom = _dragTop + padding + ((area.y + area.height) * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        _rect.set(x, y, right, bottom);
        canvas.drawRect(_rect, paint);
    }

    private void drawDraggableCorners(Area area, Canvas canvas) {
        final int padding = (int) dipToPx(DRAG_AREA_PADDING_DP);
        final int width = (int) dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP);
        final int height = (int) dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP);

        final int left = _dragLeft + padding + (area.x * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int top = _dragTop + padding + (area.y * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int right = _dragLeft + padding + ((area.x + area.width) * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int bottom = _dragTop + padding + ((area.y + area.height) * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;

        final int margin = (int) dipToPx(DRAGGABLE_PADDING_DP);
        final int side = (int) dipToPx(DRAGGABLE_SIDE_DP);

        _rect.set(left - margin - side, top - margin - side, left - margin, top - margin);
        canvas.drawRect(_rect, _draggablePaint);

        _rect.set(right + margin, top - margin - side, right + margin + side, top - margin);
        canvas.drawRect(_rect, _draggablePaint);

        _rect.set(right + margin, bottom + margin, right + margin + side, bottom + margin + side);
        canvas.drawRect(_rect, _draggablePaint);

        _rect.set(left - margin - side, bottom + margin, left - margin, bottom + margin + side);
        canvas.drawRect(_rect, _draggablePaint);
    }

    private void drawDragArea(@NonNull Canvas canvas) {
        if (_first != null && _second != null) {
            drawArea(_first, canvas, _firstPaint);
            drawArea(_second, canvas, _secondPaint);
            drawDraggableCorners(_secondSelected? _second : _first, canvas);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawLegend(canvas);
        drawDragArea(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            final float x = event.getX();
            final float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (_legendFirstRect.contains((int) x, (int) y)) {
                        _legendFirstDownDetected = true;
                        _legendSecondDownDetected = false;
                    }
                    else if (_legendSecondRect.contains((int) x, (int) y)) {
                        _legendFirstDownDetected = false;
                        _legendSecondDownDetected = true;
                    }
                    else {
                        _legendFirstDownDetected = false;
                        _legendSecondDownDetected = false;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (_legendFirstDownDetected && _legendFirstRect.contains((int) x, (int) y)) {
                        if (_secondSelected) {
                            _secondSelected = false;
                            invalidate();
                        }
                    }
                    else if (_legendSecondDownDetected && _legendSecondRect.contains((int) x, (int) y)) {
                        if (!_secondSelected) {
                            _secondSelected = true;
                            invalidate();
                        }
                    }

                    _legendFirstDownDetected = false;
                    _legendSecondDownDetected = false;
                    break;
            }
        }

        return true;
    }

    private static final class Area {
        int x;
        int y;
        int width;
        int height;
    }
}
