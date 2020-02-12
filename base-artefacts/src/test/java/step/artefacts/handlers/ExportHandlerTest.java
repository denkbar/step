package step.artefacts.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

import com.google.common.io.Files;

import junit.framework.Assert;
import step.artefacts.CheckArtefact;
import step.artefacts.Export;
import step.artefacts.Sequence;
import step.commons.helpers.FileHelper;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;

public class ExportHandlerTest {

	@Test
	public void test() throws IOException {
		File file = getTestFolder();
		
		Export e = new Export();
		e.setValue(new DynamicValue<>("report.attachments", "groovy"));
		e.getFile().setValue(file.getAbsolutePath());
		buildAndRunPlan(e);
		
		File exceptionLogFile = new File(file.getAbsolutePath()+"/exception.log");
		String firstLine = Files.readFirstLine(exceptionLogFile, Charset.defaultCharset());
		Assert.assertEquals("java.lang.RuntimeException", firstLine);
		exceptionLogFile.delete();
		
	}
	
	@Test
	public void testPrefix() throws IOException {
		File file = getTestFolder();
		
		Export e = new Export();
		e.setPrefix(new DynamicValue<String>("MyPrefix_"));
		e.setValue(new DynamicValue<>("report.attachments", "groovy"));
		e.getFile().setValue(file.getAbsolutePath());
		buildAndRunPlan(e);
		
		File exceptionLogFile = new File(file.getAbsolutePath()+"/MyPrefix_exception.log");
		String firstLine = Files.readFirstLine(exceptionLogFile, Charset.defaultCharset());
		Assert.assertEquals("java.lang.RuntimeException", firstLine);
		exceptionLogFile.delete();
		
	}
	
	@Test
	public void testFilter() throws IOException {
		File file = getTestFolder();
		
		Export e = new Export();
		e.setPrefix(new DynamicValue<String>("MyPrefix2_"));
		e.setValue(new DynamicValue<>("report.attachments", "groovy"));
		e.getFile().setValue(file.getAbsolutePath());
		e.setFilter(new DynamicValue<String>("notmatching"));

		buildAndRunPlan(e);
		
		File exceptionLogFile = new File(file.getAbsolutePath()+"/MyPrefix2_exception.log");
		Assert.assertFalse(exceptionLogFile.exists());
	}
	
	protected File getTestFolder() {
		File file = FileHelper.getClassLoaderResource(this.getClass(), "exportTest/test");
		file = file.getParentFile();
		return file;
	}
	
	protected void buildAndRunPlan(Export e) throws IOException {
		Sequence s = new Sequence();
		s.getContinueOnError().setValue(true);
		Plan plan = PlanBuilder.create().startBlock(s).add(new CheckArtefact(c->{
			throw new RuntimeException();
		})).add(e).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		runner.run(plan).printTree();
	}
}