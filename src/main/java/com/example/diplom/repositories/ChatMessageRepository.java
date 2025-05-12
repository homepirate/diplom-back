package com.example.diplom.repositories;

import com.example.diplom.models.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("SELECT m FROM ChatMessageEntity m WHERE (m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1) ORDER BY m.timestamp ASC")
    List<ChatMessageEntity> findBySenderIdAndReceiverId(@Param("user1") String user1, @Param("user2") String user2);
    void deleteBySenderIdOrReceiverId(String senderId, String receiverId);

    @Query("SELECT m FROM ChatMessageEntity m " +
            "WHERE m.senderId = :userId OR m.receiverId = :userId " +
            "ORDER BY m.timestamp DESC")
    List<ChatMessageEntity> findRecentMessagesByUser(@Param("userId") String userId);

}
