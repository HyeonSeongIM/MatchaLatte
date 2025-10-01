package project.matchalatte.storage.db.core;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class ProductEntity extends BaseEntity {

    private String name;

    private String description;

    private Long price;

    private String imageUrl;

    public ProductEntity() {
    }

    public ProductEntity(String name, String description, Long price, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }

}
