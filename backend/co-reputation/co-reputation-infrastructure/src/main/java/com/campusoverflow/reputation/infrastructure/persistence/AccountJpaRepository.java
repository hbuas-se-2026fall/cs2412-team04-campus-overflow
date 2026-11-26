package com.campusoverflow.reputation.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {

    @Query("select a from AccountJpaEntity a order by a.reputation desc, a.userId asc")
    List<AccountJpaEntity> findTop(Pageable pageable);

    /**
     * 行锁读取（审计魏-11）：PESSIMISTIC_WRITE 让同一账户的「读-算-写」串行化，
     * 用于投票日上限的锁内重算，避免并发投票都读到未达上限的旧值。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountJpaEntity a where a.userId = :userId")
    Optional<AccountJpaEntity> findByIdForUpdate(long userId);
}
