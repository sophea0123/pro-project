package com.sopheamicro.inventory_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;


    public List<Message> readAndSortFiles(String folderPath, String backupFolderPath) throws IOException {
        List<File> files = Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .map(Path::toFile)
                .collect(Collectors.toList());

        List<Message> messages = new ArrayList<>();

        for (File file : files) {
            try {
                // Read JSON data from each file
                Message message = objectMapper.readValue(file, Message.class);

                // Validate data length
                if (message.getFullMessage().length() > 4000) { // Assuming VARCHAR(4000)
                    message.setFullMessage(message.getFullMessage().substring(0, 4000));
                }
                if (message.getHashCode().length() > 64) { // Assuming VARCHAR(64)
                    message.setHashCode(message.getHashCode().substring(0, 64));
                }

                messages.add(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Sort by messageTime in ascending order
        messages.sort(Comparator.comparingLong(Message::getMessageTime));

        //AtomicLong indexCounter = new AtomicLong(1); // Initialize the index counter

        // Save to database in ascending order
        for (Message message : messages) {
            MessageEntity messageEntity = new MessageEntity();
            messageEntity.setMessageTime(message.getMessageTime());
            messageEntity.setHashCode(message.getHashCode());
            messageEntity.setFullMessage(message.getFullMessage());
            messageEntity.setCreateDate(new Date()); // Set the current date and time
            messageRepository.save(messageEntity);
        }

        // Move processed files to backup folder
        //moveFilesToBackup(files, backupFolderPath);

        return messages;
    }


    private void moveFilesToBackup(List<File> files, String backupFolderPath) throws IOException {
        for (File file : files) {
            Path source = file.toPath();
            Path target = Paths.get(backupFolderPath, file.getName());
            Files.move(source, target);
        }
    }

    public void processFile(String filePath) throws IOException, ParseException {
        List<AttendanceRecord> records = new ArrayList<>();
        File file = new File(filePath);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            // Skip the header line
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] data = line.split("\\s+");

                // Assuming your data is split by spaces, adjust if necessary
                if (data.length >= 10) {
                    AttendanceRecord record = new AttendanceRecord();
                    record.setName(data[0]);
                    record.setsTime(data[1] + " " + data[2] + " " + data[3]);
                    record.setVerifyMode(data[4]);
                    record.setMachine(data[5]);
                    record.setException(data[6]);
                    record.setChecktype(data[7]);
                    record.setSensorid(data[8]);
                    record.setWorkcode(data[9]);
                    if (data.length > 10) {
                        record.setsDate(parseDate(data[10]));
                    } else {
                        record.setsDate(new Date()); // Set a default date or handle as necessary
                    }

                    records.add(record);
                }
            }
        }

        // Save all records to the database
        attendanceRecordRepository.saveAll(records);
    }

    private Date parseDate(String date) throws ParseException {
        return DATE_FORMAT.parse(date);
    }
}
