package sword.langbook3.android.models;

public final class CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> {

    public final CharacterCompositionRepresentation representation;
    public final CharacterCompositionPart<CharacterId> first;
    public final CharacterCompositionPart<CharacterId> second;
    public final CharacterCompositionTypeId compositionType;

    public CharacterCompositionEditorModel(CharacterCompositionRepresentation representation, CharacterCompositionPart<CharacterId> first, CharacterCompositionPart<CharacterId> second, CharacterCompositionTypeId compositionType) {
        if (representation == null || compositionType != null && (first == null || second == null)) {
            throw new IllegalArgumentException();
        }

        this.representation = representation;
        this.first = first;
        this.second = second;
        this.compositionType = compositionType;
    }
}
