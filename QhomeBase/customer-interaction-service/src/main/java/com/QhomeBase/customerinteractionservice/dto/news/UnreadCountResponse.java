package com.QhomeBase.customerinteractionservice.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    private Long count;

    public static UnreadCountResponse of(Long count) {
        return UnreadCountResponse.builder()
                .count(count)
                .build();
    }
}

