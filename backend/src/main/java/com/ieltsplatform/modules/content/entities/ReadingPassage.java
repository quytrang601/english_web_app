package com.ieltsplatform.modules.content.entities;

import com.ieltsplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "reading_passages")
public class ReadingPassage extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(length = 10, nullable = false)
    private String level; // Ví dụ: "B1", "B2", "C1"

    @Column(length = 50, nullable = false)
    private String topic; // Ví dụ: "Environment", "Technology", "History"

    @Column(name = "word_count", nullable = false)
    private Integer wordCount;

    // 1. Constructor rỗng cho Hibernate
    public ReadingPassage() {
    }

    // 2. Constructor đầy đủ
    public ReadingPassage(String title, String body, String level, String topic, Integer wordCount) {
        this.title = title;
        this.body = body;
        this.level = level;
        this.topic = topic;
        this.wordCount = wordCount;
    }

    // 3. Tự viết Builder Pattern bằng Java thuần (tương thích mọi phiên bản Java)
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String body;
        private String level;
        private String topic;
        private Integer wordCount;

        public Builder title(String title) { this.title = title; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder level(String level) { this.level = level; return this; }
        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder wordCount(Integer wordCount) { this.wordCount = wordCount; return this; }

        public ReadingPassage build() {
            return new ReadingPassage(title, body, level, topic, wordCount);
        }
    }

    // 4. Các hàm Getter & Setter chuẩn
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
}