package test;

import config.ApplicationConfiguration;
import domain.BaseNode;
import domain.BaseRelationship;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import service.Neo4jService;

import javax.annotation.PostConstruct;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {ApplicationConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class MainTest {
    private static final Logger LOG = LoggerFactory.getLogger(MainTest.class);

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected MockHttpSession session;

    @Autowired
    private Neo4jService neo4j;

    protected MockMvc mockMvc;

    @PostConstruct
    public void postConstructMVC() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).alwaysExpect(status().isOk()).build();
    }

    @Test
    public void test1() throws Exception {
//        BaseNode test = new BaseNode();
//        test.addLabel("Label1");
//        test.addLabel("Label2");
//        test.addLabel("Label3");
//        test.addLabel("Label4");
//        test.addLabel("Label5");
//        test.set("key1", "value1");
//        test.set("key2", "value2");
//        test.set("key3", "value3");
//        test.set("key4", "value4");
//        test.set("key5", "value5");

//        test = this.neo4j.save(test);
//        System.out.println(test);

        BaseNode one = this.neo4j.get(6L);
        BaseNode two = this.neo4j.get(9L);

        BaseRelationship rel = new BaseRelationship("TEST_REL2");
        rel.set("key1", "value1");
        rel.set("key2", "value2");
        rel.set("key3", "value3");

        rel = this.neo4j.relate(one, rel, two);

        System.out.println(rel.toString());
    }

    @Test
    public void test2() throws Exception {
        for (int i = 1; i <= 100; i++) {
            long start = System.currentTimeMillis();
            this.mockMvc.perform(get("/rest/9")).andExpect(status().isOk()).andDo(print());
            System.out.println("time taken: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Test
    public void test3() throws Exception {
//        this.neo4j.labels().forEach(it -> System.out.println(it));
        this.neo4j.relationships().forEach(it -> System.out.println(it));
    }
}
