package cn.zero.spider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpiderApplication.class
        , webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootApplication
@MapperScan({"cn.zero.spider.dao"})
public class SpiderApplicationTests {
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();  //构造MockMvc
    }

    @Test
    public void indexTest() throws Exception {
        mvc.perform(get("/index").contentType(MediaType.APPLICATION_JSON_UTF8).accept(MediaType.APPLICATION_JSON)).andDo(print());
    }

    @Test
    public void updateIndex() throws Exception {
        mvc.perform(get("/updateIndex").contentType(MediaType.APPLICATION_JSON_UTF8).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(print());
    }

    @Test
    public void search() throws Exception {
        mvc.perform(post("/books/search").contentType(MediaType.APPLICATION_JSON_UTF8).accept(MediaType.APPLICATION_JSON)
                .param("key","斗")).andExpect(status().isOk()).andDo(print());
    }

    @Test
    public void books() throws Exception {
        mvc.perform(get("/books/2_2031").contentType(MediaType.APPLICATION_JSON_UTF8).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(print());
    }
}
