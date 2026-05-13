package com.ruoyi.common.utils.cache;

import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类
 *
 * 用途：统一封装 Redis 缓存操作，支持 TTL、key 前缀管理
 *
 * @author lingdoc
 */
@Component
public class CacheUtils
{
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "lingdoc:";

    // ==================== Vault 缓存 ====================

    public void cacheVaultFileList(Long userId, String path, Object data)
    {
        String key = PREFIX + "vault:fileList:" + userId + ":" + md5(path);
        redisTemplate.opsForValue().set(key, data, 5, TimeUnit.MINUTES);
    }

    public Object getVaultFileList(Long userId, String path)
    {
        String key = PREFIX + "vault:fileList:" + userId + ":" + md5(path);
        return redisTemplate.opsForValue().get(key);
    }

    public void invalidateVaultFileList(Long userId)
    {
        String pattern = PREFIX + "vault:fileList:" + userId + ":*";
        invalidatePattern(pattern);
    }

    // ==================== 任务状态缓存 ====================

    public void cacheFormTask(String taskId, Object data)
    {
        String key = PREFIX + "form:task:" + taskId;
        redisTemplate.opsForValue().set(key, data, 30, TimeUnit.MINUTES);
    }

    public Object getFormTask(String taskId)
    {
        String key = PREFIX + "form:task:" + taskId;
        return redisTemplate.opsForValue().get(key);
    }

    // ==================== Embedding 缓存 ====================

    public void cacheEmbedding(String textHash, float[] embedding)
    {
        String key = PREFIX + "kb:embedding:" + textHash;
        redisTemplate.opsForValue().set(key, embedding, 24, TimeUnit.HOURS);
    }

    public float[] getEmbedding(String textHash)
    {
        String key = PREFIX + "kb:embedding:" + textHash;
        return (float[]) redisTemplate.opsForValue().get(key);
    }

    // ==================== 通用操作 ====================

    public void invalidatePattern(String pattern)
    {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty())
        {
            redisTemplate.delete(keys);
        }
    }

    public void invalidate(String key)
    {
        redisTemplate.delete(PREFIX + key);
    }

    private String md5(String input)
    {
        if (StringUtils.isEmpty(input)) return "root";
        try
        {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        }
        catch (Exception e)
        {
            return String.valueOf(input.hashCode());
        }
    }
}
