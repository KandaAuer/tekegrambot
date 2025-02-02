package pro.sky.telegrambot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_task")
@Data
@NoArgsConstructor
public class NotificationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "notification_time", nullable = false)
    private LocalDateTime notificationTime;

    public NotificationTask(Long chatId, String message, LocalDateTime notificationTime) {
        this.chatId = chatId;
        this.message = message;
        this.notificationTime = notificationTime;
    }
}
