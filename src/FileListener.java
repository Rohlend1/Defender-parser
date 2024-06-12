import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

public class FileListener extends Thread{
    private final WatchService service;
    private final String OFFSET_FILE_PATH = "src/offset.txt";
    private final String directoryWatcherPath;

    //Вот эту штуку можешь менять в зависимости от того насколько много надо символов читать, грубо говоря
    // в данный момент он за раз максимум может вычитать 4096 символов или 4096 байт(Зависит от кодировки само собой)
    private final int DATA_SIZE = 4096;

    public FileListener(String uri) throws IOException {
        Path filePath = Paths.get(uri);
        this.service = FileSystems.getDefault().newWatchService();
        this.directoryWatcherPath = uri;
        filePath.register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        start();
    }

    @Override
    public void run() {
        FileInputStream fis = null;
        try{
            OffsetConverter offsetConverter = new OffsetConverter(OFFSET_FILE_PATH);
            fis = new FileInputStream(OFFSET_FILE_PATH);
            byte[] rawData = new byte[DATA_SIZE];
            while (true){
                fis = new FileInputStream(OFFSET_FILE_PATH);
                WatchKey watchKey = service.take();
                for(var key : watchKey.pollEvents()){
                    if(key.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)){
                        offsetConverter.resetOffset(key.context().toString());
                    }
                    else {
                        if (!Files.isDirectory(Paths.get(key.context().toString()))) {
                            String changedFileName = key.context().toString();
                            fis.close();
                            fis = new FileInputStream(directoryWatcherPath + changedFileName);
                            int offset = offsetConverter.getOffset(changedFileName);
                            int availableBytes = fis.available();
                            if (availableBytes - offset > 0 && !changedFileName.equals("offset.txt")) {
                                fis.skip(offset);
                                int readBytes = fis.read(rawData);
                                int newOffset = readBytes + offset;
                                offsetConverter.saveOffset(newOffset, changedFileName);
                                System.out.println(new String(rawData, 0, readBytes, StandardCharsets.UTF_8));
                            }
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

}
