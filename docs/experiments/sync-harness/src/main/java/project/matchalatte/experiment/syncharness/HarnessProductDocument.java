package project.matchalatte.experiment.syncharness;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "products_harness")
public class HarnessProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Long)
    private Long price;

    @Field(type = FieldType.Long)
    private Long userId;

    public HarnessProductDocument() {
    }

    public HarnessProductDocument(String id, String name, String description, Long price, Long userId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

}
