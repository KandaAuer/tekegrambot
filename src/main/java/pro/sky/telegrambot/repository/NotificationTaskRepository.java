package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pro.sky.telegrambot.entity.NotificationTask;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    // Выбираем только уведомления, которые должны отправиться
    @Query("SELECT n FROM NotificationTask n WHERE n.notificationTime <= :currentTime")
    List<NotificationTask> findReadyNotifications(@Param("currentTime") LocalDateTime currentTime);

    // Удаляем все отправленные уведомления одним SQL-запросом
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationTask n WHERE n.notificationTime <= :currentTime")
    void deleteOldNotifications(@Param("currentTime") LocalDateTime currentTime);
}
