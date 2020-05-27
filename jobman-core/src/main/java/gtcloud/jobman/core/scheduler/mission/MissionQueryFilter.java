package gtcloud.jobman.core.scheduler.mission;

import java.util.Date;

public class MissionQueryFilter {
	
    private Date from;

    private Date to;

    private String host;

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
