package sword.langbook3.android;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;

public final class CharacterCompositionDefinitionDrawable extends Drawable {

    private static final int FIRST_COLOR = 0x99FF0000;
    private static final int SECOND_COLOR = 0x990000FF;

    private final Rect _rect = new Rect();
    private final Paint _firstPaint;
    private final Paint _secondPaint;

    private CharacterCompositionDefinitionRegister _register;

    public CharacterCompositionDefinitionDrawable() {
        _firstPaint = new Paint();
        _firstPaint.setColor(FIRST_COLOR);

        _secondPaint = new Paint();
        _secondPaint.setColor(SECOND_COLOR);
    }

    public void setRegister(CharacterCompositionDefinitionRegister register) {
        _register = register;
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
    public void draw(Canvas canvas) {
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
