package gtcloud.springutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * ����tomcat��һЩ���ò��������������Ƭ���ݵ����紫�����ܡ�
 */
@Configuration
public class TomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

	private static final Logger LOG = LoggerFactory.getLogger(TomcatCustomizer.class);
	
    @Autowired
    private Environment env;

    private static final String[] SOCKET_OPTION_NAMES = new String[] {
        "socket.txBufSize",
        "socket.rxBufSize",
        "socket.appReadBufSize",
        "socket.appWriteBufSize",
    };

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
    	LOG.info("Customize tomcat-embed...");
        String clsName = env.getProperty("gtcloud.tomcat.protocolHandlerClassName");
        if (clsName != null) {
            factory.setProtocol(clsName);
        }
        
        if ("org.apache.coyote.http11.Http11AprProtocol".equals(clsName)) {
        	// SOCKET_OPTION_NAMES�еĸ����ò�����������APR-Connector
        	;
        } else {
	        factory.addConnectorCustomizers(connector -> {
	            for (String name : SOCKET_OPTION_NAMES) {
	                String val = env.getProperty("gtcloud.tomcat." + name);
	                if (val != null) {
	                    connector.setProperty(name, val);
	                }
	            }
	        });
        }
    }
}
