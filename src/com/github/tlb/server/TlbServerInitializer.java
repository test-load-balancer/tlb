package com.github.tlb.server;

import com.github.tlb.TlbConstants;
import com.github.tlb.server.repo.EntryRepoFactory;
import com.github.tlb.utils.SystemEnvironment;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.Protocol;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @understands running the server as a standalone process
 */
public class TlbServerInitializer implements ServerInitializer {
    private final SystemEnvironment env;
    private final Timer timer;

    public Component init() {
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, Integer.parseInt(env.getProperty(TlbConstants.Server.TLB_PORT, "7019")));
        component.getDefaultHost().attach(new TlbApplication(appContext()));
        return component;
    }

    Context appContext() {
        HashMap<String, Object> appMap = new HashMap<String, Object>();
        final EntryRepoFactory repoFactory = repoFactory();

        final int versionLifeInDays = Integer.parseInt(env.getProperty(TlbConstants.Server.VERSION_LIFE_IN_DAYS, "1"));
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                repoFactory.purgeVersionsOlderThan(versionLifeInDays);
            }
        }, 0, 1*24*60*60*1000);

        repoFactory.registerExitHook();
        appMap.put(TlbConstants.Server.REPO_FACTORY, repoFactory);
        Context applicationContext = new Context();
        applicationContext.setAttributes(appMap);
        return applicationContext;
    }

    EntryRepoFactory repoFactory() {
        return new EntryRepoFactory(env);
    }

    public TlbServerInitializer(SystemEnvironment env) {
        this(env, new Timer());
    }

    public TlbServerInitializer(SystemEnvironment env, Timer timer) {
        this.env = env;
        this.timer = timer;
    }
}