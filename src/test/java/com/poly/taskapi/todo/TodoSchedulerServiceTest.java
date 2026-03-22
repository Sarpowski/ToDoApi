package com.poly.taskapi.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

import com.poly.taskapi.todo.todoEnum.Priority;
import com.poly.taskapi.todo.todoEnum.RepeatType;
import com.poly.taskapi.user.User;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class TodoSchedulerServiceTest {

    @Mock
    private TodoRepository repository;

    @Mock
    private User user;

    @InjectMocks
    private TodoSchedulerService schedulerService;

    private static final Instant FIXED_DEADLINE = Instant.parse("2025-06-15T12:00:00Z");

    private Todo buildRecurringTodo(RepeatType repeatType, Instant deadline) {
        return Todo.builder()
                .id(UUID.randomUUID())
                .title("Buy groceries")
                .content("Milk, eggs, bread")
                .deadline(deadline)
                .done(true)
                .isDeleted(false)
                .priority(Priority.HIGH) 
                .repeatType(repeatType)
                .user(user)
                .build();
    }

    private Instant expectedDeadline(Instant base, RepeatType repeatType) {
        var zdt = base.atZone(ZoneOffset.UTC);
        return switch (repeatType) {
            case DAILY   -> zdt.plusDays(1).toInstant();
            case WEEKLY  -> zdt.plusWeeks(1).toInstant();
            case MONTHLY -> zdt.plusMonths(1).toInstant();
            case YEARLY  -> zdt.plusYears(1).toInstant();
        };
    }


    @Test
    void processRecurringTodos_whenNoTodosFound_neverCallsSave() {
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of());

        schedulerService.processRecurringTodos();

        verify(repository, never()).save(any());
    }

    @Test
    void processRecurringTodos_whenNoTodosFound_stillQueriesRepository() {
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of());

        schedulerService.processRecurringTodos();

        verify(repository, times(1)).findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse();
    }

    @ParameterizedTest
    @EnumSource(RepeatType.class)
    void processRecurringTodos_cloneCopiesAllFieldsFromOriginal(RepeatType repeatType) {
        Todo original = buildRecurringTodo(repeatType, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, times(2)).save(captor.capture());
        Todo clone = captor.getAllValues().get(0); // first save = clone

        assertThat(clone.getTitle()).isEqualTo(original.getTitle());
        assertThat(clone.getContent()).isEqualTo(original.getContent());
        assertThat(clone.getPriority()).isEqualTo(original.getPriority());
        assertThat(clone.getRepeatType()).isEqualTo(repeatType);
        assertThat(clone.getUser()).isEqualTo(user);
    }

    @ParameterizedTest
    @EnumSource(RepeatType.class)
    void processRecurringTodos_cloneIsNeitherDoneNorDeleted(RepeatType repeatType) {
        Todo original = buildRecurringTodo(repeatType, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, times(2)).save(captor.capture());
        Todo clone = captor.getAllValues().get(0);

        assertThat(clone.isDone()).isFalse();
        assertThat(clone.isDeleted()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RepeatType.class)
    void processRecurringTodos_cloneHasOriginalAsParent(RepeatType repeatType) {
        Todo original = buildRecurringTodo(repeatType, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, times(2)).save(captor.capture());
        Todo clone = captor.getAllValues().get(0);

        assertThat(clone.getParentTodo()).isEqualTo(original);
    }


    @ParameterizedTest
    @EnumSource(RepeatType.class)
    void processRecurringTodos_cloneDeadlineIsShiftedCorrectlyPerRepeatType(RepeatType repeatType) {
        Todo original = buildRecurringTodo(repeatType, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, times(2)).save(captor.capture());
        Todo clone = captor.getAllValues().get(0);

        assertThat(clone.getDeadline()).isEqualTo(expectedDeadline(FIXED_DEADLINE, repeatType));
    }

    @Test
    void processRecurringTodos_daily_addsExactlyOneDay() {
        Todo original = buildRecurringTodo(RepeatType.DAILY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getAllValues().get(0).getDeadline())
                .isEqualTo(Instant.parse("2025-06-16T12:00:00Z"));
    }

    @Test
    void processRecurringTodos_weekly_addsExactlyOneWeek() {
        Todo original = buildRecurringTodo(RepeatType.WEEKLY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getAllValues().get(0).getDeadline())
                .isEqualTo(Instant.parse("2025-06-22T12:00:00Z"));
    }

    @Test
    void processRecurringTodos_monthly_addsExactlyOneMonth() {
        Todo original = buildRecurringTodo(RepeatType.MONTHLY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getAllValues().get(0).getDeadline())
                .isEqualTo(Instant.parse("2025-07-15T12:00:00Z"));
    }

    @Test
    void processRecurringTodos_yearly_addsExactlyOneYear() {
        Todo original = buildRecurringTodo(RepeatType.YEARLY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getAllValues().get(0).getDeadline())
                .isEqualTo(Instant.parse("2026-06-15T12:00:00Z"));
    }

    @Test
    void processRecurringTodos_whenDeadlineIsNull_usesNowAsBase() {
        Todo original = buildRecurringTodo(RepeatType.DAILY, null);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        Instant before = Instant.now();
        schedulerService.processRecurringTodos();
        Instant after = Instant.now();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        Todo clone = captor.getAllValues().get(0);

        assertThat(clone.getDeadline())
                .isAfterOrEqualTo(before.plusSeconds(86_400))
                .isBeforeOrEqualTo(after.plusSeconds(86_400));
    }

    @Test
    void processRecurringTodos_nullifiesRepeatTypeOnOriginal() {
        Todo original = buildRecurringTodo(RepeatType.DAILY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        assertThat(original.getRepeatType()).isNull();
    }

    @Test
    void processRecurringTodos_savesOriginalAfterNullingRepeatType() {
        Todo original = buildRecurringTodo(RepeatType.WEEKLY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(original));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, times(2)).save(captor.capture());
        Todo savedOriginal = captor.getAllValues().get(1); 
        assertThat(savedOriginal.getRepeatType()).isNull();
        assertThat(savedOriginal.getId()).isEqualTo(original.getId());
    }

    @Test
    void processRecurringTodos_withMultipleTodos_savesEachCloneAndOriginal() {
        Todo t1 = buildRecurringTodo(RepeatType.DAILY, FIXED_DEADLINE);
        Todo t2 = buildRecurringTodo(RepeatType.MONTHLY, FIXED_DEADLINE);
        Todo t3 = buildRecurringTodo(RepeatType.YEARLY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(t1, t2, t3));

        schedulerService.processRecurringTodos();

        verify(repository, times(6)).save(any(Todo.class));
    }

    @Test
    void processRecurringTodos_withMultipleTodos_nullifiesRepeatTypeOnAllOriginals() {
        Todo t1 = buildRecurringTodo(RepeatType.DAILY, FIXED_DEADLINE);
        Todo t2 = buildRecurringTodo(RepeatType.WEEKLY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(t1, t2));

        schedulerService.processRecurringTodos();

        assertThat(t1.getRepeatType()).isNull();
        assertThat(t2.getRepeatType()).isNull();
    }

    @Test
    void processRecurringTodos_withMultipleTodos_eachCloneHasCorrectParentTodo() {
        Todo t1 = buildRecurringTodo(RepeatType.DAILY, FIXED_DEADLINE);
        Todo t2 = buildRecurringTodo(RepeatType.WEEKLY, FIXED_DEADLINE);
        when(repository.findByRepeatTypeNotNullAndDoneTrueAndIsDeletedFalse())
                .thenReturn(List.of(t1, t2));

        schedulerService.processRecurringTodos();

        ArgumentCaptor<Todo> captor = forClass(Todo.class);
        verify(repository, times(4)).save(captor.capture());

        assertThat(captor.getAllValues().get(0).getParentTodo()).isEqualTo(t1);
        assertThat(captor.getAllValues().get(2).getParentTodo()).isEqualTo(t2);
    }
}