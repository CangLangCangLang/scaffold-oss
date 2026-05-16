package com.scaffold.framework.web.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.scaffold.common.constant.CacheConstants;
import com.scaffold.common.constant.Constants;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.core.redis.RedisCache;
import com.scaffold.common.utils.ServletUtils;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.common.utils.http.UserAgentUtils;
import com.scaffold.common.utils.ip.AddressUtils;
import com.scaffold.common.utils.ip.IpUtils;
import com.scaffold.common.utils.uuid.IdUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Token 验证处理。
 * <p>
 * 使用 JJWT 0.12.x 流式 API，通过 HMAC-SHA-512 算法签发 JWT。
 * 启动期会校验密钥长度是否满足 HS512 推荐的 ≥ 64 字节，避免运行期再抛异常。
 *
 * @author scaffold
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    /** HS512 密钥推荐最小字节数 */
    private static final int MIN_SECRET_BYTES = 64;

    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.expireTime}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    private SecretKey signingKey;

    @PostConstruct
    public void init()
    {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES)
        {
            byte[] padded = new byte[MIN_SECRET_BYTES];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.signingKey = Keys.hmacShaKeyFor(padded);
            log.warn("token.secret 长度仅 {} 字节，已自动填充到 {} 字节以满足 HS512 要求；生产环境请显式提供 ≥ {} 字节随机密钥。",
                    keyBytes.length, MIN_SECRET_BYTES, MIN_SECRET_BYTES);
        }
        else
        {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public LoginUser getLoginUser(HttpServletRequest request)
    {
        return resolveLoginUser(getToken(request));
    }

    /**
     * 直接基于 raw token 解析 {@link LoginUser}，方便 WebSocket 握手等非 servlet 上下文复用。
     */
    public LoginUser getLoginUser(String token)
    {
        if (StringUtils.isEmpty(token)) return null;
        if (token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return resolveLoginUser(token);
    }

    private LoginUser resolveLoginUser(String token)
    {
        if (StringUtils.isEmpty(token)) return null;
        try
        {
            Claims claims = parseToken(token);
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            String userKey = getTokenKey(uuid);
            return redisCache.getCacheObject(userKey);
        }
        catch (Exception e)
        {
            log.error("获取用户信息异常'{}'", e.getMessage());
            return null;
        }
    }

    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        return createToken(claims);
    }

    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
        {
            refreshToken(loginUser);
        }
    }

    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    public void setUserAgent(LoginUser loginUser)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(UserAgentUtils.getBrowser(userAgent));
        loginUser.setOs(UserAgentUtils.getOperatingSystem(userAgent));
    }

    private String createToken(Map<String, Object> claims)
    {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireTime * MILLIS_MINUTE);
        JwtBuilder builder = Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, Jwts.SIG.HS512);
        Object subject = claims.get(Constants.JWT_USERNAME);
        if (subject != null)
        {
            builder.subject(String.valueOf(subject));
        }
        return builder.compact();
    }

    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token)
    {
        return parseToken(token).getSubject();
    }

    private String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }
}
