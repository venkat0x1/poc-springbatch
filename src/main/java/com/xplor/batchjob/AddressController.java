package com.xplor.batchjob;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importAddressJob;

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/importAddressesWithJobAndEntityManager")
    public String importAddressesWithJobAndEntityManager(
            @RequestParam("file") MultipartFile file,
            @RequestParam("schema") String schema) {
        try {
            long startTime = System.currentTimeMillis();
            // Create a temporary file with a unique name
            File tempFile = File.createTempFile("addresses_" + System.currentTimeMillis(), ".xlsx");
            file.transferTo(tempFile);

            // Build job parameters with a unique run ID
            JobParameters params = new JobParametersBuilder()
                    .addString("schema", schema)
                    .addString("filePath", tempFile.getAbsolutePath())
                    .addDate("time", new Date())  // This ensures each job has a unique identifier
                    .toJobParameters();

            jobLauncher.run(importAddressJob, params);

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.out.println("Total time taken for batch job: " + totalTime/1000 + " seconds");
            return "Batch job has been started with file: " + tempFile.getName();

        } catch (IOException e) {
            e.printStackTrace();
            return "Error saving file: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error starting batch job: " + e.getMessage();
        }
    }

    @Transactional
    @PostMapping("/importAddressesWithoutJobAndWithEntityManager")
    public String importAddressesWithoutJobAndWithEntityManager(
            @RequestParam("file") MultipartFile file,
            @RequestParam("schema") String schema) throws IOException {

        long startTime = System.currentTimeMillis();



        Workbook workbook = new XSSFWorkbook(file.getInputStream());

        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {

            // skip the first row as it is the header
            if (row.getRowNum() == 0) {
                System.out.println("storing headers and indexes");
                continue;
            }

            System.out.println("stored the data");

            String insertQuery = String.format("INSERT INTO %s.addresses (street, city, state, zip_code) VALUES (?, ?, ?, ?)", schema);

            entityManager.createNativeQuery(insertQuery)
                    .setParameter(1,row.getCell(0).getStringCellValue())
                    .setParameter(2, row.getCell(1).getStringCellValue())
                    .setParameter(3, row.getCell(2).getStringCellValue())
                    .setParameter(4, " ")
                    .executeUpdate();

        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Total time taken for batch job: " + totalTime/1000 + " seconds");
        return "Batch job has been started with file: " + file.getOriginalFilename();
    }

}