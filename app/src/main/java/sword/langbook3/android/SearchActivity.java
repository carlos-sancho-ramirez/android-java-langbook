package sword.langbook3.android;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;

public class SearchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        final StringBuilder builder = new StringBuilder();
        final DbManager dbManager = new DbManager(this);
        SQLiteDatabase db = dbManager.getReadableDatabase();

        DbManager.Language[] languages = dbManager.languages;
        for (DbManager.Language language : languages) {
            builder.append(language.toString()).append('\n');
        }

        try {
            final String whereClause = null;
            Cursor cursor = db.query("SymbolArrays", new String[] {"id", "str"}, whereClause, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        do {
                            builder.append(cursor.getInt(0)).append(": ").append(cursor.getString(1)).append('\n');
                        } while (cursor.moveToNext());
                    }
                }
                finally {
                    cursor.close();
                }
            }
        }
        finally {
            db.close();
        }

        final TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(builder.toString());
    }
}
