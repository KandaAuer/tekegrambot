package pro.sky.telegrambot.scheduler;

import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);
    private final NotificationTaskRepository notificationTaskRepository;
    private final TelegramBot bot;

    public NotificationScheduler(NotificationTaskRepository notificationTaskRepository, TelegramBot bot) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.bot = bot;
    }

    @Scheduled(cron = "0 * * * * *") // Запускается каждую минуту
    public void checkNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<NotificationTask> notifications = notificationTaskRepository.findAll();
        List<NotificationTask> notificationsToDelete = new ArrayList<>(); // Создаём список на удаление

        for (NotificationTask notification : notifications) {
            if (notification.getNotificationTime().truncatedTo(ChronoUnit.MINUTES).equals(now)) {
                sendMessage(notification.getChatId(), "🔔 Напоминание: " + notification.getMessage());
                notificationsToDelete.add(notification); // Добавляем в список на удаление
            }
        }

        // Удаляем все уведомления после цикла
        for(NotificationTask notification: notificationsToDelete) {
            try {
                notificationTaskRepository.delete(notification);
            } catch (Exception e) {
                logger.error("Ошибка при удалении уведомления: {}", e.getMessage());
            }
        }
    }


    private void sendMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage(chatId, messageText);
        SendResponse response = bot.execute(message);

        if (!response.isOk()) {
            logger.error("Ошибка при отправке сообщения: {} - {}", response.errorCode(), response.description());
        }
    }
}
