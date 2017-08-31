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

        SQLiteDatabase db = new DbManager(this).getReadableDatabase();
        try {
            final String whereClause = null;
            Cursor cursor = db.query("SymbolArrays", new String[] {"id", "str"}, whereClause, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        final String str = cursor.getInt(0) + ": " + cursor.getString(1);
                        final TextView textView = (TextView) findViewById(R.id.textView);
                        textView.setText(str);
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
    }
}
