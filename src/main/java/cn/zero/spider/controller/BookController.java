package cn.zero.spider.controller;

import cn.zero.spider.pojo.Article;
import cn.zero.spider.pojo.Book;
import cn.zero.spider.service.IArticleService;
import cn.zero.spider.service.IBookService;
import cn.zero.spider.util.Ajax;
import cn.zero.spider.webmagic.page.BiQuGePageProcessor;
import cn.zero.spider.webmagic.page.BiQuGeSearchPageProcessor;
import cn.zero.spider.webmagic.pipeline.BiQuGePipeline;
import cn.zero.spider.webmagic.task.AgainSpider;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.scheduler.RedisScheduler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 小说控制器
 *
 * @author 蔡元豪
 * @date 2018 /6/24 08:57
 */
@RestController
@RequestMapping("books")
public class BookController extends BaseController {

    private static final int NUMBER_OF_THREADS = 5;
    private static ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    /**
     * 加锁
     */
    private final ConcurrentHashMap<String, Integer> bookLock = new ConcurrentHashMap<>();
    /**
     * 全局书籍更新锁
     */
    private final ReentrantLock globalUpdateLock = new ReentrantLock();
    private Logger logger = LoggerFactory.getLogger(BookController.class);
    @Autowired
    private IBookService bookService;
    @Autowired
    private IArticleService articleService;
    /**
     * 小说详情和章节保存组件
     */
    @Autowired
    private BiQuGePipeline biQuGePipeline;
    @Autowired
    private BiQuGePageProcessor biQuGePageProcessor;
    @Autowired
    private AgainSpider againSpider;
    @Autowired
    private RedisScheduler redisScheduler;

    /**
     * 小说详情页面
     *
     * @param bookUrl 小说url
     * @return book book
     */
    @ApiOperation(value = "小说详情页", notes = "当没有获取到小说时会开始爬取小说")
    @ApiImplicitParam(name = "bookUrl", value = "小说地址或者id,比如：2_2031")
    @GetMapping(value = "/{bookUrl}")
    public Ajax book(@PathVariable("bookUrl") String bookUrl, HttpServletRequest request) {
        JSONObject jsonObject = new JSONObject();
        Book book = bookService.getById(bookUrl);
        if (book != null) {
            jsonObject.put("book", book);
            //获取小说读书记录
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (cookie.getName().equals(bookUrl)) {
                        //获取章节
                        Article article = articleService.getByUrl(bookUrl, cookie.getValue());
                        article.setContent(null);
                        jsonObject.put("record", article);
                    }
                }
            }
        } else {
            //尝试获取锁
            if (bookLock.putIfAbsent(bookUrl, 1) != null) {
                //加锁失败
                logger.info("小说爬取中，加锁失败");
                return new Ajax(500, null, "小说爬取中！请不要重复刷新！稍等片刻！");
            }

            if ((book = bookService.getById(bookUrl)) != null) {
                try {
                    jsonObject.put("book", book);
                    //获取小说读书记录
                    if (request.getCookies() != null) {
                        for (Cookie cookie : request.getCookies()) {
                            if (cookie.getName().equals(bookUrl)) {
                                Article article = articleService.getByUrl(bookUrl, cookie.getValue());
                                article.setContent(null);
                                jsonObject.put("record", article);
                            }
                        }
                    }
                } finally {
                    bookLock.remove(bookUrl);
                }
                return new Ajax(jsonObject, "获取成功");
            }
            //如果小说不存在 开始爬取
            logger.info("开始新抓小说：http://www.biquge.com.tw/" + bookUrl);
            executorService.submit(() -> {
                try {
                    Spider.create(biQuGePageProcessor)
                            .addUrl("http://www.biquge.com.tw/" + bookUrl).addPipeline(biQuGePipeline)
                            //url管理
                            .setScheduler(redisScheduler)
                            .thread(20).run();
                } catch (Exception e) {
                    logger.warn("抓取小说:http://www.biquge.com.tw/{} 异常", bookUrl);
                } finally {
                    bookLock.remove(bookUrl);
                    logger.info("爬取小说：http://www.biquge.com.tw/{} 完成/解锁", bookUrl);
                }

            });
            return new Ajax(500, null, "小说爬取中！");
        }
        return new Ajax(jsonObject, "获取成功");
    }

    /**
     * 查询小说
     *
     * @return m
     */
    @ApiImplicitParams({
            @ApiImplicitParam(name = "key", value = "小说关键字", required = true),
            @ApiImplicitParam(name = "page", value = "当前分页页数", required = false)

    })
    @ApiOperation(value = "搜索小说")
    @PostMapping(value = "/search")
    public Ajax search(@RequestBody Map<String, Object> map) {
        ResultItems resultItems = null;
        JSONObject jsonObject = new JSONObject();
        try {
            String encodeKey = URLEncoder.encode(String.valueOf(map.get("key")), "gb2312");
            resultItems = Spider.create(new BiQuGeSearchPageProcessor())
                    .get("http://www.biquge.com.tw/modules/article/soshu.php?searchkey=+"
                            + encodeKey + (map.get("page") == null ? "" : "&page=" + map.get("page")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (resultItems != null) {
            resultItems.getAll().forEach(jsonObject::put);
        }
        //搜索关键字
        jsonObject.put("key", String.valueOf(map.get("key")));
        //当前页面
        jsonObject.put("currentPage", map.get("page") != null ? map.get("page") : 1);
        return new Ajax(jsonObject, "查询成功");
    }

    /**
     * 手动更新小说
     */
    @GetMapping("booksUpdate")
    @ApiOperation("更新小说/手动更新数据库已有小说，可以不做到前端")
    public void update() {
        if (globalUpdateLock.tryLock()) {
            try {
                againSpider.books();
            } finally {
                globalUpdateLock.unlock();
                logger.warn("全局更新完成，解锁");
            }
        } else {
            logger.warn("全局更新进行中...");
        }
    }

}
