package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
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

    @Test
    public void dynamicQuery_BooleanBuilder() {
        //GIVEN
        String usernameParam = "member1";
        Integer ageParam  = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond){
        BooleanBuilder builder = new BooleanBuilder();

        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //다중쿼리 - where 다중 파라미터 사용
    @Test
    public void dynamicQuery_WhereParam() {
        //GIVEN
        String usernameParam = "member1";
        Integer ageParam  = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond){
        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond),ageEq(ageCond))
                .where(allEq(usernameCond,ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond == null ?  member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    @Commit
    public void bulkUpdate() {

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        //벌크연산은 영속성 컨텍스트를 거치지 않고 바로 DB에 쿼리를 날리기때문에
        //벌크연산을 한뒤에 컨텍스트를 날려줘야된다.
        em.flush();
        em.clear();
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result){
            System.out.println(member1);
        }
    }

    @Test
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    public void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }


    //사용할 함수는 OracleDialect에 추가해야된다.
    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace',{0},{1},{2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();
        for (String s: result){
            System.out.println("s = "+s);
        }
    }

    //Ansi 기본내장 함수는 밑에와 같이 호출해서 사용하면 된다.
    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower',{0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s: result){
            System.out.println("s = "+s);
        }
    }


}
