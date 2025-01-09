package pro.sky.telegrambot.scheduler;

import pro.sky.telegrambot.entity.Notification;
import pro.sky.telegrambot.repository.NotificationRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;
    private final TelegramBot bot;

    public NotificationScheduler(NotificationRepository notificationRepository, TelegramBot bot) {
        this.notificationRepository = notificationRepository;
        this.bot = bot;
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<Notification> notifications = notificationRepository.findByNotificationTime(now);

        for (Notification notification : notifications) {
            sendMessage(notification.getChatId(), "Напоминание: " + notification.getText());
            // После отправки уведомления его можно удалить или пометить как отправленное
            notificationRepository.delete(notification);
        }
    }

    private void sendMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage(chatId, messageText);
        SendResponse response = bot.execute(message);

        if (!response.isOk()) {
            System.err.println("Ошибка при отправке сообщения: " + response.errorCode() + " - " + response.description());
        }
    }
}
