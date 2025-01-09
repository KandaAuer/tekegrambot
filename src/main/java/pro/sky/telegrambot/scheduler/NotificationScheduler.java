package pro.sky.telegrambot.scheduler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;
    private final TelegramBot bot;

    public NotificationScheduler(NotificationRepository notificationRepository, TelegramBot bot) {
        this.notificationRepository = notificationRepository;
        this.bot = bot;
        setupUpdatesListener();
    }

    private void setupUpdatesListener() {
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null && update.message().text() != null) {
                    String messageText = update.message().text();
                    long chatId = update.message().chat().id();

                    // Обработка команды добавления уведомления
                    if (messageText.startsWith("/add ")) {
                        processAddNotification(messageText.substring(5), chatId);
                    } else if (messageText.startsWith("/show")) {
                        processShowNotifications(chatId);
                    }
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }


    private void processAddNotification(String messageText, long chatId) {
        // Парсинг сообщения и времени
        Pattern pattern = Pattern.compile("(.+)\\s+at\\s+(\\d{2}):(\\d{2})");
        Matcher matcher = pattern.matcher(messageText);

        if (matcher.find()) {
            String text = matcher.group(1);
            int hour = Integer.parseInt(matcher.group(2));
            int minute = Integer.parseInt(matcher.group(3));

            LocalDateTime notificationTime = LocalDateTime.now()
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .truncatedTo(ChronoUnit.MINUTES);


            Notification notification = new Notification(chatId, text, notificationTime);
            notificationRepository.save(notification);

            sendMessage(chatId, "Уведомление добавлено: " + text + " на " + notificationTime);
        } else {
            sendMessage(chatId, "Неверный формат команды. Используйте: /add <текст> at <HH>:<MM>");
        }
    }

    private void processShowNotifications(long chatId) {
        List<Notification> notifications = notificationRepository.findByChatId(chatId);
        if (notifications.isEmpty()) {
            sendMessage(chatId, "У вас нет сохраненных уведомлений.");
        } else {
            StringBuilder message = new StringBuilder("Ваши уведомления:\n");
            for (Notification notification : notifications) {
                message.append("- ").append(notification.getText()).append(" at ").append(notification.getNotificationTime()).append("\n");
            }
            sendMessage(chatId, message.toString());
        }
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
