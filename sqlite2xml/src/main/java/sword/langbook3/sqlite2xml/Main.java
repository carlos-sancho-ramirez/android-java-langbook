package sword.langbook3.sqlite2xml;

import sword.database.DbQuery;
import sword.langbook3.android.db.LangbookDbSchema;

public final class Main {
    public static void main(String[] args) {
        try (SqliteExporterDatabase db = new SqliteExporterDatabase("sample.db")) {
            final DbQuery query = new DbQuery.Builder(LangbookDbSchema.Tables.acceptations)
                    .select(LangbookDbSchema.Tables.acceptations.getIdColumnIndex());
            final int size = db.select(query).toList().size();
            System.out.println("Found " + size + " acceptation in the database");
        }
    }

    private Main() {
    }
}
