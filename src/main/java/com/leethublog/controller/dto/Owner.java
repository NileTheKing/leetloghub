package com.leethublog.controller.dto; // DTO를 모아둘 패키지를 만드는 것이 좋습니다.

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Owner {

    // 워크스페이스 레벨 토큰일 경우 이 필드가 채워집니다.
    private Boolean workspace;

    // 사용자 레벨 토큰일 경우 이 필드가 채워집니다.
    private User user;

    // User 객체의 구조를 표현하는 내부 클래스
    @Getter
    @Setter
    public static class User {
        private String object;
        private String id;
        private String name;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        private String type;

        private Person person;

        @Getter
        @Setter
        public static class Person {
            private String email;
        }
    }
}