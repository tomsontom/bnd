package biz.aQute.resolve;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.lib.converter.Converter;

public class DebugReporter {
	PrintStream						out;
	private AbstractResolveContext	context;
	private int						level;
	static FilterParser				fp	= new FilterParser();

	DebugReporter(PrintStream out, AbstractResolveContext context, int level) {
		this.out = out;
		this.context = context;
		this.level = level;
	}

	void report() {
		doRepos();
		doBlackList();
		doSystemResource();
		doAllMembers();
	}

	private void doAllMembers() {
		header("RESOURCES");

		Requirement r = CapReqBuilder.createBundleRequirement("*", null).buildSyntheticRequirement();

		List<Capability> providers = context.findProviders(r);
		Set<Resource> resources = ResourceUtils.getResources(providers);
		for (Resource resource : resources) {
			resource(resource);
		}

		resource(context.getSystemResource());
		nl();
	}

	private void doSystemResource() {
		header("SYSTEM RESOURCE");
		resource(context.getSystemResource());
		nl();
	}

	private void doRepos() {
		header("OSGi REPOSITORIES");
		for (Repository repo : context.getRepositories()) {
			out.printf("%s%n", repo.toString());
		}
		nl();
	}

	private void doBlackList() {
		header("BLACKLISTED RESOURCES");
		for (Resource r : context.getBlackList()) {
			resource(r);
		}
		nl();
	}

	private void nl() {
		out.println();
	}

	private void resource(Resource r) {
		IdentityCapability id = ResourceUtils.getIdentityCapability(r);
		String resolveError = null;
		if (!context.isSystemResource(r) && level >= 3 && id != null)
			try {
				String v = id.version() == null ? null : id.version().toString();
				Requirement req = CapReqBuilder.createBundleRequirement(id.osgi_identity(), v)
						.buildSyntheticRequirement();

				context.setInputRequirements(req);
				Resolver resolver = new BndResolver(new ResolverLogger(4));
				Map<Resource,List<Wire>> resolved = resolver.resolve(context);
			} catch (Exception e) {
				resolveError = e.toString();
			}

		String s = resolveError == null ? " " : "!";
		if (id == null)
			out.printf("%s %s%n", s, r);
		else
			out.printf("%s %-50s %-20s %s%n", s, id.osgi_identity(), noNull(id.version()), id.description(""));

		if (level >= 2) {
			for (Capability c : r.getCapabilities(null)) {
				capability("   ", c);
			}
			for (Requirement rq : r.getRequirements(null)) {
				requirement("  ", rq);
			}
		}

	}

	private void capability(String prefix, Capability c) {
		Map<String,Object> attributes = new HashMap<>(c.getAttributes());
		String name = (String) attributes.remove(c.getNamespace());
		String ns = c.getNamespace();
		switch (c.getNamespace()) {
			case IdentityNamespace.IDENTITY_NAMESPACE :
				ns = "ID";
				break;
			case PackageNamespace.PACKAGE_NAMESPACE :
				ns = "E-P";
				break;
			case BundleNamespace.BUNDLE_NAMESPACE :
				ns = "R-B";
				break;
			case ContentNamespace.CONTENT_NAMESPACE :
				ns = "Content";
				break;
			case ServiceNamespace.SERVICE_NAMESPACE :
				ns = "Service";
				try {
					name = Converter.cnv(String.class, attributes.remove(Constants.OBJECTCLASS));
				} catch (Exception e) {
					name = attributes.remove(Constants.OBJECTCLASS) + "";
				}
				break;
		}
		out.printf("%sc: %-16s %-60s %s || %s%n", prefix, ns, name, attributes, c.getDirectives());
	}

	private void requirement(String prefix, Requirement c) {
		Map<String,String> directives = new HashMap<String,String>(c.getDirectives());
		String namespace = c.getNamespace();
		String filter = directives.remove("filter");
		if (filter != null) {
			filter = fp.parse(filter).toString();
			out.printf("%sr: %-20s %s || %s%n", prefix, filter, c.getAttributes(), c.getDirectives());
		} else {
			out.printf("%sr: %-20s %s || %s%n", prefix, namespace, c.getAttributes(), directives);
		}
	}

	private String noNull(Object v) {
		if (v != null)
			return v.toString();
		return "";
	}

	public void header(String name) {
		hr();
		out.printf("%s%n", name);
		hr();
		out.println();
	}

	public void hr() {
		out.println("-------------------------------------------");
	}
}
