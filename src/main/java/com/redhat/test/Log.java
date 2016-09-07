package com.redhat.test;

import org.hibernate.search.annotations.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Indexed
public class Log implements Serializable {
    /**
     * Enumeration for log severity
     */
    public static enum Severity {
        DEBUG(1),
        INFO(2),
        ERROR(3),
        BIZ(10);

        private int order;

        Severity(int order) {
            this.order=order;
        }

        public int getOrder() {
            return order;
        }
    }

    @Field(analyze = Analyze.NO, store=Store.NO)
    @NumericField
    private Long id = null;

    @Field(analyze = Analyze.NO, store=Store.NO)
    @NumericField
    private long logTimestamp = 0;

    @Field(analyze = Analyze.NO, store=Store.NO)
    private String name;

    @Field(analyze = Analyze.NO, store=Store.NO)
    private String message;

    @Field(analyze = Analyze.NO, store=Store.NO)
    private String channel;

    @Field(analyze = Analyze.NO, store=Store.NO)
    private Severity severity;

    @IndexedEmbedded()
    private Set<String> tags = new HashSet<String>();

    public Log() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getLogTimestamp() {
        return logTimestamp;
    }

    public void setLogTimestamp(long logTimestamp) {
        this.logTimestamp = logTimestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Log{" +
                "id=" + id +
                ", logTimestamp=" + logTimestamp +
                ", name='" + name + '\'' +
                ", message='" + message + '\'' +
                ", channel='" + channel + '\'' +
                ", tags=" + tags +
                '}';
    }
}
