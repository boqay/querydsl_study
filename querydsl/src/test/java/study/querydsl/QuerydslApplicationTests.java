package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QHello.hello;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {

		JPAQueryFactory query = new JPAQueryFactory(em);


		Hello result = query
				.selectFrom(hello)
				.fetchOne();
		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.id);
	}

}
