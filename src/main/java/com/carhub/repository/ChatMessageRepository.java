package com.carhub.repository;

import com.carhub.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderBySentAtAsc(Long senderId1, Long receiverId1, Long senderId2, Long SenderId2);

    @Query("SELECT c FROM ChatMessage c WHERE c.id IN " +
            "(SELECT MAX(c2.id) FROM ChatMessage c2 " +
            "WHERE c2.sender.id = :userId OR c2.receiver.id = :userId " +
            "GROUP BY CASE WHEN c2.sender.id = :userId THEN c2.receiver.id ELSE c2.sender.id END) " +
            "ORDER BY c.sentAt DESC")
    List<ChatMessage> findRecentChatsByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatMessage c " +
            "WHERE (c.sender.id = :user1 AND c.receiver.id = :user2) " +
            "   OR (c.sender.id = :user2 AND c.receiver.id = :user1)")
    Page<ChatMessage> findChatHistoryWithPagination(@Param("user1") Long user1, @Param("user2") Long user2, Pageable pageable);
}
