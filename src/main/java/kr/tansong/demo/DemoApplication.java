package kr.tansong.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
    "kr.tansong.demo.store.mapper",
    "kr.tansong.demo.facility.mapper",
    "kr.tansong.demo.consultation.mapper",
    "kr.tansong.demo.reservation.mapper",
    "kr.tansong.demo.user.mapper"
})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}