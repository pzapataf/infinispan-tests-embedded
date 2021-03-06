import com.redhat.test.InfinispanLogManager;
import com.redhat.test.Log;
import org.infinispan.query.SearchManager;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TestCacheLogManager {

    @Test
    public void initNode2() throws Exception {
        System.setProperty("app.base.dir", "/tmp/ispn-test/node2");

        InfinispanLogManager logManager = new InfinispanLogManager();

        logManager.init();

        waitAndShowStatus(logManager);
    }

    @Test
    public void initNode3() throws Exception {
        System.setProperty("app.base.dir", "/tmp/ispn-test/node3");

        InfinispanLogManager logManager = new InfinispanLogManager();

        logManager.init();

        waitAndShowStatus(logManager);
    }

    @Test
    public void initNode1() throws Exception {
        System.setProperty("app.base.dir", "/tmp/ispn-test/node1");

        InfinispanLogManager logManager = new InfinispanLogManager();
        logManager.init();

        waitAndShowStatus(logManager);
    }

    @Test
    public void fillNode1() throws Exception {
        System.setProperty("app.base.dir", "/tmp/ispn-test/node1");

        InfinispanLogManager logManager = new InfinispanLogManager();
        logManager.init();

        initCache(logManager);

        logManager.shutdown();
    }


    public void waitAndShowStatus(InfinispanLogManager logManager) throws InterruptedException {
        while (true) {
                   long startMs;

            startMs = System.currentTimeMillis();
            System.out.println("Event count:  " + logManager.getEventCount());
            System.out.println("   Took: " + (System.currentTimeMillis() - startMs) + " ms.");

            System.out.println("All tags:     " + logManager.getAllTags());
            System.out.println("   Took: " + (System.currentTimeMillis() - startMs) + " ms.");
            System.out.println("All channels: " + logManager.getChannelIds());
            System.out.println("   Took: " + (System.currentTimeMillis() - startMs) + " ms.");
            System.out.println("-----------------------------------------------------------------------");

            startMs = System.currentTimeMillis();

            List<Log> logs = logManager.findLastLogs("channel1", "Test", 10, 5);

            System.out.println(" Results : " + logs.size());

            logs.stream().forEach(System.out::println);

            System.out.println("   Took: " + (System.currentTimeMillis() - startMs) + " ms.");
/*            System.out.println("-----------------------------------------------------------------------");

            System.out.println("Getting log count in channel 1 in the last 10 day");

            startMs = System.currentTimeMillis();
            logManager.countLogsByDay("channel1", 10).forEach(
                    (k, v) -> {
                        System.out.println("KEY=" + k + " : " + v);
                    }
            );
            System.out.println("   Took: " + (System.currentTimeMillis() - startMs) + " ms.");
            System.out.println("-----------------------------------------------------------------------");

            System.out.println("Getting log count in channel 1 in the last 24 hour");

            startMs = System.currentTimeMillis();
            logManager.countLogsByHour("channel1", 24).forEach(
                    (k, v) -> {
                        System.out.println("KEY=" + k + " : " + v);
                    }
            );
            System.out.println("   Took: " + (System.currentTimeMillis() - startMs) + " ms.");
            System.out.println("-----------------------------------------------------------------------");
*/
            SearchManager sm = org.infinispan.query.Search.getSearchManager(logManager.getEventsCache());

            System.out.println("************************************************************************");
            System.out.println("Cluster nodes: " + logManager.getEventsCache().getCacheManager().getMembers());
            System.out.println("CACHE STATUS: " + logManager.getEventsCache().getStatus());
            System.out.println("INDEX STATUS: " + sm.getStatistics().indexedEntitiesCount());
            System.out.println("************************************************************************");


            Thread.sleep(2000L);
        }
    }

    private void initCache(InfinispanLogManager logManager) {
        long startMs = System.currentTimeMillis();

        System.out.println("Creating logs...");
        int NDAYS = 12;
        int NLOGS = 100000;

        if(  logManager.getEventCount() < NLOGS) {

            long initialTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * NDAYS;
            long msBetweenLogs = (System.currentTimeMillis() - initialTimestamp) / NLOGS;
            for (int i = 0; i < NLOGS; i++) {
                if (i % 100 == 0) {
                    System.out.println("-> " + i);
                }
                Log l = logManager.createNewLog();
                l.setId(Long.valueOf(i));
                l.setChannel("channel" + (i % 10));
                l.setLogTimestamp(initialTimestamp + msBetweenLogs * i);
                l.setMessage("Test message");
                l.setName("com.redhat.test" + (i % 10));
                switch (i % 4) {
                    case 0:
                        l.setSeverity(Log.Severity.DEBUG);
                        break;
                    case 1:
                        l.setSeverity(Log.Severity.BIZ);
                        break;
                    case 2:
                        l.setSeverity(Log.Severity.ERROR);
                        break;
                    case 3:
                        l.setSeverity(Log.Severity.INFO);
                        break;
                }

                Set<String> tag = new HashSet<>();
                tag.add("tag" + (i % 2));
                tag.add("tag" + (i % 5));
                tag.add("tag" + (i % 9));
                l.setTags(tag);

                logManager.update(l);
            }

            System.out.println("   Took: " + (System.currentTimeMillis() - startMs) + " ms.");
        }
    }
}
