package pro.sky.telegrambot.listener;

import jakarta.annotation.PostConstruct;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;

    // Новый формат: /add текст на 10.02.2025 15:30
    private static final Pattern PATTERN = Pattern.compile("(.+)\\s+на\\s+(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

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
        for (Update update : updates) {
            try {
                if (update.message() != null && update.message().text() != null) {
                    String messageText = update.message().text();
                    long chatId = update.message().chat().id();

                    if (messageText.startsWith("/add ")) {
                        processAddNotification(messageText.substring(5), chatId);
                    } else if (messageText.equals("/show")) {
                        processShowNotifications(chatId);
                    } else if (messageText.equals("/start")) {
                        sendMessage(chatId, "Привет! Я ваш бот-напоминалка.\n" + "Используйте команду /add <текст> на <дд.ММ.гггг> <чч:мм> для создания напоминания.\n" +
                                "Используйте команду /show чтобы просмотреть все свои напоминания.");
                    }
                }
            } catch (Exception e) {
                logger.error("Ошибка при обработке обновления", e);
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void processAddNotification(String messageText, long chatId) {
        Matcher matcher = PATTERN.matcher(messageText);

        if (matcher.find()) {
            String text = matcher.group(1);
            String datePart = matcher.group(2);
            String timePart = matcher.group(3);

            LocalDateTime notificationTime = LocalDateTime.parse(datePart + " " + timePart, DATE_FORMATTER);

            if (notificationTime.isBefore(LocalDateTime.now())) {
                sendMessage(chatId, "Ошибка: Нельзя устанавливать напоминания в прошлом!");
                return;
            }

            NotificationTask notification = new NotificationTask(chatId, text, notificationTime);
            notificationTaskRepository.save(notification);

            sendMessage(chatId, "✅ Уведомление добавлено: " + text + " на " + notificationTime.format(DATE_FORMATTER));
        } else {
            sendMessage(chatId, "❌ Неверный формат.\nПример: /add Позвонить маме на 10.02.2025 15:30");
        }
    }
    private void processShowNotifications(long chatId) {
        List<NotificationTask> notifications = notificationTaskRepository.findByChatId(chatId);
        if (notifications.isEmpty()) {
            sendMessage(chatId, "У вас нет активных напоминаний.");
        } else {
            StringBuilder sb = new StringBuilder("Ваши напоминания:\n");
            for (NotificationTask notification : notifications) {
                sb.append("- ").append(notification.getMessage())
                        .append(" на ").append(notification.getNotificationTime().format(DATE_FORMATTER)).append("\n");
            }
            sendMessage(chatId, sb.toString());
        }
    }


    private void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage(chatId, text);
        SendResponse response = telegramBot.execute(sendMessage);
        if (!response.isOk()) {
            logger.error("Не удалось отправить сообщение: {}", response.description());
        }
    }
}
