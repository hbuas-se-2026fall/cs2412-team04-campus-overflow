package com.campusoverflow.qa.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SuppressWarnings("unchecked")
class RedisQuestionListCacheTest {

    private static final String GEN_KEY = "qa:list:gen";
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final RedisQuestionListCache cache = new RedisQuestionListCache(redis, new ObjectMapper());

    @Test
    void generationUnavailableDisablesCacheInsteadOfFallingBackToZero() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(GEN_KEY)).thenReturn(null);
        when(valueOps.setIfAbsent(eq(GEN_KEY), anyString())).thenReturn(false);

        QuestionListCache.Lookup lookup = cache.get(1L, 2L, new PageRequest(1, 20), "member:1");

        assertThat(lookup.generation()).isEqualTo(-1L);
        assertThat(lookup.value()).isEmpty();
        verify(valueOps, never()).get(contains(":member:1:"));
    }

    @Test
    void putWithUnavailableGenerationIsSkipped() {
        cache.put(1L, 2L, new PageRequest(1, 20), "member:1", -1L,
                PageResult.empty(1, 20));

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
