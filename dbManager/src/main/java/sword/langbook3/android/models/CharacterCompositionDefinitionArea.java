package sword.langbook3.android.models;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class CharacterCompositionDefinitionArea implements CharacterCompositionDefinitionAreaInterface {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public CharacterCompositionDefinitionArea(int x, int y, int width, int height) {
        ensureValidArguments(x >= 0 && x < CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT);
        ensureValidArguments(y >= 0 && y < CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT);
        ensureValidArguments(width > 0 && width <= CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - x);
        ensureValidArguments(height > 0 && height <= CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - y);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
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

    @Override
    public int hashCode() {
        return width * height;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CharacterCompositionDefinitionArea)) {
            return false;
        }

        final CharacterCompositionDefinitionArea that = (CharacterCompositionDefinitionArea) other;
        return x == that.x && y == that.y && width == that.width && height == that.height;
    }

    public static CharacterCompositionDefinitionArea cloneFrom(CharacterCompositionDefinitionAreaInterface area) {
        return new CharacterCompositionDefinitionArea(area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }
}
