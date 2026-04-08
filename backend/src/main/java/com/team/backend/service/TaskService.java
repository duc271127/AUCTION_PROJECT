package com.team.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final List<Map<String, Object>> tasks = new ArrayList<>();

    public TaskService() {
        Map<String, Object> task1 = new HashMap<>();
        task1.put("id", 1L);
        task1.put("title", "Setup backend");
        task1.put("status", "DONE");

        Map<String, Object> task2 = new HashMap<>();
        task2.put("id", 2L);
        task2.put("title", "Create task module");
        task2.put("status", "IN_PROGRESS");

        tasks.add(task1);
        tasks.add(task2);
    }

    public List<Map<String, Object>> getAllTasks() {
        return tasks;
    }

    public Map<String, Object> createTask(String title) {
        Map<String, Object> newTask = new HashMap<>();
        newTask.put("id", (long) (tasks.size() + 1));
        newTask.put("title", title);
        newTask.put("status", "NEW");

        tasks.add(newTask);
        return newTask;
    }
}