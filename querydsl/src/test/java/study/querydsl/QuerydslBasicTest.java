package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",20,teamB);

        Member member3 = new Member("member3",30,teamB);
        Member member4 = new Member("member4",40,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL(){
        String qlString =
                "select m from Member m" +
                " where m.username = :username";
        //member1을 찾아라
        Member findMember = em.createQuery(qlString,Member.class)
                .setParameter("username", "member1")
                        .getSingleResult();
        System.out.println(findMember.getUsername());
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1") )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }
    @Test
    public void group() throws Exception {
        //GIVEN
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        //WHEN
        
        //THEN
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
    }

    /**
     * 단일결과 조회는 자료형이 정해져있지만 다중결과값은 자료형이 다양하기 때문에 querydsl의 tuple 타입으로 반환된다.
     *  tuple타입은 리파지토리까지만 사용하는걸 권장하기 때문에 서비스에서는 Dto로 적재하는게 좋다.
     *
     */
    @Test
    public void simpleProjection() {
        //GIVEN
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        //WHEN

        //THEN
        for (Tuple tuple : result){
            String userName = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println(userName);
            System.out.println(age);

        }
    }

    //JPQL
    @Test
    public void findDtoByJPQL() {
        em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class);
        //GIVEN

        //WHEN

        //THEN
    }

    //setter 활용법
    @Test
    public void findDtoBySetter() {
        //GIVEN
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //WHEN
        
        //THEN
        for (MemberDto memberDto : result){
            System.out.println("memberDto = "+memberDto);
        }
    }

    //DTO Field에 아예 값을 때려박는 방식
    @Test
    public void findDtoByField() {
        //GIVEN
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //WHEN

        //THEN
        for (MemberDto memberDto : result){
            System.out.println("memberDto = "+memberDto);
        }
    }

    //생성자 접근
    @Test
    public void findDtoByConstructor() {
        //GIVEN
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        //WHEN

        //THEN
        for (MemberDto memberDto : result){
            System.out.println("memberDto = "+memberDto);
        }
    }

    //서브쿼리값을 alias로 지정해서 dto에 담기
    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");;
        //GIVEN
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub),"age")
                ))
                .from(member)
                .fetch();
        //WHEN

        //THEN
        for (UserDto userDto : result){
            System.out.println("UserDto = "+userDto);
        }
    }

    /**
     * dto를 q파일로 생성해서 호출하는 방식이다.
     * constructor와 비슷한 방식이나, constructor은 컴파일 오류가 나지 않아서 RuntimeException이 나올때 까지
     * 오류사항을 확인 못할수도 있다.
     * 해당방법이 좋긴하나 단점은 Dto에서 querydsl을 의존하는게 단점이다.
     */
    @Test
    public void findDtoByQueryProjection() {
        //GIVEN
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        //WHEN

        //THEN
        for (MemberDto memberDto : result){
            System.out.println("memberDto = "+memberDto);
        }
    }

}
