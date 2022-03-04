package sword.langbook3.android.models;

import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;

public final class CharacterDetailsModel<CharacterId, AcceptationId> {

    /**
     * Value that {@link #compositionType} will have in case there is no known composition.
     * Only in this case, {@link #first} and {@link #second} are expected to be null.
     */
    public static final int UNKNOWN_COMPOSITION_TYPE = 0;

    public final CharacterCompositionRepresentation representation;
    public final CharacterCompositionPart<CharacterId> first;
    public final CharacterCompositionPart<CharacterId> second;
    public final int compositionType;
    public final ImmutableList<CharacterCompositionPart<CharacterId>> asFirst;
    public final ImmutableList<CharacterCompositionPart<CharacterId>> asSecond;
    public final ImmutableMap<AcceptationId, AcceptationInfo> acceptationsWhereIncluded;

    public CharacterDetailsModel(CharacterCompositionRepresentation representation, CharacterCompositionPart<CharacterId> first, CharacterCompositionPart<CharacterId> second, int compositionType, ImmutableList<CharacterCompositionPart<CharacterId>> asFirst, ImmutableList<CharacterCompositionPart<CharacterId>> asSecond, ImmutableMap<AcceptationId, AcceptationInfo> acceptationsWhereIncluded) {
        if (representation == null || compositionType != 0 && (first == null || second == null) || asFirst == null || asSecond == null || acceptationsWhereIncluded == null) {
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
