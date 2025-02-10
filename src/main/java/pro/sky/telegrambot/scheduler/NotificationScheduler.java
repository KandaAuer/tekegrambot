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
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    @Scheduled(cron = "0 * * * * *") // Каждую минуту
    @Transactional
    public void checkNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        // Загружаем ТОЛЬКО те уведомления, которые нужно отправить
        List<NotificationTask> notifications = notificationTaskRepository.findReadyNotifications(now);

        // Отправляем уведомления
        for (NotificationTask notification : notifications) {
            sendMessage(notification.getChatId(), "🔔 Напоминание: " + notification.getMessage());
        }

        // Удаляем все обработанные уведомления одним SQL-запросом (без findAll!)
        notificationTaskRepository.deleteOldNotifications(now);
    }

    private void sendMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage(chatId, messageText);
        SendResponse response = bot.execute(message);

        if (!response.isOk()) {
            logger.error("Ошибка при отправке сообщения: {} - {}", response.errorCode(), response.description());
        }
    }
}
