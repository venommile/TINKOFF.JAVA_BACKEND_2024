package edu.java.scrapper.repositories.jdbc;

import edu.java.scrapper.IntegrationTest;
import edu.java.scrapper.exceptions.EntityNotFoundException;
import edu.java.scrapper.models.Link;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class JdbcLinkRepositoryTest extends IntegrationTest {
    private static JooqChatRepository jdbcChatRepository;
    private static JooqLinkRepository jdbcLinkRepository;

    @BeforeAll
    static void setUp() {
        jdbcChatRepository = new JooqChatRepository(jdbcTemplate);
        jdbcLinkRepository = new JooqLinkRepository(jdbcTemplate);
    }

    @AfterEach
    void clear() {
        jdbcChatRepository.removeAll();
        jdbcLinkRepository.removeAll();
    }

    @Test
    void add() {
        // Arrange
        Long chatId = 1L;
        URI linkUrl = URI.create("https://first.com");

        jdbcChatRepository.add(chatId);

        // Act
        Link link = jdbcLinkRepository.add(chatId, linkUrl);
        List<Link> links = jdbcLinkRepository.findAll();

        // Assert
        assertThat(links).containsOnly(
            link
        );
    }

    @Test
    void addDuplicate() {
        // Arrange
        Long chatId = 1L;
        URI linkUrl = URI.create("https://first.com");

        jdbcChatRepository.add(chatId);
        jdbcLinkRepository.add(chatId, linkUrl);

        // Act
        Throwable thrown = catchThrowable(() -> {
            jdbcLinkRepository.add(chatId, linkUrl);
        });

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void remove() {
        // Arrange
        Long chatId = 1L;
        URI linkUrl = URI.create("https://first.com");

        jdbcChatRepository.add(chatId);
        Link addedLink = jdbcLinkRepository.add(chatId, linkUrl);

        // Act
        jdbcLinkRepository.remove(addedLink.id());
        List<Link> links = jdbcLinkRepository.findAll();

        // Assert
        assertThat(links).isEmpty();
    }

    @Test
    void removeEntityNotFound() {
        // Arrange
        Long chatId = 1L;
        URI linkUrl = URI.create("https://first.com");

        // Act
        Throwable thrown = catchThrowable(() -> {
            jdbcLinkRepository.remove(chatId, linkUrl);
        });

        // Assert
        assertThat(thrown).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void removeFromChatsLinks() {
        // Arrange
        Long chat1Id = 1L;
        Long chat2Id = 2L;
        URI link1Url = URI.create("https://first.com");
        URI link2Url = URI.create("https://second.com");

        jdbcChatRepository.add(chat1Id);
        jdbcChatRepository.add(chat2Id);

        jdbcLinkRepository.add(chat1Id, link1Url);
        jdbcLinkRepository.add(chat1Id, link2Url);
        Link link21 = jdbcLinkRepository.add(chat2Id, link1Url);
        Link link22 = jdbcLinkRepository.add(chat2Id, link2Url);

        // Act
        jdbcLinkRepository.remove(chat1Id, link1Url);
        jdbcLinkRepository.remove(chat1Id, link2Url);
        List<Link> links = jdbcLinkRepository.findAll();

        // Assert
        assertThat(links).containsOnly(
            link21, link22
        );
    }

    @Test
    void removeUnusedLinks() {
        // Arrange
        Long chat1Id = 1L;
        URI link1Url = URI.create("https://first.com");
        URI link2Url = URI.create("https://second.com");
        URI link3Url = URI.create("https://third.com");

        jdbcChatRepository.add(chat1Id);

        jdbcLinkRepository.add(chat1Id, link1Url);
        jdbcLinkRepository.add(chat1Id, link2Url);
        Link link3 = jdbcLinkRepository.add(chat1Id, link3Url);

        jdbcLinkRepository.remove(chat1Id, link1Url);
        jdbcLinkRepository.remove(chat1Id, link2Url);

        // Act
        jdbcLinkRepository.removeUnusedLinks();
        List<Link> links = jdbcLinkRepository.findAll();

        // Assert
        assertThat(links).containsOnly(
            link3
        );
    }

    @Test
    void findByUrl() {
        // Arrange
        Long chat1Id = 1L;
        URI link1Url = URI.create("https://first.com");
        URI link2Url = URI.create("https://second.com");

        jdbcChatRepository.add(chat1Id);

        Link expectedLink = jdbcLinkRepository.add(chat1Id, link1Url);
        jdbcLinkRepository.add(chat1Id, link2Url);

        // Act
        Link actualLink = jdbcLinkRepository.findByUrl(link1Url);

        // Assert
        assertThat(expectedLink).isEqualTo(actualLink);
    }

    @Test
    void findAll() {
        // Arrange
        Long chat1Id = 1L;
        Long chat2Id = 2L;
        URI link1Url = URI.create("https://first.com");
        URI link2Url = URI.create("https://second.com");

        jdbcChatRepository.add(chat1Id);
        jdbcChatRepository.add(chat2Id);

        Link link1 = jdbcLinkRepository.add(chat1Id, link1Url);
        Link link2 = jdbcLinkRepository.add(chat1Id, link2Url);
        Link link3 = jdbcLinkRepository.add(chat2Id, link1Url);
        Link link4 = jdbcLinkRepository.add(chat2Id, link2Url);

        // Act
        List<Link> links = jdbcLinkRepository.findAll();

        // Assert
        assertThat(links).containsOnly(
            link1, link2, link3, link4
        );
    }

    @Test
    void findAllLinksByChatId() {
        // Arrange
        Long chat1Id = 1L;
        Long chat2Id = 2L;
        URI link1Url = URI.create("https://first.com");
        URI link2Url = URI.create("https://second.com");
        URI link3Url = URI.create("https://third.com");

        jdbcChatRepository.add(chat1Id);
        jdbcChatRepository.add(chat2Id);

        Link link1 = jdbcLinkRepository.add(chat1Id, link1Url);
        Link link2 = jdbcLinkRepository.add(chat1Id, link2Url);
        jdbcLinkRepository.add(chat2Id, link2Url);
        jdbcLinkRepository.add(chat2Id, link3Url);

        // Act
        List<Link> links = jdbcLinkRepository.findAllLinksByChatId(chat1Id);

        // Assert
        assertThat(links).containsOnly(
            link1, link2
        );
    }

    @Test
    void findByOldestUpdates() throws InterruptedException {
        // Arrange
        Long chat1Id = 1L;
        Long chat2Id = 2L;
        URI link1Url = URI.create("https://first.com");
        URI link2Url = URI.create("https://second.com");

        jdbcChatRepository.add(chat1Id);
        jdbcChatRepository.add(chat2Id);


        jdbcLinkRepository.add(chat1Id, link1Url);

        sleep(100);
        jdbcLinkRepository.add(chat1Id, link2Url);

        sleep(100);
        Link link3 = jdbcLinkRepository.add(chat2Id, link1Url);

        sleep(100);
        Link link4 = jdbcLinkRepository.add(chat2Id, link2Url);

        // Act
        List<Link> links = jdbcLinkRepository.findByOldestUpdates(2);

        // Assert
        assertThat(links).containsOnly(
            link3, link4
        );
    }
}
