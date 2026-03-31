package com.example.demo.repositories;

import com.example.demo.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByReceiverIdOrderByTimeCreatedDesc(String receiverId);

    List<Notification> findBySenderId(String senderId);

    Optional<Notification> findByIdAndReceiverId(String id, String receiverId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.read = true where n.receiver.id = :receiverId and n.read = false")
    int markAllAsReadByReceiverId(@Param("receiverId") String receiverId);
}
