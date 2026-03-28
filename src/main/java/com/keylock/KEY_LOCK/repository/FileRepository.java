package com.keylock.KEY_LOCK.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.keylock.KEY_LOCK.model.FileRecord;

@Repository
public interface FileRepository extends JpaRepository<FileRecord, Long> {

    List<FileRecord> findAllByOrderByUploadedAtDesc();
}
