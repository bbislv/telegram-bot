package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.NotificationTask;
import pro.sky.telegrambot.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Value("${telegram.bot.token}")
    private String token;

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository repository;

    @PostConstruct
    public void init() {
        telegramBot = new TelegramBot(token);
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message() != null && update.message().text() != null) {
                String text = update.message().text();
                Long chatId = update.message().chat().id();

                if (text.equals("/start")) {
                    SendMessage message = new SendMessage(chatId, "Привет! Я бот-напоминалка.\nОтправь мне дату и задачу в формате: 01.01.2022 20:00 Сделать домашку");
                    telegramBot.execute(message);
                } else {
                    String regex = "(\\d{2})\\.(\\d{2})\\.(\\d{4})\\s(\\d{2}):(\\d{2})\\s(.+)";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(text);

                    if (matcher.matches()) {
                        String dateStr = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3) + " " + matcher.group(4) + ":" + matcher.group(5);
                        String taskText = matcher.group(6);

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        LocalDateTime notificationTime = LocalDateTime.parse(dateStr, formatter);

                        NotificationTask task = new NotificationTask();
                        task.setChatId(chatId);
                        task.setMessageText(taskText);
                        task.setNotificationDate(notificationTime);

                        repository.save(task);
                        telegramBot.execute(new SendMessage(chatId, "Задача сохранена! Напомню: " + dateStr));
                    } else {
                        telegramBot.execute(new SendMessage(chatId, "Неверный формат. Используй: 01.01.2022 20:00 Текст"));
                    }
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
