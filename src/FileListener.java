import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

public class FileListener extends Thread{
    private final WatchService service;
    private final String OFFSET_FILE_PATH = "src/offset.txt";
    private final String directoryWatcherPath;
    private final int DATA_SIZE = 4096;

    public FileListener(String uri) throws IOException {
        Path filePath = Paths.get(uri);
        this.service = FileSystems.getDefault().newWatchService();
        this.directoryWatcherPath = uri;
        filePath.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
        start();
    }

    @Override
    public void run() {
        FileInputStream fis = null;
        try{
            fis = new FileInputStream(OFFSET_FILE_PATH);
            byte[] rawData = new byte[DATA_SIZE];
            while (true){
                fis = new FileInputStream(OFFSET_FILE_PATH);
                WatchKey watchKey = service.take();
                for(var key : watchKey.pollEvents()){
                    if(!Files.isDirectory(Paths.get(key.context().toString()))){
                        String changedFileName = key.context().toString() ;
                        fis.close();
                        fis = new FileInputStream(directoryWatcherPath+changedFileName);
                        int offset = getOffset();
                        offset = offset == -1 ? 0 : offset;
                        int availableBytes = fis.available();
                        if(availableBytes - offset > 0) {
                            fis.skip(offset);
                            int readBytes = fis.read(rawData);
                            int newOffset = readBytes + offset;
                            saveOffset(newOffset);
                            System.out.println(new String(rawData, 0, readBytes, StandardCharsets.UTF_8));
                        }
                    }
                }
                watchKey.reset();
            }
        }
        catch (InterruptedException | IOException e){
            System.out.printf("Thread has been interrupted\n%s%n",
                    Arrays.toString(e.getStackTrace()));
        }
        finally {
            try {
                fis.close();
            } catch (IOException e) {
                System.out.println();
            }
        }
    }

    private int getOffset() throws IOException{
        String offsetString = new String(Files.readAllBytes(Paths.get(OFFSET_FILE_PATH)));
        return offsetString.isBlank() ? 0 : Integer.parseInt(offsetString);
    }

    private void saveOffset(int offset) throws IOException{
        Files.write(Paths.get(OFFSET_FILE_PATH), String.valueOf(offset).getBytes(), StandardOpenOption.WRITE);
    }
}
