package com.wqh.knowledgebase.repository;

import com.wqh.knowledgebase.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
}
