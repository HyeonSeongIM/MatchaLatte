package project.matchalatte.storage.db.core;

import jakarta.persistence.*;

@Entity
@Table(name = "product_category")
public class ProductCategoryMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Long categoryId;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
