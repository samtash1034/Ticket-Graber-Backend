package com.project.ticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient  // 啟用 Zookeeper 的服務註冊與發現
@EnableFeignClients  // 啟用 Feign
@ComponentScan(basePackages = {"com.project.common", "com.project.ticket"})
@MapperScan("com.project.orm.mapper")
//@Import(RabbitMQConfig.class)
@SpringBootApplication
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }

}