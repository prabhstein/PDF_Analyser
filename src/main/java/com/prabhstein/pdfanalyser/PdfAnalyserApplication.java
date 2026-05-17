package com.prabhstein.pdfanalyser;

import com.prabhstein.pdfanalyser.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PdfAnalyserApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfAnalyserApplication.class, args);
    }
}
