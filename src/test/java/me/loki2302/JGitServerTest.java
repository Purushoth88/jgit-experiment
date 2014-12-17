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
        File repositoryDirectory = Paths.get("remoteGit").toFile();
        if(repositoryDirectory.exists()) {
            FileUtils.deleteDirectory(repositoryDirectory);
        }

        Git git = Git.init()
                .setDirectory(repositoryDirectory)
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
        File repositoryDirectory = Paths.get("localGit").toFile();
        if(repositoryDirectory.exists()) {
            FileUtils.deleteDirectory(repositoryDirectory);
        }

        try {
            Git git = Git.init()
                    .setDirectory(repositoryDirectory)
                    .call();

            FileUtils.writeStringToFile(Paths.get("localGit", "1.txt").toFile(), "hello");

            git.add()
                    .addFilepattern("1.txt")
                    .call();

            git.commit()
                    .setMessage("Initial version")
                    .setAuthor("loki2302", "loki2302@loki2302.me")
                    .call();

            StoredConfig config = git.getRepository().getConfig();
            config.setString("remote", "origin", "url", simpleHttpServer.getUri().toString());
            config.save();

            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("agitter", "letmein"))
                    .call();
        } finally {
            FileUtils.deleteDirectory(repositoryDirectory);
        }

        Git git = Git.cloneRepository()
                .setURI(simpleHttpServer.getUri().toString())
                .setDirectory(repositoryDirectory)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("agitter", "letmein"))
                .call();

        Iterable<RevCommit> revCommitsIterable = git.log().call();
        List<RevCommit> revCommits = Lists.newArrayList(revCommitsIterable);
        assertEquals(1, revCommits.size());
        assertEquals("Initial version", revCommits.get(0).getFullMessage());
    }
}
