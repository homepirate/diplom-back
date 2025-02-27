package com.example.diplom.repositories;

import com.example.diplom.models.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findBySenderIdAndReceiverId(String senderId, String receiverId);
}

