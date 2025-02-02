package pro.sky.telegrambot.repository;

import pro.sky.telegrambot.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByNotificationTime(LocalDateTime notificationTime);
    List<Notification> findByChatId(Long chatId);
}
