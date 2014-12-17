package me.loki2302;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.http.SimpleHttpServer;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JGitServerTest {
    private SimpleHttpServer simpleHttpServer;

    @Before
    public void startServer() throws Exception {
        File remoteRepoDirectory = Paths.get("remoteGit").toFile();
        if(remoteRepoDirectory.exists()) {
            FileUtils.deleteDirectory(remoteRepoDirectory);
        }

        // git init --bare
        Git git = Git.init()
                .setDirectory(remoteRepoDirectory)
                .setBare(true)
                .call();

        simpleHttpServer = new SimpleHttpServer(git.getRepository());
        simpleHttpServer.start();
    }

    @After
    public void stopServer() throws Exception {
        simpleHttpServer.stop();
        FileUtils.deleteDirectory(Paths.get("remoteGit").toFile());
    }

    @Test
    public void dummy() throws Exception {
        File localRepoDirectory = Paths.get("localGit").toFile();
        if(localRepoDirectory.exists()) {
            FileUtils.deleteDirectory(localRepoDirectory);
        }

        try {
            // git init
            Git git = Git.init()
                    .setDirectory(localRepoDirectory)
                    .call();

            // echo "hello" > 1.txt
            FileUtils.writeStringToFile(Paths.get("localGit", "1.txt").toFile(), "hello");

            // git add 1.txt
            git.add()
                    .addFilepattern("1.txt")
                    .call();

            // git config user.name "loki2302"
            // git config user.email "loki2302@loki2302.me"
            // git commit -m "Initial version"
            git.commit()
                    .setMessage("Initial version")
                    .setAuthor("loki2302", "loki2302@loki2302.me")
                    .call();

            // git remote add ....
            StoredConfig config = git.getRepository().getConfig();
            config.setString("remote", "origin", "url", simpleHttpServer.getUri().toString());
            config.save();

            // git push origin master
            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("agitter", "letmein"))
                    .call();
        } finally {
            FileUtils.deleteDirectory(localRepoDirectory);
        }

        try {
            // git clone http://localhost:???/....
            Git git = Git.cloneRepository()
                    .setURI(simpleHttpServer.getUri().toString())
                    .setDirectory(localRepoDirectory)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("agitter", "letmein"))
                    .call();

            // git log
            Iterable<RevCommit> revCommitsIterable = git.log().call();
            List<RevCommit> revCommits = Lists.newArrayList(revCommitsIterable);
            assertEquals(1, revCommits.size());
            assertEquals("Initial version", revCommits.get(0).getFullMessage());
        } finally {
            FileUtils.deleteDirectory(localRepoDirectory);
        }
    }
}
