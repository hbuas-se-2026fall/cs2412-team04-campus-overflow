package com.campusoverflow.reputation.interfaces;

import com.campusoverflow.reputation.application.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排行榜维护接口。URL 级授权（hasRole ADMIN）在 SecurityConfig 的 `/api/v1/admin/**` 规则中。
 *
 * <p>周/月/学期榜是可重建的派生数据，权威来源是声誉账本；Redis 数据丢失或被误清时用它收敛。</p>
 */
@Tag(name = "管理 Admin")
@RestController
@RequestMapping("/api/v1/admin/leaderboard")
public class AdminLeaderboardController {

    private final LeaderboardService leaderboard;

    public AdminLeaderboardController(LeaderboardService leaderboard) {
        this.leaderboard = leaderboard;
    }

    /**
     * 用账本重建排行榜。
     *
     * @param days 重放最近多少天的流水；0 表示全量历史
     */
    @Operation(summary = "用声誉账本重建周/月/学期排行榜")
    @PostMapping("/rebuild")
    public Map<String, Object> rebuild(
            @Parameter(description = "重放最近多少天，0 表示全量历史")
            @RequestParam(defaultValue = "200") int days) {
        int replayed = leaderboard.rebuild(days <= 0 ? null : Duration.ofDays(days));
        return Map.of("replayed", replayed, "days", days);
    }
}
