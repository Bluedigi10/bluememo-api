package com.bluedigi.bluememo.todo.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.bluedigi.bluememo.todo.domain.model.TodoStatus;
import com.bluedigi.bluememo.todo.infrastructure.persistence.entity.TodoEntity;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoJpaRepository extends JpaRepository<TodoEntity, UUID> {
    boolean existsByUser_IdAndTitle(UUID userId, String title);
    boolean existsByUser_IdAndTodoId(UUID userId, UUID todoId);
    boolean existsByUser_IdAndTitleAndTodoIdNot(UUID userId, String title, UUID todoId);
    Page<TodoEntity> findAllByUser_Id(UUID user_Id, Pageable pageable);
    Page<TodoEntity> findAllByUser_IdAndStatus(UUID user_Id, TodoStatus status, Pageable pageable);
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query("DELETE FROM TodoEntity t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
