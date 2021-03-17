package sword.langbook3.android.db;

import sword.database.DbValue;

public final class LanguageIdManager implements IntSetter<LanguageId> {

    @Override
    public LanguageId getKeyFromInt(int key) {
        return (key != 0)? new LanguageId(key) : null;
    }

    @Override
    public LanguageId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static LanguageId getNextAvailableId(ConceptsChecker manager) {
        return new LanguageId(manager.getNextAvailableConceptId());
    }

    public static int getConceptId(LanguageId alphabetId) {
        return (alphabetId != null)? alphabetId.key : 0;
    }
}
