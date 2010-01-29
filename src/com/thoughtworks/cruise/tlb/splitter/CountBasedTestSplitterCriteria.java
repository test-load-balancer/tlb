package com.thoughtworks.cruise.tlb.splitter;

import com.thoughtworks.cruise.tlb.service.TalkToCruise;
import com.thoughtworks.cruise.tlb.utils.SystemEnvironment;
import com.thoughtworks.cruise.tlb.TlbConstants;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.tools.ant.types.resources.FileResource;

/**
 * @understands the criteria for splitting tests based on the number of tests
 */
public class CountBasedTestSplitterCriteria implements TestSplitterCriteria {
    private final TalkToCruise talkToCruise;
    private final SystemEnvironment env;
    private static final String INT = "\\d+";
    private static final Pattern NUMBER_BASED_LOAD_BALANCED_JOB = Pattern.compile("(.*?)-(" + INT + ")");
    private static final String HEX = "[a-fA-F0-9]";
    private static final String UUID = HEX + "{8}-" + HEX + "{4}-" + HEX + "{4}-" + HEX + "{4}-" + HEX + "{12}";
    private static final Pattern UUID_BASED_LOAD_BALANCED_JOB = Pattern.compile("(.*?)-(" + UUID + ")");

    public CountBasedTestSplitterCriteria(TalkToCruise talkToCruise, SystemEnvironment env) {
        this.talkToCruise = talkToCruise;
        this.env = env;
    }

    /**
     * This method needs to split based on the job that is being executed. That means the index of the job is to be used
     * like an iterator index, but in a distributed fashion. The solution is as follows:
     * <p/>
     * Eg: 37 tests split across 7 jobs. The output is 5 (2/7), 5 (4/7), 5 (6/7), 6 (8/7), 5 (3/7), 5 (5/7), 6 (7/7)
     * where each of (2/7) is basically the rate at which we carry over the balance before we account for it.
     *
     * @param files
     * @return filtered list
     */
    public List<FileResource> filter(List<FileResource> files) {
        List<String> jobs = jobsInTheSameFamily(talkToCruise.getJobs());
        if (jobs.isEmpty()) {
            return files;
        }
        Collections.sort(jobs);

        int index = jobs.indexOf(jobName());
        int splitRatio = files.size() / jobs.size();
        int reminder = files.size() % jobs.size();

        double balance = (double) (reminder * (index + 1)) / jobs.size();
        double lastBalance = (double) (reminder * index) / jobs.size();
        int startIndex = isFirst(index) ? 0 : index * splitRatio + (int) Math.floor(Math.abs(lastBalance));
        int endIndex = isLast(jobs, index) ? files.size() : (index + 1) * splitRatio + (int) Math.floor(balance);

        return files.subList(startIndex, endIndex);
    }

    private List<String> jobsInTheSameFamily(List<String> jobs) {
        List<String> family = new ArrayList<String>();
        Pattern pattern = getMatcher();
        for (String job : jobs) {
            if (pattern.matcher(job).matches()) {
                family.add(job);
            }
        }
        return family;
    }

    private Pattern getMatcher() {
        return Pattern.compile(String.format("^%s-(" + INT + "|" + UUID + ")$", jobBaseName()));
    }

    private String jobBaseName() {
        Matcher matcher = NUMBER_BASED_LOAD_BALANCED_JOB.matcher(jobName());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        matcher = UUID_BASED_LOAD_BALANCED_JOB.matcher(jobName());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return jobName();
    }

    private boolean isLast(List<String> jobs, int index) {
        return (index + 1) == jobs.size();
    }

    private boolean isFirst(int index) {
        return (index == 0);
    }

    private String jobName() {
        return env.getProperty(TlbConstants.CRUISE_JOB_NAME);
    }

    private String stageName() {
        return env.getProperty(TlbConstants.CRUISE_STAGE_NAME);
    }
}
