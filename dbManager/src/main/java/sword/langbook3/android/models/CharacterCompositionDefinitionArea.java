package sword.langbook3.android.models;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class CharacterCompositionDefinitionArea {

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
}
