package sword.langbook3.android.models;

public final class CharacterCompositionPart<CharacterId> {

    /**
     * Identifier for this character within the database.
     */
    public final CharacterId id;

    /**
     * Visual representation for this character
     */
    public final CharacterCompositionRepresentation representation;

    public CharacterCompositionPart(CharacterId id, CharacterCompositionRepresentation representation) {
        if (id == null || representation == null) {
            throw new IllegalArgumentException();
        }

        this.id = id;
        this.representation = representation;
    }
}
