package org.osmorc;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileFilter;

import static org.junit.Assert.assertNotNull;

public abstract class HeavyOsgiFixtureTestCase {
  protected TempDirTestFixture myTempDirFixture;
  protected IdeaProjectTestFixture myFixture;

  public HeavyOsgiFixtureTestCase() {
    IdeaTestCase.initPlatformPrefix();
  }

  @Before
  public void setUp() throws Exception {
    myTempDirFixture = IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture();
    myTempDirFixture.setUp();
    myFixture = JavaTestFixtureFactory.createFixtureBuilder("OSGi Tests").getFixture();
    myFixture.setUp();
    Project project = myFixture.getProject();
    ((ProjectImpl)project).myOptimiseTestLoadSpeed = false;  // load module components
    loadModules(getClass().getSimpleName(), project, myTempDirFixture.getTempDirPath());
  }

  @After
  public void tearDown() throws Exception {
    myFixture.tearDown();
    myTempDirFixture.tearDown();
  }

  private static final FileFilter VISIBLE_DIR_FILTER = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isDirectory() && !pathname.getName().startsWith(".");
    }
  };

  private static void loadModules(String projectName, final Project project, String projectDirPath) throws Exception {
    final File projectDir = OsgiTestUtil.extractProject(projectName, projectDirPath);
    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        VirtualFile virtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir);
        assertNotNull(projectDir.getPath(), virtualDir);
        for (File moduleDir : projectDir.listFiles(VISIBLE_DIR_FILTER)) {
          File moduleFile = new File(moduleDir, moduleDir.getName() + ".iml");
          if (moduleFile.exists()) {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleFile);
            ModuleManager.getInstance(project).loadModule(moduleFile.getPath());
          }
        }
      }
    }.execute();
  }
}
