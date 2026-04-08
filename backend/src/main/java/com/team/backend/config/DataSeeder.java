package com.team.backend.config;

import com.team.backend.entity.Task;
import com.team.backend.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final TaskRepository taskRepository;

    public DataSeeder(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(String... args) {
        if (taskRepository.count() == 0) {
            taskRepository.save(new Task("Setup backend", "DONE"));
            taskRepository.save(new Task("Connect MySQL", "IN_PROGRESS"));
        }
    }
}