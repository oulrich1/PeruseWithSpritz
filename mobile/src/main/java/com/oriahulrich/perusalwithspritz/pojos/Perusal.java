package com.oriahulrich.perusalwithspritz.pojos;

import java.util.Date;

/**
 * Created by oriahulrich on 12/18/14.
 */
public class Perusal {

    /* Private POJO fields */

    private long    _id;    // for DB stuff
    private String  title;  // predicted or set title
    private String  text;   // text stored to be spritzed
    private int     nTextPartitionIndex; // stored whenever the selection changes
    private int     speed;  // WPM
    private Mode    mode;   // perusal mode is either URL or raw text

    // not used yet..
    private String  urlLink;//
    private Date    date;   // date that the item was added
    private int     words;  //

    // not saved to database
    public boolean bShouldSaveToDB;

    public enum SpeedState {
        SLOW,
        MODERATE,
        FAST,
        VERY_FAST,
    }

    public enum Mode {
        URL,
        TEXT
    }

    private SpeedState speedState;


    public Perusal() {
        init();
    }


    public int getTextPartitionIndex() {
        return nTextPartitionIndex;
    }

    public void setTextPartitionIndex(int nTextPartitionIndex) {
        this.nTextPartitionIndex = nTextPartitionIndex;
    }

    public void init() {
        title       = "";
        text        = "";
        nTextPartitionIndex = 0;
        speed       = 300;
        mode        = Mode.TEXT;
        date        = new Date();
        words       = 0;
        urlLink     = "https://";
        speedState  = SpeedState.SLOW;

        bShouldSaveToDB = false;
    }

        /// --- Getters and Setters --- ///

    public long getId() {
        return _id;
    }

    public void setId(long _id) {
        this._id = _id;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) { this.mode = mode; }

    public void setModeInt(int mode) {
        if (mode == Mode.TEXT.ordinal()) {
            setMode(Mode.TEXT);
        } else if (mode == Mode.URL.ordinal()) {
            setMode(Mode.URL);
        }
    }

    public SpeedState getSpeedState() {
        return speedState;
    }

    public void setSpeedState(SpeedState speedState) {
        this.speedState = speedState;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getWords() {
        return words;
    }

    public void setWords(int words) {
        this.words = words;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
        if (speed < 250) {
            speedState = SpeedState.SLOW;
        } else if (speed < 450) {
            speedState = SpeedState.MODERATE;
        } else if (speed < 750) {
            speedState = SpeedState.FAST;
        } else {
            speedState = SpeedState.VERY_FAST;
        }
    }


    // not used
    public String getUrlLink() {
        return urlLink;
    }

    public void setUrlLink(String urlLink) {
        this.urlLink = urlLink;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
