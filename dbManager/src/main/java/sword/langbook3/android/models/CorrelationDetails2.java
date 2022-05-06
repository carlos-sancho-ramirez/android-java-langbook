package sword.langbook3.android.models;

import sword.collections.ImmutableMap;

public interface CorrelationDetails2<AlphabetId, CharacterId, CorrelationId, AcceptationId> extends CorrelationDetails<AlphabetId, CorrelationId, AcceptationId> {

    /**
     * List of known characters in the database
     */
    ImmutableMap<Character, CharacterId> getCharacters();
}
