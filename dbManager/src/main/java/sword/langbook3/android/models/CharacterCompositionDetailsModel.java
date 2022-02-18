package sword.langbook3.android.models;

public final class CharacterCompositionDetailsModel<CharacterId> {

    /**
     * Visual representation of the character.
     * This will match {@link Part#INVALID_CHARACTER} in case there is no visual representation.
     */
    public final char character;
    public final Part<CharacterId> first;
    public final Part<CharacterId> second;
    public final int compositionType;

    public CharacterCompositionDetailsModel(char character, Part<CharacterId> first, Part<CharacterId> second, int compositionType) {
        if (first == null || second == null) {
            throw new IllegalArgumentException();
        }

        this.character = character;
        this.first = first;
        this.second = second;
        this.compositionType = compositionType;
    }

    public static final class Part<CharacterId> {
        /**
         * Value that {@link #character} field will have in case the character
         * has no visual representation.
         */
        public static final char INVALID_CHARACTER = 0;

        /**
         * Identifier for this character within the database.
         */
        public final CharacterId id;

        /**
         * Visual representation of the character.
         * This will match {@link #INVALID_CHARACTER} in case there is no visual representation.
         */
        public final char character;

        /**
         * Whether this part is at the same time a composition.
         * This may allow the user to click on the part to check the composition of the composition.
         */
        public final boolean isComposition;

        public Part(CharacterId id, char character, boolean isComposition) {
            if (id == null) {
                throw new IllegalArgumentException();
            }

            this.id = id;
            this.character = character;
            this.isComposition = isComposition;
        }
    }
}
