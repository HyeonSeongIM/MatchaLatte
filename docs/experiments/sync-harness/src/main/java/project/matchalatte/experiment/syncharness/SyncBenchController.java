package project.matchalatte.experiment.syncharness;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// MySQL insert 후 ES 색인까지 요청 스레드에서 순차 대기하는 "동기 버전"과,
// 운영 구조(ProductEventer/@Async 리스너)를 재현한 "비동기 버전"을 같은 컨트롤러에서 비교한다.
@RestController
public class SyncBenchController {

    private final HarnessProductJpaRepository jpaRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    private final ApplicationEventPublisher eventPublisher;

    public SyncBenchController(HarnessProductJpaRepository jpaRepository,
            ElasticsearchOperations elasticsearchOperations, ApplicationEventPublisher eventPublisher) {
        this.jpaRepository = jpaRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/sync-immediate")
    public String syncImmediate() {
        return createSync(RefreshPolicy.IMMEDIATE);
    }

    @PostMapping("/sync-none")
    public String syncNone() {
        return createSync(RefreshPolicy.NONE);
    }

    // 운영과 동일: MySQL 저장만 요청 스레드에서 대기하고, ES 색인은 이벤트로 위임한 뒤 즉시 응답
    @PostMapping("/async")
    public String async() {
        HarnessProduct saved = jpaRepository.save(new HarnessProduct("상품", "설명", 10000L, 1L));

        eventPublisher.publishEvent(new ProductCreatedEvent(saved.getId(), saved.getName(), saved.getDescription(),
                saved.getPrice(), saved.getUserId()));

        return saved.getId().toString();
    }

    private String createSync(RefreshPolicy refreshPolicy) {
        HarnessProduct saved = jpaRepository.save(new HarnessProduct("상품", "설명", 10000L, 1L));

        HarnessProductDocument document = new HarnessProductDocument(saved.getId().toString(), saved.getName(),
                saved.getDescription(), saved.getPrice(), saved.getUserId());

        elasticsearchOperations.withRefreshPolicy(refreshPolicy).save(document);

        return saved.getId().toString();
    }

}
