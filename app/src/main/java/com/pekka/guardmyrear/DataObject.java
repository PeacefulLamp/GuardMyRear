package com.pekka.guardmyrear;


/**
 * This class is meant to replace the class in StreamActivity for sharing
 * data between the threads and the background service running network operations
 */

import android.os.Parcel;
import android.os.Parcelable;

// simple class that just has one member property as an example
public class DataObject implements Parcelable {
    private String mData;

    /* everything below here is for implementing Parcelable */

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mData);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<DataObject> CREATOR = new Parcelable.Creator<DataObject>() {
        public DataObject createFromParcel(Parcel in) {
            return new DataObject(in);
        }

        public DataObject[] newArray(int size) {
            return new DataObject[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    protected DataObject(Parcel in) {
        mData = in.readString();
    }
    protected DataObject() {
        //do nothing
    }
}