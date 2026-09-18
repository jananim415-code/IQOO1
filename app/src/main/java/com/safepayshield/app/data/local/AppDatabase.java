package com.safepayshield.app.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.safepayshield.app.data.models.HistoryItem;

@Database(entities = {HistoryItem.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE history ADD COLUMN amount TEXT");
            database.execSQL("ALTER TABLE history ADD COLUMN reasons TEXT");
        }
    };

    public abstract HistoryDao historyDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "safepay_shield_db")
                                    .addMigrations(MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
