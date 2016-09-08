package com.redhat.test;

import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.SearchManager;
import org.infinispan.stream.CacheCollectors;


import java.util.*;
import java.util.stream.Collectors;

public class InfinispanLogManager {


    protected DefaultCacheManager cacheManager = null;

    /**
     * Cache of logs / events
     */
    protected Cache<Long, Log> eventsCache = null;

    /**
     * Cache to look up logs by day
     */
    protected Cache<String, Set<String>> tagsPerChannelCache = null;


    public void clear() throws Exception {
        eventsCache.clear();
        tagsPerChannelCache.clear();
    }

    public void init() throws Exception {
        cacheManager = new DefaultCacheManager(InfinispanLogManager.class.getResourceAsStream("/infinispan-test-config.xml"));

        eventsCache = cacheManager.getCache("log_events");
        tagsPerChannelCache = cacheManager.getCache("tags_per_channel");
    }

    public void shutdown() {
        if (cacheManager != null) {
            cacheManager.stop();
            cacheManager = null;
        }
    }

    public Cache<Long, Log> getEventsCache() {
        return eventsCache;
    }

    public Cache<String, Set<String>> getTagsPerChannelCache() {
        return tagsPerChannelCache;
    }

    public Log createNewLog() {
        return new Log();
    }

    public boolean update(Log log) {
        getEventsCache().put(log.getId(), log);

        // Update fast lookup tables
        if (log.getChannel() != null && log.getTags() != null) {
            Set<String> tags = getTagsPerChannelCache().get(log.getChannel());
            if (tags == null || !tags.containsAll(log.getTags())) {
                Set<String> newTags = new HashSet<>();
                if (tags != null) {
                    newTags.addAll(tags);
                }
                newTags.addAll(log.getTags());
                getTagsPerChannelCache().put(log.getChannel(), newTags);
            }
        }

        return true;
    }

    public Log findEvent(long id) {
        return getEventsCache().get(id);
    }

    public long getEventCount() {
        return getEventsCache().getAdvancedCache().getStats().getTotalNumberOfEntries();
    }

    public List<Log> findLastLogs(String channel, String text, long fromId, int maxLogs) {

        /*
        // get the DSL query factory from the cache, to be used for constructing the Query object:
        QueryFactory qf = org.infinispan.query.Search.getQueryFactory(getEventsCache());
        Query query = qf.from(Log.class)
                .orderBy("id", SortOrder.ASC)
                .having("channel").eq(channel)
                .and()
                .having("id").gte(fromId)
                .and()
                .having("message").like("%"+text+"%")
                .toBuilder()
                .maxResults(maxLogs)
                .build();

        return query.list();
        */

        // get the search manager from the cache:
        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(getEventsCache());

        QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Log.class).get();

        org.apache.lucene.search.Query luceneQueryRange = queryBuilder.range().onField("id").above(fromId).createQuery();
        org.apache.lucene.search.Query luceneChannel = queryBuilder.phrase().onField("channel").sentence(channel).createQuery();

        org.apache.lucene.search.Query fullQuery = queryBuilder.bool().must(luceneQueryRange).must(luceneChannel).createQuery();

        System.out.println(fullQuery.toString());

        CacheQuery query = searchManager.getQuery(fullQuery, Log.class);

        List res = query.maxResults(maxLogs).list();

        System.out.println(res.size());
        //query.list().stream().forEach(System.out::println);

        return res;
    }

    public Map<Log.Severity, Map<Long, Integer>> countLogsByDay(String channel, int lastDays) {
        ////
        //// startDay_channel -> Map<Severity, Long>
        ////

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(System.currentTimeMillis()));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, -lastDays);

        return countLogsBySeverity(channel, calendar.getTime().getTime());
    }

    protected long getInitialTimeForDay(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(time));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }

    protected long getInitialHourInDay(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(time));
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }

    protected long getEndTimeForDay(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(time));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime().getTime();
    }

    protected long getInitialHour(int hourWindow) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.HOUR, -hourWindow);
        return calendar.getTime().getTime();
    }

    protected long getEndHour() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 59);
        return calendar.getTime().getTime();
    }

    protected Map<Log.Severity, Map<Long, Integer>> countLogsBySeverity(String channel, long startDate) {

        long startTime = getInitialTimeForDay(startDate);
        long endTime = getEndTimeForDay(System.currentTimeMillis());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //
        CacheQuery query = findLogsForChannel(channel, startTime, endTime);

        Object res = query.list().stream().collect(
                Collectors.groupingBy(
                        event -> ((Log) event).getSeverity(),
                        Collectors.groupingBy(event -> getInitialTimeForDay(((Log) event).getLogTimestamp()),
                                Collectors.counting()
                        )
                )
        );

        return (Map<Log.Severity, Map<Long, Integer>>) res;
    }

    public Map<Log.Severity, Map<Long, Integer>> countLogsByHour(String channel, int hourWindow) {
        long startTime = getInitialHour(hourWindow);
        long endTime = getEndHour();

        CacheQuery query = findLogsForChannel(channel, startTime, endTime);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //
        Object res = query.list().stream().collect(
                Collectors.groupingBy(
                        event -> ((Log) event).getSeverity(),
                        Collectors.groupingBy(event -> getInitialHourInDay(((Log) event).getLogTimestamp()),
                                Collectors.counting()
                        )
                )
        );
        return (Map<Log.Severity, Map<Long, Integer>>) res;
    }

    private CacheQuery findLogsForChannel(String channel, long startTime, long endTime) {
        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(getEventsCache());
        QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(Log.class).get();

        org.apache.lucene.search.Query fullQuery = queryBuilder.bool().must(
                queryBuilder.range().onField("logTimestamp").from(startTime).to(endTime).createQuery()
        ).must(
                queryBuilder.phrase().onField("channel").sentence(channel).createQuery()
        ).createQuery();

        return searchManager.getQuery(fullQuery, Log.class);
    }

    public Set<String> getChannelIds() {
//        return getTagsPerChannelCache().keySet().stream().collect(
//                Collectors.toSet()
//        );
        return getTagsPerChannelCache().keySet();
    }

    public Set<String> getAllTags() {
        return
                getTagsPerChannelCache().entrySet().stream().flatMap(
                        e -> e.getValue().stream()
                ).collect(
                        CacheCollectors.serializableCollector(
                                () -> Collectors.toSet()
                        )
                );
    }
}
