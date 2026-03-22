package com.poly.taskapi.todo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.taskapi.todo.todoEnum.RepeatType;

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
      Instant nextDeadline = calculateNextDeadline(original.getDeadline(), original.getRepeatType());

      Todo clone = Todo.builder()
          .title(original.getTitle())
          .content(original.getContent())
          .deadline(nextDeadline)
          .done(false)
          .isDeleted(false)
          .priority(original.getPriority())
          .repeatType(original.getRepeatType())
          .user(original.getUser())
          .parentTodo(original)
          .build();

      repository.save(clone);

      original.setRepeatType(null);
      repository.save(original);

      count++;
    }

    log.info("Created {} recurring todo clones", count);
  }

  private Instant calculateNextDeadline(Instant current, RepeatType repeatType) {
    Instant base = (current != null) ? current : Instant.now();

    java.time.ZonedDateTime zdt = base.atZone(java.time.ZoneOffset.UTC);

    return switch (repeatType) {
      case DAILY -> zdt.plusDays(1).toInstant();
      case WEEKLY -> zdt.plusWeeks(1).toInstant();
      case MONTHLY -> zdt.plusMonths(1).toInstant();
      case YEARLY -> zdt.plusYears(1).toInstant();
    };
  }
}