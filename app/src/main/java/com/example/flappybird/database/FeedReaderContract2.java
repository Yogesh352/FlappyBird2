package com.example.flappybird.database;

import android.provider.BaseColumns;

public final class FeedReaderContract2 {

    private FeedReaderContract2() {}

    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "highscore";
        public static final String COLUMN_NAME_TITLE = "player";
        public static final String COLUMN_NAME_SUBTITLE = "highscore";
    }

    static final String SQL_CREATE_ENTRIES2 =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + "(" +
                    FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    FeedEntry.COLUMN_NAME_TITLE + " TEXT," +
                    FeedEntry.COLUMN_NAME_SUBTITLE + " INTEGER)";

    static final String SQL_DELETE_ENTRIES2 =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;
}
