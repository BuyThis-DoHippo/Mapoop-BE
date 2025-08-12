package BuyThisDoHippo.Mapoop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@EnableRedisRepositories
@EnableJpaAuditing
@SpringBootApplication
public class MapoopApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapoopApplication.class, args);
	}

}
