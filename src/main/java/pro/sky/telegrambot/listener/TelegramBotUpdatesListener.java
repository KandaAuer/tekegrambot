package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;


@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final TelegramBot telegramBot;
    public TelegramBotUpdatesListener(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }
    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }
    @Override
    public int process(List<Update> updates) {
        try {
            updates.forEach(update -> {
                logger.info("Processing update: {}", update);
                if (update.message() != null && update.message().text() != null &&
                        update.message().text().equals("/start")) {
                    long chatId = update.message().chat().id();
                    SendMessage message = new SendMessage(chatId, "Привет! Я ваш бот-напоминалка.");
                    SendResponse response = telegramBot.execute(message);
                    if (!response.isOk()) {
                        logger.error("Не удалось отправить сообщение: {}", response.description());
                    }

                }
            });

        } catch (Exception e) {
            logger.error("Ошибка при обработке обновления", e);
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
