package sword.langbook3.android.db;

import sword.collections.ImmutableSet;

public interface BunchesChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId> extends AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId> {
    ImmutableSet<AcceptationId> getAcceptationsInBunch(BunchId bunch);
    ImmutableSet<BunchId> findBunchesWhereAcceptationIsIncluded(AcceptationId acceptation);
}
