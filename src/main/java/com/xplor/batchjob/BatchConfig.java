package com.xplor.batchjob;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
//@EnableBatchProcessing
public class BatchConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;


    @Bean
    public Job importAddressJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("importAddressJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(importAddressStep(jobRepository,transactionManager))
                .build();
    }

    @Bean
    public Step importAddressStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("importAddressStep", jobRepository)
                .<Address, Address>chunk(5000, transactionManager)
                .reader(addressItemReader(null))
                .processor(addressProcessor())
                .writer(addressItemWriter(null))
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Address> addressItemReader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new ItemReader<Address>() {
            private Workbook workbook;
            private Sheet sheet;
            private int currentRow = 1;

            {
                try {
                    FileInputStream inputStream = new FileInputStream(filePath);
                    workbook = new XSSFWorkbook(inputStream);
                    sheet = workbook.getSheetAt(0);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read Excel file", e);
                }
            }

            @Override
            public Address read() {
                if (currentRow >= sheet.getPhysicalNumberOfRows()) {
                    return null;
                }

                Row row = sheet.getRow(currentRow++);
                if (row == null) {
                    return null;
                }

                Address address = new Address();
                address.setStreet(row.getCell(0).getStringCellValue());
                address.setCity(row.getCell(1).getStringCellValue());
                address.setState(row.getCell(2).getStringCellValue());
                address.setZipCode(" ");
                address.setSmartKey(null);
                address.setSmartKeyReviewed(false);

                return address;
            }
        };
    }

    @Bean
    public ItemProcessor<Address, Address> addressProcessor() {
        return address -> {
            return address;
        };
    }

    @Bean
    @StepScope
    public ItemWriter<Address> addressItemWriter(@Value("#{jobParameters['schema']}") String schema) {
        return items -> {
            for (Address address : items) {
                String insertQuery = String.format("INSERT INTO %s.addresses (street, city, state, zip_code) VALUES (?, ?, ?, ?)", schema);

                entityManager.createNativeQuery(insertQuery)
                        .setParameter(1, address.getStreet())
                        .setParameter(2, address.getCity())
                        .setParameter(3, address.getState())
                        .setParameter(4, address.getZipCode())
                        .executeUpdate();
            }
        };
    }

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(50);
        return taskExecutor;
    }

}