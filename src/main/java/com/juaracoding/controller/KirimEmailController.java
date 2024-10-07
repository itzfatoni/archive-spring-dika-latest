package com.juaracoding.controller;


import com.juaracoding.config.OtherConfig;
import com.juaracoding.config.SMTPConfig;
import com.juaracoding.core.SMTPCore;
import com.juaracoding.util.LoggingFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("api")
public class KirimEmailController {


    @PostMapping("/v1/kirim-email-content-html")
    public ResponseEntity<Object> kirimEmail(
            @RequestParam(value = "email_tujuan") String[] emailTujuan,
            @RequestParam(value = "subject") String subject,
            @RequestParam(value = "content") MultipartFile content,
            @RequestParam(value = "attachment") MultipartFile[] attachment
    ){

        String [] strAttachment = {"D:\\icon\\sample-attachment-email\\attachment\\133573751894099649.jpg",
                "D:\\icon\\sample-attachment-email\\attachment\\133580816289666176.jpg",
                "D:\\icon\\sample-attachment-email\\attachment\\133584302858445448.jpg"};

//        try {
//            Executors.newSingleThreadExecutor().
//                    submit(new SMTPCore(emailTujuan,
//                            subject,
//                            "ANGGAP SAJA HTML",strAttachment)).
//                    get(Long.parseLong(SMTPConfig.getEmailSMTPTimeout()), TimeUnit.SECONDS);//SETTINGAN DARI smtpconfig.properties SAYA SET 40 DENGAN PARAMETER Seconds
//        } catch (InterruptedException e) {
//            System.out.println(e.getMessage());
//            LoggingFile.exceptionStringz("KirimEmailController","kirimEmail",e, OtherConfig.getFlagLoging());
//        } catch (ExecutionException e) {
//            LoggingFile.exceptionStringz("KirimEmailController","kirimEmail",e, OtherConfig.getFlagLoging());
//        } catch (TimeoutException e) {
//            LoggingFile.exceptionStringz("KirimEmailController","kirimEmail",e, OtherConfig.getFlagLoging());
//        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Executors.newSingleThreadExecutor().
                            submit(new SMTPCore(emailTujuan,
                                    subject,
                                    "ANGGAP SAJA HTML",strAttachment)).
                            get(Long.parseLong(SMTPConfig.getEmailSMTPTimeout()), TimeUnit.SECONDS);//SETTINGAN DARI smtpconfig.properties SAYA SET 40 DENGAN PARAMETER Seconds
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                    LoggingFile.exceptionStringz("KirimEmailController","kirimEmail",e, OtherConfig.getFlagLogging());
                } catch (ExecutionException e) {
                    LoggingFile.exceptionStringz("KirimEmailController","kirimEmail",e, OtherConfig.getFlagLogging());
                } catch (TimeoutException e) {
                    LoggingFile.exceptionStringz("KirimEmailController","kirimEmail",e, OtherConfig.getFlagLogging());
                }
            }
        }).start();

        System.out.println("SAMPAI SINI!!");
        return ResponseEntity.ok("Kirim Email Success");
    }
}
