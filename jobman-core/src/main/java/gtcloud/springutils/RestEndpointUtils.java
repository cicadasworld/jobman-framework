package gtcloud.springutils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

public class RestEndpointUtils {
    /**
     * 获取具有被给定注记类型修饰的REST端点。
     * @param appCtx
     * @param annotationClass
     * @return REST端点列表
     */
    public static <T extends Annotation> HashSet<String> getRestEndpoints(
            ConfigurableApplicationContext appCtx,
            Class<T> annotationClass) {
        //
        // 遍历带@Controller注记的bean，再遍历bean的各个方法
        //
    	HashSet<String> result = new HashSet<>();
        Map<String, Object> beans = appCtx.getBeansWithAnnotation(Controller.class);
        for (Entry<String, Object> e : beans.entrySet()) {
            Class<?> cl = e.getValue().getClass();
            for (Method m : cl.getDeclaredMethods()) {
                if (m.isAnnotationPresent(annotationClass)) {
                    getRestEndpoints(m, annotationClass, result);
                }
            }
        }
        return result;
    }

    private static <T extends Annotation> void getRestEndpoints(
            Method m, Class<T> annotationClass, HashSet<String> result) {
        // try @RequestMapping
        RequestMapping a1 = m.getAnnotation(RequestMapping.class);
        if (a1 != null) {
            String[] vec = a1.value();
            fillRestEndpoints(vec, result);
            return;
        }

        // try @GetMapping
        GetMapping a2 = m.getAnnotation(GetMapping.class);
        if (a2 != null) {
            String[] vec = a2.value();
            fillRestEndpoints(vec, result);
            return;
        }

        // try @PostMapping
        PostMapping a3 = m.getAnnotation(PostMapping.class);
        if (a3 != null) {
            String[] vec = a3.value();
            fillRestEndpoints(vec, result);
            return;
        }

        // try @PutMapping
        PutMapping a4 = m.getAnnotation(PutMapping.class);
        if (a4 != null) {
            String[] vec = a4.value();
            fillRestEndpoints(vec, result);
            return;
        }

        // try @DeleteMapping
        DeleteMapping a5 = m.getAnnotation(DeleteMapping.class);
        if (a5 != null) {
            String[] vec = a5.value();
            fillRestEndpoints(vec, result);
            return;
        }
    }

    private static void fillRestEndpoints(String[] endpoints, HashSet<String> result) {
        if (endpoints != null && endpoints.length > 0) {
	        for (String endpoint : endpoints) {
	            result.add(endpoint);
	        }
        }
    }

}
