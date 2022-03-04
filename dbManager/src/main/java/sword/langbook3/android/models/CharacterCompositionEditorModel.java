package sword.langbook3.android.models;

import static sword.langbook3.android.models.CharacterDetailsModel.UNKNOWN_COMPOSITION_TYPE;

public final class CharacterCompositionEditorModel<CharacterId> {

    public final CharacterCompositionRepresentation representation;
    public final CharacterCompositionPart<CharacterId> first;
    public final CharacterCompositionPart<CharacterId> second;
    public final int compositionType;

    public CharacterCompositionEditorModel(CharacterCompositionRepresentation representation, CharacterCompositionPart<CharacterId> first, CharacterCompositionPart<CharacterId> second, int compositionType) {
        if (representation == null || compositionType != UNKNOWN_COMPOSITION_TYPE && (first == null || second == null)) {
            throw new IllegalArgumentException();
        }

        this.representation = representation;
        this.first = first;
        this.second = second;
        this.compositionType = compositionType;
    }
}
