package site.devtown.spadeworker.domain.article.dto;

import org.hibernate.validator.constraints.Length;
import site.devtown.spadeworker.domain.article.model.constant.ArticleStatus;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.LinkedHashSet;

public record CreateArticleRequest(

        @NotBlank(message = "게시글 제목은 필수 값입니다.")
        @Length(min = 1, max = 30, message = "게시글 제목 길이 제한은 1이상 30이하 입니다.")
        String title,

        @NotBlank(message = "게시글 본문은 필수 값입니다.")
        @Length(min = 1, message = "게시글 본문 길이 제한은 1이상 입니다.")
        String content,

        @Size(max = 10, message = "게시글 해시태그 개수 제한은 최대 10개 입니다.")
        LinkedHashSet<String> hashtags,

        @NotNull(message = "게시글 상태는 필수 값입니다.")
        ArticleStatus status,

        @NotNull(message = "썸네일 이미지가 없으면 \"\"을 입력해야 합니다.")
        String thumbnailImagePath
) {}