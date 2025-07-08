package com.sisjuan.utilities;

import com.sisjuan.models.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.LinkedHashMap;
import java.util.Map;

public class StudentCache {
    private static StudentCache instance;
    private static Map<String, CacheEntry> cacheMap = Map.of();
    private final int maxSize;
    private static long cacheDuration = 0; // in milliseconds

    private StudentCache(int maxSize, long cacheDuration) {
        cacheMap = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxSize;
            }
        };
        this.maxSize = maxSize;
        StudentCache.cacheDuration = cacheDuration;
    }

    public static synchronized StudentCache getInstance() {
        if (instance == null) {
            // Keep 10 most recent subjects for 30 minutes
            instance = new StudentCache(10, 30 * 60 * 1000);
        }
        return instance;
    }

    public void put(String subjectCode, ObservableList<Student> students) {
        cacheMap.put(subjectCode, new CacheEntry(students, System.currentTimeMillis()));
    }

    public static ObservableList<Student> get(String subjectCode) {
        CacheEntry entry = cacheMap.get(subjectCode);
        if (entry == null) return null;

        // Check if the cache entry is expired
        if (System.currentTimeMillis() - entry.timestamp > cacheDuration) {
            cacheMap.remove(subjectCode);
            return null;
        }

        return FXCollections.observableArrayList(entry.data); // Return a copy
    }

    public void clear() {
        cacheMap.clear();
    }

    public void remove(String subjectCode) {
        cacheMap.remove(subjectCode);
    }

    private static class CacheEntry {
        ObservableList<Student> data;
        long timestamp;

        CacheEntry(ObservableList<Student> data, long timestamp) {
            this.data = FXCollections.observableArrayList(data); // Store a copy
            this.timestamp = timestamp;
        }
    }
}