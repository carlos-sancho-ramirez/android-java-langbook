package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity implements View.OnClickListener, DbManager.DatabaseImportProgressListener {

    private static final int REQUEST_CODE_PICK_FILE = 1;

    private boolean _resumed;

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
        if (uri != null) {
            final DbManager dbManager = DbManager.getInstance();
            dbManager.importDatabase(uri);
            if (_resumed) {
                dbManager.setProgressListener(this);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        final View progressPanel = findViewById(R.id.progressPanel);
        progressPanel.setOnClickListener(this);

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
    public void onResume() {
        super.onResume();

        _resumed = true;
        final DbManager dbManager = DbManager.getInstance();
        if (dbManager.isImportingDatabase()) {
            dbManager.setProgressListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.importDatabaseButton:
                pickFile();
                break;

            case R.id.progressPanel:
                // Just consuming the event
                break;
        }
    }

    @Override
    public void setProgress(float progress, String message) {
        final View progressPanel = findViewById(R.id.progressPanel);

        final DbManager dbManager = DbManager.getInstance();
        final boolean inProgress = dbManager.isImportingDatabase();
        if (inProgress) {
            final ProgressBar progressBar = findViewById(R.id.progressBar);
            final TextView progressMessage = findViewById(R.id.progressMessage);

            progressBar.setProgress((int) (progress * 100));
            progressMessage.setText(message);
        }
        else {
            dbManager.setProgressListener(null);
        }

        progressPanel.setVisibility(inProgress? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPause() {
        DbManager.getInstance().setProgressListener(null);
        _resumed = false;

        super.onPause();
    }
}
