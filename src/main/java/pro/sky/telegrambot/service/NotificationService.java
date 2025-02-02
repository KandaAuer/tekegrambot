package pro.sky.telegrambot.service;

import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationTaskRepository notificationTaskRepository;
    private final TelegramBot telegramBot;

    public NotificationService(NotificationTaskRepository notificationTaskRepository, TelegramBot telegramBot) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
    }

    @Scheduled(cron = "0 * * * * *") // Выполнять каждую минуту
    public void sendNotifications() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        List<NotificationTask> tasksToSend = notificationTaskRepository.findAll().stream()
                .filter(task -> task.getNotificationTime().withSecond(0).withNano(0).equals(now))
                .toList();

        for (NotificationTask task : tasksToSend) {
            SendMessage message = new SendMessage(task.getChatId(), task.getMessage());
            SendResponse response = telegramBot.execute(message);
            if (response.isOk()) {
                logger.info("Notification sent to chat {}: {}", task.getChatId(), task.getMessage());
                notificationTaskRepository.delete(task);
            } else {
                logger.error("Failed to send notification to chat {}: {}", task.getChatId(), response.description());
            }
        }

    }
}
