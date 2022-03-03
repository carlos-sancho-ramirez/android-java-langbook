package sword.langbook3.android.models;

public final class CharacterCompositionEditorModel<CharacterId> {

    /**
     * Visual representation of the character.
     * This will match {@link CharacterDetailsModel.Part#INVALID_CHARACTER} in case there is no visual representation.
     */
    public final char character;
    public final Part<CharacterId> first;
    public final Part<CharacterId> second;
    public final int compositionType;

    public CharacterCompositionEditorModel(char character, Part<CharacterId> first, Part<CharacterId> second, int compositionType) {
        if (compositionType != 0 && (first == null || second == null)) {
            throw new IllegalArgumentException();
        }

        this.character = character;
        this.first = first;
        this.second = second;
        this.compositionType = compositionType;
    }

    public static final class Part<CharacterId> {
        /**
         * Identifier for this character within the database.
         */
        public final CharacterId id;

        /**
         * Visual representation of the character.
         * This will match {@link CharacterDetailsModel.Part#INVALID_CHARACTER} in case there is no visual representation.
         */
        public final char character;

        public Part(CharacterId id, char character) {
            if (id == null) {
                throw new IllegalArgumentException();
            }

            this.id = id;
            this.character = character;
        }
    }
}
