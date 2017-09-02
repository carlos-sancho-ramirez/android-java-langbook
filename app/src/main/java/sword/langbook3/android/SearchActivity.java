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
        SQLiteDatabase db = new DbManager(this).getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery("SELECT sourceAlphabet, targetAlphabet, S1.str, S2.str FROM " + DbManager.TableNames.conversions + " JOIN " + DbManager.TableNames.symbolArrays + " AS S1 ON source=S1.id JOIN " + DbManager.TableNames.symbolArrays + " AS S2 ON target=S2.id", null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        do {
                            builder.append(cursor.getInt(0))
                                    .append(" -> ").append(cursor.getInt(1))
                                    .append(": ").append(cursor.getString(2))
                                    .append(" -> ").append(cursor.getString(3)).append('\n');
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
