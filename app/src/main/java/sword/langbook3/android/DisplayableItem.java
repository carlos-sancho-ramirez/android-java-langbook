package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

public final class DisplayableItem implements Parcelable {

    public final int id;
    public final String text;

    public DisplayableItem(int id, String text) {
        this.id = id;
        this.text = text;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(text);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DisplayableItem> CREATOR = new Creator<DisplayableItem>() {
        @Override
        public DisplayableItem createFromParcel(Parcel in) {
            final int id = in.readInt();
            final String text = in.readString();

            return new DisplayableItem(id, text);
        }

        @Override
        public DisplayableItem[] newArray(int size) {
            return new DisplayableItem[size];
        }
    };
}
