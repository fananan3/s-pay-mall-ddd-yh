package cn.bugstack.config;


import cn.bugstack.infrastructure.gateway.IGroupBuyMarketService;
import cn.bugstack.infrastructure.gateway.IWeixinApiService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;


/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/26 16:02
 */

@Slf4j
@Configuration
public class Retrofit2Config {



    @Value("${app.config.group-buy-market.api-url}")
    private String groupBuyMarketApiUrl;

    /**
     * 容错的 ObjectMapper：上游（拼团系统、微信接口）加字段时不能把支付商城打挂。
     * 背景：拼团系统给 LockMarketPayOrderResponseDTO 新增 teamId 后，默认 Jackson 会抛
     * UnrecognizedPropertyException，导致「营销锁单失败」并降级成原价单。
     */
    private ObjectMapper tolerantObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    public IWeixinApiService weixinApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weixin.qq.com/")
                .addConverterFactory(JacksonConverterFactory.create(tolerantObjectMapper())).build();
        return retrofit.create(IWeixinApiService.class);
    }

    @Bean
    public IGroupBuyMarketService groupBuyMarketService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(groupBuyMarketApiUrl)
                .addConverterFactory(JacksonConverterFactory.create(tolerantObjectMapper())).build();
        return retrofit.create(IGroupBuyMarketService.class);
    }

}
