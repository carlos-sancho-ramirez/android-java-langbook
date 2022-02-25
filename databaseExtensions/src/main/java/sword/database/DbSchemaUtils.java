package sword.database;

public final class DbSchemaUtils {

    public static DbSchemaDifference difference(DbSchema oldSchema, DbSchema newSchema) {
        return new DbSchemaDifference(oldSchema, newSchema);
    }

    private DbSchemaUtils() {
    }
}
