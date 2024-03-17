package edu.java.scrapper.services.jooq.updaters;

import edu.java.scrapper.api.models.LinkUpdateRequest;
import edu.java.scrapper.clients.BotWebClient;
import edu.java.scrapper.clients.stackoverflow.Client;
import edu.java.scrapper.dto.stackoverflow.Response;
import edu.java.scrapper.models.Chat;
import edu.java.scrapper.models.Link;
import edu.java.scrapper.repositories.ChatRepository;
import edu.java.scrapper.repositories.LinkRepository;
import edu.java.scrapper.services.LinkUpdater;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class JooqStackOverflowLinkUpdater implements LinkUpdater {
    private final Client stackOverflowRegularWebClient;
    private final ChatRepository jooqChatRepository;
    private final LinkRepository jooqLinkRepository;
    private final BotWebClient botWebClient;

    @Override
    public int process(Link link) {
        String[] args = processLink(link.url());

        long number = Long.parseLong(args[args.length - 1]);

        Response stackOverflowResponse =
            stackOverflowRegularWebClient.fetchLatestModified(number);

        if (stackOverflowResponse.lastActivityDate().isAfter(link.lastUpdate())) {
            List<Long> chatIds = jooqChatRepository
                .findAllChatsByUrl(link.url())
                .stream()
                .map(Chat::id)
                .collect(Collectors.toList());

            try {
                botWebClient.sendUpdate(new LinkUpdateRequest(
                    link.id(),
                    link.url(),
                    getDescription(stackOverflowResponse),
                    chatIds
                ));
            } catch (Exception ignored) {
            }

            jooqLinkRepository.setLastUpdate(link.url(), stackOverflowResponse.lastActivityDate());
            return 1;
        }
        return 0;
    }

    @Override
    public boolean supports(URI url) {
        return url.toString().startsWith("https://stackoverflow.com/questions/");
    }

    @Override
    public String[] processLink(URI url) {
        return url.toString().split("/");
    }

    @Override
    public String getDomain() {
        return "stackoverflow.com";
    }

    @Override
    public void setLastUpdate(Link link) {
        String[] args = processLink(link.url());

        long number = Long.parseLong(args[args.length - 1]);

        Response stackOverflowResponse =
            stackOverflowRegularWebClient.fetchLatestModified(number);

        jooqLinkRepository.setLastUpdate(link.url(), stackOverflowResponse.lastActivityDate());
    }

    private String getDescription(Response stackOverflowResponse) {
        return
            "Новый вопрос с номером №" + stackOverflowResponse.questionId() + System.lineSeparator()
            + "от " + stackOverflowResponse.owner().displayName();
    }
}
