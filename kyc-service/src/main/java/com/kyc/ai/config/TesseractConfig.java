package com.kyc.ai.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TesseractConfig {

    @org.springframework.beans.factory.annotation.Value("${tesseract.datapath}")
    private String datapath;

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(datapath);
        return tesseract;
    }
}
