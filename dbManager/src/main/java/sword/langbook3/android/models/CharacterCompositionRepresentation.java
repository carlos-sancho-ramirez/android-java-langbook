package sword.langbook3.android.models;

import sword.langbook3.android.db.LangbookDbSchema;

public final class CharacterCompositionRepresentation {

    /**
     * Value that {@link #character} field will have in case the character
     * has no visual representation.
     */
    public static final char INVALID_CHARACTER = 0;

    /**
     * Visual representation of the character.
     * This will match {@link #INVALID_CHARACTER} in case there is no visual representation.
     */
    public final char character;

    /**
     * Token assigned to this character.
     * This should be null if {@link #character} is not {@link #INVALID_CHARACTER}.
     */
    public final String token;

    public CharacterCompositionRepresentation(char character, String token) {
        if (token != null && (character != INVALID_CHARACTER || !LangbookDbSchema.CharacterTokensTable.isValidToken(token))) {
            throw new IllegalArgumentException();
        }

        this.character = character;
        this.token = token;
    }

    public boolean canBeRepresented() {
        return character != INVALID_CHARACTER || token != null && !token.isEmpty();
    }
}
