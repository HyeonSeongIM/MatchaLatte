package project.matchalatte.experiment.syncharness;

import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// 운영 ProductListener(core-domain)와 동일하게 요청 스레드가 아닌 별도 풀에서 ES 색인을 수행.
// refresh=false(RefreshPolicy.NONE)로 저장해 실제 bulk 색인 경로와 동일한 조건을 재현한다.
@Component
public class ProductEventListener {

    private final ElasticsearchOperations elasticsearchOperations;

    public ProductEventListener(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @EventListener
    @Async("productEventExecutor")
    public void onProductCreated(ProductCreatedEvent event) {
        HarnessProductDocument document = new HarnessProductDocument(event.id().toString(), event.name(),
                event.description(), event.price(), event.userId());
        elasticsearchOperations.withRefreshPolicy(RefreshPolicy.NONE).save(document);
    }

}
