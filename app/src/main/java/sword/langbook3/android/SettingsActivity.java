package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class SettingsActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_PICK_FILE = 1;

    public static void open(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        if (!getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
        }
        else {
            Toast.makeText(this, "No file picker application found in the device", Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(Uri uri) {
        boolean allOk = false;
        if (uri != null) {
            try {
                final InputStream inStream = getContentResolver().openInputStream(uri);
                if (inStream != null) {
                    try {
                        // TODO To be implemented
                        allOk = true;
                    }
                    finally {
                        try {
                            inStream.close();
                        }
                        catch (IOException e) {
                            // Already closed? That's fine then
                        }
                    }
                }
            }
            catch (FileNotFoundException e) {
                // Nothing to be done
            }
        }

        if (!allOk) {
            Toast.makeText(this, "Unable to import file " + ((uri != null)? uri.toString() : "<null>"), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        final Button importDatabaseButton = findViewById(R.id.importDatabaseButton);
        importDatabaseButton.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_PICK_FILE:
                    importDatabase((data != null)? data.getData() : null);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.importDatabaseButton:
                pickFile();
        }
    }
}
