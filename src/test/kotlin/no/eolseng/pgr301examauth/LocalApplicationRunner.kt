package no.eolseng.pgr301examauth

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class LocalApplicationRunner

fun main(args: Array<String>) {
    SpringApplication.run(LocalApplicationRunner::class.java, "--spring.profiles.active=dev")
}
