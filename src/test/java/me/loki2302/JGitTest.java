package me.loki2302;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JGitTest {
    @Test
    public void dummy() throws IOException, GitAPIException {
        File repositoryDirectory = Paths.get("copy").toFile();
        if(repositoryDirectory.exists()) {
            FileUtils.deleteDirectory(repositoryDirectory);
        }

        try {
            Git git = Git.init()
                    .setDirectory(repositoryDirectory)
                    .call();

            FileUtils.writeStringToFile(Paths.get("copy", "1.txt").toFile(), "hello");

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
        } finally {
            FileUtils.deleteDirectory(repositoryDirectory);
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
}
