package fisco.rpc;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.RateLimiter;

/**
 * Hello world!
 *
 */
public class App 
{   
    private static AtomicInteger sended = new AtomicInteger(0);
    private static Long totalTime = 0L;
    
    public static void main(String[] args) throws Exception {
        
        
        System.out.println("开始测试...");
        System.out.println("===================================================================");
        
        //解析参数
        if(args.length != 2)
        {
            System.out.println("运行参数有误，请输入./run.sh count qps,例如：./run.sh 1000 100");
            System.exit(0);
        }
        final Integer count = Integer.parseInt(args[0]);
        Integer qps = Integer.parseInt(args[1]);
        
        System.out.println("开始压测，总交易量：" + count);
        
        RateLimiter limiter = RateLimiter.create(qps);
        CountDownLatch cdl = new CountDownLatch(count);
        
        Integer area = count / 10;
        
        Long startTime = System.currentTimeMillis();
        for (Integer i = 0; i < count; ++i) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    limiter.acquire();
                    try {
                        executeRPC();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int current = sended.incrementAndGet();
                    if (current >= area && ((current % area) == 0)) {
                        System.out.println("已发送: " + current + "/" + count +  " 交易");
                    }
                    cdl.countDown(); 
                }
            });
            t.start();
        }
        cdl.await();
        Long endTime = System.currentTimeMillis();
        totalTime = endTime - startTime;
        System.out.println("全部交易已发送: " + count);
        System.out.println("全部交易执行时间: " + totalTime / 1000);
        System.out.println("TPS: " + (double) count / ((double) totalTime / 1000));
        System.out.println("测试完成！");
        
    }
    
    
    private static void executeRPC() throws IOException, ClientProtocolException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://127.0.0.1:30302/");
        
        JSONObject jsonParam = new JSONObject(true);
        jsonParam.put("id", 1);
        jsonParam.put("jsonrpc", "2.0");
        jsonParam.put("method", "blockNumber");
        int[] data = {0};
        jsonParam.put("params", data);
        
        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        
        CloseableHttpResponse httpResponse;
        httpResponse = httpClient.execute(httpPost);      
        HttpEntity entityResponse = httpResponse.getEntity();
        String response = EntityUtils.toString(entityResponse, "utf-8");
//        System.out.println(response);
        EntityUtils.consume(entityResponse);
        httpResponse.close();  
        httpPost.releaseConnection();
    }
}
