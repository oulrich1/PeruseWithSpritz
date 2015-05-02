package com.oriahulrich.perusalwithspritz.parcelable;

import android.os.Parcel;
import android.os.Parcelable;
import android.speech.tts.Voice;

import java.util.Collections;
import java.util.Set;

/**
 * Created by oriahulrich on 5/2/15.
 */
public class Voices implements Parcelable {

    Set<Voice> voices;

    // init voices set
    public Voices(Set<Voice> voices) {
        this.voices = voices;
    }

    // parcel to voices
    public Voices(Parcel in){
        Voice[] _voices = (Voice[])in.readParcelableArray(Voice.class.getClassLoader());
        Collections.addAll(voices, _voices);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // voices to parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelableArray((Parcelable[])voices.toArray(), flags);
    }

    // -- //
//    public static final Parcelable.Creator<Voices> CREATOR= new Parcelable.Creator<Voices>() { 
//        @Override
//        public Voices createFromParcel(Parcel source) {
//            // TODO Auto-generated method stub
//            return new Voices(source);  //using parcelable constructor
//        }
//         
//        @Override
//        public Voices[] newArray(int size) {
//            // TODO Auto-generated method stub
//            return new Voices[size];
//        }
//    };
}
