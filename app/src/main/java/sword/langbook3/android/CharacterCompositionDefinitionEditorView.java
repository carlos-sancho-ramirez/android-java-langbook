package sword.langbook3.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionAreaInterface;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

import static sword.langbook3.android.CharacterCompositionDefinitionDrawable.BACKGROUND_COLOR;
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
    private final Rect _draggableCornerLeftTopRect = new Rect();
    private final Rect _draggableCornerRightTopRect = new Rect();
    private final Rect _draggableCornerRightBottomRect = new Rect();
    private final Rect _draggableCornerLeftBottomRect = new Rect();
    private final Rect _dragAreaBackRect = new Rect();

    private final Paint _firstRadioButtonPaint = new Paint();
    private final Paint _secondRadioButtonPaint = new Paint();
    private final Paint _legendSelectionPaint = new Paint();
    private final Paint _draggablePaint = new Paint();
    private final Paint _firstPaint = new Paint();
    private final Paint _secondPaint = new Paint();
    private final Paint _dragAreaBackPaint = new Paint();
    private final DisplayMetrics _displayMetrics;

    private int _legendLeft;
    private int _legendTop;
    private int _dragLeft;
    private int _dragTop;

    private boolean _secondSelected;
    private Area _first;
    private Area _second;
    private OnAreaChanged _firstListener;
    private OnAreaChanged _secondListener;

    interface DownActions {
        int NONE = 0;
        int DRAG_AREA = 1;
        int LEGEND_FIRST = 2;
        int LEGEND_SECOND = 3;

        int DRAG_CORNER_OP_MASK = 4;
        int DRAG_CORNER_LEFT_TOP = 4;
        int DRAG_CORNER_RIGHT_TOP = 5;
        int DRAG_CORNER_LEFT_BOTTOM = 6;
        int DRAG_CORNER_RIGHT_BOTTOM = 7;
    }
    private int _downAction;
    private float _downX;
    private float _downY;
    private final Area _downArea = new Area();

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

    @NonNull
    public CharacterCompositionDefinitionAreaInterface getFirstArea() {
        ensureAvailableAreas();
        return _first;
    }

    @NonNull
    public CharacterCompositionDefinitionAreaInterface getSecondArea() {
        ensureAvailableAreas();
        return _second;
    }

    private void setRegisterArea(CharacterCompositionDefinitionArea givenArea, Area area) {
        area.x = givenArea.x;
        area.y = givenArea.y;
        area.width = givenArea.width;
        area.height = givenArea.height;
    }

    private void ensureAvailableAreas() {
        if (_first == null) {
            _first = new Area();
            _second = new Area();
        }
    }

    public void setFirstRegisterArea(CharacterCompositionDefinitionArea area) {
        ensureAvailableAreas();
        setRegisterArea(area, _first);
        if (_firstListener != null) {
            _firstListener.onAreaChanged(_first);
        }

        if (!_secondSelected) {
            workOutDraggableCorners(_first);
        }
        invalidate();
    }

    public void setSecondRegisterArea(CharacterCompositionDefinitionArea area) {
        ensureAvailableAreas();
        setRegisterArea(area, _second);
        if (_secondListener != null) {
            _secondListener.onAreaChanged(_second);
        }

        if (_secondSelected) {
            workOutDraggableCorners(_second);
        }
        invalidate();
    }

    public void setRegister(CharacterCompositionDefinitionRegister register) {
        ensureAvailableAreas();
        setRegisterArea(register.first, _first);
        setRegisterArea(register.second, _second);

        if (_firstListener != null) {
            _firstListener.onAreaChanged(_first);
        }

        if (_secondListener != null) {
            _secondListener.onAreaChanged(_second);
        }
        invalidate();
    }

    private float dipToPx(float dipMeasure) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipMeasure, _displayMetrics);
    }

    private void init() {
        _measuredLegendAreaWidth = (int) dipToPx(LEGEND_HORIZONTAL_PADDING_DP * 2 + LEGEND_RADIO_BUTTON_WIDTH_DP);
        _measuredLegendAreaHeight = (int) dipToPx(LEGEND_VERTICAL_PADDING_DP * 2 + LEGEND_RADIO_BUTTON_HEIGHT_DP * 2 + LEGEND_RADIO_BUTTON_VERTICAL_SPACING_DP);
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

        _dragAreaBackPaint.setColor(BACKGROUND_COLOR);
        _dragAreaBackPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int measuredWidth = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        int measuredHeight = 0;
        switch (heightSpecMode) {
            case MeasureSpec.UNSPECIFIED:
                measuredHeight = Math.max(_measuredLegendAreaHeight, _measuredDragAreaHeight);
                break;
            case MeasureSpec.AT_MOST:
                measuredHeight = Math.min(heightSpecSize, Math.max(_measuredLegendAreaHeight, _measuredDragAreaHeight));
                break;
            case MeasureSpec.EXACTLY:
                measuredHeight = heightSpecSize;
                break;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private void workOutDraggableCorners(Area area) {
        final int padding = (int) dipToPx(DRAG_AREA_PADDING_DP);
        final int width = (int) dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP);
        final int height = (int) dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP);

        final int left = _dragLeft + padding + (area.x * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int top = _dragTop + padding + (area.y * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int right = _dragLeft + padding + ((area.x + area.width) * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int bottom = _dragTop + padding + ((area.y + area.height) * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;

        final int margin = (int) dipToPx(DRAGGABLE_PADDING_DP);
        final int side = (int) dipToPx(DRAGGABLE_SIDE_DP);

        _draggableCornerLeftTopRect.set(left - margin - side, top - margin - side, left - margin, top - margin);
        _draggableCornerRightTopRect.set(right + margin, top - margin - side, right + margin + side, top - margin);
        _draggableCornerRightBottomRect.set(right + margin, bottom + margin, right + margin + side, bottom + margin + side);
        _draggableCornerLeftBottomRect.set(left - margin - side, bottom + margin, left - margin, bottom + margin + side);
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

            final int dragAreaPadding = (int) dipToPx(DRAG_AREA_PADDING_DP);
            final int dragAreaWidth = (int) dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP);
            final int dragAreaHeight = (int) dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP);

            _dragAreaBackRect.set(_dragLeft + dragAreaPadding, _dragTop + dragAreaPadding, _dragLeft + dragAreaPadding + dragAreaWidth, _dragTop + dragAreaPadding + dragAreaHeight);
            workOutDraggableCorners(_secondSelected? _second : _first);
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

    private void drawDragArea(@NonNull Canvas canvas) {
        canvas.drawRect(_dragAreaBackRect, _dragAreaBackPaint);

        if (_first != null && _second != null) {
            drawArea(_first, canvas, _firstPaint);
            drawArea(_second, canvas, _secondPaint);
            canvas.drawRect(_draggableCornerLeftTopRect, _draggablePaint);
            canvas.drawRect(_draggableCornerRightTopRect, _draggablePaint);
            canvas.drawRect(_draggableCornerRightBottomRect, _draggablePaint);
            canvas.drawRect(_draggableCornerLeftBottomRect, _draggablePaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawLegend(canvas);
        drawDragArea(canvas);
    }

    private boolean inCurrentArea(float x, float y) {
        final int padding = (int) dipToPx(DRAG_AREA_PADDING_DP);
        final int width = (int) dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP);
        final int height = (int) dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP);
        final Area area = _secondSelected? _second : _first;

        return (int) x >= _dragLeft + padding + (area.x * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT &&
                (int) x <= _dragLeft + padding + ((area.x + area.width) * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT &&
                (int) y >= _dragTop + padding + (area.y * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT &&
                (int) y <= _dragTop + padding + ((area.y + area.height) * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            final float x = event.getX();
            final float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (_legendFirstRect.contains((int) x, (int) y)) {
                        _downAction = DownActions.LEGEND_FIRST;
                    }
                    else if (_legendSecondRect.contains((int) x, (int) y)) {
                        _downAction = DownActions.LEGEND_SECOND;
                    }
                    else if (_draggableCornerLeftTopRect.contains((int) x, (int) y)) {
                        _downAction = DownActions.DRAG_CORNER_LEFT_TOP;
                        _downX = x;
                        _downY = y;
                        _downArea.copyValuesFrom(_secondSelected? _second : _first);
                    }
                    else if (_draggableCornerRightTopRect.contains((int) x, (int) y)) {
                        _downAction = DownActions.DRAG_CORNER_RIGHT_TOP;
                        _downX = x;
                        _downY = y;
                        _downArea.copyValuesFrom(_secondSelected? _second : _first);
                    }
                    else if (_draggableCornerRightBottomRect.contains((int) x, (int) y)) {
                        _downAction = DownActions.DRAG_CORNER_RIGHT_BOTTOM;
                        _downX = x;
                        _downY = y;
                        _downArea.copyValuesFrom(_secondSelected? _second : _first);
                    }
                    else if (_draggableCornerLeftBottomRect.contains((int) x, (int) y)) {
                        _downAction = DownActions.DRAG_CORNER_LEFT_BOTTOM;
                        _downX = x;
                        _downY = y;
                        _downArea.copyValuesFrom(_secondSelected? _second : _first);
                    }
                    else if (inCurrentArea(x, y)) {
                        _downAction = DownActions.DRAG_AREA;
                        _downX = x;
                        _downY = y;
                        _downArea.copyValuesFrom(_secondSelected? _second : _first);
                    }
                    else {
                        _downAction = DownActions.NONE;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (_downAction == DownActions.LEGEND_FIRST && _legendFirstRect.contains((int) x, (int) y)) {
                        if (_secondSelected) {
                            _secondSelected = false;
                            workOutDraggableCorners(_first);
                            invalidate();
                        }
                    }
                    else if (_downAction == DownActions.LEGEND_SECOND && _legendSecondRect.contains((int) x, (int) y)) {
                        if (!_secondSelected) {
                            _secondSelected = true;
                            workOutDraggableCorners(_second);
                            invalidate();
                        }
                    }

                    _downAction = DownActions.NONE;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if ((_downAction & DownActions.DRAG_CORNER_OP_MASK) != 0) {
                        final float realDiffX = event.getX() - _downX;
                        final float realDiffY = event.getY() - _downY;
                        final Area area = _secondSelected? _second : _first;

                        if (_downAction == DownActions.DRAG_CORNER_LEFT_TOP) {
                            final int calculatedX = _downArea.x + (int) ((realDiffX * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP));
                            area.setValidXAndWidth(calculatedX, _downArea);

                            final int calculatedY = _downArea.y + (int) ((realDiffY * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP));
                            area.setValidYAndHeight(calculatedY, _downArea);
                        }
                        else if (_downAction == DownActions.DRAG_CORNER_LEFT_BOTTOM) {
                            final int calculatedX = _downArea.x + (int) ((realDiffX * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP));
                            area.setValidXAndWidth(calculatedX, _downArea);

                            area.y = _downArea.y;
                            area.height = _downArea.height + (int) ((realDiffY * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP));
                            area.adjustToValidHeight();
                        }
                        else if (_downAction == DownActions.DRAG_CORNER_RIGHT_TOP) {
                            area.x = _downArea.x;
                            area.width = _downArea.width + (int) ((realDiffX * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP));
                            area.adjustToValidWidth();

                            final int calculatedY = _downArea.y + (int) ((realDiffY * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP));
                            area.setValidYAndHeight(calculatedY, _downArea);
                        }
                        else if (_downAction == DownActions.DRAG_CORNER_RIGHT_BOTTOM) {
                            area.x = _downArea.x;
                            area.width = _downArea.width + (int) ((realDiffX * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP));
                            area.adjustToValidWidth();

                            area.y = _downArea.y;
                            area.height = _downArea.height + (int) ((realDiffY * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP));
                            area.adjustToValidHeight();
                        }

                        workOutDraggableCorners(area);
                        triggerOnAreaChanged();
                        invalidate();
                    }
                    else if (_downAction == DownActions.DRAG_AREA) {
                        final float realDiffX = event.getX() - _downX;
                        final float realDiffY = event.getY() - _downY;
                        final Area area = _secondSelected? _second : _first;

                        final int calculatedX = _downArea.x + (int) ((realDiffX * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_WIDTH_DP));
                        final int calculatedY = _downArea.y + (int) ((realDiffY * CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) / dipToPx(DRAG_AREA_MINIMUM_HEIGHT_DP));
                        area.setValidXAndY(calculatedX, calculatedY, _downArea);

                        workOutDraggableCorners(area);
                        triggerOnAreaChanged();
                        invalidate();
                    }

                    break;
            }
        }

        return true;
    }

    public interface OnAreaChanged {
        void onAreaChanged(@NonNull CharacterCompositionDefinitionAreaInterface area);
    }

    public void setOnFirstAreaChanged(OnAreaChanged listener) {
        _firstListener = listener;
    }

    public void setOnSecondAreaChanged(OnAreaChanged listener) {
        _secondListener = listener;
    }

    private void triggerOnAreaChanged() {
        if (_secondSelected && _second != null && _secondListener != null) {
            _secondListener.onAreaChanged(_second);
        }
        else if (!_secondSelected && _first != null && _firstListener != null) {
            _firstListener.onAreaChanged(_first);
        }
    }

    private static final class Area implements CharacterCompositionDefinitionAreaInterface {
        int x;
        int y;
        int width = CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        int height = CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;

        void copyValuesFrom(Area area) {
            x = area.x;
            y = area.y;
            width = area.width;
            height = area.height;
        }

        void adjustToValidWidth() {
            if (width <= 0) {
                width = 1;
            }
            else if (width > CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - x) {
                width = CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - x;
            }
        }

        void adjustToValidHeight() {
            if (height <= 0) {
                height = 1;
            }
            else if (height > CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - y) {
                height = CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - y;
            }
        }

        public void setValidXAndWidth(int calculatedX, Area oldArea) {
            if (calculatedX < 0) {
                x = 0;
                width = oldArea.x + oldArea.width;
            }
            else if (calculatedX >= oldArea.x + oldArea.width) {
                x = oldArea.x + oldArea.width - 1;
                width = 1;
            }
            else {
                x = calculatedX;
                width = oldArea.x + oldArea.width - x;
            }
        }

        public void setValidYAndHeight(int calculatedY, Area oldArea) {
            if (calculatedY < 0) {
                y = 0;
                height = oldArea.y + oldArea.height;
            }
            else if (calculatedY >= oldArea.y + oldArea.height) {
                y = oldArea.y + oldArea.height - 1;
                height = 1;
            }
            else {
                y = calculatedY;
                height = oldArea.y + oldArea.height - y;
            }
        }

        public void setValidXAndY(int calculatedX, int calculatedY, Area downArea) {
            if (calculatedX < 0) {
                x = 0;
            }
            else if (calculatedX > CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - downArea.width) {
                x = CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - downArea.width;
            }
            else {
                x = calculatedX;
            }

            if (calculatedY < 0) {
                y = 0;
            }
            else if (calculatedY > CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - downArea.height) {
                y = CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - downArea.height;
            }
            else {
                y = calculatedY;
            }

            width = downArea.width;
            height = downArea.height;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    private static CharacterCompositionDefinitionArea toImmutableArea(Area area) {
        return (area == null)? null : CharacterCompositionDefinitionArea.cloneFrom(area);
    }

    private static Area toMutableArea(CharacterCompositionDefinitionArea area) {
        if (area == null) {
            return null;
        }

        final Area result = new Area();
        result.x = area.x;
        result.y = area.y;
        result.width = area.width;
        result.height = area.height;
        return result;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            final SavedState savedState = (SavedState) state;
            super.onRestoreInstanceState(savedState._superState);
            _secondSelected = savedState._secondSelected;
            _first = toMutableArea(savedState._first);
            _second = toMutableArea(savedState._second);

            if (_firstListener != null && _first != null) {
                _firstListener.onAreaChanged(_first);
            }

            if (_secondListener != null && _second != null) {
                _secondListener.onAreaChanged(_second);
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, _secondSelected, toImmutableArea(_first), toImmutableArea(_second));
    }

    public static final class SavedState implements Parcelable {

        final Parcelable _superState;
        final boolean _secondSelected;
        final CharacterCompositionDefinitionArea _first;
        final CharacterCompositionDefinitionArea _second;

        SavedState(Parcelable superState, boolean secondSelected, CharacterCompositionDefinitionArea first, CharacterCompositionDefinitionArea second) {
            _superState = superState;
            _secondSelected = secondSelected;
            _first = first;
            _second = second;
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            private CharacterCompositionDefinitionArea readArea(Parcel in) {
                final int x = in.readInt();
                final int y = in.readInt();
                final int width = in.readInt();
                final int height = in.readInt();
                return new CharacterCompositionDefinitionArea(x, y, width, height);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                final Parcelable superState = in.readParcelable(getClass().getClassLoader());
                final int intFlags = in.readInt();
                final boolean secondSelected = (intFlags & 1) != 0;
                final CharacterCompositionDefinitionArea first = ((intFlags & 2) != 0)? readArea(in) : null;
                final CharacterCompositionDefinitionArea second = ((intFlags & 4) != 0)? readArea(in) : null;
                return new SavedState(superState, secondSelected, first, second);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        private static void writeAreaToParcelIfNotNull(Parcel dest, CharacterCompositionDefinitionArea area) {
            if (area != null) {
                dest.writeInt(area.x);
                dest.writeInt(area.y);
                dest.writeInt(area.width);
                dest.writeInt(area.height);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(_superState, flags);
            int intFlags = _secondSelected? 1 : 0;
            if (_first != null) {
                intFlags |= 2;
            }
            if (_second != null) {
                intFlags |= 4;
            }

            dest.writeInt(intFlags);
            writeAreaToParcelIfNotNull(dest, _first);
            writeAreaToParcelIfNotNull(dest, _second);
        }
    }
}
