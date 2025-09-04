package com.leethublog.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 이 클래스 자체가 properties 객체가 됩니다.
@Getter
@Setter
public class NotionDatabasePropertiesDto {
    @JsonProperty("Problem")
    private TitleProperty problem = new TitleProperty();

    @JsonProperty("URL")
    private UrlProperty url = new UrlProperty();

    @JsonProperty("Last Solved")
    private DateProperty lastSolved = new DateProperty();

    @JsonProperty("Next Review")
    private DateProperty nextReview = new DateProperty();

    @JsonProperty("Attempts")
    private NumberProperty attempts = new NumberProperty();

    @JsonProperty("Difficulty")
    private SelectProperty difficulty = new SelectProperty();

    @JsonProperty("History")
    private RichTextProperty history = new RichTextProperty();

    // 각 속성 타입을 표현하는 내부 클래스들
    public static class TitleProperty {
        public Object title = new Object();
    }
    public static class UrlProperty {
        public Object url = new Object();
    }
    public static class DateProperty {
        public Object date = new Object();
    }
    public static class RichTextProperty {
        @JsonProperty("rich_text")
        public Object richText = new Object();
    }
    public static class NumberProperty {
        public NumberFormat number = new NumberFormat();
        public static class NumberFormat {
            public String format = "number";
        }
    }
    public static class SelectProperty {
        public SelectOptions select = new SelectOptions();
        public static class SelectOptions {
            public List<Option> options = List.of(
                    new Option("🔴 어려움", "red"),
                    new Option("🟡 보통", "yellow"),
                    new Option("🟢 쉬움", "green")
            );
        }
        public static class Option {
            public String name;
            public String color;
            public Option(String name, String color) {
                this.name = name;
                this.color = color;
            }
        }
    }
}