package com.team.backend.service;

import com.team.backend.dto.CreateTaskRequest;
import com.team.backend.dto.UpdateTaskRequest;
import com.team.backend.entity.Task;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
    }

    public Task createTask(CreateTaskRequest request) {
        Task newTask = new Task(request.getTitle(), "NEW");
        return taskRepository.save(newTask);
    }

    public Task updateTask(Long id, UpdateTaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));

        task.setTitle(request.getTitle());
        task.setStatus(request.getStatus());

        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));

        taskRepository.delete(task);
    }
}