package project.matchalatte.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class SyncLockHelper {

    // true: Full Sync(배치) 작업 진행 중
    private final AtomicBoolean fullSyncInProgress = new AtomicBoolean(false);

    public boolean isAvailableForSchedule() {
        if (isFullSyncInProgress()) {
            log.warn("Full Sync 배치 작업 진행 중. Bulk 스케줄링을 건너뜁니다.");
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * 전체 동기화(Full Sync) 시작을 시도하며 Lock을 획득합니다.
     * @return Lock 획득에 성공하면 true, 이미 진행 중이면 false
     */
    public boolean startFullSync() {
        // false일 때 true로 설정. 성공하면 Lock 획득.
        boolean acquired = fullSyncInProgress.compareAndSet(false, true);
        if (acquired) {
            log.info("Full Sync Lock 획득 성공.");
        }
        else {
            log.warn("Full Sync Lock 획득 실패. 이미 작업 진행 중.");
        }
        return acquired;
    }

    /**
     * 전체 동기화(Full Sync)를 완료하고 Lock을 해제합니다.
     */
    public void stopFullSync() {
        if (fullSyncInProgress.compareAndSet(true, false)) {
            log.info("Full Sync Lock 해제 완료.");
        }
    }

    /**
     * 현재 전체 동기화가 진행 중인지 확인합니다.
     */
    public boolean isFullSyncInProgress() {
        return fullSyncInProgress.get();
    }

}