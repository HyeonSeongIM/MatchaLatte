package project.matchalatte.storage.db.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class CoreEntity extends BaseEntity{
    @Column
    private String example;


    public CoreEntity() {
    }

    public CoreEntity(String example) {
        this.example = example;
    }

    public String getExample() {
        return example;
    }
}
