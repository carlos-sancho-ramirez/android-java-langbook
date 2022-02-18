package sword.langbook3.android;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import sword.collections.ImmutableIntKeyMap;
import sword.langbook3.android.db.LangbookDbSchema;

public final class CharacterCompositionsParserResult {
    public final ImmutableIntKeyMap<CharacterComposition> compositions;
    public final ImmutableIntKeyMap<Character> characters;

    CharacterCompositionsParserResult(
            ImmutableIntKeyMap<CharacterComposition> compositions,
            ImmutableIntKeyMap<Character> characters) {
        if (compositions == null || characters == null) {
            throw new IllegalArgumentException();
        }

        this.compositions = compositions;
        this.characters = characters;
    }

    private void saveCharactersInDatabase(SQLiteDatabase db) {
        final LangbookDbSchema.UnicodeCharactersTable table = LangbookDbSchema.Tables.unicodeCharacters;
        final String tableName = table.name();
        db.execSQL("DELETE FROM " + tableName);

        final ContentValues cv = new ContentValues();
        final String idColumnName = table.columns().get(table.getIdColumnIndex()).name();
        final String unicodeColumnName = table.columns().get(table.getUnicodeColumnIndex()).name();

        final int count = characters.size();
        for (int index = 0; index < count; index++) {
            cv.put(idColumnName, characters.keyAt(index));
            final char character = characters.valueAt(index);
            cv.put(unicodeColumnName, (int) character);
            db.insert(tableName, null, cv);
        }
    }

    private void saveCompositionsInDatabase(SQLiteDatabase db) {
        final LangbookDbSchema.CharacterCompositionsTable table = LangbookDbSchema.Tables.characterCompositions;
        final String tableName = table.name();
        db.execSQL("DELETE FROM " + tableName);

        final ContentValues cv = new ContentValues();
        final String idColumnName = table.columns().get(table.getIdColumnIndex()).name();
        final String firstColumnName = table.columns().get(table.getFirstCharacterColumnIndex()).name();
        final String secondColumnName = table.columns().get(table.getSecondCharacterColumnIndex()).name();
        final String compositionTypeColumnName = table.columns().get(table.getCompositionTypeColumnIndex()).name();

        final int compositionsCount = compositions.size();
        for (int compositionIndex = 0; compositionIndex < compositionsCount; compositionIndex++) {
            cv.put(idColumnName, compositions.keyAt(compositionIndex));
            final CharacterComposition composition = compositions.valueAt(compositionIndex);
            cv.put(firstColumnName, composition.firstCharacter);
            cv.put(secondColumnName, composition.secondCharacter);
            cv.put(compositionTypeColumnName, composition.compositionType);
            db.insert(tableName, null, cv);
        }
    }

    public void saveInDatabase(SQLiteDatabase db) {
        saveCharactersInDatabase(db);
        saveCompositionsInDatabase(db);
    }
}
