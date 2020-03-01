package sword.langbook3.sqlite2xml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import sword.collections.ImmutableList;
import sword.collections.List;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbValue;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.LangbookDbSchema.Tables;

public final class Main {
    private static void printSymbolArrays(SqliteExporterDatabase db, PrintWriter writer) {
        final LangbookDbSchema.SymbolArraysTable table = Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getStrColumnIndex());
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                writer.println("<symbol-array>");
                writer.println("  <id>" + row.get(0).toInt() + "</id>");
                writer.println("  <str>" + row.get(1).toText() + "</str>");
                writer.println("</symbol-array>");
            }
        }
    }

    public static void main(String[] args) {
        try (SqliteExporterDatabase db = new SqliteExporterDatabase("sample.db")) {
            try {
                try (PrintWriter writer = new PrintWriter(new FileOutputStream("sample.xml"))) {
                    printSymbolArrays(db, writer);
                }
            }
            catch (IOException e) {
                // Nothing to be done
            }
        }
    }

    private Main() {
    }
}
