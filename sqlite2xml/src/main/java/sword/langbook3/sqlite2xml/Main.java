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
                writer.println("  <symbol-array>");
                writer.println("    <id>" + row.get(0).toInt() + "</id>");
                writer.println("    <str>" + row.get(1).toText() + "</str>");
                writer.println("  </symbol-array>");
            }
        }
    }

    private static void printCorrelations(SqliteExporterDatabase db, PrintWriter writer) {
        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getCorrelationIdColumnIndex(), table.getAlphabetColumnIndex(), table.getSymbolArrayColumnIndex());
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                writer.println("  <correlation>");
                writer.println("    <correlation-id>" + row.get(0).toInt() + "</correlation-id>");
                writer.println("    <alphabet>" + row.get(1).toInt() + "</alphabet>");
                writer.println("    <symbol-array>" + row.get(2).toInt() + "</symbol-array>");
                writer.println("  </correlation>");
            }
        }
    }

    private static void printCorrelationArrays(SqliteExporterDatabase db, PrintWriter writer) {
        final LangbookDbSchema.CorrelationArraysTable table = Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getArrayIdColumnIndex(), table.getArrayPositionColumnIndex(), table.getCorrelationColumnIndex());
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                writer.println("  <correlation-array>");
                writer.println("    <array-id>" + row.get(0).toInt() + "</array-id>");
                writer.println("    <array-position>" + row.get(1).toInt() + "</array-position>");
                writer.println("    <correlation>" + row.get(2).toInt() + "</correlation>");
                writer.println("  </correlation-array>");
            }
        }
    }

    private static void printAcceptations(SqliteExporterDatabase db, PrintWriter writer) {
        final LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getConceptColumnIndex(), table.getCorrelationArrayColumnIndex());
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                writer.println("  <acceptation>");
                writer.println("    <id>" + row.get(0).toInt() + "</id>");
                writer.println("    <concept>" + row.get(1).toInt() + "</concept>");
                writer.println("    <correlation-array>" + row.get(2).toInt() + "</correlation-array>");
                writer.println("  </acceptation>");
            }
        }
    }

    public static void main(String[] args) {
        try (SqliteExporterDatabase db = new SqliteExporterDatabase("sample.db")) {
            try {
                try (PrintWriter writer = new PrintWriter(new FileOutputStream("sample.xml"))) {
                    writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                    writer.println("<langbook-database>");
                    printSymbolArrays(db, writer);
                    printCorrelations(db, writer);
                    printCorrelationArrays(db, writer);
                    printAcceptations(db, writer);
                    writer.println("</langbook-database>");
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
