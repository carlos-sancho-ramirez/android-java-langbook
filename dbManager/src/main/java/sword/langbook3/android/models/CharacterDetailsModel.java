package sword.langbook3.android.models;

import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;

public final class CharacterDetailsModel<CharacterId, AcceptationId> {

    public final CharacterCompositionRepresentation representation;
    public final CharacterCompositionPart<CharacterId> first;
    public final CharacterCompositionPart<CharacterId> second;
    public final IdentifiableCharacterCompositionResult<AcceptationId> compositionType;
    public final ImmutableList<CharacterCompositionPart<CharacterId>> asFirst;
    public final ImmutableList<CharacterCompositionPart<CharacterId>> asSecond;
    public final ImmutableMap<AcceptationId, AcceptationInfo> acceptationsWhereIncluded;

    public CharacterDetailsModel(CharacterCompositionRepresentation representation, CharacterCompositionPart<CharacterId> first, CharacterCompositionPart<CharacterId> second, IdentifiableCharacterCompositionResult<AcceptationId> compositionType, ImmutableList<CharacterCompositionPart<CharacterId>> asFirst, ImmutableList<CharacterCompositionPart<CharacterId>> asSecond, ImmutableMap<AcceptationId, AcceptationInfo> acceptationsWhereIncluded) {
        if (representation == null || compositionType != null && (first == null || second == null || compositionType.register == null) || asFirst == null || asSecond == null || acceptationsWhereIncluded == null) {
            throw new IllegalArgumentException();
        }

        this.representation = representation;
        this.first = first;
        this.second = second;
        this.compositionType = compositionType;
        this.asFirst = asFirst;
        this.asSecond = asSecond;
        this.acceptationsWhereIncluded = acceptationsWhereIncluded;
    }

    public static final class AcceptationInfo {
        public String text;
        public boolean isDynamic;

        public AcceptationInfo(String text, boolean isDynamic) {
            if (text == null) {
                throw new IllegalArgumentException();
            }

            this.text = text;
            this.isDynamic = isDynamic;
        }
    }
}
