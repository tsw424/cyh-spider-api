package cn.zero.spider.controller;

import cn.zero.spider.pojo.NovelsList;
import cn.zero.spider.util.Ajax;
import cn.zero.spider.webmagic.page.BiQuGeIndexPageProcessor;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.scheduler.RedisScheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页 controller.
 *
 * @author 蔡元豪
 * @date 2018 /6/23 21:55
 */
@RestController
public class IndexController extends BaseController {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BiQuGeIndexPageProcessor biQuGeIndexPageProcessor;

    @Autowired
    private RedisScheduler redisScheduler;

    /**
     * 上传文件的根路径
     */
    @Value("${upload.root.path}")
    private String uploadRootPath;


    /**
     * 首页
     *
     * @return model and view
     */
    @GetMapping(value = {"", "index"})
    public Ajax index() {
        JSONObject jsonObject = new JSONObject();
        BoundHashOperations<String, String, String> boundHashOperations = stringRedisTemplate.boundHashOps("novelsList");
        List<NovelsList> novelsLists = new ArrayList<>(6);
        boundHashOperations.entries().forEach((k, v) -> novelsLists.add(JSON.parseObject(v, NovelsList.class)));
        jsonObject.put("novelsLists", novelsLists);
        return new Ajax(jsonObject);
    }

    /**
     * 手动更新首页
     *
     * @return model and view
     */
    @GetMapping("/updateIndex")
    public Ajax spiderIndex() {

        SetOperations<String, String> opsForSet = stringRedisTemplate.opsForSet();
        try {
            FileUtils.cleanDirectory(new File(uploadRootPath + "img/index/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        opsForSet.remove("set_www.biquge.com.tw", "http://www.biquge.com.tw/");
        Spider.create(biQuGeIndexPageProcessor)
                .addUrl("http://www.biquge.com.tw/")
                .setScheduler(redisScheduler)
                .run();
        return new Ajax("", "首页更新中...");
    }

}
