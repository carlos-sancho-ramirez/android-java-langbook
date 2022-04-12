package sword.langbook3.android.models;

public interface CharacterCompositionDefinitionAreaInterface {

    /**
     * Column where the drawable area should start.
     * This value must be between 0 and {@link sword.langbook3.android.db.LangbookDbSchema#CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT} minus 1.
     */
    int getX();

    /**
     * Row where the drawable area should start.
     * This value must be between 0 and {@link sword.langbook3.android.db.LangbookDbSchema#CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT} minus 1.
     */
    int getY();

    /**
     * Width for the drawable area.
     * This value must be between 1 and {@link sword.langbook3.android.db.LangbookDbSchema#CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT}.
     * The sum of this value and the value at {@link #getX()}
     * must not exceed the value at {@link sword.langbook3.android.db.LangbookDbSchema#CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT}.
     */
    int getWidth();

    /**
     * Height for the drawable area.
     * This value must be between 1 and {@link sword.langbook3.android.db.LangbookDbSchema#CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT}.
     * The sum of this value and the value at {@link #getY()}
     * must not exceed the value at {@link sword.langbook3.android.db.LangbookDbSchema#CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT}.
     */
    int getHeight();
}
