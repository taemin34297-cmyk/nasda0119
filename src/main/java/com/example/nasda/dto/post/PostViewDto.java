package com.example.nasda.dto.post;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostViewDto {
    private Integer id;
    private String title;
    private String content;
    private String category;
    private AuthorDto author;
    private List<String> images;
    private LocalDateTime createdAt;
    private boolean isOwner;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private String nickname;
    }
}