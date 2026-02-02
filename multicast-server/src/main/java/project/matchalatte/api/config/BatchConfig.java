package project.matchalatte.api.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import project.matchalatte.api.dto.ProductEvent;
import project.matchalatte.domain.service.SyncProductInfo;
import project.matchalatte.domain.entity.ProductDocument;
import project.matchalatte.domain.service.SyncItemWriter;
import project.matchalatte.domain.service.SyncJobListener;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Configuration
@Slf4j
public class BatchConfig {

    private final DataSource dataSource;

    private final ElasticsearchClient elasticsearchClient;

    public BatchConfig(DataSource dataSource, ElasticsearchClient elasticsearchClient) {
        this.dataSource = dataSource;
        this.elasticsearchClient = elasticsearchClient;
    }

    // 0. 공유 자원 데이터 생성
    // 어처피 이 Queue는 스케줄링 때만 유효하기 때문에 싱글톤 빈으로 관리하여
    // 클린 코드 유지
    @Bean(name = "batchProductQueue")
    public Queue<ProductEvent> productQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    // 💡 1. ItemReader: MySQL 데이터 읽기
    @Bean
    public ItemReader<SyncProductInfo> mysqlProductReader() throws Exception {
        JdbcPagingItemReader<SyncProductInfo> reader = jdbcPagingItemReader();

        SqlPagingQueryProviderFactoryBean factoryBean = sqlPagingQueryProviderFactoryBean();

        reader.setQueryProvider(Objects.requireNonNull(factoryBean.getObject()));

        reader.afterPropertiesSet();

        return reader;
    }

    // 💡 2. ItemProcessor: ProductInfo -> ProductDocument 변환 (람다 사용)
    @Bean
    public ItemProcessor<SyncProductInfo, ProductDocument> itemProcessor() {
        return ProductDocument::from;
    }

    // 💡 3. ItemWriter: Elasticsearch에 쓰기
    @Bean
    public SyncItemWriter elasticsearchItemWriter() {
        return new SyncItemWriter(elasticsearchClient);
    }

    // 💡 4. Step 정의
    @Bean
    public Step migrationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager)
            throws Exception {
        return new StepBuilder("migrationStep", jobRepository)
            .<SyncProductInfo, ProductDocument>chunk(1000, transactionManager)
            .reader(mysqlProductReader())
            .processor(itemProcessor())
            .writer(elasticsearchItemWriter())
            .build();
    }

    // 💡 5. Job 정의
    @Bean
    public Job mysqlToEsJob(JobRepository jobRepository, Step migrationStep, SyncJobListener listener) {
        return new JobBuilder("mysqlToEsJob", jobRepository).incrementer(new RunIdIncrementer())
            .listener(listener)
            .start(migrationStep)
            .build();
    }

    private JdbcPagingItemReader<SyncProductInfo> jdbcPagingItemReader() {
        JdbcPagingItemReader<SyncProductInfo> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(1000);
        reader.setRowMapper(new DataClassRowMapper<>(SyncProductInfo.class));
        return reader;
    }

    private SqlPagingQueryProviderFactoryBean sqlPagingQueryProviderFactoryBean() {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setSelectClause("SELECT id, name, description, price, user_id");
        factoryBean.setFromClause("FROM product");
        factoryBean.setSortKey("id");
        return factoryBean;
    }

}