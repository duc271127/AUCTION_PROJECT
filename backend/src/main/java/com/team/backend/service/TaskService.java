package com.team.backend.service;

import com.team.backend.dto.CreateTaskRequest;
import com.team.backend.dto.UpdateTaskRequest;
import com.team.backend.entity.Task;
import com.team.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();
    private Long nextId = 1L;

    public TaskService() {
        tasks.add(new Task(nextId++, "Setup backend", "DONE"));
        tasks.add(new Task(nextId++, "Create task module", "IN_PROGRESS"));
    }

    public List<Task> getAllTasks() {
        return tasks;
    }

    public Task getTaskById(Long id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
    }

    public Task createTask(CreateTaskRequest request) {
        Task newTask = new Task(nextId++, request.getTitle(), "NEW");
        tasks.add(newTask);
        return newTask;
    }

    public Task updateTask(Long id, UpdateTaskRequest request) {
        Task task = getTaskById(id);
        task.setTitle(request.getTitle());
        task.setStatus(request.getStatus());
        return task;
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        tasks.remove(task);
    }
}