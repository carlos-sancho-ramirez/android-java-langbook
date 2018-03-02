package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class SettingsActivity extends Activity implements View.OnClickListener, DbManager.DatabaseImportProgressListener, DialogInterface.OnClickListener {

    private static final int REQUEST_CODE_PICK_FILE = 1;

    private boolean _resumed;
    private EditText _dialogEditText;

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

    private void selectLocation() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.selectLocationDialogTitle)
                .setPositiveButton(R.string.selectLocationDialogOk, this)
                .create();
        final View dialogView = LayoutInflater.from(dialog.getContext()).inflate(R.layout.select_location_dialog, null);
        _dialogEditText = dialogView.findViewById(R.id.filePath);

        dialog.setView(dialogView);
        dialog.show();
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

        findViewById(R.id.importStreamedDatabaseButton).setOnClickListener(this);
        findViewById(R.id.exportStreamedDatabaseButton).setOnClickListener(this);
        findViewById(R.id.importSqliteDatabaseButton).setOnClickListener(this);
        findViewById(R.id.exportSqliteDatabaseButton).setOnClickListener(this);
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
            case R.id.importStreamedDatabaseButton:
                pickFile();
                break;

            case R.id.exportSqliteDatabaseButton:
                selectLocation();
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

    private void saveSqliteDatabase(String filePath) {
        try {
            Utils.copyFile(getDatabasePath(DbManager.DB_NAME).toString(), filePath);
            Toast.makeText(this, R.string.exportSuccess, Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            Toast.makeText(this, R.string.exportFailure, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        DbManager.getInstance().setProgressListener(null);
        _resumed = false;

        super.onPause();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        saveSqliteDatabase(_dialogEditText.getText().toString());
        dialog.dismiss();
    }
}
