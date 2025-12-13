package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BirthdayMemberItemReader implements ItemReader<Long> {
    private final MemberServiceClient memberServiceClient;
    private final int chunkSize;

    private int page = 0;
    private Iterator<Long> currentChunkIterator;

    @Override
    public Long read() {
        if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
            List<Long> nextChunk = fetchNextPage();

            // 더 이상 데이터가 없으면 null 반환 (Reader 종료)
            if (nextChunk == null || nextChunk.isEmpty()) {
                return null;
            }
            currentChunkIterator = nextChunk.iterator();
        }

        return currentChunkIterator.next();
    }

    private List<Long> fetchNextPage() {
        int currentMonth = LocalDate.now().getMonthValue();
        log.info("Fetching birthday users - Month: {}, Page: {}, Size: {}", currentMonth, page, chunkSize);

        try {
            // Member Server 호출
            List<Long> userIds = memberServiceClient.getBirthdayUserId(currentMonth, page, chunkSize);
            page++;
            return userIds;
        } catch (Exception e) {
            log.error("Failed to fetch birthday users", e);
            return null;
        }
    }
}
