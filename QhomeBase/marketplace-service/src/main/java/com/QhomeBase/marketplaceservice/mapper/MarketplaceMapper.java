package com.QhomeBase.marketplaceservice.mapper;

import com.QhomeBase.marketplaceservice.dto.*;
import com.QhomeBase.marketplaceservice.model.MarketplaceCategory;
import com.QhomeBase.marketplaceservice.model.MarketplaceComment;
import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.MarketplacePostImage;
import com.QhomeBase.marketplaceservice.service.ResidentInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketplaceMapper {

    private final ResidentInfoService residentInfoService;

    public PostResponse toPostResponse(MarketplacePost post) {
        // Fetch resident info for author
        ResidentInfoResponse author = null;
        try {
            author = residentInfoService.getResidentInfo(post.getResidentId());
            if (author == null) {
                System.err.println("WARNING: Author is null for post residentId: " + post.getResidentId() + ", postId: " + post.getId());
            } else {
                System.out.println("✅ Author info for post " + post.getId() + ": name=" + author.getName() + ", unitNumber=" + author.getUnitNumber());
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception when fetching author for post " + post.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Debug: Check images
        System.out.println("🖼️ [MarketplaceMapper] Post " + post.getId() + " has " + post.getImages().size() + " images");
        if (!post.getImages().isEmpty()) {
            post.getImages().forEach(img -> {
                System.out.println("  - Image ID: " + img.getId() + ", URL: " + img.getImageUrl() + ", Thumbnail: " + img.getThumbnailUrl());
            });
        } else {
            System.out.println("⚠️ [MarketplaceMapper] Post " + post.getId() + " has no images");
        }
        
        return PostResponse.builder()
                .id(post.getId())
                .residentId(post.getResidentId())
                .buildingId(post.getBuildingId())
                .title(post.getTitle())
                .description(post.getDescription())
                .price(post.getPrice())
                .category(post.getCategory())
                .categoryName(post.getCategory()) // Will be enhanced with category lookup
                .status(post.getStatus().name())
                .contactInfo(toContactInfoResponse(post.getContactInfo()))
                .location(post.getLocation())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .images(post.getImages().stream()
                        .map(this::toPostImageResponse)
                        .collect(Collectors.toList()))
                .author(author)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public PostImageResponse toPostImageResponse(MarketplacePostImage image) {
        return PostImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .thumbnailUrl(image.getThumbnailUrl())
                .sortOrder(image.getSortOrder())
                .build();
    }

    public ContactInfoResponse toContactInfoResponse(String contactInfoJson) {
        // Simple implementation - in production, use JSON parser
        if (contactInfoJson == null || contactInfoJson.isEmpty()) {
            return ContactInfoResponse.builder()
                    .showPhone(false)
                    .showEmail(false)
                    .build();
        }
        // TODO: Parse JSON properly
        return ContactInfoResponse.builder()
                .showPhone(true)
                .showEmail(false)
                .phoneDisplay("***") // Masked
                .build();
    }

    public CommentResponse toCommentResponse(MarketplaceComment comment) {
        // Fetch resident info for author
        ResidentInfoResponse author = null;
        try {
            author = residentInfoService.getResidentInfo(comment.getResidentId());
            if (author == null) {
                System.err.println("WARNING: Author is null for comment residentId: " + comment.getResidentId() + ", commentId: " + comment.getId());
            } else {
                System.out.println("✅ Author info for comment " + comment.getId() + ": name=" + author.getName() + ", unitNumber=" + author.getUnitNumber());
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception when fetching author for comment " + comment.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .residentId(comment.getResidentId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(comment.isDeleted() ? "[Đã xóa]" : comment.getContent())
                .author(author)
                .replies(comment.getReplies().stream()
                        .map(this::toCommentResponse)
                        .collect(Collectors.toList()))
                .replyCount(comment.getReplies().size())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isDeleted(comment.isDeleted())
                .build();
    }

    public CategoryResponse toCategoryResponse(MarketplaceCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .nameEn(category.getNameEn())
                .icon(category.getIcon())
                .displayOrder(category.getDisplayOrder())
                .active(category.getActive())
                .build();
    }

    public PostPagedResponse toPostPagedResponse(Page<MarketplacePost> page) {
        List<PostResponse> content = page.getContent().stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());

        return PostPagedResponse.builder()
                .content(content)
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}

