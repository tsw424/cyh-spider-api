package cn.zero.spider.controller;

import cn.zero.spider.pojo.Article;
import cn.zero.spider.service.IArticleService;
import cn.zero.spider.util.Ajax;
import cn.zero.spider.webmagic.page.BiQuGePageProcessor;
import cn.zero.spider.webmagic.pipeline.BiQuGePipeline;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.scheduler.RedisScheduler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * 章节控制器
 *
 * @author 蔡元豪
 * @date 2018/6/26 17:43
 */
@RestController
@RequestMapping("article")
public class ArticleController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(ArticleController.class);

    @Autowired
    private IArticleService articleService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BiQuGePipeline biQuGePipeline;

    @Autowired
    private RedisScheduler redisScheduler;


    /**
     * 小说章节内容页面
     *
     * @param bookUrl    小说url
     * @param articleUrl 章节url
     * @return article
     */
    @GetMapping(value = "/{bookUrl}/{articleUrl}")
    public Ajax article(@PathVariable("bookUrl") String bookUrl, @PathVariable("articleUrl") String articleUrl, HttpServletResponse response) {
        Cookie cookie = new Cookie(bookUrl, articleUrl);
        //30天过期
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);
        Article article = articleService.getByUrl(bookUrl, articleUrl);
        JSONObject jsonObject = new JSONObject();
        if (article == null) {
            SetOperations<String, String> removeBookUrl = stringRedisTemplate.opsForSet();
            //移出已经爬取的小说章节记录 重新爬取章节
            logger.info("移出redis爬取章节记录：" + "http://www.biquge.com.tw/" + bookUrl + "/" + articleUrl + ".html");
            removeBookUrl.remove("set_www.biquge.com.tw", "http://www.biquge.com.tw/" + bookUrl + "/" + articleUrl + ".html");
            Spider.create(new BiQuGePageProcessor()).addUrl("http://www.biquge.com.tw/" + bookUrl + "/" + articleUrl + ".html")
                    .addPipeline(biQuGePipeline)
                    .setScheduler(redisScheduler)
                    .thread(1).run();
            Article articleTemp = articleService.getByUrl(bookUrl, articleUrl);
            if (articleTemp == null) {
                return new Ajax(500, null, "无效章节");
            }
            jsonObject.put("article", articleTemp);
        } else {
            //当前章节
            jsonObject.put("article", article);
        }
        //   下一章
        Article next = articleService.getNext(bookUrl, articleUrl);
        //下一章链接
        jsonObject.put("next", next != null ? "article/" + next.getBookUrl() + "/" + next.getUrl() : null);
        //上一章
        Article previous = articleService.getPrevious(bookUrl, articleUrl);
        //上一章链接
        jsonObject.put("previous"
                , previous != null ? "article/" + previous.getBookUrl() + "/" + previous.getUrl() : null);
        return new Ajax(jsonObject, "获取章节成功");
    }

}
