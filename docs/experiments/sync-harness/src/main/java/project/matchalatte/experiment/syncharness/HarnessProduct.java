package project.matchalatte.experiment.syncharness;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// 운영 ProductEntity(storage/db-core)와 동일한 컬럼 구성 - MySQL insert 비용을 실제와 유사하게 재현하기 위한 실험용 복제본
@Entity
public class HarnessProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private Long price;

    private Long userId;

    public HarnessProduct() {
    }

    public HarnessProduct(String name, String description, Long price, Long userId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getPrice() {
        return price;
    }

    public Long getUserId() {
        return userId;
    }

}
