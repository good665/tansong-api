package kr.tansong.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("kr.tansong.demo.store.mapper")
@MapperScan("kr.tansong.demo.facility.mapper")
@MapperScan("kr.tansong.demo.consultation.mapper")
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(
        		DemoApplication.class, args
        );
    }
}