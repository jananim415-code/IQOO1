package com.safepayshield.app.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.safepayshield.app.data.models.HistoryItem;

import java.util.List;

@Dao
public interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 50")
    List<HistoryItem> getAll();

    @Insert
    void insert(HistoryItem item);

    @Query("DELETE FROM history")
    void deleteAll();
}
