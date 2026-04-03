package br.com.study.narrativeapi;

import br.com.study.genericauthorization.configuration.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableFeignClients(basePackages = {
        "br.com.study.narrativeapi",
        "br.com.study.genericauthorization"
})
public class NarrativeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NarrativeApiApplication.class, args);
    }
}