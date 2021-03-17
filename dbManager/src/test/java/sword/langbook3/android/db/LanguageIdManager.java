package sword.langbook3.android.db;

import sword.database.DbValue;

final class LanguageIdManager implements ConceptualizableSetter<ConceptIdHolder, LanguageIdHolder> {

    @Override
    public LanguageIdHolder getKeyFromInt(int key) {
        return new LanguageIdHolder(key);
    }

    @Override
    public LanguageIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    @Override
    public LanguageIdHolder getKeyFromConceptId(ConceptIdHolder concept) {
        return new LanguageIdHolder(concept.key);
    }
}
