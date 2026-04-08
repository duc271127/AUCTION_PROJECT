package com.team.backend.controller;

import com.team.backend.dto.CreateTaskRequest;
import com.team.backend.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Map<String, Object>> getAllTasks() {
        return taskService.getAllTasks();
    }

    @PostMapping
    public Map<String, Object> createTask(@RequestBody CreateTaskRequest request) {
        return taskService.createTask(request.getTitle());
    }
}