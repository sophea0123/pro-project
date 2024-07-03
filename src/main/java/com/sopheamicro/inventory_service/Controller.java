package com.sopheamicro.inventory_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class Controller {

    @Autowired
    private FileService fileService;

    //@Async
    @GetMapping("/sort-messages")
    //@Scheduled (cron = "0/10 * * ? * * ")
    public List<Message> getSortedMessages(@RequestParam(required = false) String folderPath,
                @RequestParam(required = false) String backupFolderPath) {
        String defaultPath = "C:\\Users\\sopheap\\Downloads\\inventory-service\\inventory-service\\src\\main\\resources\\static\\data";
        String defaultBackupPath = "C:\\Users\\sopheap\\Downloads\\inventory-service\\inventory-service\\src\\main\\resources\\static\\back-up";

        try {
            //System.out.println("Scheduled Run");
            return fileService.readAndSortFiles(folderPath != null ? folderPath : defaultPath, backupFolderPath != null ? backupFolderPath : defaultBackupPath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @GetMapping("/process-file")
    public String processFile(@RequestParam(required = false) String filePath) {
        String defaultPath = "C:\\Users\\sopheap\\Downloads\\inventory-service\\inventory-service\\src\\main\\resources\\static\\file\\log.txt";

        try {
            fileService.processFile(filePath != null ? filePath : defaultPath);
            return "File processed successfully.";
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return "Error processing file: " + e.getMessage();
        }
    }

}
