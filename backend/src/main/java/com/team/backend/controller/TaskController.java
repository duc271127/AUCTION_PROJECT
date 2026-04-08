package com.team.backend.controller;

import com.team.backend.dto.ApiResponse;
import com.team.backend.dto.CreateTaskRequest;
import com.team.backend.dto.UpdateTaskRequest;
import com.team.backend.entity.Task;
import com.team.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(ApiResponse.success("Get tasks successfully", tasks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success("Get task successfully", task));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTask(@Valid @RequestBody CreateTaskRequest request) {
        Task createdTask = taskService.createTask(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create task successfully", createdTask));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        Task updatedTask = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("Update task successfully", updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Delete task successfully", null));
    }
}