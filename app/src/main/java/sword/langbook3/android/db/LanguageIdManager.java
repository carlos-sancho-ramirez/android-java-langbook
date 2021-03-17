package sword.langbook3.android.db;

import sword.database.DbValue;

public final class LanguageIdManager implements ConceptualizableSetter<ConceptId, LanguageId> {

    @Override
    public LanguageId getKeyFromInt(int key) {
        return (key != 0)? new LanguageId(key) : null;
    }

    @Override
    public LanguageId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static LanguageId conceptAsLanguageId(ConceptId concept) {
        return (concept == null)? null : new LanguageId(concept.key);
    }

    @Override
    public LanguageId getKeyFromConceptId(ConceptId concept) {
        return conceptAsLanguageId(concept);
    }
}
