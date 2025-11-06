package project.matchalatte.support.scheduling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SchedulingMapper {

    private final String csvFilePath;

    public SchedulingMapper(@Value("${app.csv.file-path}") String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    public void csvMapper(String name, String description, Long price) {
        String line = dataToCsv(name, description, price);

        Path path = Paths.get(csvFilePath);

        try {
            Files.createDirectories(path.getParent());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath, true))) {
                writer.write(line);
                writer.newLine();
            }
        }
        catch (IOException e) {
            // TODO : 로그 및 에러 핸들링
            e.printStackTrace();
        }
    }

    public String dataToCsv(String name, String description, Long price) {
        return String.format("\"%s\",\"%s\",%d", name, description, price);
    }

}
