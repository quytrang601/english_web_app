package com.ieltsplatform.modules.content;

import com.ieltsplatform.modules.content.entities.ReadingPassage;
import com.ieltsplatform.modules.content.repository.ReadingPassageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReadingPassageRepositoryTest {

    @Autowired
    private ReadingPassageRepository readingPassageRepository;

    @Test
    @DisplayName("Should save reading passage and populate BaseEntity fields")
    void testSaveReadingPassage() {
        // 1. Prepare sample passage
        ReadingPassage passage = ReadingPassage.builder()
                .title("The History of Silk")
                .level("B2")
                .topic("History")
                .body("Silk is a natural protein fiber, some forms of which can be woven into textiles...")
                .wordCount(150)
                .build();

        // 2. Save to database
        ReadingPassage saved = readingPassageRepository.save(passage);

        // 3. Assertions
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("The History of Silk");
        assertThat(saved.getLevel()).isEqualTo("B2");
        assertThat(saved.getTopic()).isEqualTo("History");
        assertThat(saved.getWordCount()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should find reading passages by level")
    void testFindByLevel() {
        // 1. Prepare test data
        ReadingPassage p1 = ReadingPassage.builder()
                .title("Passage 1")
                .level("C1")
                .topic("Science")
                .body("Body 1...")
                .wordCount(100)
                .build();

        ReadingPassage p2 = ReadingPassage.builder()
                .title("Passage 2")
                .level("B1")
                .topic("Science")
                .body("Body 2...")
                .wordCount(200)
                .build();

        readingPassageRepository.save(p1);
        readingPassageRepository.save(p2);

        // 2. Call query method
        List<ReadingPassage> results = readingPassageRepository.findByLevel("C1");

        // 3. Assertions
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Passage 1");
    }

    @Test
    @DisplayName("Should find passages by level and topic with pagination")
    void testFindByLevelAndTopicWithPagination() {
        // 1. Insert 3 passages with level B2 and topic Technology
        for (int i = 1; i <= 3; i++) {
            ReadingPassage p = ReadingPassage.builder()
                    .title("Tech Passage " + i)
                    .level("B2")
                    .topic("Technology")
                    .body("Tech body content " + i)
                    .wordCount(100 * i)
                    .build();
            readingPassageRepository.save(p);
        }

        // 2. Request page 0 with size 2, sorted by title ascending
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("title").ascending());
        Page<ReadingPassage> pageResult = readingPassageRepository.findByLevelAndTopic("B2", "Technology", pageRequest);

        // 3. Assertions
        assertThat(pageResult.getTotalElements()).isEqualTo(3);
        assertThat(pageResult.getContent()).hasSize(2);
        assertThat(pageResult.getTotalPages()).isEqualTo(2);
        assertThat(pageResult.getContent().get(0).getTitle()).isEqualTo("Tech Passage 1");
        assertThat(pageResult.getContent().get(1).getTitle()).isEqualTo("Tech Passage 2");
    }
}