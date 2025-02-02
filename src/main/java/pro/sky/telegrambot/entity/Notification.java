package pro.sky.telegrambot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "notification_time", nullable = false)
    private LocalDateTime notificationTime;

    public Notification(Long chatId, String text, LocalDateTime notificationTime) {
        this.chatId = chatId;
        this.text = text;
        this.notificationTime = notificationTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(chatId, that.chatId) &&
                Objects.equals(text, that.text) &&
                Objects.equals(notificationTime, that.notificationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chatId, text, notificationTime);
    }
}
