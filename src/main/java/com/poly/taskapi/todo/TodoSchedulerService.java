package com.poly.taskapi.todo;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoSchedulerService {

  private final TodoRepository repository;


  @Scheduled(cron = "${app.repeats.cron}")
  @Transactional
  void processRecurringTodos() {
    var finishedTodos = repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse();

    if (finishedTodos.isEmpty()) {
      log.info("no recurring todos to process");
      return;
    }
    int count = 0;

    for (Todo original : finishedTodos) {

    }

  }
}
