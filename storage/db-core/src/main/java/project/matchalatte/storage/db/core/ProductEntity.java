package project.matchalatte.storage.db.core;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class ProductEntity extends BaseEntity {

    private String name;

    private String description;

    private Long price;

    private Long userId;

    public ProductEntity() {
    }

    public ProductEntity(String name, String description, Long price, Long userId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.userId = userId;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

}
