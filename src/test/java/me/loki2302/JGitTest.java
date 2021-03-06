package me.loki2302;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JGitTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void dummy() throws IOException, GitAPIException {
        File repositoryDirectory = temporaryFolder.newFolder();

        Git git = Git.init()
                .setDirectory(repositoryDirectory)
                .call();

        writeRepositoryFile(repositoryDirectory, "1.txt", "hello");

        Status status = git.status().call();
        assertEquals(1, status.getUntracked().size());
        assertEquals("1.txt", status.getUntracked().iterator().next());
        assertFalse(status.hasUncommittedChanges());

        git.add()
                .addFilepattern("1.txt")
                .call();

        status = git.status().call();
        assertTrue(status.getUntracked().isEmpty());
        assertEquals(1, status.getAdded().size());
        assertEquals("1.txt", status.getAdded().iterator().next());
        assertTrue(status.hasUncommittedChanges());

        git.commit()
                .setMessage("Initial version")
                .setAuthor("loki2302", "loki2302@loki2302.me")
                .call();

        status = git.status().call();
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getAdded().isEmpty());
        assertFalse(status.hasUncommittedChanges());

        assertThereAreNCommits(git, 1);

        assertThereAreNBranches(git, 1);
        git.checkout().setCreateBranch(true).setName("newbranch").call();

        assertThereAreNBranches(git, 2);
    }

    @Test
    public void canSquashCommits() throws IOException, GitAPIException {
        File repositoryDirectory = temporaryFolder.newFolder();

        Git git = Git.init().setDirectory(repositoryDirectory).call();

        writeRepositoryFile(repositoryDirectory, "1.txt", "initial");
        git.add().addFilepattern("1.txt").call();
        git.commit().setAuthor("loki2302", "loki2302@loki2302.me").setMessage("initial").call();

        writeRepositoryFile(repositoryDirectory, "1.txt", "initial\none");
        git.add().addFilepattern("1.txt").call();
        git.commit().setAuthor("loki2302", "loki2302@loki2302.me").setMessage("one").call();

        writeRepositoryFile(repositoryDirectory, "1.txt", "initial\none\ntwo");
        git.add().addFilepattern("1.txt").call();
        git.commit().setAuthor("loki2302", "loki2302@loki2302.me").setMessage("two").call();

        if(true) {
            Iterable<RevCommit> revCommitsIterable = git.log().call();
            List<RevCommit> revCommits = Lists.newArrayList(revCommitsIterable);
            assertEquals(3, revCommits.size());
            assertEquals("two", revCommits.get(0).getFullMessage());
            assertEquals("one", revCommits.get(1).getFullMessage());
            assertEquals("initial", revCommits.get(2).getFullMessage());
        }

        git.reset().setMode(ResetCommand.ResetType.SOFT).setRef("HEAD~2").call();
        git.commit().setAuthor("loki2302", "loki2302@loki2302.me").setMessage("one and two").call();

        if(true) {
            Iterable<RevCommit> revCommitsIterable = git.log().call();
            List<RevCommit> revCommits = Lists.newArrayList(revCommitsIterable);
            assertEquals(2, revCommits.size());
            assertEquals("one and two", revCommits.get(0).getFullMessage());
            assertEquals("initial", revCommits.get(1).getFullMessage());

            git.checkout().setStartPoint("HEAD").setAllPaths(true).call();
            assertEquals("initial\none\ntwo", readRepositoryFile(repositoryDirectory, "1.txt"));

            git.checkout().setStartPoint("HEAD~1").setAllPaths(true).call();
            assertEquals("initial", readRepositoryFile(repositoryDirectory, "1.txt"));
        }
    }

    private static void assertThereAreNCommits(Git git, int expectedNumberOfCommits) throws GitAPIException {
        Iterable<RevCommit> revCommitsIterable = git.log().call();
        List<RevCommit> revCommits = Lists.newArrayList(revCommitsIterable);
        assertEquals(expectedNumberOfCommits, revCommits.size());
    }

    private static void assertThereAreNBranches(Git git, int expectedNumberOfBranches) throws GitAPIException {
        List<Ref> branches = git.branchList().call();
        assertEquals(expectedNumberOfBranches, branches.size());
    }

    private static void writeRepositoryFile(File repositoryDirectory, String filename, String content) throws IOException {
        FileUtils.writeStringToFile(Paths.get(repositoryDirectory.getAbsolutePath(), filename).toFile(), content);
    }

    private static String readRepositoryFile(File repositoryDirectory, String filename) throws IOException {
        return FileUtils.readFileToString(Paths.get(repositoryDirectory.getAbsolutePath(), filename).toFile());
    }
}
