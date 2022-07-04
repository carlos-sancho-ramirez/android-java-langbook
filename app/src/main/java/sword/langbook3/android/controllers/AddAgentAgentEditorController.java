package sword.langbook3.android.controllers;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class AddAgentAgentEditorController extends AddAgentAbstractAgentEditorController {

    @Override
    public void setup(@NonNull MutableState state) {
        // Nothing to be done
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Nothing to be done
    }

    public static final Parcelable.Creator<AddAgentAgentEditorController> CREATOR = new Parcelable.Creator<AddAgentAgentEditorController>() {

        @Override
        public AddAgentAgentEditorController createFromParcel(Parcel source) {
            return new AddAgentAgentEditorController();
        }

        @Override
        public AddAgentAgentEditorController[] newArray(int size) {
            return new AddAgentAgentEditorController[size];
        }
    };
}
