package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.DbManager;
import sword.langbook3.android.activities.delegates.LanguagePickerActivityDelegate;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

abstract class AbstractLanguagePickerController implements LanguagePickerActivityDelegate.Controller, Fireable {

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        final LanguageId uniqueLanguage = DbManager.getInstance().getManager().getUniqueLanguage();
        if (uniqueLanguage == null) {
            presenter.openLanguagePicker(requestCode, this);
        }
        else {
            complete(presenter, requestCode, uniqueLanguage);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    abstract void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language);

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull LanguageId language) {
        complete(presenter, LanguagePickerActivityDelegate.REQUEST_CODE_NEW_WORD, language);
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == LanguagePickerActivityDelegate.REQUEST_CODE_NEW_WORD && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }
}
