package com.qiruipeng.util;
import com.alibaba.fastjson.JSON;
import com.qiruipeng.pojo.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.io.IOUtils;
import sun.misc.BASE64Decoder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil{

    /**
     * 读取资源文件
     *
     * @param fileName 文件的名称
     * @return
     */
    public static String readResourceKey(String fileName) {
        String key = null;
        try {
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            assert inputStream != null;
            key = IOUtils.toString(inputStream, String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return key;
    }

    /**
     * 构建token
     * @param memberId 用户对象
     * @param ttlMillis 过期的时间-毫秒
     * @return
     * @throws Exception
     */
    public static String buildJwtRS256(Integer memberId, long ttlMillis){
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
        // 读取私钥
        String key = readResourceKey("rsa_private_key_pkcs8.pem");

        // 生成签名密钥
        try {
            byte[] keyBytes = (new BASE64Decoder()).decodeBuffer(key);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            // 生成JWT的时间
            long nowMillis = System.currentTimeMillis();
            Date now = new Date(nowMillis);
            //创建payload的私有声明（根据特定的业务需要添加，如果要拿这个做验证，一般是需要和jwt的接收方提前沟通好验证方式的）
            Map<String,Object> claims = new HashMap<String,Object>();
            claims.put("userid", memberId);

            // 生成jwt文件
            JwtBuilder builder = Jwts.builder()
                    // 这里其实就是new一个JwtBuilder，设置jwt的body
                    // 如果有私有声明，一定要先设置这个自己创建的私有的声明，这个是给builder的claim赋值，一旦写在标准的声明赋值之后，就是覆盖了那些标准的声明的
                    .setClaims(claims)
                    .setHeaderParam("typ", "JWT")
                    .setIssuedAt(now)
                    .signWith(signatureAlgorithm, privateKey);

            // 如果配置了过期时间
            if (ttlMillis >= 0) {
                // 当前时间加上过期的秒数
                long expMillis = nowMillis + (ttlMillis * 1000);
                Date exp = new Date(expMillis);
                // 设置过期时间
                builder.setExpiration(exp);
            }
            return builder.compact();
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 解密Jwt内容
     *
     * @param jwt
     * @return
     */
    public static Integer parseJwtRS256(String jwt) {
        Claims claims = null;
        try {
            // 读取公钥
            String key = readResourceKey("rsa_public_key.pem");
            // 生成签名公钥
            byte[] keyBytes = (new BASE64Decoder()).decodeBuffer(key);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            claims = Jwts.parser()
                    .setSigningKey(publicKey)
                    .parseClaimsJws(jwt).getBody();
        } catch (Exception e) {
            return null;
        }
        return claims.get("userid", Integer.class);
    }
}