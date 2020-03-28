import com.renwei.dubbo.demo.DemoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring.xml")
public class DemoConsumerTest {

    @Autowired
    private DemoService demoService;

    @Test
    public void test(){
        String res = demoService.sayHelllo("renwei");
        System.out.println(res);
    }
}
