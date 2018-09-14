package sword.langbook3.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import sword.collections.ImmutableIntKeyMap;
import sword.langbook3.android.sdb.ProgressListener;

import static sword.langbook3.android.LangbookReadableDatabase.readAllAlphabets;

public final class SettingsActivity extends Activity implements View.OnClickListener, ProgressListener,
        DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener {

    private static final int REQUEST_CODE_PICK = 1;
    private static final int REQUEST_CODE_ASK_PERMISSION = 2;

    private interface SavedKeys {
        String FILE_FLAGS = "ff";
        String URI = "uri";
    }

    private boolean _resumed;
    private EditText _dialogEditText;
    private Spinner _preferredAlphabetSpinner;
    private ImmutableIntKeyMap<String> _alphabets;
    private Uri _uri;

    private interface FileFlags {
        /**
         * If set, the intention is to save into a file.
         * if clear, the intention is to load a file.
         */
        int SAVE = 1;

        /**
         * If set, the file format is streamed database.
         * If clear, the format is SQLite database file.
         */
        int SQLITE = 2;
    }

    private int _fileFlags;

    public static void open(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    private boolean expectsSaveFile() {
        return (_fileFlags & FileFlags.SAVE) != 0;
    }

    private boolean expectsSqliteFileFormat() {
        return (_fileFlags & FileFlags.SQLITE) != 0;
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        if (!getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
            startActivityForResult(intent, REQUEST_CODE_PICK);
        }
        else {
            selectLocation();
        }
    }

    private void selectLocation() {
        final int buttonStrId = expectsSaveFile()?
                R.string.selectLocationDialogSave : R.string.selectLocationDialogLoad;
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.selectLocationDialogTitle)
                .setPositiveButton(buttonStrId, this)
                .create();
        final View dialogView = LayoutInflater.from(dialog.getContext())
                .inflate(R.layout.select_location_dialog, null);
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

    private void updatePreferredAlphabetAdapter() {
        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _alphabets = readAllAlphabets(DbManager.getInstance().getDatabase(), preferredAlphabet);
        final int length = _alphabets.size();

        final AlphabetAdapter.Item[] items = new AlphabetAdapter.Item[length];
        for (int i = 0; i < length; i++) {
            items[i] = new AlphabetAdapter.Item(_alphabets.keyAt(i), _alphabets.valueAt(i));
        }
        final AlphabetAdapter adapter = new AlphabetAdapter(items);
        _preferredAlphabetSpinner.setAdapter(adapter);

        final int position = _alphabets.keySet().indexOf(preferredAlphabet);
        if (position >= 0) {
            _preferredAlphabetSpinner.setSelection(position);
        }

        _preferredAlphabetSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        LangbookPreferences.getInstance().setPreferredAlphabet(_alphabets.keyAt(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing to be done
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if (savedInstanceState != null) {
            _fileFlags = savedInstanceState.getInt(SavedKeys.FILE_FLAGS);
            _uri = savedInstanceState.getParcelable(SavedKeys.URI);
        }

        final View progressPanel = findViewById(R.id.progressPanel);
        progressPanel.setOnClickListener(this);

        findViewById(R.id.importStreamedDatabaseButton).setOnClickListener(this);
        findViewById(R.id.exportStreamedDatabaseButton).setOnClickListener(this);
        findViewById(R.id.importSqliteDatabaseButton).setOnClickListener(this);
        findViewById(R.id.exportSqliteDatabaseButton).setOnClickListener(this);

        _preferredAlphabetSpinner = findViewById(R.id.preferredAlphabetSpinner);
        updatePreferredAlphabetAdapter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Uri uri = (data != null)? data.getData() : null;
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK && uri != null) {
            triggerFileProcessing(uri);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        _resumed = true;
        final DbManager dbManager = DbManager.getInstance();
        if (dbManager.isProcessingDatabase()) {
            dbManager.setProgressListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.importStreamedDatabaseButton:
                _fileFlags = 0;
                pickFile();
                break;

            case R.id.importSqliteDatabaseButton:
                _fileFlags = FileFlags.SQLITE;
                pickFile();
                break;

            case R.id.exportStreamedDatabaseButton:
                _fileFlags = FileFlags.SAVE;
                selectLocation();
                break;

            case R.id.exportSqliteDatabaseButton:
                _fileFlags = FileFlags.SAVE | FileFlags.SQLITE;
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
        final boolean inProgress = dbManager.isProcessingDatabase();
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

    private void loadSqliteDatabase(Uri uri) {
        final DbManager manager = DbManager.getInstance();
        int stringId = R.string.importFailure;
        if (uri != null) {
            try {
                manager.getReadableDatabase().close();
                Utils.copyFile(this, uri, manager.getDatabasePath());
                stringId = R.string.importSuccess;
            } catch (IOException e) {
                // Nothing to be done
            }
        }

        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show();
    }

    private void saveSqliteDatabase(String filePath) {
        final String dbPath = DbManager.getInstance().getDatabasePath();
        int stringId = R.string.exportSuccess;
        try {
            Utils.copyFile(dbPath, filePath);
        }
        catch (IOException e) {
            stringId = R.string.exportFailure;
        }

        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        DbManager.getInstance().setProgressListener(null);
        _resumed = false;

        super.onPause();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final String filePath = _dialogEditText.getText().toString();
        final Uri uri = Uri.fromFile(new File(filePath));
        triggerFileProcessing(uri);
        dialog.dismiss();
    }

    @SuppressWarnings("NewApi")
    private void triggerFileProcessing(Uri uri) {
        if (expectsSaveFile()) {
            if (expectsSqliteFileFormat()) {
                if (!"file".equals(uri.getScheme())) {
                    throw new UnsupportedOperationException("Unable to save for no file uri");
                }
                saveSqliteDatabase(uri.getPath());
            }
            else {
                final DbManager manager = DbManager.getInstance();
                manager.exportStreamedDatabase(uri);
                if (_resumed) {
                    manager.setProgressListener(this);
                }
            }
        }
        else {
            if (expectsSqliteFileFormat()) {
                loadSqliteDatabase(uri);
            }
            else {
                if (ApiUtils.isAtLeastApi23() && StorageUtils.isExternalFileUri(uri) && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    _uri = uri;
                    requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSION);
                }
                else {
                    importDatabase(uri);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i])) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        importDatabase(_uri);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SavedKeys.FILE_FLAGS, _fileFlags);
        outState.putParcelable(SavedKeys.URI, _uri);
    }
}
