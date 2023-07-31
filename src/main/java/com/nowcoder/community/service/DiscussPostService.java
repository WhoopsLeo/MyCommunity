package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${caffeine.posts.max-size}") //可以通过command + "+/-"，显示Value的值
    private int maxSize; //能缓存的对象的最大数量，本项目指15个list对象即15页数据

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds; // 缓存的过期时间

    // Caffeine核心接口：Cache, LoadingCache, AsyncLoadingCache
    // 同步缓存：LoadingCache（最常用），即多个线程同时访问缓存里的同一份数据时，若缓存没有对应的数据，则只允许一个线程去数据库中取数据，其他线程则排队等候。
    // 异步缓存：AsyncLoadingCache，即多个线程可以并发地同时取数据。

    // 帖子列表的缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存，帖子总数是用于计算页码的
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        // 首次调用DiscussPostService的时候，初始化帖子列表的缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    // 当本地缓存中没有要的数据时，下方load方法内写的就是如何取获取这个数据。
                    @Override
                    // 这里的key，是下面postListCache.get("offset:limit")
                    public @Nullable List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误！缓存的Key为空");
                        }

                        // 切出来一定要是2个值
                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误！");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 添加二级缓存,先访问redis，若没有则访问mysql
                        if (redisTemplate.hasKey(key)) {
                            logger.debug("本地缓存不存在此数据,但Redis中存在此数据,正在从Redis取值");
                            // 因为redisTemplate底层是用LinkedHashSet实现的Set，所以此set是有序的。这里我们进行强转。
                            LinkedHashSet<DiscussPost> set = (LinkedHashSet<DiscussPost>) redisTemplate.opsForZSet().range(key, 0, -1);
                            // 将LinkedHashSet转换为list
                            ArrayList<DiscussPost> list = new ArrayList<>(set);
                            return list;
                        }

                        logger.debug("本地缓存中和Redis中没有值，正在从数据库取值");
                        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                        logger.debug("正在将数据库中取到的数据存入Redis");
                        // 采用Redis的编程式事务添加数据
                        redisTemplate.execute(new SessionCallback() {
                            @Override
                            public Object execute(RedisOperations operations) throws DataAccessException {
                                // 启用Redis事务
                                operations.multi();
                                for (DiscussPost discussPost : discussPosts) {
                                    operations.opsForZSet().add(key, discussPost, discussPost.getScore());
                                }
                                // 提交事务
                                return operations.exec();
                            }
                        });
                        return discussPosts;
                    }
                });
        // 初始化帖子总数的缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {

                        logger.debug("本地缓存中没有值，正在从数据库取值");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });

    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        // 只有访问首页的热门帖子的时候才需要缓存到本地
        if (userId == 0 && orderMode == 1) {
            // get方法里的参数会被传入到上面的load方法中
            // 从本地缓存中取数据，因为缓存一页数据只和offset和limit有关，所以用这2个作为key
            return postListCache.get(offset + ":" + limit);
        }
        logger.debug("此数据不需要走本地缓存，正在从数据库中查询需要被缓存到本地的数据");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        // 只有访问首页的时候，因为要计算页码所以才需要缓存。
        if (userId == 0) {
            // 从本地缓存中取数据
            return postRowsCache.get(userId);
        }
        logger.debug("此数据不需要走本地缓存，正在从数据库中查询需要被缓存到本地的数据");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
