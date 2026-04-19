package pro.sky.telegrambot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class NotificationScheduler {

    private final NotificationTaskRepository repository;
    private final TelegramBot bot;

    public NotificationScheduler(NotificationTaskRepository repository, TelegramBot bot) {
        this.repository = repository;
        this.bot = bot;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void checkNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<NotificationTask> tasks = repository.findByNotificationDate(now);

        for (NotificationTask task : tasks) {
            try {
                bot.execute(new SendMessage(task.getChatId(), task.getMessageText()));
                repository.delete(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
