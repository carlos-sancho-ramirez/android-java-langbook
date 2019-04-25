package sword.langbook3.android;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;

import static sword.langbook3.android.db.LangbookDbSchema.MAX_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;

public class KnowledgeDrawable extends Drawable {

    private static final int COLOR_MAP_TOP = 256;
    private static final ImmutableIntPairMap COLOR_MAP = new ImmutableIntPairMap.Builder()
            .put(0, 0xFFFF6666)
            .put(192, 0xFFFFFF66)
            .put(256, 0xFF66FF66)
            .build();

    private static int[] COLORS;
    private final ImmutableIntList _knowledge;
    private final int _questionCount;

    private final Paint _paint;

    KnowledgeDrawable(ImmutableIntList knowledge) {
        if (knowledge == null || knowledge.size() != getColors().length) {
            throw new IllegalArgumentException();
        }

        int amount = 0;
        for (int known : knowledge) {
            amount += known;
        }

        if (amount == 0) {
            throw new IllegalArgumentException();
        }

        _knowledge = knowledge;
        _questionCount = amount;

        _paint = new Paint();
        _paint.setStyle(Paint.Style.FILL);
    }

    private int[] getColors() {
        if (COLORS == null) {
            final int length = MAX_ALLOWED_SCORE - MIN_ALLOWED_SCORE + 1;
            final int[] colors = new int[length];
            final int colorMapMax = COLOR_MAP.size() - 1;
            final float step = ((float) COLOR_MAP_TOP) / (length - 1);
            float pos = 0;
            int lastIndex = 0;
            for (int i = 0; i < length; i++) {
                while (lastIndex < colorMapMax && COLOR_MAP.keyAt(lastIndex + 1) <= pos) {
                    lastIndex++;
                }

                if (lastIndex == colorMapMax) {
                    colors[i] = COLOR_MAP.valueAt(colorMapMax);
                }
                else {
                    final int prevColor = COLOR_MAP.valueAt(lastIndex);
                    final int nextColor = COLOR_MAP.valueAt(lastIndex + 1);
                    final float prevPos = COLOR_MAP.keyAt(lastIndex);
                    final float nextPos = COLOR_MAP.keyAt(lastIndex + 1);
                    final float distance = nextPos - prevPos;
                    final float prevPonderation = (nextPos - pos) / distance;
                    final float nextPonderation = (pos - prevPos) / distance;
                    colors[i] = 0xFF000000 |
                            ((int) ((prevColor & 0x00FF0000) * prevPonderation
                                    + (nextColor & 0x00FF0000) * nextPonderation)) & 0x00FF0000 |
                            ((int) ((prevColor & 0x0000FF00) * prevPonderation
                                    + (nextColor & 0x0000FF00) * nextPonderation)) & 0x0000FF00 |
                            ((int) ((prevColor & 0x000000FF) * prevPonderation
                                    + (nextColor & 0x000000FF) * nextPonderation)) & 0x000000FF;
                }

                pos += step;
            }

            COLORS = colors;
        }

        return COLORS;
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect bounds = getBounds();
        final int width = bounds.width();
        int count = 0;
        float x = bounds.left;

        float nextX = x;
        final int[] colors = getColors();
        for (int i = 0; i < colors.length; i++) {
            x = nextX;
            count += _knowledge.get(i);
            nextX = count * width;
            nextX /= _questionCount + bounds.left;

            _paint.setColor(colors[i]);
            canvas.drawRect(x, bounds.top, nextX, bounds.bottom, _paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // No alpha supported
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // No color filter supported
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
