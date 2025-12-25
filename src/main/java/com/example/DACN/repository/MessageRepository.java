package com.example.DACN.repository;

import com.example.DACN.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    List<Message> findByConversationConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversationConversationIdAndIsReadFalseAndSenderUserIdNot(Long conversationId, UUID currentUserId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversation.conversationId = :conversationId AND m.sender.userId != :currentUserId")
    void markConversationMessagesAsRead(@Param("conversationId") Long conversationId,
            @Param("currentUserId") UUID currentUserId);
}
