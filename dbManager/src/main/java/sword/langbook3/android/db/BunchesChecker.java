package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;

public interface BunchesChecker<LanguageId, AlphabetId> extends AcceptationsChecker<LanguageId, AlphabetId> {
    ImmutableIntSet getAcceptationsInBunch(int bunch);
    ImmutableIntSet findBunchesWhereAcceptationIsIncluded(int acceptation);
}
