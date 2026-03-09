package com.poly.taskapi.storage;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3MetaDataRepository extends JpaRepository<S3MetaData, UUID> {

  List<S3MetaData> findByTodoIdAndIsDeletedFalse(UUID todoId);
}
