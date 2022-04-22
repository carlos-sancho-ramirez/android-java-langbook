package sword.langbook3.android;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;

public final class CharacterCompositionDefinitionDrawable extends Drawable {

    static final int BACKGROUND_COLOR = 0x33808080;
    static final int FIRST_COLOR = 0x99FF0000;
    static final int SECOND_COLOR = 0x990000FF;

    private final Rect _rect = new Rect();
    private final Paint _backgroundPaint;
    private final Paint _firstPaint;
    private final Paint _secondPaint;

    private final CharacterCompositionDefinitionRegister _register;

    public CharacterCompositionDefinitionDrawable(CharacterCompositionDefinitionRegister register) {
        _register = register;

        _firstPaint = new Paint();
        _firstPaint.setColor(FIRST_COLOR);

        _secondPaint = new Paint();
        _secondPaint.setColor(SECOND_COLOR);

        _backgroundPaint = new Paint();
        _backgroundPaint.setColor(BACKGROUND_COLOR);
    }

    private void drawArea(CharacterCompositionDefinitionArea area, Canvas canvas, Paint paint) {
        final Rect bounds = getBounds();
        final int width = bounds.width();
        final int height = bounds.height();

        final int x = bounds.left + (area.x * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int y = bounds.top + (area.y * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int right = bounds.left + ((area.x + area.width) * width) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        final int bottom = bounds.top + ((area.y + area.height) * height) / CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
        _rect.set(x, y, right, bottom);
        canvas.drawRect(_rect, paint);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect bounds = getBounds();
        _rect.set(0, 0, bounds.width(), bounds.height());
        canvas.drawRect(_rect, _backgroundPaint);

        final CharacterCompositionDefinitionRegister register = _register;
        if (register != null) {
            drawArea(register.first, canvas, _firstPaint);
            drawArea(register.second, canvas, _secondPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // Nothing to be done
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Nothing to be done
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
