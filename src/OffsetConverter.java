import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OffsetConverter {
    private final Path OFFSET_FILE_PATH;
    private Map<String, String> cachedOffsets;

    public OffsetConverter(String offsetPath) throws IOException {
        this.OFFSET_FILE_PATH = Paths.get(offsetPath);
        cacheWarm();
    }

    private void cacheWarm() throws IOException {
        List<String> offsets = Files.readAllLines(OFFSET_FILE_PATH);
        cachedOffsets = offsets.stream().collect(Collectors.
                toMap(offset -> offset.split("_")[0], offset -> offset.split("_")[1]));
    }


    public int getOffset(String fileName) throws IOException {
        if(cachedOffsets.containsKey(fileName)){
            return Integer.parseInt(cachedOffsets.get(fileName));
        }
        else {
            return 0;
        }
    }

    public void saveOffset(int offset, String fileName) throws IOException{
        cachedOffsets.put(fileName, String.valueOf(offset));
        List<String> offsetsToSave = cachedOffsets.entrySet().stream().filter(pair -> !pair.getKey().equals("offset.txt"))
                .map(pair -> String.format("%s_%s", pair.getKey(), pair.getValue())).toList();
        Files.write(OFFSET_FILE_PATH, offsetsToSave, StandardOpenOption.WRITE);
    }

    public void resetOffset(String fileName) throws IOException {
        saveOffset(0, fileName);
    }
}
