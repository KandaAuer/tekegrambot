package pro.sky.telegrambot.listener;

import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;
    private final Pattern pattern = Pattern.compile("(.+)\\s+at\\s+(\\d{2}):(\\d{2})");

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        try {
            for (Update update : updates) {
                if (update.message() != null && update.message().text() != null) {
                    String messageText = update.message().text();
                    long chatId = update.message().chat().id();

                    // Обработка команды добавления уведомления
                    if (messageText.startsWith("/add ")) {
                        processAddNotification(messageText.substring(5), chatId);
                    } else if (messageText.startsWith("/show")) {
                        processShowNotifications(chatId);
                    }else if (messageText.startsWith("/start")) {
                        SendMessage message = new SendMessage(chatId, "Привет! Я ваш бот-напоминалка.");
                        SendResponse response = telegramBot.execute(message);
                        if (!response.isOk()) {
                            logger.error("Failed to send message: {}", response.description());
                        }
                        return UpdatesListener.CONFIRMED_UPDATES_ALL;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error during processing update", e);
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void processAddNotification(String messageText, long chatId) {
        // Парсинг сообщения и времени
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


            NotificationTask notification = new NotificationTask();
            notification.setChatId(chatId);
            notification.setMessage(text);
            notification.setNotificationTime(notificationTime);
            notificationTaskRepository.save(notification);

            sendMessage(chatId, "Уведомление добавлено: " + text + " на " + notificationTime);
        } else {
            sendMessage(chatId, "Неверный формат команды. Используйте: /add <текст> at <HH>:<MM>");
        }
    }

    private void processShowNotifications(long chatId) {
        List<NotificationTask> notifications = notificationTaskRepository.findByChatId(chatId);
        if (notifications.isEmpty()) {
            sendMessage(chatId, "У вас нет сохраненных уведомлений.");
        } else {
            StringBuilder message = new StringBuilder("Ваши уведомления:\n");
            for (NotificationTask notification : notifications) {
                message.append("- ").append(notification.getMessage()).append(" at ").append(notification.getNotificationTime()).append("\n");
            }
            sendMessage(chatId, message.toString());
        }
    }


    private void sendMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage(chatId, messageText);
        SendResponse response = telegramBot.execute(message);

        if (!response.isOk()) {
            System.err.println("Ошибка при отправке сообщения: " + response.errorCode() + " - " + response.description());
        }
    }
}
