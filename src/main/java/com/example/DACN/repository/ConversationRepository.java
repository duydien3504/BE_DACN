package com.example.DACN.repository;

import com.example.DACN.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE (c.userOne.userId = :userId OR c.userTwo.userId = :userId) ORDER BY c.updatedAt DESC")
    List<Conversation> findUserConversations(@Param("userId") UUID userId);

    @Query("SELECT c FROM Conversation c WHERE (c.userOne.userId = :user1Id AND c.userTwo.userId = :user2Id) OR (c.userOne.userId = :user2Id AND c.userTwo.userId = :user1Id)")
    Optional<Conversation> findConversationBetweenUsers(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);
}
