package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.TagRepository;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 标签使用计数：UPSERT 原子累加（MySQL ON DUPLICATE KEY UPDATE）。 */
@Repository
public class JdbcTagRepository implements TagRepository {

    private static final String UPSERT = """
            INSERT INTO qa_tag (name, question_count) VALUES (:name, GREATEST(:delta, 0))
            ON DUPLICATE KEY UPDATE question_count = GREATEST(question_count + :delta, 0)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcTagRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void adjustUsage(Collection<String> tagNames, int delta) {
        if (tagNames == null || tagNames.isEmpty() || delta == 0) {
            return;
        }
        Map<String, Integer> deltas = new TreeMap<>();
        tagNames.forEach(name -> deltas.merge(name, delta, Integer::sum));
        adjustDeltas(deltas);
    }

    @Override
    public void adjustDeltas(Map<String, Integer> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        // 固定按 tag 名排序：并发请求即使输入顺序相反，也会以同一顺序获取行锁。
        MapSqlParameterSource[] batch = new TreeMap<>(deltas).entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .map(entry -> new MapSqlParameterSource()
                        .addValue("name", entry.getKey()).addValue("delta", entry.getValue()))
                .toArray(MapSqlParameterSource[]::new);
        if (batch.length > 0) {
            jdbc.batchUpdate(UPSERT, batch);
        }
    }
}
