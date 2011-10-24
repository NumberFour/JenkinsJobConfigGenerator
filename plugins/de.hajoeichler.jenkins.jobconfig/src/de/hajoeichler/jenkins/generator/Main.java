package de.hajoeichler.jenkins.generator;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.generator.IGenerator;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class Main {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Aborting: no path to EMF resource provided!");
			return;
		}
		Injector injector = new de.hajoeichler.jenkins.JobConfigStandaloneSetupGenerated()
				.createInjectorAndDoEMFRegistration();
		Main main = injector.getInstance(Main.class);
		main.runGenerator(args[0]);
	}

	@Inject
	private Provider<ResourceSet> resourceSetProvider;

	@Inject
	private IResourceValidator validator;

	@Inject
	private IGenerator generator;

	@Inject
	private JavaIoFileSystemAccess fileAccess;

	protected void runGenerator(String directory) {
		// get all the jobConfig 
		File dir = new File(directory);
		File[] jobsFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jobConfig");
			}
		});
		// load the resources
		ResourceSet set = resourceSetProvider.get();
		for (File file : jobsFiles) {
			set.getResource(URI.createURI(file.getAbsolutePath()), true);
		}

		for (Resource resource : set.getResources()) {
			// validate the resource
			List<Issue> list = validator.validate(resource, CheckMode.ALL,
					CancelIndicator.NullImpl);
			if (!list.isEmpty()) {
				for (Issue issue : list) {
					System.err.println(issue);
				}
				return;
			}
			// configure and start the generator
			fileAccess.setOutputPath("target/configs/");
			generator.doGenerate(resource, fileAccess);
		}

		System.out.println("Code generation finished.");
	}
}