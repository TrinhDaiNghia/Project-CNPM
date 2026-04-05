package com.example.demo.repositories;

import com.example.demo.entities.Notification;
import com.example.demo.entities.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByReceiverIdOrderByTimeCreatedDesc(String receiverId);

    List<Notification> findByReceiverIdAndReadOrderByTimeCreatedDesc(String receiverId, boolean read);

    List<Notification> findByReceiverIdAndTypeOrderByTimeCreatedDesc(String receiverId, NotificationType type);

    List<Notification> findByReceiverIdAndReadAndTypeOrderByTimeCreatedDesc(
            String receiverId,
            boolean read,
            NotificationType type
    );

    List<Notification> findBySenderId(String senderId);

    Optional<Notification> findByIdAndReceiverId(String id, String receiverId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.read = true, n.readAt = :readAt where n.receiver.id = :receiverId and n.read = false")
    int markAllAsReadByReceiverId(@Param("receiverId") String receiverId, @Param("readAt") Date readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.id = :notificationId and n.receiver.id = :receiverId")
    int deleteByIdAndReceiverId(@Param("notificationId") String notificationId, @Param("receiverId") String receiverId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.receiver.id = :receiverId")
    int deleteAllByReceiverId(@Param("receiverId") String receiverId);
}
